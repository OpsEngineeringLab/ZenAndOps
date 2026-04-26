package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the Service domain entity.
 */
@MongoEntity(collection = "services", database = "zenandops-cmdb")
public class ServicePanacheEntity extends PanacheMongoEntity {

    public String name;
    public String description;
    public ServiceType type;

    @BsonProperty("parentId")
    public String parentId;

    @BsonProperty("organizationId")
    public String organizationId;

    @BsonProperty("businessOwner")
    public String businessOwner;

    @BsonProperty("technicalOwner")
    public String technicalOwner;

    public CriticalityLevel criticality;
    public ServiceStatus status;

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
