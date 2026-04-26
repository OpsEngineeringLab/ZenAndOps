package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.RelationshipType;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the CIRelationship domain entity.
 */
@MongoEntity(collection = "ci_relationships", database = "zenandops-cmdb")
public class CIRelationshipPanacheEntity extends PanacheMongoEntity {

    @BsonProperty("sourceCIId")
    public String sourceCIId;

    @BsonProperty("targetCIId")
    public String targetCIId;

    @BsonProperty("relationshipType")
    public RelationshipType relationshipType;

    @BsonProperty("createdAt")
    public Instant createdAt;
}
