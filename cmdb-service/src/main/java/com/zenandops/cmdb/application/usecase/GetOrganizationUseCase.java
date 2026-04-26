package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single Organization by its identifier.
 */
@ApplicationScoped
public class GetOrganizationUseCase {

    private final OrganizationRepository organizationRepository;

    @Inject
    public GetOrganizationUseCase(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Retrieve an Organization by id.
     *
     * @param id the organization identifier
     * @return the Organization
     * @throws OrganizationNotFoundException if no Organization exists with the given id
     */
    public Organization execute(String id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(
                        "Organization not found with id: " + id));
    }
}
