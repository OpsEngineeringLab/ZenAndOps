package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.entity.DataSource;
import com.zenandops.cmdb.domain.exception.DuplicateDataSourceNameException;
import com.zenandops.cmdb.domain.exception.InvalidReliabilityRatingException;
import com.zenandops.cmdb.domain.vo.DataSourceType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for creating a new DataSource.
 * Validates name uniqueness and reliability rating range [0-100].
 */
@ApplicationScoped
public class CreateDataSourceUseCase {

    private final DataSourceRepository dataSourceRepository;

    @Inject
    public CreateDataSourceUseCase(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    /**
     * Create a new DataSource.
     *
     * @param name              the data source name
     * @param type              the data source type (API, AGENT, FILE)
     * @param reliabilityRating the reliability rating (0-100)
     * @return the created DataSource
     */
    public DataSource execute(String name, DataSourceType type, int reliabilityRating) {
        if (reliabilityRating < 0 || reliabilityRating > 100) {
            throw new InvalidReliabilityRatingException(
                    "Reliability rating must be between 0 and 100, got: " + reliabilityRating);
        }

        if (dataSourceRepository.existsByName(name)) {
            throw new DuplicateDataSourceNameException(
                    "Data source with name '" + name + "' already exists");
        }

        Instant now = Instant.now();
        DataSource dataSource = new DataSource();
        dataSource.setName(name);
        dataSource.setType(type);
        dataSource.setReliabilityRating(reliabilityRating);
        dataSource.setCreatedAt(now);
        dataSource.setUpdatedAt(now);

        dataSourceRepository.save(dataSource);
        return dataSource;
    }
}
