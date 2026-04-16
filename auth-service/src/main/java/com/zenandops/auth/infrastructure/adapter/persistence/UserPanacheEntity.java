package com.zenandops.auth.infrastructure.adapter.persistence;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Panache entity mapping for the User domain entity.
 */
@MongoEntity(collection = "users")
public class UserPanacheEntity extends PanacheMongoEntity {

    public String login;
    public String name;
    public String email;

    @BsonProperty("passwordHash")
    public String passwordHash;

    public List<String> roles = new ArrayList<>();
    public List<String> tagIds = new ArrayList<>();
    public boolean active = true;

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
