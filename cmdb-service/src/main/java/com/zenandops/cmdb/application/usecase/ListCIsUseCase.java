package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing CIs with optional filtering by organizationId, type, status, and assetId.
 * Supports pagination via page and size parameters.
 */
@ApplicationScoped
public class ListCIsUseCase {

    private final CIRepository ciRepository;

    @Inject
    public ListCIsUseCase(CIRepository ciRepository) {
        this.ciRepository = ciRepository;
    }

    /**
     * List CIs with optional filters and pagination.
     *
     * @param organizationId optional organization filter
     * @param type           optional CI type filter
     * @param status         optional status filter
     * @param assetId        optional asset filter
     * @param page           zero-based page number
     * @param size           number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<CI> execute(String organizationId, CIType type, CIStatus status,
                                       String assetId, int page, int size) {
        List<CI> items = ciRepository.findWithFilters(organizationId, type, status, assetId, page, size);
        long totalItems = ciRepository.countWithFilters(organizationId, type, status, assetId);
        return new PaginatedResult<>(items, totalItems);
    }
}
