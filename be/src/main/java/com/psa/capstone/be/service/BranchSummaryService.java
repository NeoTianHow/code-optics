package com.psa.capstone.be.service;

import com.psa.capstone.be.entity.BranchSummaryEntity;
import com.psa.capstone.be.repository.BranchSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BranchSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(BranchSummaryService.class);
    private final BranchSummaryRepository branchSummaryRepository;

    public BranchSummaryService(BranchSummaryRepository branchSummaryRepository) {
        this.branchSummaryRepository = branchSummaryRepository;
    }

    /**
     * Track a branch and manage its update status based on commit ID changes.
     * Creates a new tracking entry if branch is not yet tracked.
     */
    @Transactional
    public BranchSummaryEntity trackBranch(Long projectId, String branchName, String currentCommitId,
            String comparedBranch) {
        logger.debug("Tracking branch - Project: {}, Branch: {}, Commit: {}, Compared Against: {}",
                projectId, branchName, currentCommitId, comparedBranch);

        Optional<BranchSummaryEntity> existing = branchSummaryRepository
                .findByProjectIdAndBranchNameAndComparedBranch(projectId, branchName, comparedBranch);

        if (existing.isPresent()) {
            BranchSummaryEntity summary = existing.get();
            if (!currentCommitId.equals(summary.getLastCommitId()) || summary.getDetailedSummary() == null) {
                logger.info("Commit changed for branch {} compared against {} ({}→{})",
                        branchName, comparedBranch, summary.getLastCommitId(), currentCommitId);
                summary.setLastCommitId(currentCommitId);
                summary.setNeedsUpdate(true);
                return branchSummaryRepository.save(summary);
            }
            return summary;
        } else {
            // Create new tracking entry for this comparison
            BranchSummaryEntity newSummary = new BranchSummaryEntity();
            newSummary.setProjectId(projectId);
            newSummary.setBranchName(branchName);
            newSummary.setComparedBranch(comparedBranch);
            newSummary.setLastCommitId(currentCommitId);
            newSummary.setNeedsUpdate(true);
            newSummary.setLastGeneratedAt(LocalDateTime.now());
            return branchSummaryRepository.save(newSummary);
        }
    }

    /**
     * Save or update a branch summary with new content.
     */
    @Transactional
    public BranchSummaryEntity saveSummary(
            Long projectId,
            String branchName,
            String comparedBranch,
            String detailedSummary,
            String conciseSummary,
            String commitId) {

        Optional<BranchSummaryEntity> existing = branchSummaryRepository
                .findByProjectIdAndBranchNameAndComparedBranch(projectId, branchName, comparedBranch);

        BranchSummaryEntity summaryEntity = existing.orElseGet(() -> {
            BranchSummaryEntity newEntity = new BranchSummaryEntity();
            newEntity.setProjectId(projectId);
            newEntity.setBranchName(branchName);
            newEntity.setComparedBranch(comparedBranch);
            return newEntity;
        });

        summaryEntity.setDetailedSummary(detailedSummary);
        summaryEntity.setConciseSummary(conciseSummary);
        summaryEntity.setLastCommitId(commitId);
        summaryEntity.setNeedsUpdate(false);
        summaryEntity.setLastGeneratedAt(LocalDateTime.now());

        return branchSummaryRepository.save(summaryEntity);
    }

    /**
     * Get existing branch summary if available.
     */
    public Optional<BranchSummaryEntity> getBranchSummary(Long projectId, String branchName, String comparedBranch) {
        return branchSummaryRepository.findByProjectIdAndBranchNameAndComparedBranch(projectId, branchName,
                comparedBranch);
    }
}