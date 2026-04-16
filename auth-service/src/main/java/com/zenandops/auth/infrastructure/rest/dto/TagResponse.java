package com.zenandops.auth.infrastructure.rest.dto;

import java.time.Instant;

/**
 * Response DTO representing a Tag.
 *
 * @param id          the tag identifier
 * @param key         the tag key
 * @param value       the tag value
 * @param description the tag description
 * @param createdAt   when the tag was created
 * @param updatedAt   when the tag was last updated
 */
public record TagResponse(String id, String key, String value, String description,
                          Instant createdAt, Instant updatedAt) {
}
