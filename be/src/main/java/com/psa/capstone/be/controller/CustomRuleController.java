package com.psa.capstone.be.controller;

import com.psa.capstone.be.dto.CustomRuleDTO;
import com.psa.capstone.be.dto.PaginatedResponseDTO;
import com.psa.capstone.be.service.CustomRuleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/custom-rules")
public class CustomRuleController {

    private static final Logger logger = LoggerFactory.getLogger(CustomRuleController.class);
    private final CustomRuleService customRuleService;

    public CustomRuleController(CustomRuleService customRuleService) {
        this.customRuleService = customRuleService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponseDTO<CustomRuleDTO>> getRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = true) Long parentGroupId) {

        if (page < 1 || size < 1) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Fetching custom rules - page: {}, size: {}, search: {}", page, size, search);
        PaginatedResponseDTO<CustomRuleDTO> response = customRuleService.getRules(page, size, search, parentGroupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CustomRuleDTO> createRule(@Valid @RequestBody CustomRuleDTO ruleDTO) {
        logger.info("Creating new custom rule with parent group ID: {}", ruleDTO.getParentGroupId());
        CustomRuleDTO createdRule = customRuleService.createRule(ruleDTO);
        return ResponseEntity.ok(createdRule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomRuleDTO> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody CustomRuleDTO ruleDTO) {
        logger.info("Updating custom rule with ID: {}", id);
        CustomRuleDTO updatedRule = customRuleService.updateRule(id, ruleDTO);
        return ResponseEntity.ok(updatedRule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        logger.info("Deleting custom rule with ID: {}", id);
        customRuleService.deleteRule(id);
        return ResponseEntity.ok().build();
    }

}