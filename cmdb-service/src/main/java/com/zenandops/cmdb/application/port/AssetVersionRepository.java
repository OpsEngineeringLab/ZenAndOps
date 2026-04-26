package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.AssetVersion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for AssetVersion persistence operations.
 */
public interface AssetVersionRepository {

    void save(AssetVersion version);

    List<AssetVersion> findByAssetId(String assetId);

    Optional<AssetVersion> findActiveByAssetId(String assetId);

    List<AssetVersion> findByAssetIdOrderByVersionNumber(String assetId);

    long countByAssetId(String assetId);

    int getMaxVersionNumber(String assetId);

    long countByDataSourceId(String dataSourceId);

    Optional<AssetVersion> findByAssetIdAtTime(String assetId, Instant timestamp);
}
