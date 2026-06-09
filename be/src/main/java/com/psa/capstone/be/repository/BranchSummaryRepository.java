package com.psa.capstone.be.repository;

import com.psa.capstone.be.entity.BranchSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchSummaryRepository
        extends JpaRepository<BranchSummaryEntity, BranchSummaryEntity.BranchSummaryId> {

    Optional<BranchSummaryEntity> findByProjectIdAndBranchNameAndComparedBranch(
            Long projectId, String branchName, String comparedBranch);

    void deleteByProjectIdAndBranchName(Long projectId, String branchName);
}