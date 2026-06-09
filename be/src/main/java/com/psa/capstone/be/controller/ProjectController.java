package com.psa.capstone.be.controller;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.dto.ProjectDetailsDTO;
import com.psa.capstone.be.model.ProjectInfo;
import com.psa.capstone.be.model.ReportData;
import com.psa.capstone.be.model.ReportStatus;
import com.psa.capstone.be.service.ProjectService;

import reactor.core.publisher.Mono;

@RestController
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/api/groups/subgroups/{subgroupId}/projects")
    public Mono<PaginatedResponseDTO<ProjectDetailsDTO>> getSubgroupProjects(
            @PathVariable Long subgroupId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String search) {
        if (page < 1 || size < 1) {
            return Mono.error(new IllegalArgumentException("Page number or page size must be greater than 0"));
        }
        return projectService.getSubgroupProjects(subgroupId, page, size, search);
    }

    @PostMapping("api/projects/report")
    public ResponseEntity<String> initiateReportGeneration(@RequestBody Map<String, ProjectInfo> projectDetails) {
        try {
            logger.info("Received request with payload: {}", projectDetails);

            // Convert string keys to Long and extract project info
            Map<Long, ProjectInfo> projectInfoMap = new HashMap<>();

            for (Map.Entry<String, ProjectInfo> entry : projectDetails.entrySet()) {
                Long projectId = Long.valueOf(entry.getKey());
                projectInfoMap.put(projectId, entry.getValue());
            }

            logger.info("Converted to project info map: {}", projectInfoMap);
            String reportId = projectService.initiateReportGeneration(projectInfoMap);

            logger.info("Generated report ID: {}", reportId);
            return ResponseEntity.ok().body(reportId);

        } catch (NumberFormatException e) {
            logger.error("Invalid project ID format: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid project ID format");
        } catch (Exception e) {
            logger.error("Error generating report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating report: " + e.getMessage());
        }
    }

    @GetMapping("/api/projects/report/{reportId}/status")
    public ResponseEntity<ReportStatus> getReportStatus(@PathVariable String reportId) {
        ReportStatus status = projectService.getReportStatus(reportId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * Endpoint to download report directly
     * Note: This is kept for backward compatibility, but new clients should use
     * the downloadUrl provided in the report status
     */
    @GetMapping("/api/projects/report/{reportId}/download")
    public ResponseEntity<ByteArrayResource> downloadReport(@PathVariable String reportId) {
        try {
            ReportData reportData = projectService.getReport(reportId);

            ByteArrayResource resource = new ByteArrayResource(reportData.getContent());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(reportData.getFilename())
                    .build());
            headers.setContentLength(reportData.getContent().length);

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (IllegalStateException e) {
            logger.error("Error downloading report: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error downloading report: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // @GetMapping("/api/projects/report")
    // public ResponseEntity<byte[]> generateReport() {
    // // Call the ProjectService to generate the report
    // byte[] reportBytes = projectService.generateReport();

    // // Set headers for file download
    // HttpHeaders headers = new HttpHeaders();
    // headers.setContentType(MediaType.APPLICATION_PDF);
    // headers.setContentDisposition(ContentDisposition.builder("attachment")
    // .filename("Repository_Branch_Report.pdf")
    // .build());

    // return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    // }
}
