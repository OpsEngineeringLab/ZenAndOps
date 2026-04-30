package com.zenandops.gateway.infrastructure.rest.dto;

/**
 * Request DTO for updating an existing user. All fields are nullable — only non-null fields are applied.
 */
public record UpdateUserRequest(
        String name,
        String email,
        Boolean active
) {}
