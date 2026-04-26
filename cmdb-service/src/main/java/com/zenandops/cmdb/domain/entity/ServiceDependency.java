package com.zenandops.cmdb.domain.entity;

import com.zenandops.cmdb.domain.vo.DependencyType;

import java.time.Instant;

/**
 * Directed dependency between two services (SYNCHRONOUS, ASYNCHRONOUS, CRITICAL).
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class ServiceDependency {

    private String id;
    private String sourceServiceId;
    private String targetServiceId;
    private DependencyType dependencyType;
    private Instant createdAt;

    public ServiceDependency() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceServiceId() {
        return sourceServiceId;
    }

    public void setSourceServiceId(String sourceServiceId) {
        this.sourceServiceId = sourceServiceId;
    }

    public String getTargetServiceId() {
        return targetServiceId;
    }

    public void setTargetServiceId(String targetServiceId) {
        this.targetServiceId = targetServiceId;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
