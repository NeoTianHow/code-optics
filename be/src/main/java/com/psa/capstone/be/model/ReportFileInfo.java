package com.psa.capstone.be.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportFileInfo {
    private String reportId;
    private String filename;
    private LocalDateTime generatedAt;
    private long fileSize;
    private String downloadUrl;
    private String formattedGeneratedTime;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    public ReportFileInfo() {
    }

    public ReportFileInfo(String reportId, String filename, LocalDateTime generatedAt, long fileSize,
            String downloadUrl) {
        this.reportId = reportId;
        this.filename = filename;
        this.generatedAt = generatedAt;
        this.fileSize = fileSize;
        this.downloadUrl = downloadUrl;
        this.formattedGeneratedTime = generatedAt != null ? generatedAt.format(DATE_FORMATTER) : null;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
        this.formattedGeneratedTime = generatedAt != null ? generatedAt.format(DATE_FORMATTER) : null;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFormattedGeneratedTime() {
        return formattedGeneratedTime;
    }

    public void setFormattedGeneratedTime(String formattedGeneratedTime) {
        this.formattedGeneratedTime = formattedGeneratedTime;
    }
}