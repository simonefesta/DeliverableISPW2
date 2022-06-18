package org.festinho.deliv1;


import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.festinho.entities.JavaFile;
import org.festinho.entities.Release;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Ho 16 metriche, ne devo prendere almeno 9.
 * Devo ignorare la seconda metà delle release in ordine temporale.
 * Devo considerare unicamente i file.java
 * Ogni colonna dataset calcola con script apposito, considerando tutti i commit e usandoli in base all'esigenza.
 * 1 release -> n revisioni -> n commit -> n autori. quindi autori per release.
 */
public class Metrics {

    private Metrics() {
    }

    private static final String FILE_EXTENSION = ".java";
    private static final String RENAME = "RENAME";
    private static final String DELETE = "DELETE";
    private static final String MODIFY = "MODIFY";
    private static final String ADD = "ADD";


    public static void getMetrics(List<Release> releasesList, String repo) throws IOException {

        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        Repository repository = repositoryBuilder.setGitDir(new File(repo))
                .readEnvironment()                                                                  // scan environment GIT_* variables
                .findGitDir()                                                                       // scan up the file system tree
                .setMustExist(true)
                .build();                                                                           // creazione/accesso repository object in questo modo

        for (Release release : releasesList) {

            List<JavaFile> filesJavaListPerRelease = new ArrayList<>();                                            //lista che contiene i nomi dei file dentro diffs dei commit per ogni release

            for (RevCommit commit : release.getCommitList()) {

                DiffFormatter diff = new DiffFormatter(DisabledOutputStream.INSTANCE);              //dove viene collocato il diff code. DisableOutput per throws exceptions.
                diff.setRepository(repository);                                                     //repo sorgente che mantiene gli oggetti referenziati.
                diff.setDiffComparator(RawTextComparator.DEFAULT);
                diff.setDetectRenames(true);                                                       // prima del rename devo usare setRepositoru, dopo posso ottenere l'istanza da getRenameDetector.

                String authName = commit.getAuthorIdent().getName();

                List<DiffEntry> diffs = RetrieveGIT.getDiffs(commit);                               // dentro ho tutti i diffs generati da un commit
                if (diffs != null) {
                    analyzeDiffEntryMetrics(diffs, filesJavaListPerRelease, authName, diff);
                }

            }
            updateMetricsOfFilesByRelease(filesJavaListPerRelease, release);
        }

    }


    public static void analyzeDiffEntryMetrics(List<DiffEntry> diffs, List<JavaFile> fileList, String authName, DiffFormatter diff) {
        int numTouchedClass = 0;
        for (DiffEntry diffEntry : diffs) {
            if (diffEntry.toString().contains(FILE_EXTENSION)) {
                numTouchedClass++;
            }
        }

        for (DiffEntry diffEntry : diffs) {
            String type = diffEntry.getChangeType().toString();
            if (diffEntry.toString().contains(FILE_EXTENSION) && type.equals(MODIFY) || type.equals(DELETE) || type.equals(ADD) || type.equals(RENAME)) {
                String fileName;
                if (type.equals("DELETE") || type.equals("RENAME")) fileName = diffEntry.getOldPath();
                else fileName = diffEntry.getNewPath();
                calculateMetrics(fileList, fileName, authName, numTouchedClass, diffEntry, diff);
            }

        }
    }

    public static void calculateMetrics(List<JavaFile> fileList, String fileName, String authName, int numTouchedClass, DiffEntry diffEntry, DiffFormatter diff) {
        int locAdded = 0;
        int locDeleted = 0;
        try {
            for (Edit edit : diff.toFileHeader(diffEntry).toEditList()) {   //metodo per calcolare locAdded & locDeleted
                locAdded += edit.getEndB() - edit.getBeginB();
                locDeleted += edit.getEndA() - edit.getBeginA();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int churn = locAdded - locDeleted;      //cosi definito

        boolean isFind = false;

        if (!fileList.isEmpty()) {                          //Esiste listaFile, il file che sto considerando vi appartiene?
            for (JavaFile file : fileList) {
                if (file.getName().equals(fileName)) {
                    isFind = true;                                    //il file esiste nella lista dei file esaminati per la release 'x', lo aggiorno.
                    file.setNr(file.getNr() + 1);
                    if (!file.getNAuth().contains(authName)) file.getNAuth().add(authName); //evito doppioni

                    file.setLOCadded(file.getLOCadded() + locAdded);
                    file.getLocAddedList().add(locAdded);
                    file.setChgSetSize(file.getChgSetSize() + numTouchedClass);
                    file.setChurn(file.getChurn() + churn);
                    file.getChurnList().add(churn);
                }

            }
        } else { // fileList non esiste!
            isFind = true;
            JavaFile javaFile = new JavaFile(fileName);
            applyMetricsNewFile(javaFile, numTouchedClass, locAdded, churn, fileList, authName);
        }
        if (isFind == false) {                                       //file non appartiene alla fileLIST
            JavaFile javaFile = new JavaFile(fileName);            //fa le stesse cose nel caso file list isEmpty
            applyMetricsNewFile(javaFile, numTouchedClass, locAdded, churn, fileList, authName);

        }


    }


    public static void applyMetricsNewFile(JavaFile javaFile, int numTouchedClass, int locAdded, int churn, List<JavaFile> fileList, String authName) {

        javaFile.setNr(1); //Perché appena creato

        List<String> listAuth = new ArrayList<>();
        listAuth.add(authName); // poiché appena creato

        javaFile.setNAuth(listAuth);

        javaFile.setChgSetSize(numTouchedClass);
        List<Integer> chgSetSizeList = new ArrayList<>();
        chgSetSizeList.add(numTouchedClass);
        javaFile.setChgSetSizeList(chgSetSizeList);

        javaFile.setLOCadded(locAdded);
        List<Integer> locAddedList = new ArrayList<>();
        locAddedList.add(locAdded);
        javaFile.setLocAddedList(locAddedList);

        javaFile.setChurn(churn);
        List<Integer> churnList = new ArrayList<>();
        churnList.add(churn);
        javaFile.setChurnList(churnList);

        fileList.add(javaFile);
    }


    public static void updateMetricsOfFilesByRelease(List<JavaFile> fileList, Release release) {


        for (JavaFile javaFile : fileList) {

            List<String> nAuth = javaFile.getNAuth();
            List<Integer> chgSetSize = javaFile.getChgSetSizeList();
            List<Integer> locAdded = javaFile.getLocAddedList();
            List<Integer> churn = javaFile.getChurnList();
            for (JavaFile fileInRelease : release.getFileList()) {

                if (javaFile.getName().equals(fileInRelease.getName())) { //metto le metriche prese da javaFile nel rispettivo javaFile collegato alla release.

                    //UPDATE LOC
                    fileInRelease.setLOCadded(fileInRelease.getLOCadded() + javaFile.getLOCadded());                                //aggiorno LOC added
                    List<Integer> locAddedList = fileInRelease.getLocAddedList();                                             //prelevo la lista di Loc
                    locAddedList.addAll(locAdded);                                                                           //aggiungo nuove loc
                    fileInRelease.setLocAddedList(locAddedList);                                                               //aggiorno loc

                    //UPDATE CHURN
                    fileInRelease.setChurn(fileInRelease.getChurn() + javaFile.getChurn());                                         //faccio come prima
                    List<Integer> churnList = fileInRelease.getChurnList();
                    churnList.addAll(churn);
                    fileInRelease.setChurnList(churnList);

                    //UPDATE DEGLI AUTORI
                    fileInRelease.setNr(fileInRelease.getNr() + javaFile.getNr());
                    List<String> listAuth = fileInRelease.getNAuth();                                                         //prelevo gli autori attualmente presenti nel file associato alla release
                    listAuth.addAll(nAuth);                                                                             //ci aggiungo il nuovo autore preso dal file appena ispezionat
                    listAuth = listAuth.stream().distinct().collect(Collectors.toList());                               //evito i duplicati con 'distinct'
                    fileInRelease.setNAuth(listAuth);                                                                         //metto questa nuova lista aggiornata nel file release

                    //UPDATE DEL CHGSET
                    fileInRelease.setChgSetSize(fileInRelease.getChgSetSize() + javaFile.getChgSetSize());                          //aggiorno stats 'numero file committati insieme'
                    List<Integer> chgSetSizeList = fileInRelease.getChgSetSizeList();                                         //prendo la lista file committati insieme
                    chgSetSizeList.addAll(chgSetSize);                                                                  //ci aggiungo la nuova lista analizzata
                    fileInRelease.setChgSetSizeList(chgSetSizeList);                                                          //aggiorno lista release


                }

            }

        }
    }


    public static int linesOfCode(TreeWalk treewalk, Repository repository) throws IOException {

         ObjectLoader loader = repository.open(treewalk.getObjectId(0));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        loader.copyTo(output);
        String fileContent = output.toString();
        return (int) fileContent.lines().count();
    }

}