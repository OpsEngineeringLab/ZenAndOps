package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.FileImportRecordRepository;
import com.zenandops.cmdb.domain.entity.FileImportRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all file import records.
 */
@ApplicationScoped
public class ListFileImportsUseCase {

    private final FileImportRecordRepository fileImportRecordRepository;

    @Inject
    public ListFileImportsUseCase(FileImportRecordRepository fileImportRecordRepository) {
        this.fileImportRecordRepository = fileImportRecordRepository;
    }

    /**
     * List all file import records.
     *
     * @return list of all file import records
     */
    public List<FileImportRecord> execute() {
        return fileImportRecordRepository.findAll();
    }
}
