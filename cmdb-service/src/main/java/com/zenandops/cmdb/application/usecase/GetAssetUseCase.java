package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.exception.AssetNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single Asset by its identifier.
 */
@ApplicationScoped
public class GetAssetUseCase {

    private final AssetRepository assetRepository;

    @Inject
    public GetAssetUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * Retrieve an Asset by id.
     *
     * @param id the asset identifier
     * @return the Asset
     * @throws AssetNotFoundException if no Asset exists with the given id
     */
    public Asset execute(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(
                        "Asset not found with id: " + id));
    }
}
