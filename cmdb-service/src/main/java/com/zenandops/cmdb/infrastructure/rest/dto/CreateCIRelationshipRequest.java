package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.RelationshipType;

/**
 * Request DTO for creating a new CIRelationship.
 *
 * @param sourceCIId       the source CI identifier
 * @param targetCIId       the target CI identifier
 * @param relationshipType the relationship type (DEPENDS_ON, HOSTS, CONNECTS_TO)
 */
public record CreateCIRelationshipRequest(
        String sourceCIId,
        String targetCIId,
        RelationshipType relationshipType
) {
}
