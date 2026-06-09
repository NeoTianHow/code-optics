package com.psa.capstone.be.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.psa.capstone.be.model.ReportFileInfo;

import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String REPORT_PREFIX = "Project_Report_";
    private static final String REPORT_EXTENSION = ".pdf";

    @Value("${app.report.storage.location:./reports}")
    private String reportStorageLocation;

    @Value("${app.report.retention.days:30}")
    private int reportRetentionDays;

    @PostConstruct
    public void init() {
        try {
            Path storageDir = Paths.get(reportStorageLocation);
            Files.createDirectories(storageDir);
            logger.info("Report storage directory initialized at: {}", reportStorageLocation);
        } catch (IOException e) {
            logger.error("Failed to initialize report storage directory: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    /**
     * Save a report to the file system
     * 
     * @param reportId    The unique ID of the report
     * @param content     The PDF content as byte array
     * @param generatedAt The generation timestamp
     * @return The path to the saved file
     */
    public String saveReport(String reportId, byte[] content, LocalDateTime generatedAt) {
        try {
            // Create a filename with timestamp and report ID
            String timestamp = generatedAt.format(FILE_DATE_FORMATTER);
            String filename = REPORT_PREFIX + timestamp + "_" + reportId + REPORT_EXTENSION;

            Path filePath = Paths.get(reportStorageLocation, filename);

            // Ensure directory exists
            Files.createDirectories(filePath.getParent());

            // Write the file
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(content);
            }

            logger.info("Report saved successfully: {}", filePath);
            return filename;

        } catch (IOException e) {
            logger.error("Failed to save report {}: {}", reportId, e.getMessage(), e);
            throw new RuntimeException("Failed to save report file", e);
        }
    }

    /**
     * Retrieve a report file by its filename
     * 
     * @param filename The name of the file to retrieve
     * @return The file content as byte array
     */
    public byte[] getReportFile(String filename) {
        try {
            Path filePath = Paths.get(reportStorageLocation, filename);
            if (!Files.exists(filePath)) {
                logger.error("Report file not found: {}", filePath);
                throw new RuntimeException("Report file not found: " + filename);
            }

            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            logger.error("Failed to read report file {}: {}", filename, e.getMessage(), e);
            throw new RuntimeException("Failed to read report file", e);
        }
    }

    /**
     * List all available report files in the storage directory
     * 
     * @return List of report file information
     */
    public List<ReportFileInfo> listAvailableReports() {
        try {
            File directory = new File(reportStorageLocation);
            File[] files = directory
                    .listFiles((dir, name) -> name.startsWith(REPORT_PREFIX) && name.endsWith(REPORT_EXTENSION));

            if (files == null || files.length == 0) {
                return Collections.emptyList();
            }

            List<ReportFileInfo> fileInfoList = Arrays.stream(files)
                    .map(file -> {
                        String reportId = extractReportId(file.getName());
                        LocalDateTime generatedAt = extractTimestamp(file.getName());

                        return new ReportFileInfo(
                                reportId,
                                file.getName(),
                                generatedAt,
                                file.length(),
                                "/api/reports/files/" + file.getName());
                    })
                    .sorted(Comparator.comparing(ReportFileInfo::getGeneratedAt).reversed())
                    .collect(Collectors.toList());

            return fileInfoList;
        } catch (Exception e) {
            logger.error("Failed to list report files: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Cleanup method removed as per requirements - reports should never be deleted

    private String extractReportId(String filename) {
        // Extract reportId from filename (Project_Report_YYYYMMDD_HHMMSS_REPORTID.pdf)
        try {
            int prefixEndIndex = filename.indexOf(REPORT_PREFIX) + REPORT_PREFIX.length();
            int timestampEndIndex = filename.indexOf("_", prefixEndIndex + 15); // After timestamp
            int extensionIndex = filename.lastIndexOf(REPORT_EXTENSION);

            if (timestampEndIndex > 0 && extensionIndex > timestampEndIndex) {
                return filename.substring(timestampEndIndex + 1, extensionIndex);
            }
        } catch (Exception e) {
            logger.warn("Could not extract reportId from filename: {}", filename);
        }
        return "unknown";
    }

    private LocalDateTime extractTimestamp(String filename) {
        // Extract timestamp from filename (Project_Report_YYYYMMDD_HHMMSS_REPORTID.pdf)
        try {
            int prefixEndIndex = filename.indexOf(REPORT_PREFIX) + REPORT_PREFIX.length();
            int timestampEndIndex = filename.indexOf("_", prefixEndIndex + 15); // After timestamp

            if (timestampEndIndex > prefixEndIndex) {
                String timestamp = filename.substring(prefixEndIndex, timestampEndIndex);
                return LocalDateTime.parse(timestamp, FILE_DATE_FORMATTER);
            }
        } catch (Exception e) {
            logger.warn("Could not extract timestamp from filename: {}", filename);
        }
        return LocalDateTime.now();
    }
}