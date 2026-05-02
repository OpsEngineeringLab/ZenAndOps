package com.zenandops.admin.application.port;

import java.util.Map;

/**
 * Port interface for tag definition management operations.
 * <p>
 * Abstracts realm attribute operations used for tag definitions, allowing the
 * application layer to remain decoupled from infrastructure adapters (e.g., Keycloak).
 */
public interface TagManagementPort {

    /**
     * Get the full realm representation (used to read realm attributes such as tag definitions).
     *
     * @return realm representation
     */
    Map<String, Object> getRealmRepresentation();

    /**
     * Update the realm representation (used to write realm attributes such as tag definitions).
     *
     * @param realmRepresentation the updated realm data
     */
    void updateRealmRepresentation(Map<String, Object> realmRepresentation);
}
