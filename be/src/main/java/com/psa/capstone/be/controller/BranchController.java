package com.psa.capstone.be.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.psa.capstone.be.dto.BranchDetailsDTO;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.service.BranchService;

import reactor.core.publisher.Mono;

@RestController
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping("/api/projects/{id}/branches")
    public Mono<ResponseEntity<PaginatedResponseDTO<BranchDetailsDTO>>> getBranchesForProject(
            @PathVariable Long id,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String defaultBranch,
            @RequestParam(required = false) String search) {
        if (page < 1 || size < 1) {
            return Mono.error(new IllegalArgumentException("Page number or page size must be greater than 0"));
        }
        return branchService.getProjectBranches(id, page, size, defaultBranch, search)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/api/projects/{projectId}/branches/compare/summary")
    public Mono<String> generateBranchSummary(
            @PathVariable Long projectId,
            @RequestParam String commitId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "false") boolean concise) {
        return branchService.generateBranchSummary(projectId, commitId, from, to, concise);
    }

    @GetMapping("/api/projects/{projectId}/branches/compare")
    public Mono<ResponseEntity<Map<String, Object>>> compareBranches(
            @PathVariable Long projectId,
            @RequestParam String source,
            @RequestParam String target) {
        return branchService.checkBranchMergeStatus(projectId, source, target)
                .map(ResponseEntity::ok);
    }

}
