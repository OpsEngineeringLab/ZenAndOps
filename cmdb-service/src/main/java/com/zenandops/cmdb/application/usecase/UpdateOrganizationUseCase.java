package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.domain.exception.DuplicateSiblingNameException;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for updating an existing Organization.
 * Validates sibling name uniqueness if the name is changed. Cannot change type.
 */
@ApplicationScoped
public class UpdateOrganizationUseCase {

    private final OrganizationRepository organizationRepository;

    @Inject
    public UpdateOrganizationUseCase(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Update an Organization's mutable fields.
     *
     * @param id                the organization identifier
     * @param name              the new name
     * @param responsiblePerson the new responsible person
     * @param costCenter        the new cost center
     * @return the updated Organization
     * @throws OrganizationNotFoundException if no Organization exists with the given id
     * @throws DuplicateSiblingNameException  if a sibling with the new name exists under the same parent
     */
    public Organization execute(String id, String name, String responsiblePerson, String costCenter) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(
                        "Organization not found with id: " + id));

        if (!organization.getName().equals(name)
                && organizationRepository.existsByParentIdAndName(organization.getParentId(), name)) {
            throw new DuplicateSiblingNameException(
                    "A sibling organization with name '" + name + "' already exists under the same parent");
        }

        organization.setName(name);
        organization.setResponsiblePerson(responsiblePerson);
        organization.setCostCenter(costCenter);
        organization.setUpdatedAt(Instant.now());

        organizationRepository.save(organization);
        return organization;
    }
}
