package com.zenandops.admin.application.port;

import java.util.Map;

/**
 * Port interface for user profile operations.
 * <p>
 * Abstracts user profile retrieval, update, and password reset operations,
 * allowing the application layer to remain decoupled from infrastructure
 * adapters (e.g., Keycloak).
 */
public interface ProfileManagementPort {

    /**
     * Get a single user by ID.
     *
     * @param userId the user ID
     * @return user representation
     */
    Map<String, Object> getUser(String userId);

    /**
     * Update an existing user.
     *
     * @param userId             the user ID
     * @param userRepresentation the updated user data
     */
    void updateUser(String userId, Map<String, Object> userRepresentation);

    /**
     * Reset a user's password.
     *
     * @param userId      the user ID
     * @param newPassword the new password
     * @param temporary   if true, the user must change the password on next login
     */
    void resetPassword(String userId, String newPassword, boolean temporary);
}
