package com.zenandops.cmdb.infrastructure.adapter.messaging;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka event payload for CMDB domain events.
 */
public record CmdbEvent(
        String eventId,
        String eventType,
        String entityId,
        String entityType,
        String userId,
        Instant timestamp,
        Map<String, Object> metadata
) {
}
