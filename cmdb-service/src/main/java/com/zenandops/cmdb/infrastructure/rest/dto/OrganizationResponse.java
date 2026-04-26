package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.OrganizationType;

import java.time.Instant;

/**
 * Response DTO representing an Organization.
 *
 * @param id                the organization identifier
 * @param name              the organization name
 * @param type              the organization type
 * @param parentId          the parent organization ID (null for ROOT)
 * @param responsiblePerson the responsible person
 * @param costCenter        the cost center
 * @param createdAt         when the organization was created
 * @param updatedAt         when the organization was last updated
 */
public record OrganizationResponse(
        String id,
        String name,
        OrganizationType type,
        String parentId,
        String responsiblePerson,
        String costCenter,
        Instant createdAt,
        Instant updatedAt
) {
}
