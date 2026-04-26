package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.ReconciliationRecord;

import java.util.List;

/**
 * Outbound port for ReconciliationRecord persistence operations.
 */
public interface ReconciliationRecordRepository {

    void save(ReconciliationRecord record);

    List<ReconciliationRecord> findAll();
}
