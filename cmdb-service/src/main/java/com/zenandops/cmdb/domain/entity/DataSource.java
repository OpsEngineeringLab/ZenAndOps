package com.zenandops.cmdb.domain.entity;

import com.zenandops.cmdb.domain.vo.DataSourceType;

import java.time.Instant;

/**
 * Registered data origin (API, AGENT, FILE) with reliability rating.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class DataSource {

    private String id;
    private String name;
    private DataSourceType type;
    private int reliabilityRating;
    private Instant createdAt;
    private Instant updatedAt;

    public DataSource() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataSourceType getType() {
        return type;
    }

    public void setType(DataSourceType type) {
        this.type = type;
    }

    public int getReliabilityRating() {
        return reliabilityRating;
    }

    public void setReliabilityRating(int reliabilityRating) {
        this.reliabilityRating = reliabilityRating;
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
