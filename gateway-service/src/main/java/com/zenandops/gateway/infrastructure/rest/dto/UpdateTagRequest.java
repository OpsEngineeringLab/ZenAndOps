package com.zenandops.gateway.infrastructure.rest.dto;

/**
 * Request DTO for updating an existing tag definition.
 */
public record UpdateTagRequest(
        String key,
        String value,
        String description
) {}
