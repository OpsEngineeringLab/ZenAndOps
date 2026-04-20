package com.zenandops.auth.infrastructure.adapter.persistence;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Panache entity mapping for the Role domain entity.
 * The unique index on { name: 1 } is created via MongoRoleRepository init.
 */
@MongoEntity(collection = "roles")
public class RolePanacheEntity extends PanacheMongoEntity {

    public String name;
    public String description;
    public List<String> permissions = new ArrayList<>();

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
