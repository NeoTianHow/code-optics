package com.psa.capstone.be.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.psa.capstone.be.utilities.CustomLocalDateTimeSerializer;

public class SubgroupDetailsDTO {
    private Long id;
    private String name;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastActivityAt;

    private String mostRecentProjectName;
    private int projectCount;

    @JsonProperty("web_url")
    private String webUrl;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public String getMostRecentProjectName() {
        return mostRecentProjectName;
    }

    public void setMostRecentProjectName(String mostRecentProjectName) {
        this.mostRecentProjectName = mostRecentProjectName;
    }

    public int getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(int projectCount) {
        this.projectCount = projectCount;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
}