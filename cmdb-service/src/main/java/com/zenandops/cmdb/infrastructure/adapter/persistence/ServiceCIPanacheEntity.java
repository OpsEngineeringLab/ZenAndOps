package com.zenandops.cmdb.infrastructure.adapter.persistence;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the ServiceCI domain entity.
 */
@MongoEntity(collection = "service_cis", database = "zenandops-cmdb")
public class ServiceCIPanacheEntity extends PanacheMongoEntity {

    @BsonProperty("serviceId")
    public String serviceId;

    @BsonProperty("ciId")
    public String ciId;

    @BsonProperty("createdAt")
    public Instant createdAt;
}
