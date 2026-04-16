package com.zenandops.auth.infrastructure.rest.dto;

/**
 * Request DTO for creating a new Tag.
 *
 * @param key         the tag key
 * @param value       the tag value
 * @param description optional description
 */
public record CreateTagRequest(String key, String value, String description) {
}
