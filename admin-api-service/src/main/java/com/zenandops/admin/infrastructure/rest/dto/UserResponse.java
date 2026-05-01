package com.zenandops.admin.infrastructure.rest.dto;

import java.util.List;

/**
 * ZenAndOps user response DTO.
 */
public record UserResponse(
        String id,
        String login,
        String name,
        String email,
        List<String> roles,
        List<String> tagIds,
        boolean active,
        String createdAt,
        String updatedAt
) {}
