package com.zenandops.auth.domain.entity;

import java.time.Instant;

/**
 * Tag entity representing a key-value pair used for Attribute-Based Access Control.
 * Tags are assigned to users and matched against ABAC policies.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class Tag {

    private String id;
    private String key;
    private String value;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public Tag() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
