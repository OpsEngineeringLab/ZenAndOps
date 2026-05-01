package com.zenandops.admin.infrastructure.rest.dto;

/**
 * ZenAndOps tag definition response DTO.
 */
public record TagResponse(
        String id,
        String key,
        String value,
        String description,
        String createdAt,
        String updatedAt
) {}
