package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import io.quarkus.panache.common.Page;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.Indexes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the CIRepository port.
 */
@ApplicationScoped
public class MongoCIRepository implements CIRepository {

    @Startup
    void createIndexes() {
        CIPanacheEntity.mongoCollection().createIndex(Indexes.ascending("organizationId"));
        CIPanacheEntity.mongoCollection().createIndex(Indexes.ascending("type"));
        CIPanacheEntity.mongoCollection().createIndex(Indexes.ascending("status"));
        CIPanacheEntity.mongoCollection().createIndex(Indexes.ascending("assetId"));
    }

    @Override
    public void save(CI ci) {
        CIPanacheEntity entity = toEntity(ci);
        if (ci.getId() != null) {
            entity.id = new org.bson.types.ObjectId(ci.getId());
            entity.update();
        } else {
            entity.persist();
            ci.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<CI> findById(String id) {
        return CIPanacheEntity.<CIPanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<CI> findAll() {
        return CIPanacheEntity.<CIPanacheEntity>listAll()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<CI> findByOrganizationId(String organizationId) {
        return CIPanacheEntity.<CIPanacheEntity>list("organizationId", organizationId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<CI> findByAssetId(String assetId) {
        return CIPanacheEntity.<CIPanacheEntity>list("assetId", assetId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        CIPanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsById(String id) {
        return CIPanacheEntity.findByIdOptional(new org.bson.types.ObjectId(id)).isPresent();
    }

    @Override
    public long countByOrganizationId(String organizationId) {
        return CIPanacheEntity.count("organizationId", organizationId);
    }

    @Override
    public long countByAssetId(String assetId) {
        return CIPanacheEntity.count("assetId", assetId);
    }

    @Override
    public List<CI> findWithFilters(String organizationId, CIType type, CIStatus status, String assetId) {
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
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }
        if (assetId != null) {
            conditions.add("assetId = :assetId");
            params.put("assetId", assetId);
        }

        if (conditions.isEmpty()) {
            return findAll();
        }

        String query = String.join(" and ", conditions);
        return CIPanacheEntity.<CIPanacheEntity>list(query, params)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<CI> findWithFilters(String organizationId, CIType type, CIStatus status, String assetId,
                                     int page, int size) {
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
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }
        if (assetId != null) {
            conditions.add("assetId = :assetId");
            params.put("assetId", assetId);
        }

        if (conditions.isEmpty()) {
            return CIPanacheEntity.<CIPanacheEntity>findAll()
                    .page(Page.of(page, size)).list()
                    .stream().map(this::toDomain).toList();
        }

        String query = String.join(" and ", conditions);
        return CIPanacheEntity.<CIPanacheEntity>find(query, params)
                .page(Page.of(page, size)).list()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countWithFilters(String organizationId, CIType type, CIStatus status, String assetId) {
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
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }
        if (assetId != null) {
            conditions.add("assetId = :assetId");
            params.put("assetId", assetId);
        }

        if (conditions.isEmpty()) {
            return CIPanacheEntity.count();
        }

        String query = String.join(" and ", conditions);
        return CIPanacheEntity.count(query, params);
    }

    private CI toDomain(CIPanacheEntity entity) {
        CI ci = new CI();
        ci.setId(entity.id.toString());
        ci.setName(entity.name);
        ci.setType(entity.type);
        ci.setOrganizationId(entity.organizationId);
        ci.setAssetId(entity.assetId);
        ci.setStatus(entity.status);
        ci.setControlledExceptionFlag(entity.controlledExceptionFlag);
        ci.setCreatedAt(entity.createdAt);
        ci.setUpdatedAt(entity.updatedAt);
        return ci;
    }

    private CIPanacheEntity toEntity(CI ci) {
        CIPanacheEntity entity = new CIPanacheEntity();
        entity.name = ci.getName();
        entity.type = ci.getType();
        entity.organizationId = ci.getOrganizationId();
        entity.assetId = ci.getAssetId();
        entity.status = ci.getStatus();
        entity.controlledExceptionFlag = ci.isControlledExceptionFlag();
        entity.createdAt = ci.getCreatedAt();
        entity.updatedAt = ci.getUpdatedAt();
        return entity;
    }
}
