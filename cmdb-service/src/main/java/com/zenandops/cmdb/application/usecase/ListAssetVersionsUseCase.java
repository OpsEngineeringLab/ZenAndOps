package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all versions of a given asset, ordered by version number.
 */
@ApplicationScoped
public class ListAssetVersionsUseCase {

    private final AssetVersionRepository assetVersionRepository;

    @Inject
    public ListAssetVersionsUseCase(AssetVersionRepository assetVersionRepository) {
        this.assetVersionRepository = assetVersionRepository;
    }

    /**
     * List all versions for an asset ordered by version number.
     *
     * @param assetId the asset identifier
     * @return list of asset versions ordered by version number
     */
    public List<AssetVersion> execute(String assetId) {
        return assetVersionRepository.findByAssetIdOrderByVersionNumber(assetId);
    }
}
