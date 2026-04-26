package com.zenandops.cmdb.domain.entity;

import com.zenandops.cmdb.domain.vo.DataOrigin;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of asset attributes at a point in time.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class AssetVersion {

    private String id;
    private String assetId;
    private int versionNumber;
    private String description;
    private Map<String, Object> attributes;
    private Instant startDate;
    private Instant endDate;
    private DataOrigin dataOrigin;
    private String dataSourceId;
    private String changeReference;
    private Instant createdAt;

    public AssetVersion() {
        this.attributes = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public DataOrigin getDataOrigin() {
        return dataOrigin;
    }

    public void setDataOrigin(DataOrigin dataOrigin) {
        this.dataOrigin = dataOrigin;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getChangeReference() {
        return changeReference;
    }

    public void setChangeReference(String changeReference) {
        this.changeReference = changeReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
