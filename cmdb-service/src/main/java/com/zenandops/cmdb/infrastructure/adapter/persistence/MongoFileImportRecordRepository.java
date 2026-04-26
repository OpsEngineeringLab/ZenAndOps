package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.application.port.FileImportRecordRepository;
import com.zenandops.cmdb.domain.entity.FileImportRecord;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Panache adapter implementing the FileImportRecordRepository port.
 */
@ApplicationScoped
public class MongoFileImportRecordRepository implements FileImportRecordRepository {

    @Override
    public void save(FileImportRecord record) {
        FileImportRecordPanacheEntity entity = toEntity(record);
        if (record.getId() != null) {
            entity.id = new org.bson.types.ObjectId(record.getId());
            entity.update();
        } else {
            entity.persist();
            record.setId(entity.id.toString());
        }
    }

    @Override
    public List<FileImportRecord> findAll() {
        return FileImportRecordPanacheEntity.<FileImportRecordPanacheEntity>listAll()
                .stream().map(this::toDomain).toList();
    }

    private FileImportRecord toDomain(FileImportRecordPanacheEntity entity) {
        FileImportRecord record = new FileImportRecord();
        record.setId(entity.id.toString());
        record.setFileName(entity.fileName);
        record.setFileFormat(entity.fileFormat);
        record.setDataSourceId(entity.dataSourceId);
        record.setStatus(entity.status);
        record.setTotalRecords(entity.totalRecords);
        record.setSuccessCount(entity.successCount);
        record.setFailureCount(entity.failureCount);
        record.setImportedBy(entity.importedBy);
        record.setCreatedAt(entity.createdAt);

        if (entity.errors != null) {
            List<FileImportRecord.ImportError> errors = entity.errors.stream()
                    .map(e -> new FileImportRecord.ImportError(e.recordIndex, e.field, e.message))
                    .toList();
            record.setErrors(errors);
        } else {
            record.setErrors(new ArrayList<>());
        }

        return record;
    }

    private FileImportRecordPanacheEntity toEntity(FileImportRecord record) {
        FileImportRecordPanacheEntity entity = new FileImportRecordPanacheEntity();
        entity.fileName = record.getFileName();
        entity.fileFormat = record.getFileFormat();
        entity.dataSourceId = record.getDataSourceId();
        entity.status = record.getStatus();
        entity.totalRecords = record.getTotalRecords();
        entity.successCount = record.getSuccessCount();
        entity.failureCount = record.getFailureCount();
        entity.importedBy = record.getImportedBy();
        entity.createdAt = record.getCreatedAt();

        if (record.getErrors() != null) {
            entity.errors = record.getErrors().stream().map(e -> {
                FileImportRecordPanacheEntity.ImportErrorDocument doc =
                        new FileImportRecordPanacheEntity.ImportErrorDocument();
                doc.recordIndex = e.getRecordIndex();
                doc.field = e.getField();
                doc.message = e.getMessage();
                return doc;
            }).toList();
        }

        return entity;
    }
}
