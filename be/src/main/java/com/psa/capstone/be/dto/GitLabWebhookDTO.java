package com.psa.capstone.be.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabWebhookDTO {
    @JsonProperty("object_kind")
    private String objectKind;

    @NotNull(message = "Project details are required")
    private Project project;

    @NotNull(message = "Object attributes are required")
    @JsonProperty("object_attributes")
    private ObjectAttributes objectAttributes;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Project {
        @NotNull(message = "Project ID is required")
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObjectAttributes {
        private String action;

        @JsonProperty("source_branch")
        private String sourceBranch;

        @JsonProperty("target_branch")
        private String targetBranch;

        private Long iid;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getSourceBranch() {
            return sourceBranch;
        }

        public void setSourceBranch(String sourceBranch) {
            this.sourceBranch = sourceBranch;
        }

        public String getTargetBranch() {
            return targetBranch;
        }

        public void setTargetBranch(String targetBranch) {
            this.targetBranch = targetBranch;
        }

        public Long getIid() {
            return iid;
        }

        public void setIid(Long iid) {
            this.iid = iid;
        }
    }

    public String getObjectKind() {
        return objectKind;
    }

    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ObjectAttributes getObjectAttributes() {
        return objectAttributes;
    }

    public void setObjectAttributes(ObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }
}