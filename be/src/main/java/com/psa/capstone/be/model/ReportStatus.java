package com.psa.capstone.be.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportStatus {
    private String reportId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String error;
    private String filename;
    private String downloadUrl;
    private int progress;
    private String currentStep;

    // Add these fields for formatted dates
    private String formattedStartTime;
    private String formattedCompletedTime;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    public ReportStatus() {
    }

    public ReportStatus(String reportId, String status, LocalDateTime startedAt) {
        this.reportId = reportId;
        this.status = status;
        this.startedAt = startedAt;
        this.progress = 0;
        this.currentStep = "Initializing";
        this.formattedStartTime = startedAt != null ? startedAt.format(DATE_FORMATTER) : null;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        // Ensure progress is between 0-100
        this.progress = Math.max(0, Math.min(100, progress));
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getFormattedStartTime() {
        return formattedStartTime;
    }

    public void setFormattedStartTime(String formattedStartTime) {
        this.formattedStartTime = formattedStartTime;
    }

    public String getFormattedCompletedTime() {
        return formattedCompletedTime;
    }

    public void setFormattedCompletedTime(String formattedCompletedTime) {
        this.formattedCompletedTime = formattedCompletedTime;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
        this.formattedStartTime = startedAt != null ? startedAt.format(DATE_FORMATTER) : null;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        this.formattedCompletedTime = completedAt != null ? completedAt.format(DATE_FORMATTER) : null;
    }

}