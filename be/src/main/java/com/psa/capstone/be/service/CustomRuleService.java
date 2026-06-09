package com.psa.capstone.be.service;

import com.psa.capstone.be.dto.CustomRuleDTO;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.entity.CustomRuleEntity;
import com.psa.capstone.be.repository.CustomRuleRepository;
import com.psa.capstone.be.exception.GitLabException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomRuleService {

    private static final Logger logger = LoggerFactory.getLogger(CustomRuleService.class);
    private final CustomRuleRepository customRuleRepository;

    public CustomRuleService(CustomRuleRepository customRuleRepository) {
        this.customRuleRepository = customRuleRepository;
    }

    public PaginatedResponseDTO<CustomRuleDTO> getRules(int page, int size, String search, Long parentGroupId) {
        logger.info("Fetching custom rules - page: {}, size: {}, search: {}, parentGroupId: {}",
                page, size, search, parentGroupId);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<CustomRuleEntity> rulePage;
        long totalItems;

        if (parentGroupId != null) {
            // Filter by parent group ID
            if (search != null && !search.trim().isEmpty()) {
                rulePage = customRuleRepository.findByParentGroupIdAndDescriptionContainingIgnoreCase(
                        parentGroupId, search.trim(), pageable);
                totalItems = customRuleRepository.countByParentGroupIdAndDescriptionContainingIgnoreCase(
                        parentGroupId, search.trim());
            } else {
                rulePage = customRuleRepository.findByParentGroupId(parentGroupId, pageable);
                totalItems = customRuleRepository.countByParentGroupId(parentGroupId);
            }
        } else {
            throw new GitLabException("Parent group ID is required");
        }

        List<CustomRuleDTO> rules = rulePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PaginatedResponseDTO<CustomRuleDTO>(
                rules,
                rulePage.getNumber() + 1, // adding 1 because page number is 0-based in Spring but 1-based in our API
                rulePage.getSize(),
                (int) totalItems);
    }

    @Transactional
    public CustomRuleDTO createRule(CustomRuleDTO ruleDTO) {
        CustomRuleEntity entity = new CustomRuleEntity();
        entity.setDescription(ruleDTO.getDescription());
        entity.setSeverity(ruleDTO.getSeverity());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setEnabled(ruleDTO.isEnabled());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setParentGroupId(ruleDTO.getParentGroupId());
        entity.setRemark(ruleDTO.getRemark());
        entity.setCategory(ruleDTO.getCategory());

        CustomRuleEntity savedEntity = customRuleRepository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Transactional
    public CustomRuleDTO updateRule(Long id, CustomRuleDTO ruleDTO) {
        logger.debug("Updating custom rule with ID: {}", id);

        CustomRuleEntity entity = customRuleRepository.findById(id)
                .orElseThrow(() -> new GitLabException("Custom rule not found with ID: " + id));

        entity.setDescription(ruleDTO.getDescription());
        entity.setSeverity(ruleDTO.getSeverity());
        entity.setEnabled(ruleDTO.isEnabled());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setParentGroupId(ruleDTO.getParentGroupId());
        entity.setRemark(ruleDTO.getRemark());
        entity.setCategory(ruleDTO.getCategory());

        CustomRuleEntity updatedEntity = customRuleRepository.save(entity);
        return convertToDTO(updatedEntity);
    }

    @Transactional
    public void deleteRule(Long id) {
        logger.debug("Deleting custom rule with ID: {}", id);

        if (!customRuleRepository.existsById(id)) {
            throw new GitLabException("Custom rule not found with ID: " + id);
        }

        customRuleRepository.deleteById(id);
    }

    public List<CustomRuleEntity> getAllRules() {
        return customRuleRepository.findAll();
    }

    public List<CustomRuleEntity> getAllEnabledRules() {
        return customRuleRepository.findByEnabledTrue();
    }

    public List<CustomRuleEntity> getEnabledRulesByParentGroup(Long parentGroupId) {
        return customRuleRepository.findByParentGroupIdAndEnabledTrue(parentGroupId);
    }

    public List<CustomRuleEntity> getEnabledRulesByParentGroupAndCategory(
            Long parentGroupId, CustomRuleEntity.RuleCategory category) {
        return customRuleRepository.findByParentGroupIdAndCategoryAndEnabledTrue(parentGroupId, category);
    }

    private CustomRuleDTO convertToDTO(CustomRuleEntity entity) {
        CustomRuleDTO dto = new CustomRuleDTO();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setSeverity(entity.getSeverity());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setEnabled(entity.isEnabled());
        dto.setRemark(entity.getRemark());
        dto.setParentGroupId(entity.getParentGroupId());
        dto.setCategory(entity.getCategory());
        return dto;
    }
}