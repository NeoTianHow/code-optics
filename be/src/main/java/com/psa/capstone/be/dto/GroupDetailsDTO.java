package com.psa.capstone.be.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.psa.capstone.be.utilities.CustomLocalDateTimeSerializer;

public class GroupDetailsDTO {
    private Long id;
    private String name;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastActivityAt;

    private String mostRecentSubGroupName;

    @JsonProperty("web_url")
    private String webUrl;

    private int subgroupCount;

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

    public String getWebUrl() {
        return webUrl;
    }

    public String getMostRecentSubGroupName() {
        return mostRecentSubGroupName;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public int getSubgroupCount() {
        return subgroupCount;
    }

    public void setSubgroupCount(int subgroupCount) {
        this.subgroupCount = subgroupCount;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void setMostRecentSubGroupName(String mostRecentSubGroupName) {
        this.mostRecentSubGroupName = mostRecentSubGroupName;
    }
}