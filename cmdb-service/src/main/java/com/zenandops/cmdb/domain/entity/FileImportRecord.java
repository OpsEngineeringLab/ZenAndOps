package com.zenandops.cmdb.domain.entity;

import com.zenandops.cmdb.domain.vo.ImportStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Record of a file import operation with status and summary.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class FileImportRecord {

    private String id;
    private String fileName;
    private String fileFormat;
    private String dataSourceId;
    private ImportStatus status;
    private int totalRecords;
    private int successCount;
    private int failureCount;
    private List<ImportError> errors;
    private String importedBy;
    private Instant createdAt;

    public FileImportRecord() {
        this.errors = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public void setStatus(ImportStatus status) {
        this.status = status;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<ImportError> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportError> errors) {
        this.errors = errors;
    }

    public String getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Represents a single error encountered during file import processing.
     */
    public static class ImportError {

        private int recordIndex;
        private String field;
        private String message;

        public ImportError() {
        }

        public ImportError(int recordIndex, String field, String message) {
            this.recordIndex = recordIndex;
            this.field = field;
            this.message = message;
        }

        public int getRecordIndex() {
            return recordIndex;
        }

        public void setRecordIndex(int recordIndex) {
            this.recordIndex = recordIndex;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
