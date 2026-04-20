package com.zenandops.auth.infrastructure.rest.dto;

/**
 * Request DTO for changing the current user's password.
 *
 * @param currentPassword the user's current password
 * @param newPassword     the new password to set
 */
public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
