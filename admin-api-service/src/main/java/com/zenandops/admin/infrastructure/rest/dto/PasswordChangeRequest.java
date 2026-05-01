package com.zenandops.admin.infrastructure.rest.dto;

/**
 * Request DTO for changing a user's password.
 */
public record PasswordChangeRequest(
        String newPassword
) {}
