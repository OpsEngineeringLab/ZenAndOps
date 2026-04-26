package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.DataSourceType;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the DataSource domain entity.
 */
@MongoEntity(collection = "data_sources", database = "zenandops-cmdb")
public class DataSourcePanacheEntity extends PanacheMongoEntity {

    public String name;
    public DataSourceType type;

    @BsonProperty("reliabilityRating")
    public int reliabilityRating;

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
