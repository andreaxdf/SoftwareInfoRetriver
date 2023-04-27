package main;

import enums.CsvNamesEnum;
import model.ReleaseCommits;
import model.Ticket;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import retrivers.CommitRetriever;
import retrivers.MetricsRetriever;
import retrivers.TicketRetriever;
import view.FileCreator;

import java.io.IOException;
import java.util.List;

public class ExecutionFlow {

    private ExecutionFlow() {}

    public static void collectData(String projName) {
        TicketRetriever ticketRetriever = new TicketRetriever(projName);

        List<Ticket> tickets = ticketRetriever.getTickets();

        CommitRetriever commitRetriever = ticketRetriever.getCommitRetriever();

        try {
            System.out.println("\n" + projName + " - NUMERO DI COMMIT: " + commitRetriever.retrieveCommit().size() + "\n");
            List<ReleaseCommits> releaseCommitsList = commitRetriever.getReleaseCommits(ticketRetriever.getVersionRetriever(), commitRetriever.retrieveCommit());
            MetricsRetriever.computeBuggynessAndFixedDefects(releaseCommitsList, tickets, commitRetriever, ticketRetriever.getVersionRetriever());
            MetricsRetriever.computeMetrics(releaseCommitsList, tickets, commitRetriever, ticketRetriever.getVersionRetriever());
            FileCreator.writeOnCsv(projName, releaseCommitsList, CsvNamesEnum.BUGGY, 0);
            for(ReleaseCommits releaseCommits: releaseCommitsList) {

            }
            printReleaseCommit(projName, releaseCommitsList);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printReleaseCommit(String projName, @NotNull List<ReleaseCommits> releaseCommitsList) {
        for(ReleaseCommits rc: releaseCommitsList) {
            System.out.println(projName + " version: " + rc.getRelease().getName() + ";" +
                    " Commits: " + rc.getCommits().size() + ";" +
                    " Java classes: " + rc.getJavaClasses().size() + ";" +
                    " Buggy classes: " + rc.getBuggyClasses());
        }
    }
}