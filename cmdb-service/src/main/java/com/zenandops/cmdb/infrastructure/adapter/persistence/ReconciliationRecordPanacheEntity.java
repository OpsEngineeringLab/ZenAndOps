package com.zenandops.cmdb.infrastructure.adapter.persistence;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB Panache entity mapping for the ReconciliationRecord domain entity.
 */
@MongoEntity(collection = "reconciliation_records", database = "zenandops-cmdb")
public class ReconciliationRecordPanacheEntity extends PanacheMongoEntity {

    @BsonProperty("entityType")
    public String entityType;

    @BsonProperty("recordsAnalyzed")
    public int recordsAnalyzed;

    @BsonProperty("duplicatesFound")
    public int duplicatesFound;

    @BsonProperty("conflictsResolved")
    public int conflictsResolved;

    @BsonProperty("unresolvedConflicts")
    public int unresolvedConflicts;

    public List<ReconciliationDetailDocument> details;

    @BsonProperty("triggeredBy")
    public String triggeredBy;

    @BsonProperty("createdAt")
    public Instant createdAt;

    /**
     * Embedded document for reconciliation details.
     */
    public static class ReconciliationDetailDocument {
        @BsonProperty("entityId")
        public String entityId;
        @BsonProperty("entityName")
        public String entityName;
        @BsonProperty("conflictType")
        public String conflictType;
        public String resolution;
        @BsonProperty("preferredSourceId")
        public String preferredSourceId;
    }
}
