package com.zenandops.cmdb.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO representing a ReconciliationRecord.
 *
 * @param id                  the reconciliation record identifier
 * @param entityType          the entity type reconciled (ASSET or CI)
 * @param recordsAnalyzed     total number of records analyzed
 * @param duplicatesFound     number of duplicates found
 * @param conflictsResolved   number of conflicts auto-resolved
 * @param unresolvedConflicts number of conflicts requiring manual review
 * @param details             list of reconciliation details
 * @param triggeredBy         the user who triggered the reconciliation
 * @param createdAt           when the reconciliation was performed
 */
public record ReconciliationResponse(
        String id,
        String entityType,
        int recordsAnalyzed,
        int duplicatesFound,
        int conflictsResolved,
        int unresolvedConflicts,
        List<ReconciliationDetailResponse> details,
        String triggeredBy,
        Instant createdAt
) {

    /**
     * Response DTO for a single reconciliation detail.
     *
     * @param entityId          the entity identifier
     * @param entityName        the entity name
     * @param conflictType      the type of conflict (DUPLICATE, ATTRIBUTE_MISMATCH)
     * @param resolution        the resolution (AUTO_RESOLVED, MANUAL_REVIEW)
     * @param preferredSourceId the preferred data source ID (if auto-resolved)
     */
    public record ReconciliationDetailResponse(
            String entityId,
            String entityName,
            String conflictType,
            String resolution,
            String preferredSourceId
    ) {
    }
}
