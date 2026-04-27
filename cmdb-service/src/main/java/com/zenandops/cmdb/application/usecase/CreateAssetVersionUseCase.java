package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import com.zenandops.cmdb.domain.exception.AssetNotFoundException;
import com.zenandops.cmdb.domain.exception.DataSourceNotFoundException;
import com.zenandops.cmdb.domain.vo.DataOrigin;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Map;

/**
 * Use case for creating a new AssetVersion.
 * Validates asset and data source exist, auto-assigns the next sequential version number,
 * closes the previous active version, and publishes a version created event.
 */
@ApplicationScoped
public class CreateAssetVersionUseCase {

    private final AssetRepository assetRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public CreateAssetVersionUseCase(AssetRepository assetRepository,
                                     AssetVersionRepository assetVersionRepository,
                                     DataSourceRepository dataSourceRepository,
                                     CmdbEventPublisher eventPublisher) {
        this.assetRepository = assetRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new AssetVersion.
     *
     * @param assetId         the asset identifier
     * @param description     the version description
     * @param attributes      the version attributes (JSON map)
     * @param dataOrigin      the data origin (API, AGENT, FILE)
     * @param dataSourceId    the data source identifier
     * @param changeReference optional change reference
     * @param userId          the authenticated user performing the action
     * @return the created AssetVersion
     */
    public AssetVersion execute(String assetId, String description,
                                Map<String, Object> attributes, DataOrigin dataOrigin,
                                String dataSourceId, String changeReference, String userId) {
        if (!assetRepository.existsById(assetId)) {
            throw new AssetNotFoundException("Asset not found with id: " + assetId);
        }

        if (!dataSourceRepository.existsById(dataSourceId)) {
            throw new DataSourceNotFoundException(
                    "Data source not found with id: " + dataSourceId);
        }

        Instant now = Instant.now();

        // Close previous active version
        assetVersionRepository.findActiveByAssetId(assetId).ifPresent(previous -> {
            previous.setEndDate(now);
            assetVersionRepository.save(previous);
        });

        // Auto-assign next sequential version number
        int nextVersion = assetVersionRepository.getMaxVersionNumber(assetId) + 1;

        AssetVersion version = new AssetVersion();
        version.setAssetId(assetId);
        version.setVersionNumber(nextVersion);
        version.setDescription(description);
        version.setAttributes(attributes != null ? attributes : Map.of());
        version.setStartDate(now);
        version.setEndDate(null);
        version.setDataOrigin(dataOrigin);
        version.setDataSourceId(dataSourceId);
        version.setChangeReference(changeReference);
        version.setCreatedAt(now);

        assetVersionRepository.save(version);
        eventPublisher.publishVersionCreated(version.getId(), "ASSET_VERSION", userId);
        return version;
    }
}
