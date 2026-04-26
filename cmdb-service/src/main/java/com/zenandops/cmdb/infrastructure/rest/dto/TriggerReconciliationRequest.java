package com.zenandops.cmdb.infrastructure.rest.dto;

/**
 * Request DTO for triggering a reconciliation process.
 *
 * @param entityType the entity type to reconcile (ASSET or CI)
 */
public record TriggerReconciliationRequest(
        String entityType
) {
}
