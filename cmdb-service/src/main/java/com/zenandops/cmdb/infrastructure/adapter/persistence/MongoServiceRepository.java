package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.Indexes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the ServiceRepository port.
 */
@ApplicationScoped
public class MongoServiceRepository implements ServiceRepository {

    @Startup
    void createIndexes() {
        ServicePanacheEntity.mongoCollection().createIndex(Indexes.ascending("organizationId"));
        ServicePanacheEntity.mongoCollection().createIndex(Indexes.ascending("parentId"));
        ServicePanacheEntity.mongoCollection().createIndex(Indexes.ascending("type"));
        ServicePanacheEntity.mongoCollection().createIndex(Indexes.ascending("criticality"));
        ServicePanacheEntity.mongoCollection().createIndex(Indexes.ascending("status"));
    }

    @Override
    public void save(Service service) {
        ServicePanacheEntity entity = toEntity(service);
        if (service.getId() != null) {
            entity.id = new org.bson.types.ObjectId(service.getId());
            entity.update();
        } else {
            entity.persist();
            service.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<Service> findById(String id) {
        return ServicePanacheEntity.<ServicePanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<Service> findAll() {
        return ServicePanacheEntity.<ServicePanacheEntity>listAll()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Service> findByParentId(String parentId) {
        return ServicePanacheEntity.<ServicePanacheEntity>list("parentId", parentId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Service> findByOrganizationId(String organizationId) {
        return ServicePanacheEntity.<ServicePanacheEntity>list("organizationId", organizationId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        ServicePanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsById(String id) {
        return ServicePanacheEntity.findByIdOptional(new org.bson.types.ObjectId(id)).isPresent();
    }

    @Override
    public long countByParentId(String parentId) {
        return ServicePanacheEntity.count("parentId", parentId);
    }

    @Override
    public long countByOrganizationId(String organizationId) {
        return ServicePanacheEntity.count("organizationId", organizationId);
    }

    @Override
    public List<Service> findWithFilters(String organizationId, ServiceType type,
                                          CriticalityLevel criticality, ServiceStatus status) {
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
        if (criticality != null) {
            conditions.add("criticality = :criticality");
            params.put("criticality", criticality);
        }
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }

        if (conditions.isEmpty()) {
            return findAll();
        }

        query.append(String.join(" and ", conditions));
        return ServicePanacheEntity.<ServicePanacheEntity>list(query.toString(), params)
                .stream().map(this::toDomain).toList();
    }

    private Service toDomain(ServicePanacheEntity entity) {
        Service svc = new Service();
        svc.setId(entity.id.toString());
        svc.setName(entity.name);
        svc.setDescription(entity.description);
        svc.setType(entity.type);
        svc.setParentId(entity.parentId);
        svc.setOrganizationId(entity.organizationId);
        svc.setBusinessOwner(entity.businessOwner);
        svc.setTechnicalOwner(entity.technicalOwner);
        svc.setCriticality(entity.criticality);
        svc.setStatus(entity.status);
        svc.setCreatedAt(entity.createdAt);
        svc.setUpdatedAt(entity.updatedAt);
        return svc;
    }

    private ServicePanacheEntity toEntity(Service svc) {
        ServicePanacheEntity entity = new ServicePanacheEntity();
        entity.name = svc.getName();
        entity.description = svc.getDescription();
        entity.type = svc.getType();
        entity.parentId = svc.getParentId();
        entity.organizationId = svc.getOrganizationId();
        entity.businessOwner = svc.getBusinessOwner();
        entity.technicalOwner = svc.getTechnicalOwner();
        entity.criticality = svc.getCriticality();
        entity.status = svc.getStatus();
        entity.createdAt = svc.getCreatedAt();
        entity.updatedAt = svc.getUpdatedAt();
        return entity;
    }
}
