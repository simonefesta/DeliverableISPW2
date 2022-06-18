package org.festinho.part1Retrieve;

import org.festinho.entities.Ticket;

import java.util.ArrayList;
import java.util.List;

public class Proportion {

private static int percentageMovingWindows;


    public static void proportion(List<Ticket> ticketList){

        List<Ticket> ticketListTrivialIV = new ArrayList<>();         //P = (FV-IV)/(FV-OV), se FV=OV -> FV = IV, sennò ho errore su proportion. Ci metto ticket con IV trivial (banale).
        for (Ticket ticket : ticketList) {
            if (ticket.getOV().equals(ticket.getFV()) && ticket.getIV() == 0) {
                ticket.setIV(ticket.getFV());
                ticketListTrivialIV.add(ticket);
            }
        }
        int numTickets = ticketList.size();                                 //sorted by date
        percentageMovingWindows = numTickets/100;                          //ticket di cui calcolare P con moving windows ( = 1%)
        List<Ticket> listTicketProportion = new ArrayList<>();             //ci metto un numero di ticket pari a 'perc'

        for (Ticket ticket: ticketList){
            if (!ticketListTrivialIV.contains(ticket)){       //escludo ticket con 'IV banale', ovvero OV=FV=IV

                if (ticket.getIV()!= 0){                // In movingWindows ci metto tickets CON IV DEFINITO.
                    addTicket2MovingWindowsIVDefined(listTicketProportion,ticket);
                }
                else{ //rimangono ticket con IV da calcolare.
                    setIvUsingProportion(listTicketProportion,ticket); //Se ticket non ha IV, uso MovingWindows per calcolarlo, vedendo i P dei ticket precedenti.
                }
            }
        }

    }

    public static void addTicket2MovingWindowsIVDefined(List<Ticket> listTicketProportion, Ticket ticket)
    {
        if (listTicketProportion.size() < percentageMovingWindows) listTicketProportion.add(ticket); //c'è ancora spazio per aggiungere ticket
        else
            {
                listTicketProportion.remove(0);   //rimuovo il più vecchio.
                listTicketProportion.add(ticket); //faccio scorrere la moving windows
            }
    }



    public static void setIvUsingProportion(List<Ticket> listTicketProportion,Ticket ticket){

    float p, pTotalSum = 0;
    for(Ticket t : listTicketProportion){ //qui ho IV sempre definiti
        p = calculatePFormula(t);
        pTotalSum = pTotalSum + p;
    }   //ho calcolato quelli precedenti, ora mi interessa quello 'attuale'

    int avgPFloor = (int)Math.floor(pTotalSum/percentageMovingWindows); //calcolo media IV
    int fv = ticket.getFV();
    int ov = ticket.getOV();
    int predictedIv = fv-(fv-ov)*avgPFloor;
    ticket.setIV(Math.min(predictedIv, ov)); //non ha senso 'trovare un bug in una versione' prima che essa esista.


}

    private static int calculatePFormula(Ticket t) {

    float fv = t.getFV();
    float ov = t.getOV();
    float iv = t.getIV();
    int pFloor = 0;
    if (fv != ov) {
             float p = (fv-iv)/(fv-ov);
             pFloor = (int) Math.floor(p); //ritorno la proportion calcolata per il ticket
    }
    return pFloor;
    }







}






