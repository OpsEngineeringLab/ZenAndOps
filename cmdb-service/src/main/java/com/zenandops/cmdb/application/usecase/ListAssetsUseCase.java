package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing assets with optional filtering by organizationId, type, costType, status, and supplier.
 * Supports pagination via page and size parameters.
 */
@ApplicationScoped
public class ListAssetsUseCase {

    private final AssetRepository assetRepository;

    @Inject
    public ListAssetsUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * List assets with optional filters and pagination.
     *
     * @param organizationId optional organization filter
     * @param type           optional asset type filter
     * @param costType       optional cost type filter
     * @param status         optional status filter
     * @param supplier       optional supplier filter
     * @param page           zero-based page number
     * @param size           number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<Asset> execute(String organizationId, AssetType type, CostType costType,
                                          AssetStatus status, String supplier, int page, int size) {
        List<Asset> items = assetRepository.findWithFilters(organizationId, type, costType, status, supplier, page, size);
        long totalItems = assetRepository.countWithFilters(organizationId, type, costType, status, supplier);
        return new PaginatedResult<>(items, totalItems);
    }
}
