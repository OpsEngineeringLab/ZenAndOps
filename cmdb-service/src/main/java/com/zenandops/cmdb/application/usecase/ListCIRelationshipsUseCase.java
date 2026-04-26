package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import com.zenandops.cmdb.domain.entity.CIRelationship;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Use case for listing all relationships for a given CI.
 * Returns both upstream (where CI is target) and downstream (where CI is source)
 * relationships, combining results from findBySourceCIId and findByTargetCIId.
 */
@ApplicationScoped
public class ListCIRelationshipsUseCase {

    private final CIRelationshipRepository ciRelationshipRepository;

    @Inject
    public ListCIRelationshipsUseCase(CIRelationshipRepository ciRelationshipRepository) {
        this.ciRelationshipRepository = ciRelationshipRepository;
    }

    /**
     * List all relationships for a CI (both upstream and downstream).
     *
     * @param ciId the CI identifier
     * @return combined list of upstream and downstream relationships
     */
    public List<CIRelationship> execute(String ciId) {
        List<CIRelationship> downstream = ciRelationshipRepository.findBySourceCIId(ciId);
        List<CIRelationship> upstream = ciRelationshipRepository.findByTargetCIId(ciId);

        Set<String> seenIds = new LinkedHashSet<>();
        List<CIRelationship> combined = new ArrayList<>();

        for (CIRelationship rel : downstream) {
            if (seenIds.add(rel.getId())) {
                combined.add(rel);
            }
        }
        for (CIRelationship rel : upstream) {
            if (seenIds.add(rel.getId())) {
                combined.add(rel);
            }
        }

        return combined;
    }
}
