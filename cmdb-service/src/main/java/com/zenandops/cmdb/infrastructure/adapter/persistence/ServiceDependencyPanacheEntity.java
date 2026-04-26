package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.DependencyType;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the ServiceDependency domain entity.
 */
@MongoEntity(collection = "service_dependencies", database = "zenandops-cmdb")
public class ServiceDependencyPanacheEntity extends PanacheMongoEntity {

    @BsonProperty("sourceServiceId")
    public String sourceServiceId;

    @BsonProperty("targetServiceId")
    public String targetServiceId;

    @BsonProperty("dependencyType")
    public DependencyType dependencyType;

    @BsonProperty("createdAt")
    public Instant createdAt;
}
