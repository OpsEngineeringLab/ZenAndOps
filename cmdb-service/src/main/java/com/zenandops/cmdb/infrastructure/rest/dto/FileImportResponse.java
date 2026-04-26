package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.ImportStatus;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO representing a FileImportRecord.
 *
 * @param id            the import record identifier
 * @param fileName      the uploaded file name
 * @param fileFormat    the file format (CSV, JSON, XML)
 * @param dataSourceId  the associated data source ID
 * @param status        the import status
 * @param totalRecords  total number of records in the file
 * @param successCount  number of successfully imported records
 * @param failureCount  number of failed records
 * @param errors        list of import errors
 * @param importedBy    the user who performed the import
 * @param createdAt     when the import was performed
 */
public record FileImportResponse(
        String id,
        String fileName,
        String fileFormat,
        String dataSourceId,
        ImportStatus status,
        int totalRecords,
        int successCount,
        int failureCount,
        List<ImportErrorResponse> errors,
        String importedBy,
        Instant createdAt
) {

    /**
     * Response DTO for a single import error.
     *
     * @param recordIndex the index of the failed record
     * @param field       the field that caused the error
     * @param message     the error message
     */
    public record ImportErrorResponse(int recordIndex, String field, String message) {
    }
}
