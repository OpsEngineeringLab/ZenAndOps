package com.zenandops.cmdb.application.dto;

import java.util.List;

/**
 * Represents an entity affected by an impact analysis traversal.
 *
 * @param id               the entity identifier
 * @param name             the entity name
 * @param entityType       the entity type (CI or SERVICE)
 * @param relationshipPath the path of relationships from root to this entity
 * @param depth            the depth in the traversal graph
 */
public record AffectedEntity(
        String id,
        String name,
        String entityType,
        List<String> relationshipPath,
        int depth
) {
}
