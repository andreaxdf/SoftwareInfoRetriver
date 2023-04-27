package utils;

import enums.ProjectsEnum;
import model.Ticket;
import retrivers.TicketRetriever;

import java.util.ArrayList;
import java.util.List;

public class ColdStart {

    private ColdStart() {}

    /**When the number of valid tickets used for compute the proportion value are less than 5, use the tickets of other
    * project.*/
    public static List<Ticket> getTicketForColdStart(ProjectsEnum project) {
        TicketRetriever retriever = new TicketRetriever(project.toString(), true);

        return new ArrayList<>(retriever.getTickets());
    }
}
