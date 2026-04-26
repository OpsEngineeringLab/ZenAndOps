package com.zenandops.cmdb.infrastructure.rest.dto;

/**
 * Request DTO for updating an existing Organization.
 *
 * @param name              the new organization name
 * @param responsiblePerson the new responsible person
 * @param costCenter        the new cost center
 */
public record UpdateOrganizationRequest(
        String name,
        String responsiblePerson,
        String costCenter
) {
}
