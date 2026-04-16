package com.zenandops.auth.infrastructure.adapter.persistence;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the Tag domain entity.
 * The unique compound index on { key: 1, value: 1 } is created via MongoTagRepository init.
 */
@MongoEntity(collection = "tags")
public class TagPanacheEntity extends PanacheMongoEntity {

    public String key;
    public String value;
    public String description;

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
