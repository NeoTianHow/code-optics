package com.psa.capstone.be.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.psa.capstone.be.entity.CustomRuleEntity;
import com.psa.capstone.be.entity.CustomRuleEntity.RuleSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.psa.capstone.be.utilities.CustomLocalDateTimeSerializer;
import java.time.LocalDateTime;

public class CustomRuleDTO {

    private Long id;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Severity is required")
    private RuleSeverity severity;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @NotNull(message = "Enabled status is required")
    private boolean enabled;

    @NotNull(message = "Parent Group ID is required")
    private Long parentGroupId;

    private String remark;

    @NotNull(message = "Category is required")
    private CustomRuleEntity.RuleCategory category;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RuleSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RuleSeverity severity) {
        this.severity = severity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getParentGroupId() {
        return parentGroupId;
    }

    public void setParentGroupId(Long parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public CustomRuleEntity.RuleCategory getCategory() {
        return category;
    }

    public void setCategory(CustomRuleEntity.RuleCategory category) {
        this.category = category;
    }
}