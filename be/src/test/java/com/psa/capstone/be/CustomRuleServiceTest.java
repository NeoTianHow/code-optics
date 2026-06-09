package com.psa.capstone.be;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.psa.capstone.be.dto.CustomRuleDTO;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.entity.CustomRuleEntity;
import com.psa.capstone.be.entity.CustomRuleEntity.RuleCategory;
import com.psa.capstone.be.entity.CustomRuleEntity.RuleSeverity;
import com.psa.capstone.be.exception.GitLabException;
import com.psa.capstone.be.repository.CustomRuleRepository;
import com.psa.capstone.be.service.CustomRuleService;

@ExtendWith(MockitoExtension.class)
class CustomRuleServiceTest {

    @Mock
    private CustomRuleRepository customRuleRepository;

    @InjectMocks
    private CustomRuleService customRuleService;

    @Test
    void getRulesRequiresParentGroupId() {
        GitLabException exception = assertThrows(
                GitLabException.class,
                () -> customRuleService.getRules(1, 10, null, null));

        assertEquals("Parent group ID is required", exception.getMessage());
        verify(customRuleRepository, never()).findAll();
    }

    @Test
    void getRulesTrimsSearchAndMapsPagination() {
        CustomRuleEntity entity = ruleEntity(7L, "Avoid inline secrets", RuleSeverity.HIGH, true, 99L,
                RuleCategory.BE);
        Pageable expectedPageable = PageRequest.of(1, 5);

        when(customRuleRepository.findByParentGroupIdAndDescriptionContainingIgnoreCase(
                99L, "secrets", expectedPageable))
                .thenReturn(new PageImpl<>(List.of(entity), expectedPageable, 12));
        when(customRuleRepository.countByParentGroupIdAndDescriptionContainingIgnoreCase(99L, "secrets"))
                .thenReturn(12L);

        PaginatedResponseDTO<CustomRuleDTO> response = customRuleService.getRules(2, 5, "  secrets  ", 99L);

        assertEquals(2, response.getCurrentPage());
        assertEquals(5, response.getPageSize());
        assertEquals(12, response.getTotalItems());
        assertEquals(3, response.getTotalPages());
        assertEquals(1, response.getItems().size());
        assertEquals("Avoid inline secrets", response.getItems().get(0).getDescription());
        assertEquals(RuleCategory.BE, response.getItems().get(0).getCategory());
    }

    @Test
    void createRulePersistsIncomingFieldsAndSetsTimestamps() {
        CustomRuleDTO request = ruleDto(null, "Use DTO validation", RuleSeverity.MEDIUM, true, 42L,
                "Backend rule", RuleCategory.SVC);

        when(customRuleRepository.save(any(CustomRuleEntity.class))).thenAnswer(invocation -> {
            CustomRuleEntity saved = invocation.getArgument(0);
            saved.setId(123L);
            return saved;
        });

        CustomRuleDTO result = customRuleService.createRule(request);

        ArgumentCaptor<CustomRuleEntity> entityCaptor = ArgumentCaptor.forClass(CustomRuleEntity.class);
        verify(customRuleRepository).save(entityCaptor.capture());

        CustomRuleEntity savedEntity = entityCaptor.getValue();
        assertEquals("Use DTO validation", savedEntity.getDescription());
        assertEquals(RuleSeverity.MEDIUM, savedEntity.getSeverity());
        assertEquals(42L, savedEntity.getParentGroupId());
        assertEquals("Backend rule", savedEntity.getRemark());
        assertEquals(RuleCategory.SVC, savedEntity.getCategory());
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());

        assertEquals(123L, result.getId());
        assertEquals("Use DTO validation", result.getDescription());
    }

    @Test
    void updateRuleThrowsWhenRuleDoesNotExist() {
        when(customRuleRepository.findById(404L)).thenReturn(Optional.empty());

        GitLabException exception = assertThrows(
                GitLabException.class,
                () -> customRuleService.updateRule(404L,
                        ruleDto(null, "Missing", RuleSeverity.LOW, true, 1L, null, RuleCategory.FE)));

        assertEquals("Custom rule not found with ID: 404", exception.getMessage());
        verify(customRuleRepository, never()).save(any());
    }

    @Test
    void deleteRuleChecksExistenceBeforeDeleting() {
        when(customRuleRepository.existsById(5L)).thenReturn(true);

        customRuleService.deleteRule(5L);

        verify(customRuleRepository).deleteById(5L);
    }

    @Test
    void deleteRuleThrowsWhenRuleDoesNotExist() {
        when(customRuleRepository.existsById(404L)).thenReturn(false);

        GitLabException exception = assertThrows(GitLabException.class, () -> customRuleService.deleteRule(404L));

        assertEquals("Custom rule not found with ID: 404", exception.getMessage());
        verify(customRuleRepository, never()).deleteById(404L);
    }

    private static CustomRuleDTO ruleDto(
            Long id,
            String description,
            RuleSeverity severity,
            boolean enabled,
            Long parentGroupId,
            String remark,
            RuleCategory category) {
        CustomRuleDTO dto = new CustomRuleDTO();
        dto.setId(id);
        dto.setDescription(description);
        dto.setSeverity(severity);
        dto.setEnabled(enabled);
        dto.setParentGroupId(parentGroupId);
        dto.setRemark(remark);
        dto.setCategory(category);
        return dto;
    }

    private static CustomRuleEntity ruleEntity(
            Long id,
            String description,
            RuleSeverity severity,
            boolean enabled,
            Long parentGroupId,
            RuleCategory category) {
        CustomRuleEntity entity = new CustomRuleEntity();
        entity.setId(id);
        entity.setDescription(description);
        entity.setSeverity(severity);
        entity.setEnabled(enabled);
        entity.setParentGroupId(parentGroupId);
        entity.setCategory(category);
        entity.setRemark("remark");
        entity.setCreatedAt(LocalDateTime.now().minusDays(1));
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
