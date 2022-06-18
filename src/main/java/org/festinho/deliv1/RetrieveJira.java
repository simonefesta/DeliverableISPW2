package org.festinho.deliv1;
import org.festinho.entities.Release;
import org.festinho.entities.Ticket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class RetrieveJira {

    private static Map<LocalDateTime, String> releasesNameVersion;
    private static Map<LocalDateTime, String> releasesID;
    private static List<LocalDateTime> releasesOnlyDate;


    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }


    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream();
             BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.name()))) {

            String jsonText = readAll(rd);
            return (new JSONObject(jsonText));
        }
    }

    /** OPERAZIONI PER OTTENERE LE RELEASE **/

    public static List<Release> getListRelease(String projName) throws IOException, JSONException {

        ArrayList<Release> releaseList = new ArrayList<>();

        // Fills the arraylist with releases dates and orders them
        // Ignores releases with missing dates
        releasesOnlyDate = new ArrayList<>();
        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        releasesNameVersion = new HashMap<>();
        releasesID = new HashMap<>();
        for (i = 0; i < versions.length(); i++) {
            String name = "";
            String id = "";
            String releaseDate="";
            if (versions.getJSONObject(i).has("releaseDate")) {                 //qui sfrutto le api per prelevare nome, id, release date dei ticket JIRA.
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                if (versions.getJSONObject(i).has("releaseDate"))
                    releaseDate = versions.getJSONObject(i).get("releaseDate").toString();
                addRelease(releaseDate, name, id);              // Questo metodo popola gli attributi della classe presente a inizio codice.

            }
        }
        releasesOnlyDate.sort(LocalDateTime::compareTo);                                //ordinamento in base alla data

        CSVcreator.createCSVReleases(projName,releasesID,releasesNameVersion,releasesOnlyDate); //dopo aver popolato le tabelle hash delle release, le scrivo su un file csv.

        for (int j = 0; j <releasesOnlyDate.size(); j++)
        {
            LocalDateTime releaseDatetime = releasesOnlyDate.get(j);
            String releaseNameVersion = releasesNameVersion.get(releaseDatetime);
            Release release = new Release(j+1,releaseDatetime,releaseNameVersion); //primo parametro è index
            releaseList.add(release);
        }

        return releaseList;
    }


    private static void addRelease(String releaseDate, String name, String id) {
        LocalDate date = LocalDate.parse(releaseDate);                                          //like 2007-12-03
        LocalDateTime dateTime = date.atStartOfDay();                                           //like 2007-12-03T10:15:30., ma con orario posto a 0.
        if (!releasesOnlyDate.contains(dateTime))                                               //se tale data non è già presente.
            releasesOnlyDate.add(dateTime);                                                     //aggiungo alla lista

        releasesNameVersion.put(dateTime, name);                                                //metto data e nome
        releasesID.put(dateTime, id);                                                           //metto data e id
    }



    /**  OPERAZIONI PER OTTENERE LE I TICKET **/


    //adesso che ho info sulle release, voglio i ticket associati a queste release.
    public static List<Ticket> getTickets( List<Release> releases, String projName) throws IOException { //lavoro con 50% delle release!

        int j = 0;
        int i = 0;
        int total = 1;
        //creo nuova lista di ticket
        ArrayList<Ticket> ticketList = new ArrayList<>();
        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,affectedVersion,versions,created&startAt="
                    + i + "&maxResults=" + j;

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                //Iterate through each bug = ticket
                String key = issues.getJSONObject(i%1000).get("key").toString(); //prendo key, come 'AVRO-1105'
                LocalDateTime creationDate = LocalDateTime.parse(issues.getJSONObject(i%1000).getJSONObject("fields").getString("created").substring(0,16)); //consistente con Data della Release(faccio substring per avere formato di data come quello della release)
                JSONArray affectedVersion = issues.getJSONObject(i%1000).getJSONObject("fields").getJSONArray("versions"); //Affected versions, è un JSON array, posso avere da 0 ad n elementi di AV (contiene anche altre info che non ci interessano!)
                List<Integer> listAVForTicket = getAV(affectedVersion,releases);     //sostanzialmente prende SOLO i nomi delle AV dal JSONArray e le associa agli index delle release che ho.

                Ticket ticket = new Ticket(key,creationDate,listAVForTicket);


                if ( ! (listAVForTicket.isEmpty() || listAVForTicket.get(0) == null) ) {         // Controllo che la lista generate NON SIA VUOTA. Il primo di questi è l'Injected version.
                    ticket.setIV(listAVForTicket.get(0));                                        // Il primo elemento in un array NON VUOTO di Affected Versions è IV.
                } else {
                    ticket.setIV(0);                                                             //Altrimenti metto come IV index 0, che non corrisponde a nulla, ma serve per capire che IV non è noto.
                }

                ticket.setOV(compareDateVersion(creationDate,releases));                        //Adesso ho settato tutte info del ticket. (Al massimo può mancare IV)
                ticketList.add(ticket); //aggiungo a lista ticket
            }
        } while (i < total);

        return ticketList;                                                                      //lista di ticket con nome, iv (se presente), av, ov.
        }


    private static List<Integer> getAV(JSONArray versions, List<Release> releases) {
        ArrayList<Integer> listaAV = new ArrayList<>();

        if (versions.length() == 0) listaAV.add(null); //non ci sono affected version
        else {
              for (int j=0; j<versions.length(); j++ ) {
                  String av = versions.getJSONObject(j).getString("name"); //nome release affected (4.3.0)
                  for (Release release : releases) {
                      if (av.equals(release.getRelease())) { //confronto nome AV con nomi delle release (nome = nome versione)
                          listaAV.add(release.getIndex()); //mi dice indice release AV
                      }
                  }
              }
        }
    return listaAV;
    }


    public static Integer compareDateVersion(LocalDateTime date, List<Release> releases) {

        int releaseInd = 0;
        for (int i = 0; i<releases.size(); i++){
            if (date.isBefore(releases.get(i).getDate())) { // se una data è in mezzo a due release, essa è associata sempre alla release di destra!
                releaseInd = releases.get(i).getIndex();    //esco appena trovo la prima data di una versione appena superiore a quella del ticket.
                break;
            }
            if(date.isAfter(releases.get(releases.size()-1).getDate())) { //se data è dopo ultima release, devo associare ticket a questa ultima release, non posso associarlo a release che non esiste!
                releaseInd = releases.get(releases.size()-1).getIndex();
            }
         }
        return releaseInd;

    }
}
