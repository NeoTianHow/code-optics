package com.psa.capstone.be.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.psa.capstone.be.dto.GroupDetailsDTO;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.dto.SubgroupDetailsDTO;
import com.psa.capstone.be.exception.GitLabException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class GroupService {
  private static final Duration API_TIMEOUT = Duration.ofSeconds(30);
  private final WebClient gitlabWebClient;
  private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
  private static final int CONCURRENT_REQUESTS = 15;

  public GroupService(WebClient gitlabWebClient) {
    this.gitlabWebClient = gitlabWebClient;
  }

  public Mono<PaginatedResponseDTO<GroupDetailsDTO>> getParentGroups(
      int page, int size, String searchTerm) {
    logger.info("Fetching parent groups - page: {}, size: {}, search: {}", page, size, searchTerm);
    return gitlabWebClient.get()
        .uri(uriBuilder -> {
          uriBuilder.path("/groups")
              .queryParam("top_level_only", true)
              .queryParam("page", page)
              .queryParam("per_page", size);

          if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            uriBuilder.queryParam("search", searchTerm.trim());
          }

          return uriBuilder.build();
        })
        .retrieve()
        .toEntityList(GroupDetailsDTO.class)
        .flatMap(response -> {
          List<GroupDetailsDTO> groups = response.getBody();
          String totalHeader = response.getHeaders().getFirst("X-Total");
          int totalItems = totalHeader != null ? Integer.parseInt(totalHeader) : 0;

          // Filter out group with ID 65
          List<GroupDetailsDTO> filteredGroups = groups.stream()
              .filter(group -> !Long.valueOf(65L).equals(group.getId()))
              .collect(Collectors.toList());

          // Log that we're excluding the group
          if (groups.size() > filteredGroups.size()) {
            logger.info("Filtered out group with ID: {}", 65);
          }

          List<Mono<GroupDetailsDTO>> enrichedGroups = filteredGroups.stream()
              .map(group -> enrichGroupWithCounts(group))
              .collect(Collectors.toList());

          return Flux.fromIterable(enrichedGroups)
              .flatMap(mono -> mono, CONCURRENT_REQUESTS)
              .collectList()
              .map(enrichedGroupList -> {
                // **Filter out empty Mono responses (skipped groups)**
                List<GroupDetailsDTO> validGroups = enrichedGroupList.stream()
                    .filter(group -> group != null) // Ensure only valid groups remain
                    .collect(Collectors.toList());

                // **Sort by lastActivityAt in descending order (most recent first)**
                validGroups.sort(Comparator.comparing(
                    GroupDetailsDTO::getLastActivityAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));

                return new PaginatedResponseDTO<>(validGroups, page, size, totalItems);
              });
        })
        .timeout(API_TIMEOUT)
        .doOnError(e -> logger.error("Error fetching parent groups: {}", e.getMessage()))
        // Remove the error mapping here to preserve the original error
        .onErrorMap(e -> {
          if (e instanceof GitLabException) {
            return e; // Pass through GitLabException unchanged
          }
          // Only wrap non-GitLabException errors
          return new GitLabException("Failed to fetch parent groups", e);
        });
  }

  private Mono<GroupDetailsDTO> enrichGroupWithCounts(GroupDetailsDTO group) {
    return Mono.zip(
        getSubgroupCount(group.getId()),
        getLastActivityDate(group.getId())).map(tuple -> {
          group.setSubgroupCount(tuple.getT1());
          SubGroupActivityInfo activityInfo = tuple.getT2();
          group.setLastActivityAt(activityInfo.lastActivityAt());
          group.setMostRecentSubGroupName(activityInfo.subGroupName());
          return group;
        })
        .doOnError(e -> logger.error("Error enriching group details for group {}: {}", group.getId(), e.getMessage()))
        .onErrorMap(e -> new GitLabException(
            String.format("Failed to enrich details for group %d", group.getId()), e));
  }

  private Mono<Integer> getSubgroupCount(Long groupId) {
    return gitlabWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/groups/{id}/subgroups")
            .queryParam("page", 1)
            .queryParam("per_page", 1)
            .build(groupId))
        .retrieve()
        .toEntityList(Object.class)
        .map(response -> {
          String totalHeader = response.getHeaders().getFirst("X-Total");
          if (totalHeader != null) {
            return Integer.parseInt(totalHeader);
          }
          throw new GitLabException("Missing X-Total header in GitLab response");
        })
        .doOnError(e -> logger.error("Error fetching subgroup count for group {}: {}", groupId, e.getMessage()))
        .onErrorMap(e -> new GitLabException(
            String.format("Failed to fetch subgroup count for group %d", groupId), e));
  }

  public record SubGroupActivityInfo(LocalDateTime lastActivityAt, String subGroupName) {
  }

  // Simplified getLastActivityDate method
  private Mono<SubGroupActivityInfo> getLastActivityDate(Long groupId) {
    logger.debug("Fetching last activity date for group {}", groupId);

    return gitlabWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/groups/{id}/projects")
            .queryParam("include_subgroups", true)
            .queryParam("order_by", "last_activity_at")
            .queryParam("sort", "desc")
            .queryParam("per_page", 1)
            .build(groupId))
        .retrieve()
        .bodyToMono(List.class)
        .map(projects -> {
          if (projects == null || projects.isEmpty()) {
            return new SubGroupActivityInfo(null, null);
          }

          Map<String, Object> project = (Map<String, Object>) projects.get(0);

          // Check if required fields are present
          if (!project.containsKey("last_activity_at") ||
              !project.containsKey("namespace")) {
            return new SubGroupActivityInfo(null, null);
          }

          String lastActivityStr = (String) project.get("last_activity_at");
          Map<String, Object> namespace = (Map<String, Object>) project.get("namespace");
          String subgroupName = (String) namespace.get("name");

          try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(lastActivityStr,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            LocalDateTime activityDate = zonedDateTime
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();

            return new SubGroupActivityInfo(activityDate, subgroupName);
          } catch (Exception e) {
            logger.warn("Failed to parse last_activity_at for project: {}", e.getMessage());
            return new SubGroupActivityInfo(null, null);
          }
        })
        .onErrorResume(e -> {
          logger.error("Error fetching activity date for group {}: {}", groupId, e.getMessage());
          return Mono.just(new SubGroupActivityInfo(null, null));
        });
  }

  public Mono<PaginatedResponseDTO<SubgroupDetailsDTO>> getGroupSubgroups(
      Long groupId,
      int page,
      int size,
      String searchTerm) {
    logger.debug("Fetching subgroups for group {} - page: {}, size: {}, search: {}",
        groupId, page, size, searchTerm);

    return gitlabWebClient.get()
        .uri(uriBuilder -> {
          uriBuilder.path("/groups/{groupId}/subgroups")
              .queryParam("page", page)
              .queryParam("per_page", size);

          if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            uriBuilder.queryParam("search", searchTerm.trim());
          }

          return uriBuilder.build(groupId);
        })
        .retrieve()
        .toEntityList(SubgroupDetailsDTO.class)
        .flatMap(response -> {
          List<SubgroupDetailsDTO> subgroups = response.getBody();
          String totalHeader = response.getHeaders().getFirst("X-Total");
          int totalItems = totalHeader != null ? Integer.parseInt(totalHeader) : 0;

          List<Mono<SubgroupDetailsDTO>> enrichedSubgroups = subgroups.stream()
              .map(this::enrichSubgroupWithCounts)
              .collect(Collectors.toList());

          return Flux.fromIterable(enrichedSubgroups)
              .flatMap(mono -> mono, CONCURRENT_REQUESTS)
              .collectList()
              .map(enrichedSubgroupList -> {
                // **Sort by lastActivityAt in descending order (most recent first)**
                enrichedSubgroupList.sort(Comparator.comparing(
                    SubgroupDetailsDTO::getLastActivityAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));

                return new PaginatedResponseDTO<>(enrichedSubgroupList, page, size, totalItems);
              });
        })
        .timeout(API_TIMEOUT)
        .doOnError(e -> logger.error("Error fetching subgroups: {}", e.getMessage()))
        .onErrorMap(e -> {
          if (e instanceof GitLabException) {
            return e;
          }
          return new GitLabException("Failed to fetch subgroups", e);
        });
  }

  private Mono<SubgroupDetailsDTO> enrichSubgroupWithCounts(SubgroupDetailsDTO subgroup) {
    return Mono.zip(
        getProjectCount(subgroup.getId()),
        getLastProjectActivityInfo(subgroup.getId()))
        .map(tuple -> {
          subgroup.setProjectCount(tuple.getT1());
          ProjectActivityInfo activityInfo = tuple.getT2();
          subgroup.setLastActivityAt(activityInfo.lastActivityAt());
          subgroup.setMostRecentProjectName(activityInfo.projectName());
          return subgroup;
        })
        .doOnError(e -> logger.error("Error enriching subgroup details for subgroup {}: {}",
            subgroup.getId(), e.getMessage()))
        .onErrorMap(e -> new GitLabException(
            String.format("Failed to enrich details for subgroup %d", subgroup.getId()), e));
  }

  private Mono<Integer> getProjectCount(Long groupId) {
    return gitlabWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/groups/{id}/projects")
            .queryParam("page", 1)
            .queryParam("per_page", 1)
            .queryParam("include_subgroups", true)
            .queryParam("simple", true)
            .build(groupId))
        .retrieve()
        .toEntityList(Object.class)
        .map(response -> {
          String totalHeader = response.getHeaders().getFirst("X-Total");
          if (totalHeader != null) {
            return Integer.parseInt(totalHeader);
          }
          throw new GitLabException("Missing X-Total header in GitLab response");
        })
        .doOnError(e -> logger.error("Error fetching project count for group {}: {}",
            groupId, e.getMessage()))
        .onErrorMap(e -> new GitLabException(
            String.format("Failed to fetch project count for group %d", groupId), e));
  }

  public record ProjectActivityInfo(LocalDateTime lastActivityAt, String projectName) {
  }

  private Mono<ProjectActivityInfo> getLastProjectActivityInfo(Long subgroupId) {
    logger.debug("Fetching last project activity info for subgroup {}", subgroupId);

    return gitlabWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/groups/{id}/projects")
            .queryParam("include_subgroups", false)
            .queryParam("order_by", "last_activity_at")
            .queryParam("sort", "desc")
            .queryParam("per_page", 1)
            .build(subgroupId))
        .retrieve()
        .bodyToMono(List.class)
        .map(projects -> {
          if (projects == null || projects.isEmpty()) {
            return new ProjectActivityInfo(null, null);
          }

          Map<String, Object> project = (Map<String, Object>) projects.get(0);

          // Check if required fields are present
          if (!project.containsKey("last_activity_at") ||
              !project.containsKey("name")) {
            return new ProjectActivityInfo(null, null);
          }

          String lastActivityStr = (String) project.get("last_activity_at");
          String projectName = (String) project.get("name");

          try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(lastActivityStr,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            LocalDateTime activityDate = zonedDateTime
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();

            return new ProjectActivityInfo(activityDate, projectName);
          } catch (Exception e) {
            logger.warn("Failed to parse last_activity_at for project: {}", e.getMessage());
            return new ProjectActivityInfo(null, null);
          }
        })
        .onErrorResume(e -> {
          logger.error("Error fetching project activity for subgroup {}: {}", subgroupId, e.getMessage());
          return Mono.just(new ProjectActivityInfo(null, null));
        });
  }

}