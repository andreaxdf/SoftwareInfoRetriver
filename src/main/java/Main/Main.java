package Main;

import model.Ticket;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import retrivers.CommitRetriever;
import retrivers.TicketRetriever;
import util.GitUtils;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER");

        TicketRetriever syncopeRetriever = new TicketRetriever("SYNCOPE");

        ArrayList<Ticket> bookTickets = bookkeeperRetriever.getTickets();

        //CommitRetriever bookCommitRetriever = new CommitRetriever("/home/andrea/Documenti/bookkeeper");

        //associateTicketAndCommit(bookCommitRetriever, bookTickets);



    }

    private static void associateTicketAndCommit(CommitRetriever bookCommitRetriever, ArrayList<Ticket> bookTickets) {
        try {
            ArrayList<RevCommit> commits = bookCommitRetriever.retrieveCommit();
            for (Ticket ticket : bookTickets) {
                ArrayList<RevCommit> associatedCommits = bookCommitRetriever.retrieveAssociatedCommit(commits, ticket);
                ticket.setAssociatedCommits(associatedCommits);
                //GitUtils.printCommit(associatedCommits);
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

}
