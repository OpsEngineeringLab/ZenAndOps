package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for updating an existing Role.
 *
 * @param name        the role name
 * @param description the role description
 * @param permissions the list of permission strings
 */
public record UpdateRoleRequest(String name, String description, List<String> permissions) {
}
