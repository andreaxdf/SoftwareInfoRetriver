package Main;

import model.Ticket;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import retrivers.CommitRetriever;
import retrivers.TicketRetriever;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        String issueType = "Bug";
        String status = "closed";
        String resolution = "fixed";

        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER", issueType, status, resolution);

        TicketRetriever syncopeRetriever = new TicketRetriever("SYNCOPE", issueType, status, resolution);

        ArrayList<Ticket> bookTickets = bookkeeperRetriever.getTickets();

        CommitRetriever bookCommitRetriever = new CommitRetriever("/home/andrea/Documenti/bookkeeper");

        associateTicketAndCommit(bookCommitRetriever, bookTickets);

    }

    private static void associateTicketAndCommit(CommitRetriever bookCommitRetriever, ArrayList<Ticket> bookTickets) {
        try {
            for (Ticket ticket : bookTickets) {
                ArrayList<RevCommit> commits = bookCommitRetriever.retrieveCommit(ticket);
                ticket.setAssociatedCommits(commits);
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

}
