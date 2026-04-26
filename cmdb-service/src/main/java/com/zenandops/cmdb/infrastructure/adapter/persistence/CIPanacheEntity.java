package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the CI domain entity.
 */
@MongoEntity(collection = "cis", database = "zenandops-cmdb")
public class CIPanacheEntity extends PanacheMongoEntity {

    public String name;
    public CIType type;

    @BsonProperty("organizationId")
    public String organizationId;

    @BsonProperty("assetId")
    public String assetId;

    public CIStatus status;

    @BsonProperty("controlledExceptionFlag")
    public boolean controlledExceptionFlag;

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
