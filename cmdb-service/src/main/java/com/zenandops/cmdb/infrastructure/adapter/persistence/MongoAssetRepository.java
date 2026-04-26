package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.Indexes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the AssetRepository port.
 */
@ApplicationScoped
public class MongoAssetRepository implements AssetRepository {

    @Startup
    void createIndexes() {
        AssetPanacheEntity.mongoCollection().createIndex(Indexes.ascending("organizationId"));
        AssetPanacheEntity.mongoCollection().createIndex(Indexes.ascending("type"));
        AssetPanacheEntity.mongoCollection().createIndex(Indexes.ascending("costType"));
        AssetPanacheEntity.mongoCollection().createIndex(Indexes.ascending("status"));
        AssetPanacheEntity.mongoCollection().createIndex(Indexes.ascending("supplier"));
    }

    @Override
    public void save(Asset asset) {
        AssetPanacheEntity entity = toEntity(asset);
        if (asset.getId() != null) {
            entity.id = new org.bson.types.ObjectId(asset.getId());
            entity.update();
        } else {
            entity.persist();
            asset.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<Asset> findById(String id) {
        return AssetPanacheEntity.<AssetPanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<Asset> findAll() {
        return AssetPanacheEntity.<AssetPanacheEntity>listAll()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Asset> findByOrganizationId(String organizationId) {
        return AssetPanacheEntity.<AssetPanacheEntity>list("organizationId", organizationId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        AssetPanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsById(String id) {
        return AssetPanacheEntity.findByIdOptional(new org.bson.types.ObjectId(id)).isPresent();
    }

    @Override
    public long countByOrganizationId(String organizationId) {
        return AssetPanacheEntity.count("organizationId", organizationId);
    }

    @Override
    public List<Asset> findWithFilters(String organizationId, AssetType type, CostType costType,
                                        AssetStatus status, String supplier) {
        StringBuilder query = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();

        if (organizationId != null) {
            conditions.add("organizationId = :organizationId");
            params.put("organizationId", organizationId);
        }
        if (type != null) {
            conditions.add("type = :type");
            params.put("type", type);
        }
        if (costType != null) {
            conditions.add("costType = :costType");
            params.put("costType", costType);
        }
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }
        if (supplier != null) {
            conditions.add("supplier = :supplier");
            params.put("supplier", supplier);
        }

        if (conditions.isEmpty()) {
            return findAll();
        }

        query.append(String.join(" and ", conditions));
        return AssetPanacheEntity.<AssetPanacheEntity>list(query.toString(), params)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Asset> getCostSummary() {
        return findAll();
    }

    private Asset toDomain(AssetPanacheEntity entity) {
        Asset asset = new Asset();
        asset.setId(entity.id.toString());
        asset.setName(entity.name);
        asset.setType(entity.type);
        asset.setOrganizationId(entity.organizationId);
        asset.setCost(entity.cost);
        asset.setCostType(entity.costType);
        asset.setAcquisitionDate(entity.acquisitionDate);
        asset.setStatus(entity.status);
        asset.setSupplier(entity.supplier);
        asset.setCreatedAt(entity.createdAt);
        asset.setUpdatedAt(entity.updatedAt);
        return asset;
    }

    private AssetPanacheEntity toEntity(Asset asset) {
        AssetPanacheEntity entity = new AssetPanacheEntity();
        entity.name = asset.getName();
        entity.type = asset.getType();
        entity.organizationId = asset.getOrganizationId();
        entity.cost = asset.getCost();
        entity.costType = asset.getCostType();
        entity.acquisitionDate = asset.getAcquisitionDate();
        entity.status = asset.getStatus();
        entity.supplier = asset.getSupplier();
        entity.createdAt = asset.getCreatedAt();
        entity.updatedAt = asset.getUpdatedAt();
        return entity;
    }
}
