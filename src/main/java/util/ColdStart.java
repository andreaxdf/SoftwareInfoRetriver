package util;

import enums.ProjectsEnum;
import model.Ticket;
import retrivers.TicketRetriever;

import java.util.ArrayList;

public class ColdStart {

    /**When the number of valid tickets used for compute the proportion value are less than 5, use the tickets of other
    * project.*/
    public static ArrayList<Ticket> coldStart() {
        ArrayList<Ticket> consistentTickets = new ArrayList<>();
        for(ProjectsEnum project: ProjectsEnum.values()) {
            TicketRetriever retriever = new TicketRetriever(project.toString(), true);
            consistentTickets.addAll(retriever.getTickets());
        }

        return consistentTickets;
    }
}
