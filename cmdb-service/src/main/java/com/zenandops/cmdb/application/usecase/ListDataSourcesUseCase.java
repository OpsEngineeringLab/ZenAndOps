package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.entity.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all DataSource entities.
 */
@ApplicationScoped
public class ListDataSourcesUseCase {

    private final DataSourceRepository dataSourceRepository;

    @Inject
    public ListDataSourcesUseCase(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    /**
     * List all data sources.
     *
     * @return list of all data sources
     */
    public List<DataSource> execute() {
        return dataSourceRepository.findAll();
    }
}
