package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.domain.exception.CIInUseException;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a CI.
 * Validates no versions, relationships, or service associations exist before deletion.
 */
@ApplicationScoped
public class DeleteCIUseCase {

    private final CIRepository ciRepository;
    private final CIVersionRepository ciVersionRepository;
    private final CIRelationshipRepository ciRelationshipRepository;
    private final ServiceCIRepository serviceCIRepository;

    @Inject
    public DeleteCIUseCase(CIRepository ciRepository,
                           CIVersionRepository ciVersionRepository,
                           CIRelationshipRepository ciRelationshipRepository,
                           ServiceCIRepository serviceCIRepository) {
        this.ciRepository = ciRepository;
        this.ciVersionRepository = ciVersionRepository;
        this.ciRelationshipRepository = ciRelationshipRepository;
        this.serviceCIRepository = serviceCIRepository;
    }

    /**
     * Delete a CI by id.
     *
     * @param id the CI identifier
     * @throws CINotFoundException if no CI exists with the given id
     * @throws CIInUseException    if the CI has versions, relationships, or service associations
     */
    public void execute(String id) {
        if (!ciRepository.existsById(id)) {
            throw new CINotFoundException("CI not found with id: " + id);
        }

        if (ciVersionRepository.countByCiId(id) > 0) {
            throw new CIInUseException("CI has versions and cannot be deleted");
        }

        if (ciRelationshipRepository.countBySourceCIIdOrTargetCIId(id) > 0) {
            throw new CIInUseException("CI has relationships and cannot be deleted");
        }

        if (serviceCIRepository.countByCiId(id) > 0) {
            throw new CIInUseException(
                    "CI has service associations and cannot be deleted");
        }

        ciRepository.deleteById(id);
    }
}
