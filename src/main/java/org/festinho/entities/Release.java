package org.festinho.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;
public class Release {


    private Integer index;
    private LocalDateTime date;
    private String rel;
    private List<RevCommit> commitList;
    private List <JavaFile> fileList;

    public Release(Integer index, LocalDateTime date, String rel)
    {
        this.index = index;
        this.date = date;
        this.rel = rel;
        this.commitList = new ArrayList<>();
        this.fileList = new ArrayList<>();


    }

    //metodi get & set

    public String getRelease() {
        return rel;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Integer getIndex() {
        return index;
    }

    public List<RevCommit> getCommitList() {
        return commitList;
    }
    public List<JavaFile> getFileList() {
        return fileList;
    }


    //set
    public void setRelease(String release) {
        this.rel = release;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setCommitList(List<RevCommit> commitList) {
        this.commitList = commitList;
    }

    public void setFileList(List<JavaFile> fileList) {
        this.fileList = fileList;
    }



}

