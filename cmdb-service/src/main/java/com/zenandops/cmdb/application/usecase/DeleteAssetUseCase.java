package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.domain.exception.AssetInUseException;
import com.zenandops.cmdb.domain.exception.AssetNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting an Asset.
 * Validates no CIs reference this asset and no active versions exist before deletion.
 */
@ApplicationScoped
public class DeleteAssetUseCase {

    private final AssetRepository assetRepository;
    private final CIRepository ciRepository;
    private final AssetVersionRepository assetVersionRepository;

    @Inject
    public DeleteAssetUseCase(AssetRepository assetRepository,
                              CIRepository ciRepository,
                              AssetVersionRepository assetVersionRepository) {
        this.assetRepository = assetRepository;
        this.ciRepository = ciRepository;
        this.assetVersionRepository = assetVersionRepository;
    }

    /**
     * Delete an Asset by id.
     *
     * @param id the asset identifier
     * @throws AssetNotFoundException if no Asset exists with the given id
     * @throws AssetInUseException    if the Asset has CIs or active versions
     */
    public void execute(String id) {
        if (!assetRepository.existsById(id)) {
            throw new AssetNotFoundException("Asset not found with id: " + id);
        }

        if (ciRepository.countByAssetId(id) > 0) {
            throw new AssetInUseException(
                    "Asset has associated CIs and cannot be deleted");
        }

        if (assetVersionRepository.countByAssetId(id) > 0) {
            throw new AssetInUseException(
                    "Asset has active versions and cannot be deleted");
        }

        assetRepository.deleteById(id);
    }
}
