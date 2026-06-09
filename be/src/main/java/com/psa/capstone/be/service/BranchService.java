package com.psa.capstone.be.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.psa.capstone.be.dto.BranchDetailsDTO;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.entity.BranchSummaryEntity;
import com.psa.capstone.be.exception.GitLabException;
import com.psa.capstone.be.exception.GitLabNotFoundException;
import com.psa.capstone.be.repository.BranchSummaryRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class BranchService {
        private static final Logger logger = LoggerFactory.getLogger(BranchService.class);

        private final WebClient gitlabWebClient;
        private final BranchComparisonAIService qwenAIService;
        private final BranchSummaryService branchSummaryService;
        private final BranchSummaryRepository branchSummaryRepository;
        private static final LocalDateTime THIRTY_DAYS_AGO = LocalDateTime.now().minus(30, ChronoUnit.DAYS);

        private static final int CONCURRENT_REQUESTS = 20;

        public BranchService(WebClient gitlabWebClient, BranchComparisonAIService qwenAIService,
                        BranchSummaryService branchSummaryService, BranchSummaryRepository branchSummaryRepository) {
                this.gitlabWebClient = gitlabWebClient;
                this.qwenAIService = qwenAIService;
                this.branchSummaryService = branchSummaryService;
                this.branchSummaryRepository = branchSummaryRepository;
        }

        // public List<BranchDetailsDTO> getAllProjectBranches(Long projectId, String
        // defaultBranchName) {
        // if (projectId == null) {
        // throw new IllegalArgumentException("Project ID cannot be null");
        // }

        // try {
        // // Fetch all branches first
        // List<BranchDetailsDTO> allBranches = gitlabWebClient.get()
        // .uri(uriBuilder -> uriBuilder.path("/projects/{id}/repository/branches")
        // .build(projectId))
        // .retrieve()
        // .onStatus(
        // status -> status.value() == 404,
        // response -> response.bodyToMono(String.class)
        // .map(body -> new GitLabNotFoundException(
        // String.format("Branches not found for project %d: %s",
        // projectId,
        // body))))
        // .bodyToFlux(BranchDetailsDTO.class)
        // .collectList()
        // .block();

        // logger.info("Total branches from GitLab for project {}: {}", projectId,
        // allBranches.size());

        // // Enrich branches with status using the provided defaultBranchName
        // List<BranchDetailsDTO> enrichedBranches =
        // enrichBranchesWithStatus(allBranches, projectId,
        // defaultBranchName)
        // .block();

        // // Sort branches by date (existing code)
        // if (enrichedBranches != null) {
        // enrichedBranches.sort(Comparator.comparing(
        // branch -> branch.getCommit() != null
        // ? branch.getCommit().getCommittedDate()
        // : LocalDateTime.MIN,
        // Comparator.reverseOrder()));
        // }

        // return enrichedBranches != null ? enrichedBranches : new ArrayList<>();
        // } catch (WebClientResponseException e) {
        // logger.error("Error fetching all branches for project {}: {}", projectId,
        // e.getMessage(), e);
        // throw new GitLabException("Failed to fetch all branches", e);
        // }
        // }

        // public PaginatedResponseDTO<BranchDetailsDTO> getProjectBranches(
        // Long projectId,
        // int page,
        // int size,
        // String searchTerm,
        // String defaultBranch) {

        // if (projectId == null) {
        // throw new IllegalArgumentException("Project ID cannot be null");
        // }

        // logger.info("Fetching branches for project {} - page: {}, size: {}, search:
        // {}, default branch: {}",
        // projectId, page, size, searchTerm, defaultBranch);

        // try {
        // // Use GitLab's pagination instead of retrieving all branches
        // return gitlabWebClient.get()
        // .uri(uriBuilder -> {
        // uriBuilder.path("/projects/{id}/repository/branches")
        // .queryParam("page", page)
        // .queryParam("per_page", size);

        // if (searchTerm != null && !searchTerm.trim().isEmpty()) {
        // uriBuilder.queryParam("search", searchTerm.trim());
        // }

        // return uriBuilder.build(projectId);
        // })
        // .retrieve()
        // .toEntityList(BranchDetailsDTO.class)
        // .flatMap(response -> {
        // List<BranchDetailsDTO> branches = response.getBody();
        // String totalHeader = response.getHeaders().getFirst("X-Total");
        // int totalItems = totalHeader != null ? Integer.parseInt(totalHeader)
        // : 0;

        // logger.debug("Retrieved {} branches for project {}, total: {}",
        // branches.size(), projectId, totalItems);

        // List<Mono<BranchDetailsDTO>> enrichedBranches = branches.stream()
        // .map(branch -> enrichBranch(branch, projectId,
        // defaultBranch))
        // .collect(Collectors.toList());

        // return Flux.fromIterable(enrichedBranches)
        // .flatMap(mono -> mono, CONCURRENT_REQUESTS)
        // .collectList()
        // .map(enrichedBranchesList -> {
        // // Sort by commit date in descending order (most
        // // recent first)
        // enrichedBranchesList.sort(Comparator.comparing(
        // branch -> branch.getCommit() != null
        // ? branch.getCommit()
        // .getCommittedDate()
        // : LocalDateTime.MIN,
        // Comparator.reverseOrder()));

        // return new PaginatedResponseDTO<>(
        // enrichedBranchesList, page,
        // size, totalItems);
        // });
        // })
        // .block(); // Necessary for current method signature

        // } catch (WebClientResponseException e) {
        // logger.error("Error fetching branches for project {}: {}", projectId,
        // e.getMessage(), e);
        // throw new GitLabException("Failed to fetch branches", e);
        // }
        // }

        public Mono<List<BranchDetailsDTO>> getAllProjectBranches(Long projectId, String defaultBranchName) {
                if (projectId == null) {
                        return Mono.error(new IllegalArgumentException("Project ID cannot be null"));
                }

                // Fetch all branches first
                return gitlabWebClient.get()
                                .uri(uriBuilder -> uriBuilder.path("/projects/{id}/repository/branches")
                                                .build(projectId))
                                .retrieve()
                                .onStatus(
                                                status -> status.value() == 404,
                                                response -> response.bodyToMono(String.class)
                                                                .flatMap(body -> Mono.error(new GitLabNotFoundException(
                                                                                String.format("Branches not found for project %d: %s",
                                                                                                projectId, body)))))
                                .bodyToFlux(BranchDetailsDTO.class)
                                .collectList()
                                .flatMap(allBranches -> {
                                        logger.info("Total branches from GitLab for project {}: {}", projectId,
                                                        allBranches.size());

                                        // Enrich branches with status using the provided defaultBranchName
                                        return enrichBranchesWithStatus(allBranches, projectId, defaultBranchName)
                                                        .map(enrichedBranches -> {
                                                                // Sort branches by date
                                                                enrichedBranches.sort(Comparator.comparing(
                                                                                branch -> branch.getCommit() != null
                                                                                                ? branch.getCommit()
                                                                                                                .getCommittedDate()
                                                                                                : LocalDateTime.MIN,
                                                                                Comparator.reverseOrder()));
                                                                return enrichedBranches;
                                                        });
                                })
                                .onErrorResume(WebClientResponseException.class, e -> {
                                        logger.error("Error fetching all branches for project {}: {}", projectId,
                                                        e.getMessage(), e);
                                        return Mono.error(new GitLabException("Failed to fetch all branches", e));
                                });
        }

        public Mono<PaginatedResponseDTO<BranchDetailsDTO>> getProjectBranches(
                        Long projectId,
                        int page,
                        int size,
                        String defaultBranch,
                        String searchTerm) {

                if (projectId == null) {
                        return Mono.error(new IllegalArgumentException("Project ID cannot be null"));
                }

                logger.info("Fetching branches for project {} - page: {}, size: {}, search: {}, default branch: {}",
                                projectId, page, size, searchTerm, defaultBranch);

                // Use GitLab's native pagination
                return gitlabWebClient.get()
                                .uri(uriBuilder -> {
                                        uriBuilder.path("/projects/{id}/repository/branches")
                                                        .queryParam("page", page)
                                                        .queryParam("per_page", size);

                                        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                                                uriBuilder.queryParam("search", searchTerm.trim());
                                        }

                                        return uriBuilder.build(projectId);
                                })
                                .retrieve()
                                .onStatus(
                                                status -> status.value() == 404,
                                                response -> response.bodyToMono(String.class)
                                                                .flatMap(body -> Mono.error(new GitLabNotFoundException(
                                                                                String.format("Branches not found for project %d: %s",
                                                                                                projectId, body)))))
                                .toEntityList(BranchDetailsDTO.class)
                                .flatMap(response -> {
                                        List<BranchDetailsDTO> branches = response.getBody();
                                        String totalHeader = response.getHeaders().getFirst("X-Total");
                                        int totalItems = totalHeader != null ? Integer.parseInt(totalHeader) : 0;

                                        logger.debug("Retrieved {} branches for project {}, total: {}",
                                                        branches.size(), projectId, totalItems);

                                        // Use flatMap to process each branch in parallel, with concurrent limit
                                        return Flux.fromIterable(branches)
                                                        .flatMap(branch -> enrichBranch(branch, projectId,
                                                                        defaultBranch), CONCURRENT_REQUESTS)
                                                        .collectList()
                                                        .map(enrichedBranches -> {
                                                                // Sort by commit date in descending order (most recent
                                                                // first)
                                                                enrichedBranches.sort(Comparator.comparing(
                                                                                branch -> branch.getCommit() != null
                                                                                                ? branch.getCommit()
                                                                                                                .getCommittedDate()
                                                                                                : LocalDateTime.MIN,
                                                                                Comparator.reverseOrder()));

                                                                return new PaginatedResponseDTO<>(enrichedBranches,
                                                                                page, size, totalItems);
                                                        });
                                })
                                .onErrorResume(WebClientResponseException.class, e -> {
                                        logger.error("Error fetching branches for project {}: {}", projectId,
                                                        e.getMessage(), e);
                                        return Mono.error(new GitLabException("Failed to fetch branches", e));
                                });
        }

        private Mono<BranchDetailsDTO> enrichBranch(BranchDetailsDTO branch, Long projectId, String defaultBranch) {
                // Check if the branch is active based on commit date
                if (branch.getCommit() != null && branch.getCommit().getCommittedDate() != null) {
                        boolean isActive = branch.getCommit().getCommittedDate().isAfter(THIRTY_DAYS_AGO);
                        branch.setActive(isActive);
                }

                // Set merge status for branch
                Mono<BranchDetailsDTO> enrichedBranch;
                if (branch.getDefault()) {
                        // Default branch is always considered merged
                        branch.setMerged(true);
                        enrichedBranch = Mono.just(branch);
                } else {
                        // Check if this branch is merged into the default branch
                        enrichedBranch = isBranchMerged(projectId, branch.getBranchName(), defaultBranch)
                                        .doOnNext(isMerged -> {
                                                logger.info("isBranchMerged: {} -> {}, the result is {}",
                                                                branch.getBranchName(), defaultBranch, isMerged);
                                                branch.setMerged(isMerged);
                                        })
                                        .thenReturn(branch);
                }

                // Handle summary tracking for the branch
                return enrichedBranch.flatMap(enrichedBr -> {
                        if (enrichedBr.getCommit() != null) {
                                String currentCommitId = enrichedBr.getCommit().getId();
                                // Convert this to reactive if branchSummaryService.trackBranch is blocking
                                return Mono.fromCallable(() -> {
                                        BranchSummaryEntity summaryEntity = branchSummaryService.trackBranch(
                                                        projectId,
                                                        enrichedBr.getBranchName(),
                                                        currentCommitId,
                                                        defaultBranch);
                                        enrichedBr.setDetailedSummary(summaryEntity.getDetailedSummary());
                                        enrichedBr.setNeedsUpdate(summaryEntity.isNeedsUpdate());
                                        return enrichedBr;
                                }).subscribeOn(Schedulers.boundedElastic());
                        }
                        return Mono.just(enrichedBr);
                });
        }

        private Mono<List<BranchDetailsDTO>> enrichBranchesWithStatus(List<BranchDetailsDTO> branches, Long projectId,
                        String defaultBranch) {

                logger.info("default branch name: {}", defaultBranch);

                return Flux.fromIterable(branches)
                                .flatMap(branch -> enrichBranch(branch, projectId, defaultBranch), CONCURRENT_REQUESTS)
                                .collectList();
        }

        // private Mono<BranchDetailsDTO> enrichBranch(BranchDetailsDTO branch, Long
        // projectId, String defaultBranch) {
        // logger.info("HELLO default branch is {}", defaultBranch);
        // // Check if the branch is active based on commit date
        // if (branch.getCommit() != null && branch.getCommit().getCommittedDate() !=
        // null) {
        // boolean isActive =
        // branch.getCommit().getCommittedDate().isAfter(THIRTY_DAYS_AGO);
        // branch.setActive(isActive);
        // }

        // // Set merge status
        // if (branch.getDefault()) {
        // branch.setMerged(true);
        // } else {
        // // Check if this branch is merged into the default branch
        // branch.setMerged(isBranchMerged(projectId, branch.getBranchName(),
        // defaultBranch));
        // }

        // // Handle summary tracking
        // if (branch.getCommit() != null) {
        // String currentCommitId = branch.getCommit().getId();
        // BranchSummaryEntity summaryEntity = branchSummaryService.trackBranch(
        // projectId,
        // branch.getBranchName(),
        // currentCommitId,
        // defaultBranch);
        // branch.setDetailedSummary(summaryEntity.getDetailedSummary());
        // branch.setNeedsUpdate(summaryEntity.isNeedsUpdate());
        // }

        // return Mono.just(branch);
        // }

        // private Mono<List<BranchDetailsDTO>>
        // enrichBranchesWithStatus(List<BranchDetailsDTO> branches, Long projectId,
        // String defaultBranch) {

        // logger.info("default branch name: {}", defaultBranch);
        // return Flux.fromIterable(branches)
        // .parallel(CONCURRENT_REQUESTS) // Process multiple branches in parallel
        // .runOn(Schedulers.boundedElastic())
        // .map(branch -> {
        // // Check if the branch is active based on commit date
        // if (branch.getCommit() != null
        // && branch.getCommit().getCommittedDate() != null) {
        // boolean isActive = branch.getCommit().getCommittedDate()
        // .isAfter(THIRTY_DAYS_AGO);
        // branch.setActive(isActive);
        // }

        // // Set merge status for all branches against default branch
        // if (branch.getDefault()) {
        // branch.setMerged(true);
        // } else {
        // logger.info("isBranchMerged: {} -> {}, the result is {}",
        // branch.getBranchName(), defaultBranch,
        // isBranchMerged(projectId, branch.getBranchName(),
        // defaultBranch));
        // // Check if this branch is merged into the default branch
        // branch.setMerged(isBranchMerged(projectId, branch.getBranchName(),
        // defaultBranch));

        // }

        // // Handle summary tracking for all branches
        // if (branch.getCommit() != null) {
        // String currentCommitId = branch.getCommit().getId();
        // BranchSummaryEntity summaryEntity = branchSummaryService.trackBranch(
        // projectId,
        // branch.getBranchName(),
        // currentCommitId,
        // defaultBranch);
        // branch.setDetailedSummary(summaryEntity.getDetailedSummary());
        // branch.setNeedsUpdate(summaryEntity.isNeedsUpdate());
        // }

        // return branch;
        // })
        // .sequential() // Convert back to sequential flux
        // .collectList();
        // }

        // public String generateBranchSummary(Long projectId, String commitId, String
        // fromBranch, String toBranch,
        // boolean concise) {
        // if (projectId == null) {
        // throw new IllegalArgumentException("Project ID cannot be null");
        // }
        // if (commitId == null) {
        // throw new IllegalArgumentException("Commit ID cannot be null");
        // }
        // if (fromBranch == null || fromBranch.trim().isEmpty()) {
        // throw new IllegalArgumentException("Source branch cannot be null or empty");
        // }
        // if (toBranch == null || toBranch.trim().isEmpty()) {
        // throw new IllegalArgumentException("Target branch cannot be null or empty");
        // }

        // JsonNode compareResult = fetchCompareResults(projectId, fromBranch,
        // toBranch);

        // if (!hasChanges(compareResult)) {
        // String summary = "No changes found between branch '" + toBranch + "' and '" +
        // fromBranch + "'.";
        // branchSummaryService.saveSummary(projectId, toBranch, fromBranch, summary,
        // summary, commitId);
        // return summary;
        // }

        // // Generate and save summary
        // String fullDiff = buildFullDiff(compareResult, fromBranch, toBranch);
        // String commitMessages = buildCommitMessages(compareResult);

        // String detailedSummary = qwenAIService.generateSummaryWithChunking(fullDiff,
        // commitMessages, fromBranch,
        // toBranch,
        // false);

        // String conciseSummary = qwenAIService.generateSummaryWithChunking(
        // detailedSummary, commitMessages, fromBranch, toBranch, true);

        // branchSummaryService.saveSummary(
        // projectId,
        // toBranch, // The branch being summarized
        // fromBranch, // The branch it's being compared against
        // detailedSummary,
        // conciseSummary,
        // commitId);

        // return concise ? conciseSummary : detailedSummary;
        // }

        // Update getAllProjectBranches to properly handle reactive streams
        // public Mono<List<BranchDetailsDTO>> getAllProjectBranches(Long projectId,
        // String defaultBranchName) {
        // if (projectId == null) {
        // return Mono.error(new IllegalArgumentException("Project ID cannot be null"));
        // }

        // // Fetch all branches first
        // return gitlabWebClient.get()
        // .uri(uriBuilder -> uriBuilder.path("/projects/{id}/repository/branches")
        // .build(projectId))
        // .retrieve()
        // .onStatus(
        // status -> status.value() == 404,
        // response -> response.bodyToMono(String.class)
        // .flatMap(body -> Mono.error(new GitLabNotFoundException(
        // String.format("Branches not found for project %d: %s",
        // projectId, body)))))
        // .bodyToFlux(BranchDetailsDTO.class)
        // .collectList()
        // .flatMap(allBranches -> {
        // logger.info("Total branches from GitLab for project {}: {}", projectId,
        // allBranches.size());

        // // Enrich branches with status using the provided defaultBranchName
        // return enrichBranchesWithStatus(allBranches, projectId, defaultBranchName)
        // .map(enrichedBranches -> {
        // // Sort branches by date
        // enrichedBranches.sort(Comparator.comparing(
        // branch -> branch.getCommit() != null
        // ? branch.getCommit()
        // .getCommittedDate()
        // : LocalDateTime.MIN,
        // Comparator.reverseOrder()));
        // return enrichedBranches;
        // });
        // })
        // .onErrorResume(WebClientResponseException.class, e -> {
        // logger.error("Error fetching all branches for project {}: {}", projectId,
        // e.getMessage(), e);
        // return Mono.error(new GitLabException("Failed to fetch all branches", e));
        // });
        // }

        // Update fetchCompareResults to properly handle the reactive isBranchMerged
        public Mono<JsonNode> fetchCompareResults(Long projectId, String fromBranch, String toBranch) {
                return isBranchMerged(projectId, toBranch, fromBranch)
                                .flatMap(isMerged -> {
                                        if (isMerged) {
                                                return Mono.empty(); // Return empty since previous implementation
                                                                     // returned null
                                        }

                                        return gitlabWebClient.get()
                                                        .uri(uriBuilder -> uriBuilder
                                                                        .path("/projects/{id}/repository/compare")
                                                                        .queryParam("from", fromBranch)
                                                                        .queryParam("to", toBranch)
                                                                        .build(projectId))
                                                        .retrieve()
                                                        .onStatus(
                                                                        status -> status.value() == 404,
                                                                        response -> response.bodyToMono(String.class)
                                                                                        .flatMap(body -> Mono.error(
                                                                                                        new GitLabNotFoundException(
                                                                                                                        "Comparison not found: "
                                                                                                                                        + body))))
                                                        .onStatus(
                                                                        status -> status.is5xxServerError(),
                                                                        response -> response.bodyToMono(String.class)
                                                                                        .flatMap(body -> Mono.error(
                                                                                                        new GitLabException(
                                                                                                                        "GitLab server error comparing branches: "
                                                                                                                                        + body))))
                                                        .bodyToMono(JsonNode.class);
                                })
                                .onErrorResume(WebClientResponseException.class, e -> {
                                        logger.error("Error comparing branches for project {}: {}", projectId,
                                                        e.getMessage(), e);
                                        return Mono.error(new GitLabException("Failed to compare branches", e));
                                });
        }

        // Update generateBranchSummary to use the reactive fetchCompareResults
        public Mono<String> generateBranchSummary(Long projectId, String commitId, String fromBranch, String toBranch,
                        boolean concise) {
                if (projectId == null) {
                        return Mono.error(new IllegalArgumentException("Project ID cannot be null"));
                }
                if (commitId == null) {
                        return Mono.error(new IllegalArgumentException("Commit ID cannot be null"));
                }
                if (fromBranch == null || fromBranch.trim().isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Source branch cannot be null or empty"));
                }
                if (toBranch == null || toBranch.trim().isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Target branch cannot be null or empty"));
                }

                Optional<BranchSummaryEntity> existingSummary = branchSummaryService.getBranchSummary(projectId,
                                toBranch, fromBranch);
                if (existingSummary.isPresent()) {
                        BranchSummaryEntity summary = existingSummary.get();
                        String cachedSummary = concise ? summary.getConciseSummary() : summary.getDetailedSummary();
                        if (!summary.isNeedsUpdate()
                                        && commitId.equals(summary.getLastCommitId())
                                        && cachedSummary != null
                                        && !cachedSummary.isBlank()) {
                                return Mono.just(cachedSummary);
                        }
                }

                return fetchCompareResults(projectId, fromBranch, toBranch)
                                .flatMap(compareResult -> {
                                        if (!hasChanges(compareResult)) {
                                                String summary = "No changes found between branch '" + toBranch
                                                                + "' and '" + fromBranch + "'.";

                                                // This is a blocking call, wrap it in subscribeOn
                                                return Mono.fromCallable(() -> {
                                                        branchSummaryService.saveSummary(projectId, toBranch,
                                                                        fromBranch, summary, summary, commitId);
                                                        return summary;
                                                }).subscribeOn(Schedulers.boundedElastic());
                                        }

                                        // These operations can be wrapped in a single subscribeOn since they may be CPU
                                        // intensive
                                        return Mono.fromCallable(() -> {
                                                // Generate and save summary
                                                String fullDiff = buildFullDiff(compareResult, fromBranch, toBranch);
                                                String commitMessages = buildCommitMessages(compareResult);

                                                String detailedSummary = qwenAIService.generateSummaryWithChunking(
                                                                fullDiff, commitMessages, fromBranch,
                                                                toBranch, false);

                                                String conciseSummary = qwenAIService.generateSummaryWithChunking(
                                                                detailedSummary, commitMessages, fromBranch, toBranch,
                                                                true);

                                                branchSummaryService.saveSummary(
                                                                projectId,
                                                                toBranch, // The branch being summarized
                                                                fromBranch, // The branch it's being compared against
                                                                detailedSummary,
                                                                conciseSummary,
                                                                commitId);

                                                return concise ? conciseSummary : detailedSummary;
                                        }).subscribeOn(Schedulers.boundedElastic());
                                })
                                .switchIfEmpty(Mono.fromCallable(() -> {
                                        String summary = "No changes found between branch '" + toBranch + "' and '"
                                                        + fromBranch + "'.";
                                        branchSummaryService.saveSummary(projectId, toBranch, fromBranch, summary,
                                                        summary, commitId);
                                        return summary;
                                }).subscribeOn(Schedulers.boundedElastic()));
        }

        private boolean hasChanges(JsonNode compareResult) {
                return compareResult != null &&
                                compareResult.has("diffs") &&
                                compareResult.get("diffs").size() > 0;
        }

        // public JsonNode fetchCompareResults(Long projectId, String fromBranch, String
        // toBranch) {
        // try {

        // // First check if the branch is merged
        // boolean isMerged = isBranchMerged(projectId, toBranch, fromBranch);

        // if (isMerged) {
        // return null;
        // }

        // return gitlabWebClient.get()
        // .uri(uriBuilder -> uriBuilder.path("/projects/{id}/repository/compare")
        // .queryParam("from", fromBranch)
        // .queryParam("to", toBranch)
        // .build(projectId))
        // .retrieve()
        // .onStatus(
        // status -> status.value() == 404,
        // response -> response.bodyToMono(String.class)
        // .map(body -> new GitLabNotFoundException(
        // "Comparison not found: "
        // + body)))
        // .onStatus(
        // status -> status.is5xxServerError(),
        // response -> response.bodyToMono(String.class)
        // .map(body -> new GitLabException(
        // "GitLab server error comparing branches: "
        // + body)))
        // .bodyToMono(JsonNode.class)
        // .block();

        // } catch (WebClientResponseException e) {
        // logger.error("Error comparing branches for project {}: {}", projectId,
        // e.getMessage(), e);
        // throw new GitLabException("Failed to compare branches", e);
        // }
        // }

        // public Mono<JsonNode> fetchCompareResults(Long projectId, String fromBranch,
        // String toBranch) {
        // return isBranchMerged(projectId, toBranch, fromBranch)
        // .flatMap(isMerged -> {
        // if (isMerged) {
        // return Mono.empty(); // Return empty since previous implementation
        // // returned null
        // }

        // return gitlabWebClient.get()
        // .uri(uriBuilder -> uriBuilder
        // .path("/projects/{id}/repository/compare")
        // .queryParam("from", fromBranch)
        // .queryParam("to", toBranch)
        // .build(projectId))
        // .retrieve()
        // .onStatus(
        // status -> status.value() == 404,
        // response -> response.bodyToMono(String.class)
        // .flatMap(body -> Mono.error(
        // new GitLabNotFoundException(
        // "Comparison not found: "
        // + body))))
        // .onStatus(
        // status -> status.is5xxServerError(),
        // response -> response.bodyToMono(String.class)
        // .flatMap(body -> Mono.error(
        // new GitLabException(
        // "GitLab server error comparing branches: "
        // + body))))
        // .bodyToMono(JsonNode.class);
        // })
        // .onErrorResume(WebClientResponseException.class, e -> {
        // logger.error("Error comparing branches for project {}: {}", projectId,
        // e.getMessage(), e);
        // return Mono.error(new GitLabException("Failed to compare branches", e));
        // });
        // }

        public String buildFullDiff(JsonNode compareResult, String fromBranch, String toBranch) {
                StringBuilder diffBuilder = new StringBuilder();

                // Branch comparison header
                diffBuilder.append("Comparing branch '")
                                .append(toBranch)
                                .append("' with '")
                                .append(fromBranch)
                                .append("':\n\n");

                // Code changes
                JsonNode diffs = compareResult.get("diffs");
                for (JsonNode diff : diffs) {
                        String oldPath = diff.get("old_path").asText();
                        String newPath = diff.get("new_path").asText();
                        String diffContent = diff.get("diff").asText();

                        diffBuilder.append("File: ")
                                        .append(oldPath.equals(newPath) ? oldPath : oldPath + " → " + newPath)
                                        .append("\nChanges:\n")
                                        .append(diffContent)
                                        .append("\n\n");
                }

                return diffBuilder.toString();
        }

        private String buildCommitMessages(JsonNode compareResult) {
                StringBuilder messageBuilder = new StringBuilder("Commit Messages:\n");
                JsonNode commits = compareResult.get("commits");
                if (commits != null && commits.isArray() && commits.size() > 0) {
                        for (JsonNode commit : commits) {
                                messageBuilder.append("- ")
                                                .append(commit.get("title").asText())
                                                .append("\n");
                        }
                }
                return messageBuilder.toString();
        }

        public Mono<Map<String, Object>> checkBranchMergeStatus(Long projectId, String sourceBranch,
                        String targetBranch) {
                return isBranchMerged(projectId, sourceBranch, targetBranch)
                                .flatMap(isMerged -> {
                                        Map<String, Object> result = new HashMap<>();
                                        logger.info("Branch merge status: {} -> {} : {}", sourceBranch, targetBranch,
                                                        isMerged);
                                        result.put("isMerged", isMerged);

                                        // Get latest commit of source branch
                                        return gitlabWebClient.get()
                                                        .uri(uriBuilder -> uriBuilder
                                                                        .path("/projects/{id}/repository/commits")
                                                                        .queryParam("ref_name", sourceBranch)
                                                                        .queryParam("per_page", "1")
                                                                        .build(projectId))
                                                        .retrieve()
                                                        .bodyToMono(JsonNode.class)
                                                        .flatMap(sourceCommits -> {
                                                                if (sourceCommits == null || !sourceCommits.isArray()
                                                                                || sourceCommits.size() == 0) {
                                                                        return Mono.error(new GitLabException(
                                                                                        "Could not fetch source branch commits"));
                                                                }

                                                                String latestCommitId = sourceCommits.get(0).get("id")
                                                                                .asText();

                                                                // This is still a blocking call, consider making
                                                                // branchSummaryRepository reactive
                                                                return Mono.fromCallable(() -> {
                                                                        // Check for existing summary for this specific
                                                                        // comparison
                                                                        Optional<BranchSummaryEntity> existingSummary = branchSummaryRepository
                                                                                        .findByProjectIdAndBranchNameAndComparedBranch(
                                                                                                        projectId,
                                                                                                        sourceBranch,
                                                                                                        targetBranch);

                                                                        boolean needsUpdate = false;

                                                                        if (existingSummary.isEmpty()) {
                                                                                // No summary exists for this
                                                                                // comparison, create a new tracking
                                                                                // entry
                                                                                branchSummaryService.trackBranch(
                                                                                                projectId, sourceBranch,
                                                                                                latestCommitId,
                                                                                                targetBranch);
                                                                                result.put("detailedSummary", null);
                                                                                needsUpdate = true;
                                                                        } else {
                                                                                BranchSummaryEntity summary = existingSummary
                                                                                                .get();
                                                                                // Check if source branch has new
                                                                                // commits since last summary
                                                                                String lastTrackedCommit = summary
                                                                                                .getLastCommitId();
                                                                                needsUpdate = !latestCommitId.equals(
                                                                                                lastTrackedCommit);
                                                                                logger.info("needs update: {}",
                                                                                                needsUpdate);

                                                                                if (needsUpdate) {
                                                                                        // Update the existing tracking
                                                                                        // entry with new commit
                                                                                        branchSummaryService
                                                                                                        .trackBranch(projectId,
                                                                                                                        sourceBranch,
                                                                                                                        latestCommitId,
                                                                                                                        targetBranch);
                                                                                }

                                                                                // Include existing summaries in
                                                                                // response
                                                                                result.put("detailedSummary", summary
                                                                                                .getDetailedSummary());
                                                                        }

                                                                        result.put("needsUpdate", needsUpdate);
                                                                        return result;
                                                                }).subscribeOn(Schedulers.boundedElastic());
                                                        });
                                })
                                .onErrorResume(WebClientResponseException.class, e -> {
                                        logger.error("Error checking merge status: {}", e.getMessage(), e);
                                        return Mono.error(new GitLabException(
                                                        "[Merge Status] Failed to check merge status", e));
                                });
        }

        // public Map<String, Object> checkBranchMergeStatus(Long projectId, String
        // sourceBranch, String targetBranch) {
        // Map<String, Object> result = new HashMap<>();

        // try {
        // // Check merge status
        // boolean isMerged = isBranchMerged(projectId, sourceBranch, targetBranch);
        // logger.info("Branch merge status: {} -> {} : {}", sourceBranch, targetBranch,
        // isMerged);
        // result.put("isMerged", isMerged);

        // // Get latest commit of source branch
        // JsonNode sourceCommits = gitlabWebClient.get()
        // .uri(uriBuilder -> uriBuilder.path("/projects/{id}/repository/commits")
        // .queryParam("ref_name", sourceBranch)
        // .queryParam("per_page", "1")
        // .build(projectId))
        // .retrieve()
        // .bodyToMono(JsonNode.class)
        // .block();

        // if (sourceCommits == null || !sourceCommits.isArray() || sourceCommits.size()
        // == 0) {
        // throw new GitLabException("Could not fetch source branch commits");
        // }

        // String latestCommitId = sourceCommits.get(0).get("id").asText();

        // // Check for existing summary for this specific comparison
        // Optional<BranchSummaryEntity> existingSummary = branchSummaryRepository
        // .findByProjectIdAndBranchNameAndComparedBranch(projectId, sourceBranch,
        // targetBranch);

        // boolean needsUpdate = false;

        // if (existingSummary.isEmpty()) {
        // // No summary exists for this comparison, create a new tracking entry
        // branchSummaryService.trackBranch(projectId, sourceBranch, latestCommitId,
        // targetBranch);
        // result.put("detailedSummary", null);
        // needsUpdate = true;
        // } else {
        // BranchSummaryEntity summary = existingSummary.get();
        // // Check if source branch has new commits since last summary
        // String lastTrackedCommit = summary.getLastCommitId();
        // needsUpdate = !latestCommitId.equals(lastTrackedCommit);
        // logger.info("needs update: {}", needsUpdate);

        // if (needsUpdate) {
        // // Update the existing tracking entry with new commit
        // branchSummaryService.trackBranch(projectId, sourceBranch, latestCommitId,
        // targetBranch);
        // }

        // // Include existing summaries in response
        // result.put("detailedSummary", summary.getDetailedSummary());
        // }

        // result.put("needsUpdate", needsUpdate);
        // return result;

        // } catch (WebClientResponseException e) {
        // logger.error("Error checking merge status: {}", e.getMessage(), e);
        // throw new GitLabException("[Merge Status] Failed to check merge status", e);
        // }
        // }

        // public boolean isBranchMerged(Long projectId, String sourceBranch, String
        // targetBranch) {
        // try {
        // logger.debug("Checking if branch '{}' is merged into '{}'", sourceBranch,
        // targetBranch);

        // // Look ONLY for merge requests that specifically merged this source branch
        // to
        // // the target branch
        // JsonNode mergeRequests = gitlabWebClient.get()
        // .uri(uriBuilder -> uriBuilder.path("/projects/{id}/merge_requests")
        // .queryParam("source_branch", sourceBranch) // Only this specific
        // // branch
        // .queryParam("target_branch", targetBranch) // To this target
        // // branch
        // .queryParam("state", "merged") // That were successfully merged
        // .build(projectId))
        // .retrieve()
        // .bodyToMono(JsonNode.class)
        // .block();

        // // If we found any merge requests matching these exact criteria, then this
        // // specific branch was merged
        // if (mergeRequests != null && mergeRequests.isArray() && mergeRequests.size()
        // > 0) {
        // logger.debug("Found {} merge requests from '{}' to '{}' in 'merged' state",
        // mergeRequests.size(), sourceBranch, targetBranch);
        // return true;
        // }

        // // If we get here, this specific branch was never merged to the target branch
        // logger.debug("No merge requests found from '{}' to '{}' in 'merged' state",
        // sourceBranch,
        // targetBranch);
        // return false;

        // } catch (WebClientResponseException e) {
        // logger.error("Error checking merge status: {}", e.getMessage(), e);
        // throw new GitLabException("[Merge Status] Failed to check merge status", e);
        // }
        // }

        public Mono<Boolean> isBranchMerged(Long projectId, String sourceBranch, String targetBranch) {
                try {
                        logger.debug("Checking if branch '{}' is merged into '{}'", sourceBranch, targetBranch);

                        // Look ONLY for merge requests that specifically merged this source branch to
                        // the target branch
                        return gitlabWebClient.get()
                                        .uri(uriBuilder -> uriBuilder.path("/projects/{id}/merge_requests")
                                                        .queryParam("source_branch", sourceBranch) // Only this specific
                                                                                                   // branch
                                                        .queryParam("target_branch", targetBranch) // To this target
                                                                                                   // branch
                                                        .queryParam("state", "merged") // That were successfully merged
                                                        .build(projectId))
                                        .retrieve()
                                        .bodyToMono(JsonNode.class)
                                        .map(mergeRequests -> {
                                                // If we found any merge requests matching these exact criteria, then
                                                // this specific branch was merged
                                                if (mergeRequests != null && mergeRequests.isArray()
                                                                && mergeRequests.size() > 0) {
                                                        logger.debug("Found {} merge requests from '{}' to '{}' in 'merged' state",
                                                                        mergeRequests.size(), sourceBranch,
                                                                        targetBranch);
                                                        return true;
                                                }

                                                // If we get here, this specific branch was never merged to the target
                                                // branch
                                                logger.debug("No merge requests found from '{}' to '{}' in 'merged' state",
                                                                sourceBranch,
                                                                targetBranch);
                                                return false;
                                        })
                                        .onErrorResume(WebClientResponseException.class, e -> {
                                                logger.error("Error checking merge status: {}", e.getMessage(), e);
                                                return Mono.error(new GitLabException(
                                                                "[Merge Status] Failed to check merge status", e));
                                        });

                } catch (Exception e) {
                        logger.error("Unexpected error checking merge status: {}", e.getMessage(), e);
                        return Mono.error(new GitLabException("[Merge Status] Failed to check merge status", e));
                }
        }
}
