package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the ServiceCIRepository port.
 */
@ApplicationScoped
public class MongoServiceCIRepository implements ServiceCIRepository {

    @Startup
    void createIndexes() {
        ServiceCIPanacheEntity.mongoCollection().createIndex(Indexes.ascending("serviceId"));
        ServiceCIPanacheEntity.mongoCollection().createIndex(Indexes.ascending("ciId"));
        ServiceCIPanacheEntity.mongoCollection().createIndex(
                Indexes.ascending("serviceId", "ciId"), new IndexOptions().unique(true));
    }

    @Override
    public void save(ServiceCI serviceCI) {
        ServiceCIPanacheEntity entity = toEntity(serviceCI);
        if (serviceCI.getId() != null) {
            entity.id = new org.bson.types.ObjectId(serviceCI.getId());
            entity.update();
        } else {
            entity.persist();
            serviceCI.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<ServiceCI> findById(String id) {
        return ServiceCIPanacheEntity.<ServiceCIPanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<ServiceCI> findByServiceId(String serviceId) {
        return ServiceCIPanacheEntity.<ServiceCIPanacheEntity>list("serviceId", serviceId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ServiceCI> findByCiId(String ciId) {
        return ServiceCIPanacheEntity.<ServiceCIPanacheEntity>list("ciId", ciId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        ServiceCIPanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsByServiceIdAndCiId(String serviceId, String ciId) {
        return ServiceCIPanacheEntity.count(
                "serviceId = ?1 and ciId = ?2", serviceId, ciId) > 0;
    }

    @Override
    public long countByCiId(String ciId) {
        return ServiceCIPanacheEntity.count("ciId", ciId);
    }

    @Override
    public long countByServiceId(String serviceId) {
        return ServiceCIPanacheEntity.count("serviceId", serviceId);
    }

    private ServiceCI toDomain(ServiceCIPanacheEntity entity) {
        ServiceCI sci = new ServiceCI();
        sci.setId(entity.id.toString());
        sci.setServiceId(entity.serviceId);
        sci.setCiId(entity.ciId);
        sci.setCreatedAt(entity.createdAt);
        return sci;
    }

    private ServiceCIPanacheEntity toEntity(ServiceCI sci) {
        ServiceCIPanacheEntity entity = new ServiceCIPanacheEntity();
        entity.serviceId = sci.getServiceId();
        entity.ciId = sci.getCiId();
        entity.createdAt = sci.getCreatedAt();
        return entity;
    }
}
