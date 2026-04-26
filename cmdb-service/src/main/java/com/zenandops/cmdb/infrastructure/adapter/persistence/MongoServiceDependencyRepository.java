package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import com.zenandops.cmdb.domain.entity.ServiceDependency;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the ServiceDependencyRepository port.
 */
@ApplicationScoped
public class MongoServiceDependencyRepository implements ServiceDependencyRepository {

    @Startup
    void createIndexes() {
        ServiceDependencyPanacheEntity.mongoCollection().createIndex(Indexes.ascending("sourceServiceId"));
        ServiceDependencyPanacheEntity.mongoCollection().createIndex(Indexes.ascending("targetServiceId"));
        ServiceDependencyPanacheEntity.mongoCollection().createIndex(
                Indexes.ascending("sourceServiceId", "targetServiceId"), new IndexOptions().unique(true));
    }

    @Override
    public void save(ServiceDependency dependency) {
        ServiceDependencyPanacheEntity entity = toEntity(dependency);
        if (dependency.getId() != null) {
            entity.id = new org.bson.types.ObjectId(dependency.getId());
            entity.update();
        } else {
            entity.persist();
            dependency.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<ServiceDependency> findById(String id) {
        return ServiceDependencyPanacheEntity.<ServiceDependencyPanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<ServiceDependency> findBySourceServiceId(String sourceServiceId) {
        return ServiceDependencyPanacheEntity.<ServiceDependencyPanacheEntity>list("sourceServiceId", sourceServiceId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ServiceDependency> findByTargetServiceId(String targetServiceId) {
        return ServiceDependencyPanacheEntity.<ServiceDependencyPanacheEntity>list("targetServiceId", targetServiceId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        ServiceDependencyPanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsBySourceServiceIdAndTargetServiceId(String sourceServiceId, String targetServiceId) {
        return ServiceDependencyPanacheEntity.count(
                "sourceServiceId = ?1 and targetServiceId = ?2", sourceServiceId, targetServiceId) > 0;
    }

    @Override
    public long countBySourceServiceIdOrTargetServiceId(String serviceId) {
        return ServiceDependencyPanacheEntity.count(
                "sourceServiceId = ?1 or targetServiceId = ?1", serviceId);
    }

    private ServiceDependency toDomain(ServiceDependencyPanacheEntity entity) {
        ServiceDependency dep = new ServiceDependency();
        dep.setId(entity.id.toString());
        dep.setSourceServiceId(entity.sourceServiceId);
        dep.setTargetServiceId(entity.targetServiceId);
        dep.setDependencyType(entity.dependencyType);
        dep.setCreatedAt(entity.createdAt);
        return dep;
    }

    private ServiceDependencyPanacheEntity toEntity(ServiceDependency dep) {
        ServiceDependencyPanacheEntity entity = new ServiceDependencyPanacheEntity();
        entity.sourceServiceId = dep.getSourceServiceId();
        entity.targetServiceId = dep.getTargetServiceId();
        entity.dependencyType = dep.getDependencyType();
        entity.createdAt = dep.getCreatedAt();
        return entity;
    }
}
