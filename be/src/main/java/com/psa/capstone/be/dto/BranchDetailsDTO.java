package com.psa.capstone.be.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.psa.capstone.be.utilities.CustomLocalDateTimeDeserializer;
import com.psa.capstone.be.utilities.CustomLocalDateTimeSerializer;

public class BranchDetailsDTO {

    @JsonProperty("name")
    private String branchName;

    @JsonProperty("web_url")
    private String webUrl;

    private boolean isDefault;

    private boolean isMerged;

    private String mergedBy;

    private String mergeStatusError;

    private boolean isActive;

    private String detailedSummary;

    private String conciseSummary;

    private boolean needsUpdate;

    private String projectName;

    private Commit commit;

    public static class Commit {
        private String id;

        @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
        @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
        private LocalDateTime committedDate;

        private String committerName;

        private String message;

        public LocalDateTime getCommittedDate() {
            return committedDate;
        }

        public void setCommittedDate(LocalDateTime commitedDate) {
            this.committedDate = commitedDate;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCommitterName() {
            return committerName;
        }

        public void setCommitterName(String committerName) {
            this.committerName = committerName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public String getBranchName() {
        return branchName;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public boolean getDefault() {
        return isDefault;
    }

    public boolean getActive() {
        return isActive;
    }

    public String getDetailedSummary() {
        return detailedSummary;
    }

    public String getConciseSummary() {
        return conciseSummary;
    }

    public Commit getCommit() {
        return commit;
    }

    public boolean getNeedsUpdate() {
        return needsUpdate;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean getMerged() {
        return isMerged;
    }

    public String getMergedBy() {
        return mergedBy;
    }

    public String getMergeStatusError() {
        return mergeStatusError;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setMerged(boolean merged) {
        isMerged = merged;
    }

    public void setMergedBy(String mergedBy) {
        this.mergedBy = mergedBy;
    }

    public void setMergeStatusError(String mergeStatusError) {
        this.mergeStatusError = mergeStatusError;
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setDetailedSummary(String detailedSummary) {
        this.detailedSummary = detailedSummary;
    }

    public void setConciseSummary(String conciseSummary) {
        this.conciseSummary = conciseSummary;
    }
}
