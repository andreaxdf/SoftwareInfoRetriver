package main;

import model.Ticket;
import retrivers.CommitRetriever;
import retrivers.TicketRetriever;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        TicketRetriever bookkeeperRetriever = new TicketRetriever("BOOKKEEPER");

        //TicketRetriever syncopeRetriever = new TicketRetriever("SYNCOPE");

        List<Ticket> bookTickets = bookkeeperRetriever.getTickets();

        CommitRetriever commitRetriever = new CommitRetriever("/home/andrea/Documenti/GitRepositories/bookkeeper");

        //commitRetriever.retrieveChangesFromTickets(bookTickets);
    }

}
