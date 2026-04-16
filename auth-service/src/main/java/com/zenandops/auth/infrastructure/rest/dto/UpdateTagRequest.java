package com.zenandops.auth.infrastructure.rest.dto;

/**
 * Request DTO for updating an existing Tag's description.
 *
 * @param description the new description
 */
public record UpdateTagRequest(String description) {
}
