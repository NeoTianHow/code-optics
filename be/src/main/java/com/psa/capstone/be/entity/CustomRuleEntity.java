package com.psa.capstone.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_rules")
public class CustomRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleSeverity severity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "parent_group_id", nullable = false)
    private Long parentGroupId;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private RuleCategory category;

    public enum RuleCategory {
        FE,
        BE,
        SVC
    }

    public enum RuleSeverity {
        LOW,
        MEDIUM,
        HIGH
    }

    // Default constructor
    public CustomRuleEntity() {
    }

    // Constructor with required fields
    public CustomRuleEntity(String description, RuleSeverity severity, Long parentGroupId) {
        this.description = description;
        this.severity = severity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

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

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public RuleCategory getCategory() {
        return category;
    }

    public void setCategory(RuleCategory category) {
        this.category = category;
    }
}