package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.domain.entity.CIVersion;
import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the CIVersionRepository port.
 */
@ApplicationScoped
public class MongoCIVersionRepository implements CIVersionRepository {

    @Startup
    void createIndexes() {
        CIVersionPanacheEntity.mongoCollection().createIndex(
                Indexes.ascending("ciId", "versionNumber"), new IndexOptions().unique(true));
        CIVersionPanacheEntity.mongoCollection().createIndex(Indexes.ascending("ciId", "endDate"));
        CIVersionPanacheEntity.mongoCollection().createIndex(Indexes.ascending("dataSourceId"));
    }

    @Override
    public void save(CIVersion version) {
        CIVersionPanacheEntity entity = toEntity(version);
        if (version.getId() != null) {
            entity.id = new org.bson.types.ObjectId(version.getId());
            entity.update();
        } else {
            entity.persist();
            version.setId(entity.id.toString());
        }
    }

    @Override
    public List<CIVersion> findByCiId(String ciId) {
        return CIVersionPanacheEntity.<CIVersionPanacheEntity>list("ciId", ciId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<CIVersion> findActiveByCiId(String ciId) {
        return CIVersionPanacheEntity.<CIVersionPanacheEntity>find(
                "ciId = ?1 and endDate is null", ciId).firstResultOptional().map(this::toDomain);
    }

    @Override
    public List<CIVersion> findByCiIdOrderByVersionNumber(String ciId) {
        return CIVersionPanacheEntity.<CIVersionPanacheEntity>find(
                "ciId", Sort.ascending("versionNumber"), ciId)
                .list().stream().map(this::toDomain).toList();
    }

    @Override
    public long countByCiId(String ciId) {
        return CIVersionPanacheEntity.count("ciId", ciId);
    }

    @Override
    public int getMaxVersionNumber(String ciId) {
        return CIVersionPanacheEntity.<CIVersionPanacheEntity>find(
                "ciId", Sort.descending("versionNumber"), ciId)
                .firstResultOptional()
                .map(e -> e.versionNumber)
                .orElse(0);
    }

    @Override
    public long countByDataSourceId(String dataSourceId) {
        return CIVersionPanacheEntity.count("dataSourceId", dataSourceId);
    }

    @Override
    public Optional<CIVersion> findByCiIdAtTime(String ciId, Instant timestamp) {
        return CIVersionPanacheEntity.<CIVersionPanacheEntity>find(
                "ciId = ?1 and startDate <= ?2 and (endDate is null or endDate > ?2)",
                ciId, timestamp).firstResultOptional().map(this::toDomain);
    }

    private CIVersion toDomain(CIVersionPanacheEntity entity) {
        CIVersion version = new CIVersion();
        version.setId(entity.id.toString());
        version.setCiId(entity.ciId);
        version.setVersionNumber(entity.versionNumber);
        version.setAttributes(entity.attributes);
        version.setStartDate(entity.startDate);
        version.setEndDate(entity.endDate);
        version.setDataOrigin(entity.dataOrigin);
        version.setDataSourceId(entity.dataSourceId);
        version.setChangeReference(entity.changeReference);
        version.setCreatedAt(entity.createdAt);
        return version;
    }

    private CIVersionPanacheEntity toEntity(CIVersion version) {
        CIVersionPanacheEntity entity = new CIVersionPanacheEntity();
        entity.ciId = version.getCiId();
        entity.versionNumber = version.getVersionNumber();
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
