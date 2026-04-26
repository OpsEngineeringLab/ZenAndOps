package com.zenandops.cmdb.application.usecase;

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
 */
@ApplicationScoped
public class ListAssetsUseCase {

    private final AssetRepository assetRepository;

    @Inject
    public ListAssetsUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * List assets with optional filters. Pass null for any filter to skip it.
     *
     * @param organizationId optional organization filter
     * @param type           optional asset type filter
     * @param costType       optional cost type filter
     * @param status         optional status filter
     * @param supplier       optional supplier filter
     * @return filtered list of assets
     */
    public List<Asset> execute(String organizationId, AssetType type, CostType costType,
                               AssetStatus status, String supplier) {
        return assetRepository.findWithFilters(organizationId, type, costType, status, supplier);
    }
}
