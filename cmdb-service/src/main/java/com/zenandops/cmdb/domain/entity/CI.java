package com.zenandops.cmdb.domain.entity;

import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;

import java.time.Instant;

/**
 * Operational component (VM, DATABASE, API, STORAGE, NETWORK) managed to deliver IT services.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class CI {

    private String id;
    private String name;
    private CIType type;
    private String organizationId;
    private String assetId;
    private CIStatus status;
    private boolean controlledExceptionFlag;
    private Instant createdAt;
    private Instant updatedAt;

    public CI() {
        this.controlledExceptionFlag = false;
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

    public CIType getType() {
        return type;
    }

    public void setType(CIType type) {
        this.type = type;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public CIStatus getStatus() {
        return status;
    }

    public void setStatus(CIStatus status) {
        this.status = status;
    }

    public boolean isControlledExceptionFlag() {
        return controlledExceptionFlag;
    }

    public void setControlledExceptionFlag(boolean controlledExceptionFlag) {
        this.controlledExceptionFlag = controlledExceptionFlag;
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
