package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import com.zenandops.cmdb.domain.entity.CIRelationship;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all relationships for a given CI with pagination support.
 * Uses the paginated repository method that combines both upstream and downstream relationships.
 */
@ApplicationScoped
public class ListCIRelationshipsUseCase {

    private final CIRelationshipRepository ciRelationshipRepository;

    @Inject
    public ListCIRelationshipsUseCase(CIRelationshipRepository ciRelationshipRepository) {
        this.ciRelationshipRepository = ciRelationshipRepository;
    }

    /**
     * List all relationships for a CI with pagination.
     *
     * @param ciId the CI identifier
     * @param page zero-based page number
     * @param size number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<CIRelationship> execute(String ciId, int page, int size) {
        List<CIRelationship> items = ciRelationshipRepository.findWithFilters(ciId, page, size);
        long totalItems = ciRelationshipRepository.countWithFilters(ciId);
        return new PaginatedResult<>(items, totalItems);
    }
}
