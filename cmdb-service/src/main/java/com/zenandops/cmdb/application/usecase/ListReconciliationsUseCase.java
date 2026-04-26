package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ReconciliationRecordRepository;
import com.zenandops.cmdb.domain.entity.ReconciliationRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all reconciliation records.
 */
@ApplicationScoped
public class ListReconciliationsUseCase {

    private final ReconciliationRecordRepository reconciliationRecordRepository;

    @Inject
    public ListReconciliationsUseCase(ReconciliationRecordRepository reconciliationRecordRepository) {
        this.reconciliationRecordRepository = reconciliationRecordRepository;
    }

    /**
     * List all reconciliation records.
     *
     * @return list of all reconciliation records
     */
    public List<ReconciliationRecord> execute() {
        return reconciliationRecordRepository.findAll();
    }
}
