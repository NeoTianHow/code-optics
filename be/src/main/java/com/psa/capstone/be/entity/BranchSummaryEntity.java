package com.psa.capstone.be.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "branch_summaries")
@IdClass(BranchSummaryEntity.BranchSummaryId.class)
public class BranchSummaryEntity {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Id
    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "compared_branch")
    private String comparedBranch;

    @Column(columnDefinition = "TEXT")
    private String detailedSummary;

    @Column(columnDefinition = "TEXT")
    private String conciseSummary;

    @Column(name = "last_commit_id")
    private String lastCommitId;

    @Column(name = "last_generated_at")
    private LocalDateTime lastGeneratedAt;

    @Column(name = "needs_update")
    private boolean needsUpdate;

    // Default constructor
    public BranchSummaryEntity() {
    }

    // Constructor with required fields
    public BranchSummaryEntity(Long projectId, String branchName, String detailedSummary, String conciseSummary,
            String lastCommitId) {
        this.projectId = projectId;
        this.branchName = branchName;
        this.detailedSummary = detailedSummary;
        this.conciseSummary = conciseSummary;
        this.lastCommitId = lastCommitId;
        this.lastGeneratedAt = LocalDateTime.now();
        this.needsUpdate = false;
    }

    // Composite key class
    public static class BranchSummaryId implements Serializable {
        private Long projectId;
        private String branchName;
        private String comparedBranch;

        public BranchSummaryId() {
        }

        public BranchSummaryId(Long projectId, String branchName, String comparedBranch) {
            this.projectId = projectId;
            this.branchName = branchName;
            this.comparedBranch = comparedBranch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            BranchSummaryId that = (BranchSummaryId) o;
            return Objects.equals(projectId, that.projectId) &&
                    Objects.equals(branchName, that.branchName) &&
                    Objects.equals(comparedBranch, that.comparedBranch);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectId, branchName, comparedBranch);
        }

        // Getters and Setters for ID class
        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public String getBranchName() {
            return branchName;
        }

        public void setBranchName(String branchName) {
            this.branchName = branchName;
        }

        public String getComparedBranch() {
            return comparedBranch;
        }

        public void setComparedBranch(String comparedBranch) {
            this.comparedBranch = comparedBranch;
        }
    }

    // Getters and Setters
    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getDetailedSummary() {
        return detailedSummary;
    }

    public void setDetailedSummary(String detailedSummary) {
        this.detailedSummary = detailedSummary;
    }

    public String getConciseSummary() {
        return conciseSummary;
    }

    public void setConciseSummary(String conciseSummary) {
        this.conciseSummary = conciseSummary;
    }

    public String getLastCommitId() {
        return lastCommitId;
    }

    public void setLastCommitId(String lastCommitId) {
        this.lastCommitId = lastCommitId;
    }

    public String getComparedBranch() {
        return comparedBranch;
    }

    public void setComparedBranch(String comparedBranch) {
        this.comparedBranch = comparedBranch;
    }

    public LocalDateTime getLastGeneratedAt() {
        return lastGeneratedAt;
    }

    public void setLastGeneratedAt(LocalDateTime lastGeneratedAt) {
        this.lastGeneratedAt = lastGeneratedAt;
    }

    public boolean isNeedsUpdate() {
        return needsUpdate;
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }
}