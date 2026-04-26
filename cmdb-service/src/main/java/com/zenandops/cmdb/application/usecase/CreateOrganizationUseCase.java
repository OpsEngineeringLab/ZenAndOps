package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.domain.exception.DuplicateRootOrganizationException;
import com.zenandops.cmdb.domain.exception.DuplicateSiblingNameException;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import com.zenandops.cmdb.domain.vo.OrganizationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for creating a new Organization.
 * Enforces single ROOT invariant, parent existence validation, and sibling name uniqueness.
 */
@ApplicationScoped
public class CreateOrganizationUseCase {

    private final OrganizationRepository organizationRepository;

    @Inject
    public CreateOrganizationUseCase(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Create a new Organization.
     *
     * @param name              the organization name
     * @param type              the organization type
     * @param parentId          optional parent organization ID
     * @param responsiblePerson the responsible person
     * @param costCenter        the cost center
     * @return the created Organization
     * @throws DuplicateRootOrganizationException if a ROOT organization already exists
     * @throws OrganizationNotFoundException      if the parent organization does not exist
     * @throws DuplicateSiblingNameException       if a sibling with the same name exists under the same parent
     */
    public Organization execute(String name, OrganizationType type, String parentId,
                                String responsiblePerson, String costCenter) {
        if (type == OrganizationType.ROOT && organizationRepository.countByType(OrganizationType.ROOT) > 0) {
            throw new DuplicateRootOrganizationException();
        }

        if (parentId != null && !organizationRepository.existsById(parentId)) {
            throw new OrganizationNotFoundException("Parent organization not found with id: " + parentId);
        }

        if (organizationRepository.existsByParentIdAndName(parentId, name)) {
            throw new DuplicateSiblingNameException(
                    "A sibling organization with name '" + name + "' already exists under the same parent");
        }

        Instant now = Instant.now();
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        organization.setName(name);
        organization.setType(type);
        organization.setParentId(parentId);
        organization.setResponsiblePerson(responsiblePerson);
        organization.setCostCenter(costCenter);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);

        organizationRepository.save(organization);
        return organization;
    }
}
