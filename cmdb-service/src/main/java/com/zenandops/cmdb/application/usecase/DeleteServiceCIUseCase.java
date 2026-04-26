package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import com.zenandops.cmdb.domain.exception.LastServiceAssociationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a ServiceCI association.
 * Validates the association exists. If this is the last association for the CI
 * AND the CI does NOT have controlledExceptionFlag, throws LastServiceAssociationException.
 */
@ApplicationScoped
public class DeleteServiceCIUseCase {

    private final ServiceCIRepository serviceCIRepository;
    private final CIRepository ciRepository;

    @Inject
    public DeleteServiceCIUseCase(ServiceCIRepository serviceCIRepository,
                                  CIRepository ciRepository) {
        this.serviceCIRepository = serviceCIRepository;
        this.ciRepository = ciRepository;
    }

    /**
     * Delete a service-CI association by id.
     *
     * @param id the association identifier
     * @throws IllegalArgumentException        if no association exists with the given id
     * @throws LastServiceAssociationException if this is the last association and CI lacks exception flag
     */
    public void execute(String id) {
        ServiceCI serviceCI = serviceCIRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Service-CI association not found with id: " + id));

        String ciId = serviceCI.getCiId();
        long associationCount = serviceCIRepository.countByCiId(ciId);

        if (associationCount <= 1) {
            CI ci = ciRepository.findById(ciId).orElse(null);
            if (ci != null && !ci.isControlledExceptionFlag()) {
                throw new LastServiceAssociationException(
                        "Cannot remove last service association from CI " + ciId
                                + " without controlled exception flag");
            }
        }

        serviceCIRepository.deleteById(id);
    }
}
