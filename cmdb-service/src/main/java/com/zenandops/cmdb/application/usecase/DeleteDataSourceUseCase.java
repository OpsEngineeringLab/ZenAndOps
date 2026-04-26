package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.exception.DataSourceInUseException;
import com.zenandops.cmdb.domain.exception.DataSourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a DataSource.
 * Validates not referenced by any AssetVersion or CIVersion before deletion.
 */
@ApplicationScoped
public class DeleteDataSourceUseCase {

    private final DataSourceRepository dataSourceRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final CIVersionRepository ciVersionRepository;

    @Inject
    public DeleteDataSourceUseCase(DataSourceRepository dataSourceRepository,
                                   AssetVersionRepository assetVersionRepository,
                                   CIVersionRepository ciVersionRepository) {
        this.dataSourceRepository = dataSourceRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.ciVersionRepository = ciVersionRepository;
    }

    /**
     * Delete a DataSource by id.
     *
     * @param id the data source identifier
     * @throws DataSourceNotFoundException if no DataSource exists with the given id
     * @throws DataSourceInUseException    if the DataSource is referenced by versions
     */
    public void execute(String id) {
        if (!dataSourceRepository.existsById(id)) {
            throw new DataSourceNotFoundException(
                    "Data source not found with id: " + id);
        }

        if (assetVersionRepository.countByDataSourceId(id) > 0) {
            throw new DataSourceInUseException(
                    "Data source is referenced by asset versions and cannot be deleted");
        }

        if (ciVersionRepository.countByDataSourceId(id) > 0) {
            throw new DataSourceInUseException(
                    "Data source is referenced by CI versions and cannot be deleted");
        }

        dataSourceRepository.deleteById(id);
    }
}
