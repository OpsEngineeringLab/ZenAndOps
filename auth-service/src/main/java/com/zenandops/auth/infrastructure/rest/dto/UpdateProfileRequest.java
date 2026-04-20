package com.zenandops.auth.infrastructure.rest.dto;

/**
 * Request DTO for updating the current user's profile.
 *
 * @param name  the user display name
 * @param email the user email address
 */
public record UpdateProfileRequest(String name, String email) {
}
