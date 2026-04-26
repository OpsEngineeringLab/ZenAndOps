package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.DataSource;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for DataSource persistence operations.
 */
public interface DataSourceRepository {

    void save(DataSource dataSource);

    Optional<DataSource> findById(String id);

    List<DataSource> findAll();

    void deleteById(String id);

    boolean existsById(String id);

    boolean existsByName(String name);

    Optional<DataSource> findByName(String name);
}
