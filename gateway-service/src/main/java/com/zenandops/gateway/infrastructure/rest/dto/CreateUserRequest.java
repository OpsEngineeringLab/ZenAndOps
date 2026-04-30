package com.zenandops.gateway.infrastructure.rest.dto;

/**
 * Request DTO for creating a new user.
 */
public record CreateUserRequest(
        String login,
        String name,
        String email,
        String password,
        boolean active
) {}
