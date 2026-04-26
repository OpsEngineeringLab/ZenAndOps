package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.RelationshipType;

import java.time.Instant;

/**
 * Response DTO representing a CIRelationship.
 *
 * @param id               the relationship identifier
 * @param sourceCIId       the source CI identifier
 * @param targetCIId       the target CI identifier
 * @param relationshipType the relationship type
 * @param createdAt        when the relationship was created
 */
public record CIRelationshipResponse(
        String id,
        String sourceCIId,
        String targetCIId,
        RelationshipType relationshipType,
        Instant createdAt
) {
}
