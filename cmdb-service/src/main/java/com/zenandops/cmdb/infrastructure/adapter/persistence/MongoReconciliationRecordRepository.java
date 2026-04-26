package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.ReconciliationRecordRepository;
import com.zenandops.cmdb.domain.entity.ReconciliationRecord;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Panache adapter implementing the ReconciliationRecordRepository port.
 */
@ApplicationScoped
public class MongoReconciliationRecordRepository implements ReconciliationRecordRepository {

    @Override
    public void save(ReconciliationRecord record) {
        ReconciliationRecordPanacheEntity entity = toEntity(record);
        if (record.getId() != null) {
            entity.id = new org.bson.types.ObjectId(record.getId());
            entity.update();
        } else {
            entity.persist();
            record.setId(entity.id.toString());
        }
    }

    @Override
    public List<ReconciliationRecord> findAll() {
        return ReconciliationRecordPanacheEntity.<ReconciliationRecordPanacheEntity>listAll()
                .stream().map(this::toDomain).toList();
    }

    private ReconciliationRecord toDomain(ReconciliationRecordPanacheEntity entity) {
        ReconciliationRecord record = new ReconciliationRecord();
        record.setId(entity.id.toString());
        record.setEntityType(entity.entityType);
        record.setRecordsAnalyzed(entity.recordsAnalyzed);
        record.setDuplicatesFound(entity.duplicatesFound);
        record.setConflictsResolved(entity.conflictsResolved);
        record.setUnresolvedConflicts(entity.unresolvedConflicts);
        record.setTriggeredBy(entity.triggeredBy);
        record.setCreatedAt(entity.createdAt);

        if (entity.details != null) {
            List<ReconciliationRecord.ReconciliationDetail> details = entity.details.stream()
                    .map(d -> new ReconciliationRecord.ReconciliationDetail(
                            d.entityId, d.entityName, d.conflictType, d.resolution, d.preferredSourceId))
                    .toList();
            record.setDetails(details);
        } else {
            record.setDetails(new ArrayList<>());
        }

        return record;
    }

    private ReconciliationRecordPanacheEntity toEntity(ReconciliationRecord record) {
        ReconciliationRecordPanacheEntity entity = new ReconciliationRecordPanacheEntity();
        entity.entityType = record.getEntityType();
        entity.recordsAnalyzed = record.getRecordsAnalyzed();
        entity.duplicatesFound = record.getDuplicatesFound();
        entity.conflictsResolved = record.getConflictsResolved();
        entity.unresolvedConflicts = record.getUnresolvedConflicts();
        entity.triggeredBy = record.getTriggeredBy();
        entity.createdAt = record.getCreatedAt();

        if (record.getDetails() != null) {
            entity.details = record.getDetails().stream().map(d -> {
                ReconciliationRecordPanacheEntity.ReconciliationDetailDocument doc =
                        new ReconciliationRecordPanacheEntity.ReconciliationDetailDocument();
                doc.entityId = d.getEntityId();
                doc.entityName = d.getEntityName();
                doc.conflictType = d.getConflictType();
                doc.resolution = d.getResolution();
                doc.preferredSourceId = d.getPreferredSourceId();
                return doc;
            }).toList();
        }

        return entity;
    }
}
