package com.psa.capstone.be.controller;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psa.capstone.be.model.ReportFileInfo;
import com.psa.capstone.be.service.FileStorageService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final FileStorageService fileStorageService;

    public ReportController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Lists all available report files
     * 
     * @return List of report file information
     */
    @GetMapping("/files")
    public ResponseEntity<List<ReportFileInfo>> listReports() {
        List<ReportFileInfo> reports = fileStorageService.listAvailableReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Get a specific report file by filename
     * 
     * @param filename The filename of the report to download
     * @return The report file for download
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<ByteArrayResource> getReportFile(@PathVariable String filename) {
        byte[] reportData = fileStorageService.getReportFile(filename);

        ByteArrayResource resource = new ByteArrayResource(reportData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(filename)
                .build());
        headers.setContentLength(reportData.length);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    // Cleanup endpoint removed as per requirements - reports should never be
    // deleted
}
