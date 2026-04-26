package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Organization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all Organizations.
 */
@ApplicationScoped
public class ListOrganizationsUseCase {

    private final OrganizationRepository organizationRepository;

    @Inject
    public ListOrganizationsUseCase(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Retrieve all Organizations.
     *
     * @return list of all Organizations
     */
    public List<Organization> execute() {
        return organizationRepository.findAll();
    }
}
