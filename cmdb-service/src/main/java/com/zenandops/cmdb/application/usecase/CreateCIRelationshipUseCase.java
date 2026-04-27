package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.domain.entity.CIRelationship;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import com.zenandops.cmdb.domain.exception.DuplicateDependencyException;
import com.zenandops.cmdb.domain.exception.SelfReferenceException;
import com.zenandops.cmdb.domain.vo.RelationshipType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for creating a new CIRelationship.
 * Validates both CIs exist, prevents self-reference and duplicates
 * with the same source+target+type combination.
 */
@ApplicationScoped
public class CreateCIRelationshipUseCase {

    private final CIRepository ciRepository;
    private final CIRelationshipRepository ciRelationshipRepository;

    @Inject
    public CreateCIRelationshipUseCase(CIRepository ciRepository,
                                       CIRelationshipRepository ciRelationshipRepository) {
        this.ciRepository = ciRepository;
        this.ciRelationshipRepository = ciRelationshipRepository;
    }

    /**
     * Create a new CI relationship.
     *
     * @param sourceCIId       the source CI identifier
     * @param targetCIId       the target CI identifier
     * @param relationshipType the relationship type (DEPENDS_ON, HOSTS, CONNECTS_TO)
     * @return the created CIRelationship
     */
    public CIRelationship execute(String sourceCIId, String targetCIId,
                                  RelationshipType relationshipType) {
        if (sourceCIId.equals(targetCIId)) {
            throw new SelfReferenceException(
                    "Source and target CI cannot be the same: " + sourceCIId);
        }

        if (!ciRepository.existsById(sourceCIId)) {
            throw new CINotFoundException(
                    "Source CI not found with id: " + sourceCIId);
        }

        if (!ciRepository.existsById(targetCIId)) {
            throw new CINotFoundException(
                    "Target CI not found with id: " + targetCIId);
        }

        if (ciRelationshipRepository.existsBySourceCIIdAndTargetCIIdAndRelationshipType(
                sourceCIId, targetCIId, relationshipType)) {
            throw new DuplicateDependencyException(
                    "CI relationship already exists between source " + sourceCIId
                            + " and target " + targetCIId + " with type " + relationshipType);
        }

        CIRelationship relationship = new CIRelationship();
        relationship.setSourceCIId(sourceCIId);
        relationship.setTargetCIId(targetCIId);
        relationship.setRelationshipType(relationshipType);
        relationship.setCreatedAt(Instant.now());

        ciRelationshipRepository.save(relationship);
        return relationship;
    }
}
