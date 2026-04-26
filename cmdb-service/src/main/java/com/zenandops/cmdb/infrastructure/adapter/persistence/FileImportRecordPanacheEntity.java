package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.ImportStatus;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB Panache entity mapping for the FileImportRecord domain entity.
 */
@MongoEntity(collection = "file_import_records", database = "zenandops-cmdb")
public class FileImportRecordPanacheEntity extends PanacheMongoEntity {

    @BsonProperty("fileName")
    public String fileName;

    @BsonProperty("fileFormat")
    public String fileFormat;

    @BsonProperty("dataSourceId")
    public String dataSourceId;

    public ImportStatus status;

    @BsonProperty("totalRecords")
    public int totalRecords;

    @BsonProperty("successCount")
    public int successCount;

    @BsonProperty("failureCount")
    public int failureCount;

    public List<ImportErrorDocument> errors;

    @BsonProperty("importedBy")
    public String importedBy;

    @BsonProperty("createdAt")
    public Instant createdAt;

    /**
     * Embedded document for import errors.
     */
    public static class ImportErrorDocument {
        @BsonProperty("recordIndex")
        public int recordIndex;
        public String field;
        public String message;
    }
}
