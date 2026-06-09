package com.psa.capstone.be.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import com.psa.capstone.be.dto.*;
import com.psa.capstone.be.service.GroupService;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public Mono<PaginatedResponseDTO<GroupDetailsDTO>> getParentGroups(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String search) {
        if (page < 1 || size < 1) {
            return Mono.error(new IllegalArgumentException("Page number or page size must be greater than 0"));
        }
        return groupService.getParentGroups(page, size, search);
    }

    @GetMapping("/{groupId}/subgroups")
    public Mono<PaginatedResponseDTO<SubgroupDetailsDTO>> getGroupSubgroups(
            @PathVariable Long groupId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String search) {
        if (page < 1 || size < 1) {
            return Mono.error(new IllegalArgumentException("Page number or page size must be greater than 0"));
        }
        return groupService.getGroupSubgroups(groupId, page, size, search);
    }

}