package org.festinho.part1Retrieve;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.festinho.entities.JavaFile;
import org.festinho.entities.Release;
import org.festinho.entities.Ticket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RetrieveGIT {


    private static Repository repository;
    private static final Logger logger = Logger.getLogger(RetrieveGIT.class.getName());


    //path è percorso url repository

    public static List<RevCommit> getAllCommit(List<Release> releaseList, Path repo) throws GitAPIException, IOException {

        ArrayList<RevCommit> commitList = new ArrayList<>(); //ritorno una lista di RevCommit, ciascuno che include tutte le informazioni di quel commit.

        //InitCommand init = Git.init();       //creo repo vuota o reinizializzo una esistente
        //init.setDirectory(repo.toFile());    //dove voglio che stia


        try (Git git = Git.open(repo.toFile())) {                   //accesso alla git repository con jgit

            Iterable<RevCommit> logs = git.log().all().call();      //tutti i log(cioè i commit), con call() la eseguo.

            for (RevCommit singleCommit : logs)                               //il singolo elemento iterato è 'rev'
            {
                commitList.add(singleCommit);
                LocalDateTime commitDate = singleCommit.getAuthorIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();        //data commit

                int releaseOfCommit = RetrieveJira.compareDateVersion(commitDate, releaseList);                                                           //INDICE della release in cui è stato fatto il commit

                for (Release release : releaseList) { //aggiungo il commit alla release appena trovata.
                    if (release.getIndex().equals(releaseOfCommit)) { //appena trovo corrispondenza
                        release.getCommitList().add(singleCommit); //aggiungo questo commit alla lista di commit della release
                    }
                }
            }
        }
        return commitList;  //ritorno tutti i commit
    }








    public static void getJavaFiles(Path repoPath, List<Release> releasesList) throws IOException {

        InitCommand init = Git.init();
        init.setDirectory(repoPath.toFile());

        try (Git git = Git.open(repoPath.toFile())) {
            for (Release release : releasesList) {
                List<String> fileNameList = new ArrayList<>();
                for (RevCommit commit : release.getCommitList()) {
                    ObjectId treeId = commit.getTree();
                    // now try to find a specific file, treewalk è proprio l'oggetto 'commit' iterato.
                    try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) { //creazione treeparser per il retrieve delle infod
                        treeWalk.reset(treeId);
                        treeWalk.setRecursive(true); //automaticamente entra nei sottoalberi
                        while (treeWalk.next()) {
                            addJavaFile(treeWalk, release, fileNameList);
                        }

                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error during the getJavaFiles operation.");
                        System.exit(1);

                    }
                }
            }
        }
        for (int k = 0; k<releasesList.size(); k++) { //potrebbe esistere una release che non aggiunge nessun file, ma lavora sui precedenti!.
            if(releasesList.get(k).getFileList().isEmpty()) {
                releasesList.get(k).setFileList(releasesList.get(k-1).getFileList());
            }
        }
    }


    private static void addJavaFile(TreeWalk treeWalk, Release release, List<String> fileNameList) throws IOException {

        //goal: aggiungo il file java nella lista di file appartenenti alla release.
        String filename = treeWalk.getPathString(); //nome del file
        if (filename.endsWith(".java")) {  //path, dove il commit ha toccato.
            JavaFile file = new JavaFile(filename); //creo nuova istanza file java con nome appena trovato.

            if (!fileNameList.contains(filename)) { //se questo file non è mai stato 'visto' prima d'ora

                fileNameList.add(filename);
                file.setBugg("No");
                file.setNr(0);
                file.setNAuth(new ArrayList<>());
                file.setChgSetSize(0);
                file.setChgSetSizeList(new ArrayList<>());
                file.setLOCadded(0);
                file.setLocAddedList(new ArrayList<>());
                file.setChurn(0);
                file.setChurnList(new ArrayList<>());
                file.setSize(Metrics.linesOfCode(treeWalk, repository));
                release.getFileList().add(file);
            }
        }
    }

    public static void checkBuggyness(List<Release> releaseList, List<Ticket> ticketList) throws IOException {
        //buggy definition: classi appartenenti all'insieme [IV,FV)
        for (Ticket ticket : ticketList) //prendo elemento ticket appartenente a ticketList
        {
            List<Integer> av = ticket.getAV();
            for (RevCommit commit : ticket.getCommitList()) //prendo i commit dalla lista di commit di quel ticket
            {
                List<DiffEntry> diffs = getDiffs(commit); //usato per differenze. Rappresenta singolo cambiamento ad un file (add remove modify).
                if (diffs != null) {
                    analyzeDiffEntryBug(diffs, releaseList, av);
                }
            }
        }

    }

    public static List<DiffEntry> getDiffs(RevCommit commit) throws IOException {
        List<DiffEntry> diffs;
        //SETTING DI DIFF FORMATTER
        DiffFormatter diff = new DiffFormatter(DisabledOutputStream.INSTANCE);                                              //dove viene collocato il diff code. DisableOutput per throws exceptions.
        diff.setRepository(repository);                                                                                    //repo sorgente che mantiene gli oggetti referenziati.
        diff.setContext(0);                                                                                                //param = linee di codice da vedere prima della prima modifica e dopo l'ultima.
        diff.setDetectRenames(true);                                                                                       // prima del rename devo usare setRepository, dopo posso ottenere l'istanza da getRenameDetector.

        if (commit.getParentCount() != 0) //il commit ha un parente, vedo la differenza tra i due
        {
            RevCommit parent = (RevCommit) commit.getParent(0).getId(); //prendo id parent
            diffs = diff.scan(parent.getTree(), commit.getTree()); //differenze tra alberi. E' del tipo DiffEntry[ADD/MODIFY/... pathfile]

        } else {
            RevWalk rw = new RevWalk(repository); //a RevWalk allows to walk over commits based on some filtering that is defined
            diffs = diff.scan(new EmptyTreeIterator(), new CanonicalTreeParser(null, rw.getObjectReader(), commit.getTree())); //se un commit non ha un parent devo usare un emptytreeIterator.
        }


        return diffs;
    }


    public static void analyzeDiffEntryBug(List<DiffEntry> diffs, List<Release> releasesList, List<Integer> av)
    {

        for (DiffEntry diff : diffs)
        {
            String type = diff.getChangeType().toString(); //prendo i cambiamenti

            if (diff.toString().contains(".java") && type.equals("MODIFY") || type.equals("DELETE"))
            {

                /*Check BUGGY, releaseCommit è contenuta in AV? se si file relase è buggy.
                // se AV vuota -> faccio nulla, file già buggyness = nO.
                ELSE: file buggy se release commit appartiene a AV del ticket. Quindi prendo nome file, la release dalla lista, e setto buggy. */

                String file;
                if (diff.getChangeType() == DiffEntry.ChangeType.DELETE || diff.getChangeType() == DiffEntry.ChangeType.RENAME )
                    { // if (diff.getChangeType() == DiffEntry.ChangeType.DELETE || diff.getChangeType() == DiffEntry.ChangeType.RENAME) {
                        file = diff.getOldPath(); //file modificato
                     }
                else
                    { //MODIFY
                        file = diff.getNewPath();
                    }

                for (Release release : releasesList)
                {
                    for (JavaFile javaFile : release.getFileList())

                    {
                        if (    (javaFile.getName().equals(file)) && (av.contains((release.getIndex())))    ) {
                                javaFile.setBugg("Yes");
                            }
                    }

                }
            }
        }
    }

    /*Ticket mi dice che c'è un bug, questo bug ha toccato le release x,y,z. Ricordo che prendo tutti ticket risolti, ovvero so la release in qui li ho fixati.
    Il ticket include dei commit, i quali vanno a modificare dei file.java. Li modifico perchè quei file=classi hanno dei problemi, e li hanno dalla release x.
    Allora il file.java nelle release x,y,z avevano problemi, ovvero erano buggy.*/

    //utility di setup
    public static void setBuilder(String repo) throws IOException {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repository = repositoryBuilder.setGitDir(new File(repo)).readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .setMustExist(true).build();
    }

}
