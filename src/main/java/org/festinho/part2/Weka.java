package org.festinho.part2;
/*
 *  How to use WEKA API in Java
 *  Copyright (C) 2014
 *  @author Dr Noureddin M. Sadawi (noureddin.sadawi@gmail.com)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it as you wish ...
 *  I ask you only, as a professional courtesy, to cite my name, web page
 *  and my YouTube Channel!
 *
 */
import org.festinho.part1.CSVCreator;
import org.festinho.part1.MainClass;
import org.festinho.part1.RetrieveJira;
import org.festinho.entities.WekaRecord;
import org.festinho.entities.Release;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;




public class Weka {
    static Logger logger = Logger.getLogger(Weka.class.getName());
    private static List<WekaRecord> wekaRecordList;
    private static final String ERROR_CLASSIFIER = "Error in building classifier";

    //added for resolve code smells

    private static final String USERPC = "festinho";
    private static final String PROJECTPATH = "IdeaProjects";
    private static final String NAMEPROJECT = "Deliverable1";
    private  static final String SEPARATOR = "/";

    private static final String PATH = "/Users/" + USERPC + SEPARATOR + PROJECTPATH + SEPARATOR + NAMEPROJECT + SEPARATOR;




    public static void main(String[] args) throws Exception{

        //load datasets
        List<Release> releasesList = RetrieveJira.getListRelease(MainClass.NAMEPROJECT);
        removeHalf(releasesList);
        csv2arff();
        wekaRecordList = new ArrayList<>();


        String arffPath =  PATH+MainClass.NAMEPROJECT.toLowerCase()+".Buggyness.arff";
        logger.log(Level.INFO, "Starting WalkForward...");
        walkForward(arffPath, releasesList);
        logger.log(Level.INFO, "... Done! Now creating CSV file.");

        CSVCreator.writeWekaCSV(wekaRecordList,MainClass.NAMEPROJECT);
    }


    public static void csv2arff() throws IOException {
        String csvPath = PATH+MainClass.NAMEPROJECT.toLowerCase()+".Buggyness.csv";
        CSVLoader loader = new CSVLoader();
            loader.setSource(new File(csvPath));
            Instances data = loader.getDataSet();//get instances object
            // save ARFF
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);//set the dataset we want to convert
            //and save as ARFF
            saver.setFile(new File(PATH+MainClass.NAMEPROJECT.toLowerCase()+".Buggyness.arff"));
            saver.writeBatch();

        }



    public static void removeHalf(List<Release> releasesList) {

        int releaseNumber = releasesList.size();
        float half = (float) releaseNumber / 2;
        int halfRelease = (int) half; // arrotondo in difetto, ora il numero di release che voglio e' la meta'

        //(code smell) in pratica rimuovo tutte le release successive alla half release
        releasesList.removeIf(s -> s.getIndex() > halfRelease);
    }


    public static void walkForward(String arffPath, List<Release> releaseList) { //gli passo metà release!
        DataSource source;
        List <Instances> instancesList = new ArrayList<>(); //Weka lavora con Instances. COME E' FATTO? [ tutte le righe del csv buggyness release1, tutte le righe del csv buggyness release2,...]
        List <String> classifierNames = Arrays.asList("Random Forest","Ibk","Naive Bayes");
       try {

            source = new DataSource(arffPath); //'carico' i dati dal file .arff
            /* PRENDO SOLO LE RIGHE ASSOCIATE ALLA META' DELLE RELEASE */

          for (Release release : releaseList)
            {   //per ogni release in lista di release
                Instances instances = source.getDataSet(); // ritorna full dataset
                Iterator<Instance> instanceIterator = instances.iterator(); //creo un iteratore, instance è una singola istanza di dato!
                int indexRelease = release.getIndex(); //indice release (parte da 1!)

                while (instanceIterator.hasNext())
                {   // c'è next line in arff?
                    Instance i = instanceIterator.next(); //parto dalla riga1, cioè prima classe in prima release.
                    int indexArffEntry = (int) (i.value(0)); //prendo la release (attributo 0 nel file.arff) associato a quella istanza.
                    if (indexArffEntry != indexRelease)
                        {
                          instanceIterator.remove(); //data release x, rimuovo le entry dell'arff non associate con release x.
                        }
                }
               instancesList.add(instances); //le istanze rimanenti sono associate alla release di riferimento
            }

          //instanceList include per ogni 'tupla' tutte le classi/righe arff ordinate per release. [tupla 1 = tutte classi + info release 1, tupla 2 = tutte classi + info release 2..]

           //Ovvero, per testare release k, uso come training le prime k-1 release.

        for(int j = 2; j < releaseList.size()+1;j++) { //Implementazione vera e propria del WF. Parto da release 2 perchè non posso testare la 1 (non ho train). Inoltre releaseList.size + 1 perchè le release partono da 1 (non da 0).

            Instances training = null;
            Instances testing = null;
            WekaRecord entry = new WekaRecord(MainClass.NAMEPROJECT); //sono info dell'analisi che faccio: cosa uso, che ottengo...
            int numTrain = j - 1; // se indice j = 2, allora come allenamento uso solo dataset 1

            entry.setNumTrainingRelease(numTrain);

            training = new Instances(instancesList.get(0)); //in training sicuramente metto le istances della release 1 (che ha index = 0).



            for (int remainingReleasesIndex = 1; remainingReleasesIndex < numTrain; remainingReleasesIndex++) {  //per ogni release precedente alla release da testare
                for (Instance i : instancesList.get(remainingReleasesIndex)) { //prendo il gruppo di instances
                    training.add(i); //singola 'istanza' delle istanze di una release usata come train. Il metodo 'add' permette di aggiungere una singola 'instance'.

                }
            }

            //test set
            testing = instancesList.get(numTrain); // numTrain == 'index' nell'array di release del test. Anche se ho definito release con index 1, 2... per semplicità, se metto queste release in un array, la prima release sta in posizione 0 (anche
                                                    //se per come l'ho interpretata io è la 'release 1'.
            logger.log(Level.INFO, "Setting instances for testing release {0}",numTrain+1);

            int numAttr = training.numAttributes();
            training.setClassIndex(numAttr - 1); //l'ultimo attributo, dove 'predico', è il target.
            testing.setClassIndex(numAttr - 1);

            float percentageTraining = (float) training.size()*100 / source.getDataSet().size(); // % ( training / total data)
            entry.setTrainingPerc(percentageTraining);


            float positiveInstancesTraining = calculatePositiveInstances(training);
            float percentageDefectTraining = (positiveInstancesTraining*100)/ training.size();
            entry.setDefectPercTrain(percentageDefectTraining);


            float positiveInstancesTest = calculatePositiveInstances(testing);
            float percentageDefectTest = (positiveInstancesTest*100)/ testing.size();
            entry.setDefectPercTest(percentageDefectTest);

            chooseClassifier(classifierNames, training, testing, entry);
        }

        }

        catch (Exception e) {
            logger.log(Level.SEVERE,"Error in walkForward");

        }

}

    public static void chooseClassifier(List<String> classifierNames, Instances training, Instances testing, WekaRecord entry) throws Exception {
        AbstractClassifier classifier = null;
      //

        for (int i = 0; i<classifierNames.size(); i++) {

            //sembra una copia dei dati trovati sopra!
            Instances trainingCopy = new Instances(training);
            Instances testingCopy = new Instances(testing);
            WekaRecord entryCopy = new WekaRecord(MainClass.NAMEPROJECT);
            entryCopy.setNumTrainingRelease(entry.getNumTrainingRelease());
            entryCopy.setTrainingPerc(entry.getTrainingPerc());
            entryCopy.setDefectPercTrain(entry.getDefectPercTrain());
            entryCopy.setDefectPercTest(entry.getDefectPercTest());
            //al variare di i associo classifier alla entry

            switch(i) {
                case 0: //Random Forest
                    classifier = new RandomForest();
                    entryCopy.setClassifierName("Random Forest");
                    break;

                case 1: //Ibk
                    classifier = new IBk();
                    entryCopy.setClassifierName("Ibk");

                    break;

                case 2: //Naive Bayes
                    classifier = new NaiveBayes();
                    entryCopy.setClassifierName("Naive Bayes");

                    break;
                default:
                    logger.log(Level.SEVERE,"Error in classifier selection ");
                    System.exit(1);
                    break;
            }


            //ho selezionato il classificatore, ora devo selezionare la tecnica di featureSelection
            chooseFeatureSelection(classifier,entryCopy, trainingCopy, testingCopy);
        }

}

    private static void chooseFeatureSelection(AbstractClassifier classifier, WekaRecord entry, Instances training, Instances testing) throws Exception {
        List<String> featureSelectionNames = Arrays.asList("No", "Best first", "Greedy Backward");


        for (String featureSelectionName : featureSelectionNames) {     //In questo metodo ci arrivo con Random, Ibk, Naive. Per ognuno di essi ho due varianti: uno senza FeatureSelection, uno con Feature Selection.

            Instances trainingCopy = new Instances(training);
            Instances testingCopy = new Instances(testing);

            WekaRecord entryCopy = new WekaRecord(MainClass.NAMEPROJECT);
            entryCopy.setNumTrainingRelease(entry.getNumTrainingRelease());
            entryCopy.setClassifierName(entry.getClassifierName());
            entryCopy.setTrainingPerc(entry.getTrainingPerc());
            entryCopy.setDefectPercTrain(entry.getDefectPercTrain());
            entryCopy.setDefectPercTest(entry.getDefectPercTest());


            entryCopy.setFeatureSelection(featureSelectionName);
            applyFeatureSelection(classifier, entryCopy, trainingCopy, testingCopy);

        }
}

    public static void applyFeatureSelection(AbstractClassifier classifier, WekaRecord entry, Instances training, Instances testing) throws Exception
    {
        Instances trainingCopy = new Instances(training);
        Instances testingCopy = new Instances(testing);
        String featureSelection = entry.getFeatureSelection();

        if (featureSelection.equals("No"))
        {
            chooseBalancing(classifier, entry, trainingCopy, testingCopy);
        }
        else
        {
            AttributeSelection filter = new AttributeSelection();
            CfsSubsetEval evaluator = new CfsSubsetEval();

            if (featureSelection.equals("Best First"))
            {
                BestFirst search = new BestFirst ();
                filter.setEvaluator(evaluator);
                filter.setSearch(search);

            }
            else
            {   //feature selection == Greedy backward
                GreedyStepwise search = new GreedyStepwise ();
                search.setSearchBackwards(true); //type backward
                filter.setEvaluator(evaluator);
                filter.setSearch(search);
            }

        //specify the dataset
        Instances filteredTraining;
        Instances filteredTesting;
        filter.setInputFormat(training);

        //apply
        filteredTraining = Filter.useFilter(trainingCopy, filter);
        filteredTesting = Filter.useFilter(testingCopy, filter);

        int numAttrFiltered = filteredTraining.numAttributes();
        filteredTraining.setClassIndex(numAttrFiltered - 1);
        filteredTesting.setClassIndex(numAttrFiltered - 1);

        //lavora con feature selection
        chooseBalancing(classifier, entry, filteredTraining,  filteredTesting);
        }

    }



    public static void chooseBalancing(AbstractClassifier classifier, WekaRecord entry, Instances training, Instances testing) {

        List<String> balancingNames = Arrays.asList("No", "oversampling", "undersampling", "SMOTE");

        for (String balancingName : balancingNames) {

            Instances trainingBalanced = new Instances(training);
            Instances testingBalanced = new Instances(testing);
            FilteredClassifier filteredClassifier = null;


            WekaRecord entryCopy = new WekaRecord(MainClass.NAMEPROJECT);
            entryCopy.setNumTrainingRelease(entry.getNumTrainingRelease());
            entryCopy.setClassifierName(entry.getClassifierName());
            entryCopy.setTrainingPerc(entry.getTrainingPerc());
            entryCopy.setFeatureSelection(entry.getFeatureSelection());
            entryCopy.setDefectPercTrain(entry.getDefectPercTrain());
            entryCopy.setDefectPercTest(entry.getDefectPercTest());


            switch (balancingName) {
                case "No":
                    entryCopy.setBalancing(balancingName);
                    break;
                case "oversampling":
                    entryCopy.setBalancing(balancingName);
                    try {
                        Resample resample = new Resample();
                        resample.setInputFormat(trainingBalanced);

                        /* see https://waikato.github.io/weka-blog/posts/2019-01-30-sampling/ */
                        /*Based on this, to under-sample the majority class so that both classes have the same number of instances,
                        we can configure the filter to use biasToUniformClass=1.0 and sampleSizePercent=X,
                        where X/2 is (approximately) the percentage of data that belongs to the minority class.  */

                        resample.setBiasToUniformClass(1.0f);

                        int totInstances = trainingBalanced.size();
                        int positiveInstances = calculatePositiveInstances(trainingBalanced);
                        double percentagePositiveInstances = (positiveInstances*100)/(float) totInstances;
                        resample.setSampleSizePercent(percentagePositiveInstances * 2);
                        filteredClassifier = new FilteredClassifier();
                        filteredClassifier.setClassifier(classifier);
                        filteredClassifier.setFilter(resample);
                        trainingBalanced = Filter.useFilter(trainingBalanced, resample); //bilancio il training

                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error in oversampling");

                    }
                    break;

                case "undersampling":

                    entryCopy.setBalancing(balancingName);
                    SpreadSubsample spreadSubsample; //uso lui poichè produce subsample
                    try {
                        spreadSubsample = new SpreadSubsample();
                        spreadSubsample.setInputFormat(trainingBalanced);
                        String[] opts = new String[]{"-M", "1.0"};
                        spreadSubsample.setOptions(opts);
                        filteredClassifier = new FilteredClassifier();
                        filteredClassifier.setClassifier(classifier);
                        filteredClassifier.setFilter(spreadSubsample);
                        trainingBalanced = Filter.useFilter(trainingBalanced, spreadSubsample);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error in undersampling");

                    }
                    break;

                case "SMOTE":
                    entryCopy.setBalancing(balancingName);
                    SMOTE smote;
                    try {
                        smote = new SMOTE();
                        smote.setInputFormat(trainingBalanced);
                        filteredClassifier = new FilteredClassifier();
                        filteredClassifier.setClassifier(classifier);
                        filteredClassifier.setFilter(smote);
                        trainingBalanced = Filter.useFilter(trainingBalanced, smote); //bilancio il training

                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error in SMOTE");

                    }
                    break;

                default:
                    logger.log(Level.SEVERE,"Error in Choose Balancing ");
                    System.exit(1);
                    break;
            }
            chooseCostSensitive(classifier, filteredClassifier, entryCopy , trainingBalanced,  testingBalanced);
        }
  }

    public static void chooseCostSensitive(AbstractClassifier classifier, FilteredClassifier filteredClassifier, WekaRecord entry , Instances training, Instances testing) {
        List<String> costSensitiveNames = Arrays.asList("No", "Sensitive Threshold", "Sensitive Learning");

        for(String costSensitiveName: costSensitiveNames) {
            CostSensitiveClassifier costSensitiveClassifier =null;
            Instances trainingCopy = new Instances(training);
            Instances testingCopy = new Instances(testing);

            WekaRecord entryCopy = new WekaRecord(MainClass.NAMEPROJECT);
            entryCopy.setNumTrainingRelease(entry.getNumTrainingRelease());
            entryCopy.setClassifierName(entry.getClassifierName());
            entryCopy.setFeatureSelection(entry.getFeatureSelection());
            entryCopy.setBalancing(entry.getBalancing());
            entryCopy.setTrainingPerc(entry.getTrainingPerc());
            entryCopy.setDefectPercTrain(entry.getDefectPercTrain());
            entryCopy.setDefectPercTest(entry.getDefectPercTest());

            switch (costSensitiveName) {
                case "No": //No cost sensitive
                    //nulla, quindi CostSensitiveClassifier=null
                    entryCopy.setSensitivity(costSensitiveName);
                    break;

                case "Sensitive Threshold": //Sensitive Threshold
                    entryCopy.setSensitivity(costSensitiveName);
                    costSensitiveClassifier = new CostSensitiveClassifier();


                    if (filteredClassifier == null){
                        costSensitiveClassifier.setClassifier(classifier);
                    }
                    else {
                        costSensitiveClassifier.setClassifier(filteredClassifier);

                    }
                    costSensitiveClassifier.setMinimizeExpectedCost(true); //preso da slide
                    costSensitiveClassifier.setCostMatrix(createCostMatrix(10.0,1.0)); //all'incontrario
                    break;

                case "Sensitive Learning": //Sensitive Learning

                    entryCopy.setSensitivity(costSensitiveName);
                    costSensitiveClassifier = new CostSensitiveClassifier();

                    if (filteredClassifier == null) {
                        costSensitiveClassifier.setClassifier(classifier);
                    }
                    else {
                        costSensitiveClassifier.setClassifier(filteredClassifier);
                    }
                    costSensitiveClassifier.setMinimizeExpectedCost(false); //preso da slide
                    costSensitiveClassifier.setCostMatrix(createCostMatrix(10.0,1.0));

                    break;

                default:
                    logger.log(Level.SEVERE,"Error in cost sensitive ");
                    System.exit(1);
                    break;

            }


            evaluateModel(classifier, filteredClassifier, entryCopy , costSensitiveClassifier, trainingCopy,  testingCopy);
            wekaRecordList.add(entryCopy);
        }
    }

   public static void evaluateModel(AbstractClassifier classifier, FilteredClassifier filteredClassifier, WekaRecord entry, CostSensitiveClassifier costSensitiveClassifier, Instances training, Instances testing)
   {
        //classifier e' sempre diverso da null
        //filteredClassifier puo' essere = null --> vuol dire che non sto usando balancing
        // costClassifier puo' essere = null --> vuol dire che non sto usando cost sensitive
        Evaluation eval;
        try
        {
            eval = new Evaluation(testing);
            if(costSensitiveClassifier == null)
            {
                evaluateWithoutCSC(eval, filteredClassifier, classifier, entry, training, testing);
            }
            else
            {
                evaluateWithCSC(eval, costSensitiveClassifier, entry, training, testing);

            }
        } catch (Exception e)
        {
            logger.log(Level.SEVERE,"Error in initializing the evaluator");

        }

    }

    public static void evaluateWithoutCSC(Evaluation eval, FilteredClassifier filteredClassifier, AbstractClassifier classifier, WekaRecord entry, Instances training, Instances testing) {

        //controllo balancing, quindi filteredClassifier
        if (filteredClassifier == null) {
            // NON STO USANDO BALANCING
            try {
                classifier.buildClassifier(training);
                eval.evaluateModel(classifier, testing); //valuto il testing, non il training. in 'eval' ho il risultato.
                addEvaluationToEntry(eval,entry);

            } catch (Exception e) {
                logger.log(Level.SEVERE,ERROR_CLASSIFIER);

            }

        }
        else {	//sto usando balancing, quindi filteredClassifier
            //STO USANDO BALANCING
            try {
                filteredClassifier.buildClassifier(training);
                eval.evaluateModel(filteredClassifier, testing);
                addEvaluationToEntry(eval,entry);
            } catch (Exception e) {
                logger.log(Level.SEVERE,ERROR_CLASSIFIER);
            }
        }
    }

    public static void evaluateWithCSC(Evaluation eval, CostSensitiveClassifier costSensitiveClassifier, WekaRecord entry, Instances training, Instances testing) {

        try {
            costSensitiveClassifier.buildClassifier(training);
            eval.evaluateModel(costSensitiveClassifier, testing);
            addEvaluationToEntry(eval,entry);

        } catch (Exception e) {
            logger.log(Level.SEVERE,ERROR_CLASSIFIER);
        }
    }

    public static int calculatePositiveInstances(Instances dataset) {

        int positiveInstances = 0;

        for(int d=0;d< dataset.size();d++) {
            if(dataset.get(d).toString(dataset.numAttributes()-1).equals("Yes")) {
                positiveInstances++;
            }
        }
        return positiveInstances;
    }


    public static void addEvaluationToEntry(Evaluation eval, WekaRecord entry) {
        entry.setPrecision(eval.precision(1));
        entry.setRecall(eval.recall(1));
        entry.setAuc(eval.areaUnderROC(1));
        entry.setKappa(eval.kappa());
        entry.setTP((int)eval.numTruePositives(1));
        entry.setTN((int)eval.numTrueNegatives(1));
        entry.setFP((int)eval.numFalsePositives(1));
        entry.setFN((int)eval.numFalseNegatives(1));

    }


    public static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) { //presa da slide
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, weightFalsePositive);
        costMatrix.setCell(0, 1, weightFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }



}




