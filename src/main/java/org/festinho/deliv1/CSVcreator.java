package org.festinho.deliv1;

import org.festinho.entities.JavaFile;
import org.festinho.entities.WekaRecord;
import org.festinho.entities.Release;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSVcreator {

    static Logger logger = Logger.getLogger(CSVcreator.class.getName());
    private static final String ERROR = "Error in csv writer";


    public static void createCSVReleases(String projName,Map<LocalDateTime, String> releasesID,Map<LocalDateTime, String> releasesNameVersion,List<LocalDateTime> releasesOnlyDate)
    {
        /*CREAZIONE FILE CSV CON LE RELEASE*/
        try (FileWriter fileWriter = new FileWriter(projName.toLowerCase() + ".ReleasesList.csv")) {

            fileWriter.append("Index,VersionID,VersionName,Date");
            fileWriter.append("\n");

            for (int i = 0; i < releasesOnlyDate.size(); i++) { //Ho ordinato le releases in base alla data. Poiché sono hash, li esploro usando un indice, e accedo al valore (ID, nome,data), in questo ordine.
                int index = i+1;
                fileWriter.append(Integer.toString(index));                     // indice nel file csv che identifica la tupla.
                fileWriter.append(",");
                fileWriter.append(releasesID.get(releasesOnlyDate.get(i)));      //prelevo l'Id, univoco, andando in releaseOnlyDate ordinato e prendendo la data d'indice 'i'.
                fileWriter.append(",");
                fileWriter.append((releasesNameVersion.get(releasesOnlyDate.get(i)))); //nome della release (es 4.0.1)
                fileWriter.append(",");
                fileWriter.append(((releasesOnlyDate.get(i).toString())));      //data release.
                fileWriter.append("\n");
            }

        } catch (Exception e) {
            System.err.print("Something went wrong");
        }


    }


    public static void writeCSVBuggyness(List<Release> releasesList, String project) {
        try {
            //creo file csv.
            FileWriter fileWriter = new FileWriter(project.toLowerCase()+".Buggyness.csv");
            fileWriter.append("RELEASE,FILENAME,SIZE,LOC_added,MAX_LOC_Added,AVG_LOC_Added,CHURN,MAX_Churn,AVG_Churn,NR,NAUTH,CHGSETSIZE,MAX_ChgSet,AVG_ChgSet,BUGGYNESS\n");
            for (Release release : releasesList) {

                for (JavaFile file : release.getFileList()) {
                    //per ogni file appartenente alla release 'x'

                    appendMetrics(fileWriter, release, file);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ERROR);
            ex.printStackTrace();
        }
    }

    private static void appendMetrics(FileWriter fileWriter, Release release, JavaFile file) throws IOException {
        fileWriter.append(release.getIndex().toString());
        fileWriter.append(",");
        fileWriter.append(file.getName()); //nome del file
        fileWriter.append(",");
        fileWriter.append(file.getSize().toString()); //LOC
        fileWriter.append(",");
        fileWriter.append(file.getLOCadded().toString()); //LOC_added
        fileWriter.append(",");

        if (file.getLOCadded().equals(0)) { //se non ho aggiunto nulla niente max e avg
            fileWriter.append("0");
            fileWriter.append(",");
            fileWriter.append("0");
        } else {
            int maxLocAdded = Collections.max((file.getLocAddedList())); //prendo il max dalla lista
            fileWriter.append(String.valueOf(maxLocAdded)); //scrivo tale massimo
            fileWriter.append(",");
            int avgLocAdded = (int)file.getLocAddedList().stream().mapToInt(Integer::intValue).average().orElse(0.0); //easy way to avg
            fileWriter.append(String.valueOf(avgLocAdded));
        }
        fileWriter.append(",");
        fileWriter.append(file.getChurn().toString());
        fileWriter.append(",");
        if (file.getChurn().equals(0)) {
            fileWriter.append("0");
            fileWriter.append(",");
            fileWriter.append("0");
        } else {
            int maxChurn = Collections.max((file.getChurnList()));
            fileWriter.append(String.valueOf(maxChurn));
            fileWriter.append(",");
            int avgChurn = (int) file.getChurnList().stream().mapToInt(Integer::intValue).average().orElse(0.0); //easy way
            fileWriter.append(String.valueOf(avgChurn));
        }
        fileWriter.append(",");

        fileWriter.append(file.getNr().toString());
        fileWriter.append(",");
        int size = file.getNAuth().size();
        fileWriter.append(String.valueOf(size));
        fileWriter.append(",");
        fileWriter.append(file.getChgSetSize().toString());
        fileWriter.append(",");
        if (file.getChgSetSize().equals(0)) {
            fileWriter.append("0");
            fileWriter.append(",");
            fileWriter.append("0");
        } else {
            int maxChgSet = Collections.max((file.getChgSetSizeList()));
            fileWriter.append(String.valueOf(maxChgSet));
            fileWriter.append(",");
            int avgChgSet = (int) file.getChgSetSizeList().stream().mapToInt(Integer::intValue).average().orElse(0.0); //da calcolare
            fileWriter.append(String.valueOf(avgChgSet));

        }
        fileWriter.append(",");
        fileWriter.append(file.getBugg());
        fileWriter.append("\n");
        fileWriter.flush();
    }

// per arrotondare alla seconda cifra decimale!

    public static String doubleTransform(Double value) {
        DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(value);
    }




public static void writeWekaCSV(List<WekaRecord> wekaRecordList, String projName) {
    try (
            FileWriter fileWriter = new FileWriter(projName.toLowerCase()+".WekaResults.csv")) {

        fileWriter.append("Dataset,#TrainingRelease,%training/total,%Defective/training,%Defective/testing,Classifier,"
                + "Feature Selection,Balancing,Sensitivity,TP,FP,TN,FN,Precision,Recall,AUC,Kappa\n");


        for(WekaRecord entry : wekaRecordList) {

            fileWriter.append(entry.getDatasetName());
            fileWriter.append(",");
            fileWriter.append(entry.getNumTrainingRelease().toString());
            fileWriter.append(",");
            fileWriter.append(doubleTransform(entry.getTrainingPerc()));
            fileWriter.append(",");
            fileWriter.append(doubleTransform(entry.getDefectPercTrain()));
            fileWriter.append(",");
            fileWriter.append(doubleTransform(entry.getDefectPercTest()));
            fileWriter.append(",");
            fileWriter.append(entry.getClassifierName());
            fileWriter.append(",");
            fileWriter.append(entry.getFeatureSelection());
            fileWriter.append(",");
            fileWriter.append(entry.getBalancing());
            fileWriter.append(",");
            fileWriter.append(entry.getSensitivity());
            fileWriter.append(",");
            fileWriter.append(entry.getTP().toString());
            fileWriter.append(",");
            fileWriter.append(entry.getFP().toString());
            fileWriter.append(",");
            fileWriter.append(entry.getTN().toString());
            fileWriter.append(",");
            fileWriter.append(entry.getFN().toString());
            fileWriter.append(",");
            fileWriter.append(doubleTransform(entry.getPrecision()));
            fileWriter.append(",");
            fileWriter.append(doubleTransform(entry.getRecall()));
            fileWriter.append(",");
            fileWriter.append(doubleTransform(entry.getAuc()));
            fileWriter.append(",");
            fileWriter.append(doubleTransform(entry.getKappa()));
            fileWriter.append("\n");
            fileWriter.flush();
        }

    } catch (Exception ex) {
        logger.log(Level.SEVERE,ERROR);
        ex.printStackTrace();

    }
}


}





