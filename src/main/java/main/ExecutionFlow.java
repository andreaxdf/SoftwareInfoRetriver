package main;

import model.ReleaseCommits;
import model.Ticket;
import org.eclipse.jgit.api.errors.GitAPIException;
import retrivers.CommitRetriever;
import retrivers.TicketRetriever;

import java.io.IOException;
import java.util.List;

public class ExecutionFlow {

    public ExecutionFlow(String projName) {
        TicketRetriever ticketRetriever = new TicketRetriever(projName);

        List<Ticket> tickets = ticketRetriever.getTickets();

        CommitRetriever commitRetriever = ticketRetriever.getCommitRetriever();

        try {
            List<ReleaseCommits> releaseCommitsList = commitRetriever.getReleaseCommits(ticketRetriever.getVersionRetriever(), commitRetriever.retrieveCommit());
            printReleaseCommit(releaseCommitsList);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printReleaseCommit(List<ReleaseCommits> releaseCommitsList) {
        for(ReleaseCommits rc: releaseCommitsList) {
            System.out.println(rc.getRelease().getName() + " -> " + rc.getCommits().size());
        }
    }
}
