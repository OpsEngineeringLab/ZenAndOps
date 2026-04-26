package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.entity.DataSource;
import com.zenandops.cmdb.domain.exception.DataSourceNotFoundException;
import com.zenandops.cmdb.domain.exception.DuplicateDataSourceNameException;
import com.zenandops.cmdb.domain.exception.InvalidReliabilityRatingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for updating an existing DataSource.
 * Validates name uniqueness if changed and reliability rating range.
 */
@ApplicationScoped
public class UpdateDataSourceUseCase {

    private final DataSourceRepository dataSourceRepository;

    @Inject
    public UpdateDataSourceUseCase(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    /**
     * Update a DataSource's mutable fields.
     *
     * @param id                the data source identifier
     * @param name              the new name
     * @param reliabilityRating the new reliability rating (0-100)
     * @return the updated DataSource
     */
    public DataSource execute(String id, String name, int reliabilityRating) {
        if (reliabilityRating < 0 || reliabilityRating > 100) {
            throw new InvalidReliabilityRatingException(
                    "Reliability rating must be between 0 and 100, got: " + reliabilityRating);
        }

        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceNotFoundException(
                        "Data source not found with id: " + id));

        if (!dataSource.getName().equals(name) && dataSourceRepository.existsByName(name)) {
            throw new DuplicateDataSourceNameException(
                    "Data source with name '" + name + "' already exists");
        }

        dataSource.setName(name);
        dataSource.setReliabilityRating(reliabilityRating);
        dataSource.setUpdatedAt(Instant.now());

        dataSourceRepository.save(dataSource);
        return dataSource;
    }
}
