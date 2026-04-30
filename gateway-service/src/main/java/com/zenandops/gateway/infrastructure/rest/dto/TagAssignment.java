package com.zenandops.gateway.infrastructure.rest.dto;

/**
 * Represents a tag assignment on a user (key-value pair).
 */
public record TagAssignment(
        String key,
        String value
) {}
