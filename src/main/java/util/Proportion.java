package util;

import model.Ticket;

import java.util.ArrayList;

public class Proportion {


    public static double computeProportionValue(ArrayList<Ticket> consistentTickets) {

        double proportionSum = 0;
        int valiatedCount = 0;

        ArrayList<Ticket> tickets = new ArrayList<>();

        for(Ticket ticket: consistentTickets) {
            //P = (FV-IV)/(FV-OV)
            if(ticket.getInjectedRelease() == null && ticket.getOpeningRelease() == null && ticket.getFixedRelease() == null)
                throw new RuntimeException(); //create an exception for inconsistency ticket list
            int IV = ticket.getInjectedRelease().getIndex();
            int OV = ticket.getOpeningRelease().getIndex();
            int FV = ticket.getFixedRelease().getIndex();
            if(FV!=OV && OV!=IV) {
                double prop = (1.0) * (FV - IV) / (FV - OV);
                proportionSum = proportionSum + prop;
                valiatedCount++;
                tickets.add(ticket);
            }
        }

        System.out.println("Consistent tickets: " + consistentTickets.size() + " Tickets used for proportion: " + valiatedCount);
        TicketUtils.printTickets(tickets);
        return proportionSum/valiatedCount;
    }
}
