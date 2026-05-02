package com.zenandops.admin.application.port;

import java.util.List;
import java.util.Map;

/**
 * Port interface for realm role management operations.
 * <p>
 * Abstracts role CRUD operations, allowing the application layer to remain
 * decoupled from infrastructure adapters (e.g., Keycloak).
 */
public interface RoleManagementPort {

    /**
     * List all realm roles.
     *
     * @return list of role representations
     */
    List<Map<String, Object>> listRealmRoles();

    /**
     * Get a realm role by name.
     *
     * @param roleName the role name
     * @return role representation
     */
    Map<String, Object> getRealmRoleByName(String roleName);

    /**
     * Get a realm role by ID.
     *
     * @param roleId the role ID
     * @return role representation
     */
    Map<String, Object> getRealmRoleById(String roleId);

    /**
     * Create a new realm role.
     *
     * @param roleRepresentation the role data
     */
    void createRealmRole(Map<String, Object> roleRepresentation);

    /**
     * Update a realm role by ID.
     *
     * @param roleId             the role ID
     * @param roleRepresentation the updated role data
     */
    void updateRealmRole(String roleId, Map<String, Object> roleRepresentation);

    /**
     * Delete a realm role by ID.
     *
     * @param roleId the role ID
     */
    void deleteRealmRole(String roleId);
}
