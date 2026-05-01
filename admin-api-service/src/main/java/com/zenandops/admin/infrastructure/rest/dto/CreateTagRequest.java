package com.zenandops.admin.infrastructure.rest.dto;

/**
 * Request DTO for creating a new tag definition.
 */
public record CreateTagRequest(
        String key,
        String value,
        String description
) {}
