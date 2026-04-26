package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.OrganizationType;

/**
 * Request DTO for creating a new Organization.
 *
 * @param name              the organization name
 * @param type              the organization type (ROOT, BUSINESS_UNIT, DEPARTMENT, TEAM)
 * @param parentId          optional parent organization ID
 * @param responsiblePerson the responsible person
 * @param costCenter        the cost center
 */
public record CreateOrganizationRequest(
        String name,
        OrganizationType type,
        String parentId,
        String responsiblePerson,
        String costCenter
) {
}
