package util;

import model.Ticket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Proportion {

    private Proportion() {}

    public static double computeProportionValue(@NotNull List<Ticket> consistentTickets) {

        double proportionSum = 0;
        int validatedCount = 0;

        ArrayList<Ticket> tickets = new ArrayList<>();

        for(Ticket ticket: consistentTickets) {
            //P = (FV-IV)/(FV-OV)
            if(ticket.getInjectedRelease() == null && ticket.getOpeningRelease() == null && ticket.getFixedRelease() == null)
                throw new RuntimeException(); //create an exception for inconsistency ticket list: there is an inconsistent ticket in the consistent list
            int iv = ticket.getInjectedRelease().getIndex();
            int ov = ticket.getOpeningRelease().getIndex();
            int fv = ticket.getFixedRelease().getIndex();
            if(fv!=ov && ov!=iv) {
                double prop = (1.0) * (fv - iv) / (fv - ov);
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
