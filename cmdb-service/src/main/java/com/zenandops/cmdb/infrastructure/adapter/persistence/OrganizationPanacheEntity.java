package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.OrganizationType;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the Organization domain entity.
 */
@MongoEntity(collection = "organizations", database = "zenandops-cmdb")
public class OrganizationPanacheEntity extends PanacheMongoEntity {

    public String name;
    public OrganizationType type;

    @BsonProperty("parentId")
    public String parentId;

    @BsonProperty("responsiblePerson")
    public String responsiblePerson;

    @BsonProperty("costCenter")
    public String costCenter;

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
