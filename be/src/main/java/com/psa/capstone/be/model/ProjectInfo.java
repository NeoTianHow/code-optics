package com.psa.capstone.be.model;

public class ProjectInfo {
    private String name;
    private String defaultBranch;

    // Add a default constructor for Jackson deserialization
    public ProjectInfo() {
        // Default constructor is required for Jackson
    }

    public ProjectInfo(String name, String defaultBranch) {
        this.name = name;
        this.defaultBranch = defaultBranch;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
}