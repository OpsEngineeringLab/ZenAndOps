package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import com.zenandops.cmdb.domain.exception.AssetNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;

/**
 * Use case for finding the AssetVersion that was active at a specific point in time.
 * Finds the version where startDate <= timestamp AND (endDate > timestamp OR endDate is null).
 */
@ApplicationScoped
public class GetAssetVersionAtTimeUseCase {

    private final AssetRepository assetRepository;
    private final AssetVersionRepository assetVersionRepository;

    @Inject
    public GetAssetVersionAtTimeUseCase(AssetRepository assetRepository,
                                        AssetVersionRepository assetVersionRepository) {
        this.assetRepository = assetRepository;
        this.assetVersionRepository = assetVersionRepository;
    }

    /**
     * Find the AssetVersion active at the given timestamp.
     *
     * @param assetId   the asset identifier
     * @param timestamp the point in time to query
     * @return the AssetVersion active at that time
     * @throws AssetNotFoundException if the asset does not exist
     * @throws NotFoundException      if no version was active at the given time
     */
    public AssetVersion execute(String assetId, Instant timestamp) {
        if (!assetRepository.existsById(assetId)) {
            throw new AssetNotFoundException("Asset not found with id: " + assetId);
        }

        return assetVersionRepository.findByAssetIdAtTime(assetId, timestamp)
                .orElseThrow(() -> new NotFoundException(
                        "No asset version found for asset " + assetId
                                + " at time " + timestamp));
    }
}
