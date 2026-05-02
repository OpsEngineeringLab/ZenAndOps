package com.zenandops.admin.application.port;

import java.util.List;
import java.util.Map;

/**
 * Port interface for user management operations.
 * <p>
 * Abstracts user CRUD and role assignment operations, allowing the application
 * layer to remain decoupled from infrastructure adapters (e.g., Keycloak).
 */
public interface UserManagementPort {

    /**
     * List users with optional pagination.
     *
     * @param first index of the first result (nullable)
     * @param max   maximum number of results (nullable)
     * @return list of user representations
     */
    List<Map<String, Object>> listUsers(Integer first, Integer max);

    /**
     * Get a single user by ID.
     *
     * @param userId the user ID
     * @return user representation
     */
    Map<String, Object> getUser(String userId);

    /**
     * Create a new user. Returns the user ID.
     *
     * @param userRepresentation the user data
     * @return the created user's ID
     */
    String createUser(Map<String, Object> userRepresentation);

    /**
     * Update an existing user.
     *
     * @param userId             the user ID
     * @param userRepresentation the updated user data
     */
    void updateUser(String userId, Map<String, Object> userRepresentation);

    /**
     * Delete a user.
     *
     * @param userId the user ID
     */
    void deleteUser(String userId);

    /**
     * Get realm-level role mappings for a user.
     *
     * @param userId the user ID
     * @return list of role representations assigned to the user
     */
    List<Map<String, Object>> getUserRealmRoles(String userId);

    /**
     * Assign realm-level roles to a user.
     *
     * @param userId the user ID
     * @param roles  the roles to assign
     */
    void assignRealmRoles(String userId, List<Map<String, Object>> roles);

    /**
     * Remove realm-level roles from a user.
     *
     * @param userId the user ID
     * @param roles  the roles to remove
     */
    void removeRealmRoles(String userId, List<Map<String, Object>> roles);
}
