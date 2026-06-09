package com.psa.capstone.be.model;

import java.time.LocalDateTime;

public class ReportData {
    private byte[] content;
    private LocalDateTime generatedAt;
    private String filename; // New field for stored filename
    private String downloadUrl; // New field for direct download URL

    public ReportData() {
    }

    public ReportData(byte[] content, LocalDateTime generatedAt) {
        this.content = content;
        this.generatedAt = generatedAt;
    }

    public ReportData(byte[] content, LocalDateTime generatedAt, String filename, String downloadUrl) {
        this.content = content;
        this.generatedAt = generatedAt;
        this.filename = filename;
        this.downloadUrl = downloadUrl;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}