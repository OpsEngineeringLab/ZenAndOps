package com.zenandops.admin.infrastructure.rest.dto;

import java.util.List;

/**
 * ZenAndOps role response DTO.
 */
public record RoleResponse(
        String id,
        String name,
        String description,
        List<String> permissions,
        String createdAt,
        String updatedAt
) {}
