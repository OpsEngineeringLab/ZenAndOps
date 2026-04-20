package com.zenandops.auth.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO representing a Role.
 *
 * @param id          the role identifier
 * @param name        the role name
 * @param description the role description
 * @param permissions the list of permission strings
 * @param createdAt   when the role was created
 * @param updatedAt   when the role was last updated
 */
public record RoleResponse(String id, String name, String description, List<String> permissions,
                           Instant createdAt, Instant updatedAt) {
}
