package com.psa.capstone.be.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.psa.capstone.be.dto.BranchDetailsDTO;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.dto.ProjectDetailsDTO;
import com.psa.capstone.be.entity.BranchSummaryEntity;
import com.psa.capstone.be.exception.GitLabException;
import com.psa.capstone.be.model.ProjectInfo;
import com.psa.capstone.be.model.ReportData;
import com.psa.capstone.be.model.ReportStatus;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ProjectService {
        private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

        private final BranchService branchService;
        private final BranchSummaryService branchSummaryService;

        private final WebClient gitlabWebClient;
        private static final LocalDateTime THIRTY_DAYS_AGO = LocalDateTime.now().minus(30, ChronoUnit.DAYS);

        private static final Duration API_TIMEOUT = Duration.ofSeconds(10);
        private static final int CONCURRENT_REQUESTS = 20;

        private final Map<String, ReportStatus> reportStatusMap = new ConcurrentHashMap<>();
        private final Map<String, byte[]> reportStorage = new ConcurrentHashMap<>();

        private final FileStorageService fileStorageService;

        @Value("${app.reports.base-url:/api/reports/files}")
        private String reportsBaseUrl;

        public ProjectService(WebClient gitlabWebClient, BranchComparisonAIService qwenAIService,
                        BranchSummaryService branchSummaryService, BranchService branchService,
                        FileStorageService fileStorageService) {
                this.gitlabWebClient = gitlabWebClient;
                this.branchService = branchService;
                this.branchSummaryService = branchSummaryService;
                this.fileStorageService = fileStorageService;
        }

        public Mono<PaginatedResponseDTO<ProjectDetailsDTO>> getSubgroupProjects(
                        Long subgroupId,
                        int page,
                        int size,
                        String searchTerm) {
                logger.info("Fetching projects for subgroup {}, page {}, size {}, search term: {}",
                                subgroupId, page, size, searchTerm);

                return gitlabWebClient.get()
                                .uri(uriBuilder -> {
                                        uriBuilder.path("/groups/{id}/projects")
                                                        .queryParam("include_subgroups", "false")
                                                        .queryParam("archived", "false")
                                                        .queryParam("simple", "true")
                                                        .queryParam("page", page)
                                                        .queryParam("per_page", size);

                                        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                                                uriBuilder.queryParam("search", searchTerm.trim());
                                        }

                                        return uriBuilder.build(subgroupId);
                                })
                                .retrieve()
                                .toEntityList(ProjectDetailsDTO.class)
                                .flatMap(response -> {
                                        List<ProjectDetailsDTO> projects = response.getBody();
                                        String totalHeader = response.getHeaders().getFirst("X-Total");
                                        int totalItems = totalHeader != null ? Integer.parseInt(totalHeader) : 0;

                                        return Flux.fromIterable(projects)
                                                        .flatMap(project -> enrichProjectDetailsAsync(project)
                                                                        .timeout(API_TIMEOUT)
                                                                        .onErrorResume(e -> {
                                                                                logger.error("Error enriching project {}: {}",
                                                                                                project.getId(),
                                                                                                e.getMessage());
                                                                                return Mono.error(new GitLabException(
                                                                                                String.format("Failed to fetch project details for project %d",
                                                                                                                project.getId()),
                                                                                                e));
                                                                        }), CONCURRENT_REQUESTS)
                                                        .collectList()
                                                        .map(enrichedProjects -> {
                                                                // Sort by most recent activity
                                                                enrichedProjects.sort(Comparator.comparing(
                                                                                ProjectDetailsDTO::getLastActivityAt,
                                                                                Comparator.nullsLast(Comparator
                                                                                                .reverseOrder())));
                                                                return new PaginatedResponseDTO<>(enrichedProjects,
                                                                                page, size, totalItems);
                                                        });
                                })
                                .timeout(API_TIMEOUT)
                                .onErrorResume(e -> {
                                        logger.error("Error getting project details: {}", e.getMessage(), e);
                                        return Mono.error(new GitLabException("Failed to get project details", e));
                                });
        }

        private Mono<ProjectDetailsDTO> enrichProjectDetailsAsync(ProjectDetailsDTO project) {
                return gitlabWebClient.get()
                                .uri(uriBuilder -> uriBuilder.path("/projects/{id}/repository/branches")
                                                .queryParam("page", 1)
                                                .queryParam("per_page", 100)
                                                .build(project.getId()))
                                .retrieve()
                                .bodyToFlux(BranchDetailsDTO.class)
                                .collectList()
                                .flatMap(branches -> enrichBranchesWithMetrics(project, branches))
                                .timeout(API_TIMEOUT)
                                .onErrorResume(e -> {
                                        String errorMessage = String.format(
                                                        "Failed to fetch branch details for project %d",
                                                        project.getId());
                                        logger.error(errorMessage + ": {}", e.getMessage());
                                        return Mono.error(new GitLabException(errorMessage, e));
                                });
        }

        // private Mono<ProjectDetailsDTO> enrichBranchesWithMetrics(ProjectDetailsDTO
        // project,
        // List<BranchDetailsDTO> branches) {

        // logger.info("default branch name is {}", project.getDefaultBranch());
        // return Flux.fromIterable(branches)
        // .parallel(CONCURRENT_REQUESTS)
        // .runOn(Schedulers.boundedElastic())
        // .map(branch -> {
        // // Check if branch is active
        // if (branch.getCommit() != null
        // && branch.getCommit().getCommittedDate() != null) {
        // LocalDateTime commitDate = branch.getCommit().getCommittedDate();
        // boolean isActive = commitDate.isAfter(THIRTY_DAYS_AGO);
        // branch.setActive(isActive);
        // }

        // // Set merge status
        // if (branch.getDefault()) {
        // branch.setMerged(true);
        // } else {
        // // Check merge status against default branch
        // branch.setMerged(branchService.isBranchMerged(
        // project.getId(),
        // branch.getBranchName(),
        // project.getDefaultBranch()));
        // }

        // return branch;
        // })
        // .sequential()
        // .collectList()
        // .map(enrichedBranches -> {
        // updateProjectMetrics(project, enrichedBranches);
        // return project;
        // });
        // }

        private Mono<ProjectDetailsDTO> enrichBranchesWithMetrics(ProjectDetailsDTO project,
                        List<BranchDetailsDTO> branches) {

                logger.info("default branch name is {}", project.getDefaultBranch());

                return Flux.fromIterable(branches)
                                .flatMap(branch -> {
                                        // Check if branch is active
                                        if (branch.getCommit() != null
                                                        && branch.getCommit().getCommittedDate() != null) {
                                                LocalDateTime commitDate = branch.getCommit().getCommittedDate();
                                                boolean isActive = commitDate.isAfter(THIRTY_DAYS_AGO);
                                                branch.setActive(isActive);
                                        }

                                        // Set merge status
                                        if (branch.getDefault()) {
                                                branch.setMerged(true);
                                                return Mono.just(branch);
                                        } else {
                                                // Check merge status against default branch - this now returns
                                                // Mono<Boolean>
                                                return branchService.isBranchMerged(
                                                                project.getId(),
                                                                branch.getBranchName(),
                                                                project.getDefaultBranch())
                                                                .map(isMerged -> {
                                                                        branch.setMerged(isMerged);
                                                                        return branch;
                                                                });
                                        }
                                }, CONCURRENT_REQUESTS) // Process up to CONCURRENT_REQUESTS branches in parallel
                                .collectList()
                                .map(enrichedBranches -> {
                                        updateProjectMetrics(project, enrichedBranches);
                                        return project;
                                });
        }

        private void updateProjectMetrics(ProjectDetailsDTO project, List<BranchDetailsDTO> enrichedBranches) {
                int staleBranchCount = 0;
                int mergedBranchCount = 0;
                LocalDateTime mostRecentCommitDate = null;
                String mostRecentBranchName = null;

                for (BranchDetailsDTO branch : enrichedBranches) {
                        // Check stale/active status
                        if (branch.getCommit() != null && branch.getCommit().getCommittedDate() != null) {
                                LocalDateTime commitDate = branch.getCommit().getCommittedDate();

                                // Update most recent commit information
                                if (mostRecentCommitDate == null || commitDate.isAfter(mostRecentCommitDate)) {
                                        mostRecentCommitDate = commitDate;
                                        mostRecentBranchName = branch.getBranchName();
                                }

                                // Check if branch is stale
                                if (!branch.getActive()) {
                                        staleBranchCount++;
                                }
                        }

                        // Count merged branches
                        if (branch.getMerged()) {
                                mergedBranchCount++;
                        }
                }

                int totalBranchCount = enrichedBranches.size();

                // Set all metrics
                project.setStaleBranches(staleBranchCount);
                project.setActiveBranches(totalBranchCount - staleBranchCount);
                project.setMergedBranches(mergedBranchCount);
                project.setUnmergedBranches(totalBranchCount - mergedBranchCount);
                project.setLastActivityAt(mostRecentCommitDate);
                project.setMostRecentBranchName(mostRecentBranchName);
        }

        @Async
        public String initiateReportGeneration(Map<Long, ProjectInfo> projectInfoMap) {
                logger.info("Initiating report generation for {} projects", projectInfoMap.size());
                String reportId = UUID.randomUUID().toString();
                ReportStatus status = new ReportStatus(reportId, "PENDING", LocalDateTime.now());
                reportStatusMap.put(reportId, status);

                CompletableFuture.runAsync(() -> {
                        try {
                                status.setStatus("PROCESSING");
                                status.setCurrentStep("Collecting branch data");
                                status.setProgress(10);

                                List<BranchDetailsDTO> allBranchDetails = new ArrayList<>();
                                int projectCount = projectInfoMap.size();
                                int currentProject = 0;

                                for (Map.Entry<Long, ProjectInfo> entry : projectInfoMap.entrySet()) {
                                        Long projectId = entry.getKey();
                                        ProjectInfo projectInfo = entry.getValue();
                                        String projectName = projectInfo.getName();
                                        String defaultBranchName = projectInfo.getDefaultBranch();

                                        currentProject++;
                                        status.setCurrentStep("Processing project " + currentProject + "/"
                                                        + projectCount + ": " + projectName);
                                        status.setProgress(10 + (60 * currentProject / projectCount));

                                        // Get all branches for project - use block() within this async context
                                        List<BranchDetailsDTO> branches = branchService
                                                        .getAllProjectBranches(projectId, defaultBranchName)
                                                        .block(); // Safe to block here as we're already in an async
                                                                  // thread

                                        if (branches == null) {
                                                logger.warn("No branches found for project {}", projectId);
                                                continue;
                                        }

                                        // Filter for unmerged branches
                                        List<BranchDetailsDTO> unmergedBranches = branches.stream()
                                                        .filter(branch -> !branch.getMerged())
                                                        .collect(Collectors.toList());

                                        // Set project name for each branch
                                        unmergedBranches.forEach(branch -> branch.setProjectName(projectName));

                                        // Process each unmerged branch
                                        for (BranchDetailsDTO branch : unmergedBranches) {
                                                // Check for existing summary in database
                                                Optional<BranchSummaryEntity> existingSummary = branchSummaryService
                                                                .getBranchSummary(
                                                                                projectId, branch.getBranchName(),
                                                                                defaultBranchName);

                                                if (existingSummary.isPresent()
                                                                && existingSummary.get().getConciseSummary() != null) {
                                                        BranchSummaryEntity summaryEntity = existingSummary.get();
                                                        branch.setConciseSummary(summaryEntity.getConciseSummary());
                                                } else {
                                                        // Generate new summaries - use block() within this async
                                                        // context
                                                        String conciseSummary = branchService.generateBranchSummary(
                                                                        projectId, branch.getCommit().getId(),
                                                                        defaultBranchName, branch.getBranchName(), true)
                                                                        .block(); // Safe to block here as we're already
                                                                                  // in an async thread

                                                        branch.setConciseSummary(conciseSummary);
                                                }
                                                allBranchDetails.add(branch);
                                        }
                                }

                                // Generate PDF report with only unmerged branches
                                status.setCurrentStep("Generating PDF report");
                                status.setProgress(80);
                                byte[] pdfContent = generateReport(allBranchDetails);

                                // Save to file system
                                status.setCurrentStep("Saving report to file system");
                                status.setProgress(90);
                                LocalDateTime now = LocalDateTime.now();
                                String filename = fileStorageService.saveReport(reportId, pdfContent, now);
                                String downloadUrl = reportsBaseUrl + "/" + filename;

                                status.setStatus("COMPLETED");
                                status.setCompletedAt(now);
                                status.setFilename(filename);
                                status.setDownloadUrl(downloadUrl);
                                status.setProgress(100);
                                status.setCurrentStep("Report completed");

                        } catch (Exception e) {
                                logger.error("Error generating report: {}", e.getMessage(), e);
                                status.setStatus("FAILED");
                                status.setError(e.getMessage());
                                status.setCompletedAt(LocalDateTime.now());
                                status.setProgress(0);
                                status.setCurrentStep("Report generation failed");
                        }
                });

                return reportId;
        }

        // @Async
        // public String initiateReportGeneration(Map<Long, ProjectInfo> projectInfoMap)
        // {
        // logger.info("Initiating report generation for {} projects",
        // projectInfoMap.size());
        // String reportId = UUID.randomUUID().toString();
        // ReportStatus status = new ReportStatus(reportId, "PENDING",
        // LocalDateTime.now());
        // reportStatusMap.put(reportId, status);

        // CompletableFuture.runAsync(() -> {
        // try {
        // status.setStatus("PROCESSING");
        // status.setCurrentStep("Collecting branch data");
        // status.setProgress(10);

        // List<BranchDetailsDTO> allBranchDetails = new ArrayList<>();
        // int projectCount = projectInfoMap.size();
        // int currentProject = 0;

        // for (Map.Entry<Long, ProjectInfo> entry : projectInfoMap.entrySet()) {
        // Long projectId = entry.getKey();
        // ProjectInfo projectInfo = entry.getValue();
        // String projectName = projectInfo.getName();
        // String defaultBranchName = projectInfo.getDefaultBranch();

        // currentProject++;
        // status.setCurrentStep("Processing project " + currentProject + "/"
        // + projectCount + ": " + projectName);
        // status.setProgress(10 + (60 * currentProject / projectCount));

        // // Get all branches for project
        // List<BranchDetailsDTO> branches =
        // branchService.getAllProjectBranches(projectId,
        // defaultBranchName);

        // // *** FILTER: KEEP ONLY UNMERGED BRANCHES (THIS IS WRONG, NEED THE
        // ISMERGED()
        // // METHOD. CHECK THROUGH ALL PROPERLY) ***
        // List<BranchDetailsDTO> unmergedBranches = branches.stream()
        // .filter(branch -> !branch.getMerged())
        // .collect(Collectors.toList());

        // // Set project name for each branch
        // unmergedBranches.forEach(branch -> branch.setProjectName(projectName));

        // // Process each unmerged branch
        // for (BranchDetailsDTO branch : unmergedBranches) {
        // // Check for existing summary in database
        // Optional<BranchSummaryEntity> existingSummary = branchSummaryService
        // .getBranchSummary(projectId, branch.getBranchName(),
        // defaultBranchName);

        // if (existingSummary.isPresent()
        // && existingSummary.get().getConciseSummary() != null) {
        // BranchSummaryEntity summaryEntity = existingSummary.get();
        // branch.setConciseSummary(summaryEntity.getConciseSummary());
        // } else {
        // // Generate new summaries
        // String conciseSummary = branchService.generateBranchSummary(
        // projectId,
        // branch.getCommit().getId(),
        // defaultBranchName,
        // branch.getBranchName(),
        // true);
        // branch.setConciseSummary(conciseSummary);
        // }
        // allBranchDetails.add(branch);
        // }
        // }

        // // Generate PDF report with only unmerged branches
        // status.setCurrentStep("Generating PDF report");
        // status.setProgress(80);
        // byte[] pdfContent = generateReport(allBranchDetails);

        // // Save to file system
        // status.setCurrentStep("Saving report to file system");
        // status.setProgress(90);
        // LocalDateTime now = LocalDateTime.now();
        // String filename = fileStorageService.saveReport(reportId, pdfContent, now);
        // String downloadUrl = reportsBaseUrl + "/" + filename;

        // status.setStatus("COMPLETED");
        // status.setCompletedAt(now);
        // status.setFilename(filename);
        // status.setDownloadUrl(downloadUrl);
        // status.setProgress(100);
        // status.setCurrentStep("Report completed");

        // } catch (Exception e) {
        // logger.error("Error generating report: {}", e.getMessage(), e);
        // status.setStatus("FAILED");
        // status.setError(e.getMessage());
        // status.setCompletedAt(LocalDateTime.now());
        // status.setProgress(0);
        // status.setCurrentStep("Report generation failed");
        // }
        // });

        // return reportId;
        // }

        public ReportStatus getReportStatus(String reportId) {
                return reportStatusMap.get(reportId);
        }

        public ReportData getReport(String reportId) {
                ReportStatus status = reportStatusMap.get(reportId);
                if (status == null || !status.getStatus().equals("COMPLETED")) {
                        throw new IllegalStateException("Report not ready or doesn't exist");
                }

                if (status.getFilename() != null && !status.getFilename().isEmpty()) {
                        // Retrieve file from storage
                        byte[] content = fileStorageService.getReportFile(status.getFilename());
                        return new ReportData(content, status.getCompletedAt(), status.getFilename(),
                                        status.getDownloadUrl());
                } else {
                        throw new IllegalStateException("Report file information not available");
                }
        }

        private byte[] generateReport(List<BranchDetailsDTO> branches) {
                // First, read all resources into memory
                byte[] templateBytes;
                byte[] logoBytes;

                // Read the template
                try (InputStream templateStream = getClass()
                                .getResourceAsStream("/templates/project_report_template.jrxml")) {
                        if (templateStream == null) {
                                throw new IllegalStateException("Report template not found");
                        }
                        templateBytes = templateStream.readAllBytes();
                } catch (IOException e) {
                        logger.error("Error reading template file: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to read report template", e);
                }

                // Read the logo
                try (InputStream logoStream = getClass().getResourceAsStream("/static/psa-logo.png")) {
                        if (logoStream != null) {
                                logoBytes = logoStream.readAllBytes();
                        } else {
                                logger.warn("Logo file not found in resources");
                                logoBytes = null;
                        }
                } catch (IOException e) {
                        logger.warn("Error reading logo file: {}", e.getMessage());
                        logoBytes = null;
                }

                try {
                        // Compile the report from the byte array
                        JasperReport jasperReport = JasperCompileManager
                                        .compileReport(new ByteArrayInputStream(templateBytes));

                        // Create the data source
                        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(branches);

                        // Set report parameters
                        Map<String, Object> parameters = new HashMap<>();
                        parameters.put("GeneratedDate",
                                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                        // Add logo if available
                        if (logoBytes != null) {
                                parameters.put("logo", new ByteArrayInputStream(logoBytes));
                        }

                        // Fill the report
                        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
                                        parameters, dataSource);

                        // Export to PDF
                        return JasperExportManager.exportReportToPdf(jasperPrint);

                } catch (JRException e) {
                        logger.error("Error generating PDF report: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to generate PDF report", e);
                }
        }
}