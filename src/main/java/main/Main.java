package main;

import model.ReleaseCommits;
import model.Ticket;
import org.eclipse.jgit.api.errors.GitAPIException;
import retrivers.CommitRetriever;
import retrivers.TicketRetriever;
import util.TicketUtils;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER");

        //TicketRetriever syncopeRetriever = new TicketRetriever("SYNCOPE");

        List<Ticket> bookTickets = bookkeeperRetriever.getTickets();

        CommitRetriever bookCommitRetriever = bookkeeperRetriever.getCommitRetriever();

        //commitRetriever.retrieveChangesFromTickets(bookTickets);

        try {
            List<ReleaseCommits> releaseCommitsList = bookCommitRetriever.getReleaseCommits(bookkeeperRetriever.getVersionRetriever(), TicketUtils.getAssociatedCommit(bookTickets));
            printReleaseCommit(releaseCommitsList);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void printReleaseCommit(List<ReleaseCommits> releaseCommitsList) {
        for(ReleaseCommits rc: releaseCommitsList) {
            System.out.println(rc.getJavaClasses().keySet());
        }
    }

}
