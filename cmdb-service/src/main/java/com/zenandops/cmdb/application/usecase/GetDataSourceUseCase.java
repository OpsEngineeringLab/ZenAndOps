package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.entity.DataSource;
import com.zenandops.cmdb.domain.exception.DataSourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single DataSource by its identifier.
 */
@ApplicationScoped
public class GetDataSourceUseCase {

    private final DataSourceRepository dataSourceRepository;

    @Inject
    public GetDataSourceUseCase(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    /**
     * Retrieve a DataSource by id.
     *
     * @param id the data source identifier
     * @return the DataSource
     * @throws DataSourceNotFoundException if no DataSource exists with the given id
     */
    public DataSource execute(String id) {
        return dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceNotFoundException(
                        "Data source not found with id: " + id));
    }
}
