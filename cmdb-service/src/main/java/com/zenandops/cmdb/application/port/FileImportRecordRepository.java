package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.FileImportRecord;

import java.util.List;

/**
 * Outbound port for FileImportRecord persistence operations.
 */
public interface FileImportRecordRepository {

    void save(FileImportRecord record);

    List<FileImportRecord> findAll();
}
