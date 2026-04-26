package com.zenandops.cmdb.domain.entity;

import com.zenandops.cmdb.domain.vo.RelationshipType;

import java.time.Instant;

/**
 * Directed relationship between two CIs (DEPENDS_ON, HOSTS, CONNECTS_TO).
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class CIRelationship {

    private String id;
    private String sourceCIId;
    private String targetCIId;
    private RelationshipType relationshipType;
    private Instant createdAt;

    public CIRelationship() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceCIId() {
        return sourceCIId;
    }

    public void setSourceCIId(String sourceCIId) {
        this.sourceCIId = sourceCIId;
    }

    public String getTargetCIId() {
        return targetCIId;
    }

    public void setTargetCIId(String targetCIId) {
        this.targetCIId = targetCIId;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
