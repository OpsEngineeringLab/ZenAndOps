package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case for creating a new Asset.
 * Validates that the referenced organization exists, sets timestamps,
 * and publishes an asset created event.
 */
@ApplicationScoped
public class CreateAssetUseCase {

    private final AssetRepository assetRepository;
    private final OrganizationRepository organizationRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public CreateAssetUseCase(AssetRepository assetRepository,
                              OrganizationRepository organizationRepository,
                              CmdbEventPublisher eventPublisher) {
        this.assetRepository = assetRepository;
        this.organizationRepository = organizationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new Asset.
     *
     * @param name            the asset name
     * @param type            the asset type (HARDWARE, SOFTWARE, CLOUD)
     * @param organizationId  the owning organization ID
     * @param cost            the asset cost
     * @param costType        the cost type (CAPEX, OPEX)
     * @param acquisitionDate the acquisition date
     * @param status          the asset status
     * @param supplier        the supplier name
     * @param userId          the authenticated user performing the action
     * @return the created Asset
     */
    public Asset execute(String name, AssetType type, String organizationId,
                         BigDecimal cost, CostType costType, Instant acquisitionDate,
                         AssetStatus status, String supplier, String userId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(
                    "Organization not found with id: " + organizationId);
        }

        Instant now = Instant.now();
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID().toString());
        asset.setName(name);
        asset.setType(type);
        asset.setOrganizationId(organizationId);
        asset.setCost(cost);
        asset.setCostType(costType);
        asset.setAcquisitionDate(acquisitionDate);
        asset.setStatus(status);
        asset.setSupplier(supplier);
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);

        assetRepository.save(asset);
        eventPublisher.publishAssetCreated(asset, userId);
        return asset;
    }
}
