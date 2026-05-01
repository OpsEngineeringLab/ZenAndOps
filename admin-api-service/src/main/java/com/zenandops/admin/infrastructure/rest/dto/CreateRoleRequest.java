package com.zenandops.admin.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for creating a new role.
 */
public record CreateRoleRequest(
        String name,
        String description,
        List<String> permissions
) {}
