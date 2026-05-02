package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import com.zenandops.cmdb.domain.entity.CIRelationship;
import com.zenandops.cmdb.domain.vo.RelationshipType;
import io.quarkus.panache.common.Page;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the CIRelationshipRepository port.
 */
@ApplicationScoped
public class MongoCIRelationshipRepository implements CIRelationshipRepository {

    @Startup
    void createIndexes() {
        CIRelationshipPanacheEntity.mongoCollection().createIndex(Indexes.ascending("sourceCIId"));
        CIRelationshipPanacheEntity.mongoCollection().createIndex(Indexes.ascending("targetCIId"));
        CIRelationshipPanacheEntity.mongoCollection().createIndex(
                Indexes.ascending("sourceCIId", "targetCIId", "relationshipType"),
                new IndexOptions().unique(true));
    }

    @Override
    public void save(CIRelationship relationship) {
        CIRelationshipPanacheEntity entity = toEntity(relationship);
        if (relationship.getId() != null) {
            entity.id = new org.bson.types.ObjectId(relationship.getId());
            entity.update();
        } else {
            entity.persist();
            relationship.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<CIRelationship> findById(String id) {
        return CIRelationshipPanacheEntity.<CIRelationshipPanacheEntity>findByIdOptional(
                new org.bson.types.ObjectId(id)).map(this::toDomain);
    }

    @Override
    public List<CIRelationship> findBySourceCIId(String sourceCIId) {
        return CIRelationshipPanacheEntity.<CIRelationshipPanacheEntity>list("sourceCIId", sourceCIId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<CIRelationship> findByTargetCIId(String targetCIId) {
        return CIRelationshipPanacheEntity.<CIRelationshipPanacheEntity>list("targetCIId", targetCIId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        CIRelationshipPanacheEntity.deleteById(new org.bson.types.ObjectId(id));
    }

    @Override
    public boolean existsBySourceCIIdAndTargetCIIdAndRelationshipType(String sourceCIId, String targetCIId,
                                                                       RelationshipType relationshipType) {
        return CIRelationshipPanacheEntity.count(
                "sourceCIId = ?1 and targetCIId = ?2 and relationshipType = ?3",
                sourceCIId, targetCIId, relationshipType) > 0;
    }

    @Override
    public long countBySourceCIIdOrTargetCIId(String ciId) {
        return CIRelationshipPanacheEntity.count(
                "sourceCIId = ?1 or targetCIId = ?1", ciId);
    }

    @Override
    public List<CIRelationship> findWithFilters(String ciId, int page, int size) {
        return CIRelationshipPanacheEntity.<CIRelationshipPanacheEntity>find(
                        "sourceCIId = ?1 or targetCIId = ?1", ciId)
                .page(Page.of(page, size)).list()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countWithFilters(String ciId) {
        return CIRelationshipPanacheEntity.count(
                "sourceCIId = ?1 or targetCIId = ?1", ciId);
    }

    private CIRelationship toDomain(CIRelationshipPanacheEntity entity) {
        CIRelationship rel = new CIRelationship();
        rel.setId(entity.id.toString());
        rel.setSourceCIId(entity.sourceCIId);
        rel.setTargetCIId(entity.targetCIId);
        rel.setRelationshipType(entity.relationshipType);
        rel.setCreatedAt(entity.createdAt);
        return rel;
    }

    private CIRelationshipPanacheEntity toEntity(CIRelationship rel) {
        CIRelationshipPanacheEntity entity = new CIRelationshipPanacheEntity();
        entity.sourceCIId = rel.getSourceCIId();
        entity.targetCIId = rel.getTargetCIId();
        entity.relationshipType = rel.getRelationshipType();
        entity.createdAt = rel.getCreatedAt();
        return entity;
    }
}
