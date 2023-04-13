package util;

import model.Ticket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Proportion {


    public static double computeProportionValue(@NotNull ArrayList<Ticket> consistentTickets) {

        double proportionSum = 0;
        int validatedCount = 0;

        ArrayList<Ticket> tickets = new ArrayList<>();

        for(Ticket ticket: consistentTickets) {
            //P = (FV-IV)/(FV-OV)
            if(ticket.getInjectedRelease() == null && ticket.getOpeningRelease() == null && ticket.getFixedRelease() == null)
                throw new RuntimeException(); //create an exception for inconsistency ticket list: there is an inconsistent ticket in the consistent list
            int IV = ticket.getInjectedRelease().getIndex();
            int OV = ticket.getOpeningRelease().getIndex();
            int FV = ticket.getFixedRelease().getIndex();
            if(FV!=OV && OV!=IV) {
                double prop = (1.0) * (FV - IV) / (FV - OV);
                proportionSum = proportionSum + prop;
                validatedCount++;
                tickets.add(ticket);
            }
        }

        System.out.println("Consistent tickets: " + consistentTickets.size() + " Tickets used for proportion: " + validatedCount);
        TicketUtils.printTickets(tickets);
        return proportionSum/validatedCount;
    }
}
