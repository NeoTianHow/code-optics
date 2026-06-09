package com.psa.capstone.be.repository;

import com.psa.capstone.be.entity.CustomRuleEntity;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomRuleRepository extends JpaRepository<CustomRuleEntity, Long> {

        // Find by parent group ID
        Page<CustomRuleEntity> findByParentGroupId(Long parentGroupId, Pageable pageable);

        // Find by parent group ID and search term
        @Query("SELECT r FROM CustomRuleEntity r WHERE " +
                        "r.parentGroupId = :parentGroupId AND " +
                        "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))")
        Page<CustomRuleEntity> findByParentGroupIdAndDescriptionContainingIgnoreCase(
                        @Param("parentGroupId") Long parentGroupId,
                        @Param("search") String search,
                        Pageable pageable);

        // Count by parent group ID
        long countByParentGroupId(Long parentGroupId);

        // Count by parent group ID and search term
        @Query("SELECT COUNT(r) FROM CustomRuleEntity r WHERE " +
                        "r.parentGroupId = :parentGroupId AND " +
                        "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))")
        long countByParentGroupIdAndDescriptionContainingIgnoreCase(
                        @Param("parentGroupId") Long parentGroupId,
                        @Param("search") String search);

        // Find all enabled rules for a parent group
        List<CustomRuleEntity> findByParentGroupIdAndEnabledTrue(Long parentGroupId);

        // Get all enabled rules
        List<CustomRuleEntity> findByEnabledTrue();

        // Find by parent group ID and category
        List<CustomRuleEntity> findByParentGroupIdAndCategoryAndEnabledTrue(
                        Long parentGroupId, CustomRuleEntity.RuleCategory category);

}