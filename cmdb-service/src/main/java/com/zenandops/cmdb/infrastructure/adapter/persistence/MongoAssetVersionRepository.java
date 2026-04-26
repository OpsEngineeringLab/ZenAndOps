package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the AssetVersionRepository port.
 */
@ApplicationScoped
public class MongoAssetVersionRepository implements AssetVersionRepository {

    @Startup
    void createIndexes() {
        AssetVersionPanacheEntity.mongoCollection().createIndex(
                Indexes.ascending("assetId", "versionNumber"), new IndexOptions().unique(true));
        AssetVersionPanacheEntity.mongoCollection().createIndex(Indexes.ascending("assetId", "endDate"));
        AssetVersionPanacheEntity.mongoCollection().createIndex(Indexes.ascending("dataSourceId"));
    }

    @Override
    public void save(AssetVersion version) {
        AssetVersionPanacheEntity entity = toEntity(version);
        if (version.getId() != null) {
            entity.id = new org.bson.types.ObjectId(version.getId());
            entity.update();
        } else {
            entity.persist();
            version.setId(entity.id.toString());
        }
    }

    @Override
    public List<AssetVersion> findByAssetId(String assetId) {
        return AssetVersionPanacheEntity.<AssetVersionPanacheEntity>list("assetId", assetId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AssetVersion> findActiveByAssetId(String assetId) {
        return AssetVersionPanacheEntity.<AssetVersionPanacheEntity>find(
                "assetId = ?1 and endDate is null", assetId).firstResultOptional().map(this::toDomain);
    }

    @Override
    public List<AssetVersion> findByAssetIdOrderByVersionNumber(String assetId) {
        return AssetVersionPanacheEntity.<AssetVersionPanacheEntity>find(
                "assetId", Sort.ascending("versionNumber"), assetId)
                .list().stream().map(this::toDomain).toList();
    }

    @Override
    public long countByAssetId(String assetId) {
        return AssetVersionPanacheEntity.count("assetId", assetId);
    }

    @Override
    public int getMaxVersionNumber(String assetId) {
        return AssetVersionPanacheEntity.<AssetVersionPanacheEntity>find(
                "assetId", Sort.descending("versionNumber"), assetId)
                .firstResultOptional()
                .map(e -> e.versionNumber)
                .orElse(0);
    }

    @Override
    public long countByDataSourceId(String dataSourceId) {
        return AssetVersionPanacheEntity.count("dataSourceId", dataSourceId);
    }

    @Override
    public Optional<AssetVersion> findByAssetIdAtTime(String assetId, Instant timestamp) {
        return AssetVersionPanacheEntity.<AssetVersionPanacheEntity>find(
                "assetId = ?1 and startDate <= ?2 and (endDate is null or endDate > ?2)",
                assetId, timestamp).firstResultOptional().map(this::toDomain);
    }

    private AssetVersion toDomain(AssetVersionPanacheEntity entity) {
        AssetVersion version = new AssetVersion();
        version.setId(entity.id.toString());
        version.setAssetId(entity.assetId);
        version.setVersionNumber(entity.versionNumber);
        version.setDescription(entity.description);
        version.setAttributes(entity.attributes);
        version.setStartDate(entity.startDate);
        version.setEndDate(entity.endDate);
        version.setDataOrigin(entity.dataOrigin);
        version.setDataSourceId(entity.dataSourceId);
        version.setChangeReference(entity.changeReference);
        version.setCreatedAt(entity.createdAt);
        return version;
    }

    private AssetVersionPanacheEntity toEntity(AssetVersion version) {
        AssetVersionPanacheEntity entity = new AssetVersionPanacheEntity();
        entity.assetId = version.getAssetId();
        entity.versionNumber = version.getVersionNumber();
        entity.description = version.getDescription();
        entity.attributes = version.getAttributes();
        entity.startDate = version.getStartDate();
        entity.endDate = version.getEndDate();
        entity.dataOrigin = version.getDataOrigin();
        entity.dataSourceId = version.getDataSourceId();
        entity.changeReference = version.getChangeReference();
        entity.createdAt = version.getCreatedAt();
        return entity;
    }
}
