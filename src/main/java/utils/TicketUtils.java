package utils;

import model.Ticket;

import java.util.ArrayList;
import java.util.List;

public class TicketUtils {

    private TicketUtils() {}

    /**
     * Select the tickets with opening version <= versionBound
     * @param tickets List of all tickets
     * @param versionBound Indicate the upperbound for the release to use
     * @return List of tickets that are opened in a release earlier or egual than versionBound.
     */
    public static List<Ticket> getTicketsUntilRelease(List<Ticket> tickets, int versionBound) {
        List<Ticket> ticketList = new ArrayList<>();
        for(Ticket ticket: tickets) {
            if(ticket.getOpeningRelease().getIndex() <= versionBound) {
                ticketList.add(ticket);
            }
        }

        return ticketList;
    }

}
