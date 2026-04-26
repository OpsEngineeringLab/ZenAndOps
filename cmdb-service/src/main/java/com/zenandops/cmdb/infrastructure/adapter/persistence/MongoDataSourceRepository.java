package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.entity.DataSource;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the DataSourceRepository port.
 */
@ApplicationScoped
public class MongoDataSourceRepository implements DataSourceRepository {

    @Startup
    void createIndexes() {
        DataSourcePanacheEntity.mongoCollection().createIndex(
                Indexes.ascending("name"), new IndexOptions().unique(true));
    }

    @Override
    public void save(DataSource dataSource) {
        DataSourcePanacheEntity entity = toEntity(dataSource);
        if (dataSource.getId() != null) {
            entity.id = new org.bson.types.ObjectId(dataSource.getId());
            entity.update();
        } else {
            entity.persist();
            dataSource.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<DataSource> findById(String id) {
        return DataSourcePanacheEntity.<DataSourcePanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<DataSource> findAll() {
        return DataSourcePanacheEntity.<DataSourcePanacheEntity>listAll()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        DataSourcePanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsById(String id) {
        return DataSourcePanacheEntity.findByIdOptional(new org.bson.types.ObjectId(id)).isPresent();
    }

    @Override
    public boolean existsByName(String name) {
        return DataSourcePanacheEntity.count("name", name) > 0;
    }

    @Override
    public Optional<DataSource> findByName(String name) {
        return DataSourcePanacheEntity.<DataSourcePanacheEntity>find("name", name)
                .firstResultOptional().map(this::toDomain);
    }

    private DataSource toDomain(DataSourcePanacheEntity entity) {
        DataSource ds = new DataSource();
        ds.setId(entity.id.toString());
        ds.setName(entity.name);
        ds.setType(entity.type);
        ds.setReliabilityRating(entity.reliabilityRating);
        ds.setCreatedAt(entity.createdAt);
        ds.setUpdatedAt(entity.updatedAt);
        return ds;
    }

    private DataSourcePanacheEntity toEntity(DataSource ds) {
        DataSourcePanacheEntity entity = new DataSourcePanacheEntity();
        entity.name = ds.getName();
        entity.type = ds.getType();
        entity.reliabilityRating = ds.getReliabilityRating();
        entity.createdAt = ds.getCreatedAt();
        entity.updatedAt = ds.getUpdatedAt();
        return entity;
    }
}
