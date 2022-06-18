package org.festinho.entities;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ticket {

    private String id;
    private Integer fv;
    private Integer ov;
    private Integer iv;
    private List<Integer> av;
    private Integer index;

    private LocalDateTime resolutionDate;
    private LocalDateTime creationDate;

    private List<RevCommit> commitList;
    private List<String> fileList;

    //private int p;

    //costruttore

    public Ticket (String id, LocalDateTime creationDate, List<Integer> av)
    {
        this.id = id;
        this.av = av;
        this.creationDate = creationDate;
        this.commitList = new ArrayList<>();
        this.fileList = new ArrayList<>();
    }

    //get
    public String getID() {
        return id;
    }

    public List<Integer> getAV() {
        return av;
    }

    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Integer getFV() {
        return fv;
    }

    public Integer getOV() {
        return ov;
    }

    public Integer getIndex() {
        return index;
    }

    public List<String> getFileList() {
        return fileList;
    }

    public Integer getIV() {
        return iv;
    }



    public List<RevCommit> getCommitList() {
        return commitList;
    }

    //set

    public void setID(String id) {
        this.id = id;
    }

    public void setAV(List<Integer> av) {
        this.av = av;
    }

    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public void setFV(Integer fv) {
        this.fv = fv;
    }

    public void setOV(Integer ov) {
        this.ov = ov;
    }

    public void setIV(Integer iv) {
        this.iv = iv;
    }


    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setFileList(List<String> fileList) {
        this.fileList = fileList;
    }

    public void setCommitList(List<RevCommit> commitList) {
        this.commitList = commitList;
    }
}
