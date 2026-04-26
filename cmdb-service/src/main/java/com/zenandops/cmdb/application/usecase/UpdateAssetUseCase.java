package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.exception.AssetNotFoundException;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.CostType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Use case for updating an existing Asset.
 * Validates the asset exists, updates mutable fields, and publishes an asset updated event.
 */
@ApplicationScoped
public class UpdateAssetUseCase {

    private final AssetRepository assetRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public UpdateAssetUseCase(AssetRepository assetRepository,
                              CmdbEventPublisher eventPublisher) {
        this.assetRepository = assetRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Update an Asset's mutable fields.
     *
     * @param id       the asset identifier
     * @param name     the new name
     * @param cost     the new cost
     * @param costType the new cost type
     * @param status   the new status
     * @param supplier the new supplier
     * @param userId   the authenticated user performing the action
     * @return the updated Asset
     * @throws AssetNotFoundException if no Asset exists with the given id
     */
    public Asset execute(String id, String name, BigDecimal cost, CostType costType,
                         AssetStatus status, String supplier, String userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(
                        "Asset not found with id: " + id));

        asset.setName(name);
        asset.setCost(cost);
        asset.setCostType(costType);
        asset.setStatus(status);
        asset.setSupplier(supplier);
        asset.setUpdatedAt(Instant.now());

        assetRepository.save(asset);
        eventPublisher.publishAssetUpdated(asset, userId);
        return asset;
    }
}
