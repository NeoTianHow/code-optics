package com.psa.capstone.be.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.psa.capstone.be.utilities.CustomLocalDateTimeDeserializer;
import com.psa.capstone.be.utilities.CustomLocalDateTimeSerializer;

public class ProjectDetailsDTO {
    private Long id;
    @JsonProperty("name")
    private String projectName;
    private int activeBranches;
    private int staleBranches;
    private int mergedBranches;
    private int unmergedBranches;
    @JsonProperty("web_url")
    private String webUrl;
    private String mostRecentBranchName;

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastActivityAt;
    private String defaultBranch;

    public Long getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public int getActiveBranches() {
        return activeBranches;
    }

    public int getStaleBranches() {
        return staleBranches;
    }

    public int getMergedBranches() {
        return mergedBranches;
    }

    public int getUnmergedBranches() {
        return unmergedBranches;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public String getMostRecentBranchName() {
        return mostRecentBranchName;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setActiveBranches(int activeBranches) {
        this.activeBranches = activeBranches;
    }

    public void setStaleBranches(int staleBranches) {
        this.staleBranches = staleBranches;
    }

    public void setMergedBranches(int mergedBranches) {
        this.mergedBranches = mergedBranches;
    }

    public void setUnmergedBranches(int unmergedBranches) {
        this.unmergedBranches = unmergedBranches;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public void setMostRecentBranchName(String mostRecentBranchName) {
        this.mostRecentBranchName = mostRecentBranchName;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
}