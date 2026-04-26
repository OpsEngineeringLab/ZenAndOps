package com.zenandops.cmdb.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Record of a reconciliation operation with report.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class ReconciliationRecord {

    private String id;
    private String entityType;
    private int recordsAnalyzed;
    private int duplicatesFound;
    private int conflictsResolved;
    private int unresolvedConflicts;
    private List<ReconciliationDetail> details;
    private String triggeredBy;
    private Instant createdAt;

    public ReconciliationRecord() {
        this.details = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getRecordsAnalyzed() {
        return recordsAnalyzed;
    }

    public void setRecordsAnalyzed(int recordsAnalyzed) {
        this.recordsAnalyzed = recordsAnalyzed;
    }

    public int getDuplicatesFound() {
        return duplicatesFound;
    }

    public void setDuplicatesFound(int duplicatesFound) {
        this.duplicatesFound = duplicatesFound;
    }

    public int getConflictsResolved() {
        return conflictsResolved;
    }

    public void setConflictsResolved(int conflictsResolved) {
        this.conflictsResolved = conflictsResolved;
    }

    public int getUnresolvedConflicts() {
        return unresolvedConflicts;
    }

    public void setUnresolvedConflicts(int unresolvedConflicts) {
        this.unresolvedConflicts = unresolvedConflicts;
    }

    public List<ReconciliationDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ReconciliationDetail> details) {
        this.details = details;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Detail of a single reconciliation finding for an entity.
     */
    public static class ReconciliationDetail {

        private String entityId;
        private String entityName;
        private String conflictType;
        private String resolution;
        private String preferredSourceId;

        public ReconciliationDetail() {
        }

        public ReconciliationDetail(String entityId, String entityName, String conflictType,
                                    String resolution, String preferredSourceId) {
            this.entityId = entityId;
            this.entityName = entityName;
            this.conflictType = conflictType;
            this.resolution = resolution;
            this.preferredSourceId = preferredSourceId;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public String getConflictType() {
            return conflictType;
        }

        public void setConflictType(String conflictType) {
            this.conflictType = conflictType;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getPreferredSourceId() {
            return preferredSourceId;
        }

        public void setPreferredSourceId(String preferredSourceId) {
            this.preferredSourceId = preferredSourceId;
        }
    }
}
