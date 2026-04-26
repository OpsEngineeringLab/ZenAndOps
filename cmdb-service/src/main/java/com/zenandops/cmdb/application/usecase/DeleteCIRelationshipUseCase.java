package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a CIRelationship.
 * Validates the relationship exists before deletion.
 */
@ApplicationScoped
public class DeleteCIRelationshipUseCase {

    private final CIRelationshipRepository ciRelationshipRepository;

    @Inject
    public DeleteCIRelationshipUseCase(CIRelationshipRepository ciRelationshipRepository) {
        this.ciRelationshipRepository = ciRelationshipRepository;
    }

    /**
     * Delete a CI relationship by id.
     *
     * @param id the relationship identifier
     * @throws IllegalArgumentException if no relationship exists with the given id
     */
    public void execute(String id) {
        ciRelationshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "CI relationship not found with id: " + id));

        ciRelationshipRepository.deleteById(id);
    }
}
