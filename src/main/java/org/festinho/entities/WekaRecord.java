package org.festinho.entities;


public class WekaRecord {

    private String datasetName;
    private Integer numTrainingRelease;
    private double trainingPerc;
    private double defectPercTrain;
    private double defectPercTest;
    private String classifierName;
    private String featureSelection;
    private String balancing;
    private String sensitivity;
    private Integer tP;
    private Integer fP;
    private Integer tN;
    private Integer fN;

    private double precision;
    private double recall;
    private double auc;
    private double kappa;

    public WekaRecord(String name) {
        this.datasetName = name;
    }

    // get
    public String getDatasetName() {
        return datasetName;
    }
    public Integer getNumTrainingRelease() {
        return numTrainingRelease;
    }
    public double getTrainingPerc() {
        return trainingPerc;
    }
    public double getDefectPercTrain() {
        return defectPercTrain;
    }
    public double getDefectPercTest() {
        return defectPercTest;
    }
    public String getClassifierName() {
        return classifierName;
    }
    public String getFeatureSelection() {
        return featureSelection;
    }
    public String getBalancing() {
        return balancing;
    }
    public String getSensitivity() {
        return sensitivity;
    }
    public Integer getTP() {
        return tP;
    }
    public Integer getFP() {
        return fP;
    }
    public Integer getTN() {
        return tN;
    }
    public Integer getFN() {
        return fN;
    }
    public double getPrecision() {
        return precision;
    }
    public double getRecall() {
        return recall;
    }
    public double getAuc() {
        return auc;
    }
    public double getKappa() {
        return kappa;
    }


    //set
    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }
    public void setNumTrainingRelease(Integer numTrainingRelease) {
        this.numTrainingRelease = numTrainingRelease;
    }
    public void setTrainingPerc(double trainingPerc) {
        this.trainingPerc = trainingPerc;
    }
    public void setDefectPercTrain(double defectPercTrain) {
        this.defectPercTrain = defectPercTrain;
    }
    public void setDefectPercTest(double defectPercTest) {
        this.defectPercTest = defectPercTest;
    }
    public void setClassifierName(String classifierName) {
        this.classifierName = classifierName;
    }
    public void setFeatureSelection(String featureSelection) {
        this.featureSelection = featureSelection;
    }
    public void setBalancing(String balancing) {
        this.balancing = balancing;
    }
    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }
    public void setTP(Integer tP) {
        this.tP = tP;
    }
    public void setFP(Integer fP) {
        this.fP = fP;
    }
    public void setTN(Integer tN) {
        this.tN = tN;
    }
    public void setFN(Integer fN) {
        this.fN = fN;
    }

    public void setPrecision(double d) {
        this.precision = d;
    }
    public void setRecall(double d) {
        this.recall = d;
    }
    public void setAuc(double auc) {
        this.auc = auc;
    }
    public void setKappa(double kappa) {
        this.kappa = kappa;
    }
}