package org.festinho.part1;


import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.festinho.entities.Release;
import org.festinho.entities.Ticket;
import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainClass {

    private static final Logger logger = Logger.getLogger(MainClass.class.getName());


    private static List<Release> releasesList;
    private static List<Ticket> ticketList;
    private static List<RevCommit> commitList;
    public static final String NAMEPROJECT = "BOOKKEEPER"; // OR 'AVRO'


    public static void main(String[] args) throws IllegalStateException, GitAPIException, IOException, JSONException {

        String repo = "/Users/festinho/IdeaProjects/" + NAMEPROJECT.toLowerCase() + "/.git";
        Path repoPath = Paths.get("/Users/festinho/IdeaProjects/" + NAMEPROJECT.toLowerCase());

        // in releases List metto tutte le release del progetto
        releasesList = RetrieveJira.getListRelease(NAMEPROJECT);

        // in commit List metto tutti i commit del progetto
        commitList = RetrieveGIT.getAllCommit(releasesList, repoPath);

        //prendo tutti i ticket da Jira in accordo alle specifiche
        ticketList = RetrieveJira.getTickets(releasesList, NAMEPROJECT);


        logger.log(Level.INFO, "Eseguo il linkage Tickets - Commits");
        linkTicketCommits();
        removeHalfRelease(releasesList, ticketList);


        cleanTicketInconsistencies();
        RetrieveGIT.setBuilder(repo);
        logger.log(Level.INFO, "Numero ticket = {0}.", ticketList.size());

        Collections.reverse(ticketList); //reverse perchè è moving window
        Proportion.proportion(ticketList);

        cleanTicketInconsistencies();   //devo rifarlo perchè, avendo settato nuovi IV, voglio togliere possibili incongruenze!

        RetrieveGIT.getJavaFiles(repoPath, releasesList);

        RetrieveGIT.checkBuggyness(releasesList, ticketList); //inizialmente buggyness = NO per ogni release

        Metrics.getMetrics(releasesList, repo);
        CSVCreator.writeCSVBuggyness(releasesList, NAMEPROJECT.toLowerCase());

    }


    private static void linkTicketCommits() {
        for (Ticket ticket : ticketList) {
            ArrayList<LocalDateTime> commitDateList = new ArrayList<>();

            String ticketID = ticket.getID();

            for (RevCommit commit : commitList) {   //LINKAGE ticketList - commitList, se c'è prendo la data di creazione del commit
                String message = commit.getFullMessage();

                if (existsLinkMessageCommit(message,ticketID)) {
                    LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    commitDateList.add(commitDate);
                    ticket.getCommitList().add(commit);
                }


            }
            if (!commitDateList.isEmpty()) { //Esistono ticket chiusi senza commit (es: BOOKKEEPER-884: 4.3.2 link points to 4.3.1 documentation). Questi ticket non mi sono di aiuto.

                Collections.sort(commitDateList);
                LocalDateTime resolutionDate = commitDateList.get(commitDateList.size() - 1); //la resolution date di un ticket è l'ultimo commit fatto associato a quel ticket.
                ticket.setResolutionDate(resolutionDate);
                ticket.setFV(RetrieveJira.compareDateVersion(resolutionDate, releasesList));


            }

        }
        //Rimuovo ticket che non hanno alcun commit associato, li riconosco perchè non hanno resolutationDate (data ultimo commit).
        Iterator<Ticket> ticket = ticketList.iterator();

        while (ticket.hasNext()) {
            Ticket t = ticket.next();
            if (t.getResolutionDate() == null) {
                ticket.remove();
            }
        }
    }

    private static boolean existsLinkMessageCommit(String message, String ticketID) {
        // Senza questo check, potrei linkare commit contenente BOOKKEEPER-1074 con ticketID = 107 ad esempio, poichè contains ritornerebbe true.
        return message.contains(ticketID + "\n") || message.contains(ticketID + " ") || message.contains(ticketID + ":")
                || message.contains(ticketID + ".") || message.contains(ticketID + "/") || message.endsWith(ticketID) ||
                message.contains(ticketID + "]") || message.contains(ticketID + "_") || message.contains(ticketID + "-") || message.contains(ticketID + ")");
    }


    public static void removeHalfRelease(List<Release> releasesList, List<Ticket> ticketList) {

        float releaseNumber = releasesList.size();

        int halfRelease = (int) Math.floor(releaseNumber / 2); // arrotondo in difetto, ora il numero di release che voglio e' la meta'

        logger.log(Level.INFO, "NUMERO RELEASE == = {0}.", releaseNumber);
        logger.log(Level.INFO, "HALF RELEASE == = {0}.", halfRelease);

        Iterator<Release> i = releasesList.iterator();
        while (i.hasNext()) {
            Release s = i.next();
            if (s.getIndex() > halfRelease) {
                i.remove();
            }
        }
        removeAndSetAVTickets(halfRelease, ticketList);
    }


    public static void removeAndSetAVTickets(int halfRelease, List<Ticket> ticketList) {

        Iterator<Ticket> iterator = ticketList.iterator();
        while (iterator.hasNext()) {
            Ticket t = iterator.next();
            if (t.getIV() > halfRelease) {    //se IV > halfRelease --> tolgo ticket
                iterator.remove();
            }
            if (t.getOV() > halfRelease || t.getFV() > halfRelease) {       //Se IV < half, ma OV o FV sono > half? tutte le versioni da IV < half ad half sono AV.
                List<Integer> affectedVersionsListByTicket = new ArrayList<>();
                for (int k = t.getIV(); k < halfRelease; k++) {
                    affectedVersionsListByTicket.add(k);
                }
                t.setAV(affectedVersionsListByTicket);
            }
        }
    }



    public static void cleanTicketInconsistencies() {

        for (Ticket ticket : ticketList) {

            if (ticket.getIV() != 0) {    //se IV è definita

                checkTimeOrderTickets(ticket);
                checkBaseCaseOV(ticket);

            }

        }
    }



    public static void checkTimeOrderTickets(Ticket ticket) {

        if (ticket.getFV() > ticket.getIV() && ticket.getOV() >= ticket.getIV()) { //Caso corretto

            ticket.getAV().clear(); //svuoto la lista di AV per poi aggiornarla con valori corretti
            for (int i = ticket.getIV(); i < ticket.getFV(); i++) {
                ticket.getAV().add(i);
            }

        }
        else
         {  //caso di errore, cioè IV viene dopo FV oppure OV, e non può essere.
            ticket.setIV(0);        //setto come errore
            ticket.getAV().clear();
            ticket.getAV().add(0);
        }

        if (ticket.getFV().equals(ticket.getIV())) {    //se FV = IV -> AV vuota. (caso 'base')
            ticket.getAV().clear();
            ticket.getAV().add(0);
        }
    }



    private static void checkBaseCaseOV(Ticket ticket) { //Condizioni caso base in cui OV = prima release.

        if (ticket.getOV() == 1) {
            ticket.getAV().clear(); //svuoto la lista di AV per poi aggiornarla con valori corretti
            ticket.setIV(1);

            if (ticket.getFV() == 1) {
                ticket.getAV().add(0);      //se OV=FV allora AV = 0, cioè non metto nulla.
            } else {

                for (int i = ticket.getIV(); i < ticket.getFV(); i++) { //se FV non è prima release, assegno ad AV tutte le release da IV ad FV.
                    ticket.getAV().add(i);
                }
            }

        }

    }
}

