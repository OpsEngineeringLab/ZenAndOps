package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.domain.vo.OrganizationType;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * MongoDB Panache adapter implementing the OrganizationRepository port.
 */
@ApplicationScoped
public class MongoOrganizationRepository implements OrganizationRepository {

    @Startup
    void createIndexes() {
        OrganizationPanacheEntity.mongoCollection().createIndex(Indexes.ascending("parentId"));
        OrganizationPanacheEntity.mongoCollection().createIndex(Indexes.ascending("type"));
        OrganizationPanacheEntity.mongoCollection().createIndex(
                Indexes.ascending("parentId", "name"), new IndexOptions().unique(true));
    }

    @Override
    public void save(Organization organization) {
        OrganizationPanacheEntity entity = toEntity(organization);
        if (organization.getId() != null) {
            entity.id = new org.bson.types.ObjectId(organization.getId());
            entity.update();
        } else {
            entity.persist();
            organization.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<Organization> findById(String id) {
        return OrganizationPanacheEntity.<OrganizationPanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<Organization> findAll() {
        return OrganizationPanacheEntity.<OrganizationPanacheEntity>listAll()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Organization> findByParentId(String parentId) {
        return OrganizationPanacheEntity.<OrganizationPanacheEntity>list("parentId", parentId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        OrganizationPanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsById(String id) {
        return OrganizationPanacheEntity.findByIdOptional(new org.bson.types.ObjectId(id)).isPresent();
    }

    @Override
    public long countByParentId(String parentId) {
        return OrganizationPanacheEntity.count("parentId", parentId);
    }

    @Override
    public boolean existsByParentIdAndName(String parentId, String name) {
        if (parentId == null) {
            return OrganizationPanacheEntity.count("parentId is null and name", name) > 0;
        }
        return OrganizationPanacheEntity.count("parentId = ?1 and name = ?2", parentId, name) > 0;
    }

    @Override
    public long countByType(OrganizationType type) {
        return OrganizationPanacheEntity.count("type", type);
    }

    private Organization toDomain(OrganizationPanacheEntity entity) {
        Organization org = new Organization();
        org.setId(entity.id.toString());
        org.setName(entity.name);
        org.setType(entity.type);
        org.setParentId(entity.parentId);
        org.setResponsiblePerson(entity.responsiblePerson);
        org.setCostCenter(entity.costCenter);
        org.setCreatedAt(entity.createdAt);
        org.setUpdatedAt(entity.updatedAt);
        return org;
    }

    private OrganizationPanacheEntity toEntity(Organization org) {
        OrganizationPanacheEntity entity = new OrganizationPanacheEntity();
        entity.name = org.getName();
        entity.type = org.getType();
        entity.parentId = org.getParentId();
        entity.responsiblePerson = org.getResponsiblePerson();
        entity.costCenter = org.getCostCenter();
        entity.createdAt = org.getCreatedAt();
        entity.updatedAt = org.getUpdatedAt();
        return entity;
    }
}
