package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.exception.OrganizationInUseException;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting an Organization.
 * Validates no children, services, or assets are associated before deletion.
 */
@ApplicationScoped
public class DeleteOrganizationUseCase {

    private final OrganizationRepository organizationRepository;
    private final ServiceRepository serviceRepository;
    private final AssetRepository assetRepository;

    @Inject
    public DeleteOrganizationUseCase(OrganizationRepository organizationRepository,
                                     ServiceRepository serviceRepository,
                                     AssetRepository assetRepository) {
        this.organizationRepository = organizationRepository;
        this.serviceRepository = serviceRepository;
        this.assetRepository = assetRepository;
    }

    /**
     * Delete an Organization by id.
     *
     * @param id the organization identifier
     * @throws OrganizationNotFoundException if no Organization exists with the given id
     * @throws OrganizationInUseException    if the Organization has children, services, or assets
     */
    public void execute(String id) {
        if (!organizationRepository.existsById(id)) {
            throw new OrganizationNotFoundException("Organization not found with id: " + id);
        }

        if (organizationRepository.countByParentId(id) > 0) {
            throw new OrganizationInUseException(
                    "Organization has child organizations and cannot be deleted");
        }

        if (serviceRepository.countByOrganizationId(id) > 0) {
            throw new OrganizationInUseException(
                    "Organization has associated services and cannot be deleted");
        }

        if (assetRepository.countByOrganizationId(id) > 0) {
            throw new OrganizationInUseException(
                    "Organization has associated assets and cannot be deleted");
        }

        organizationRepository.deleteById(id);
    }
}
