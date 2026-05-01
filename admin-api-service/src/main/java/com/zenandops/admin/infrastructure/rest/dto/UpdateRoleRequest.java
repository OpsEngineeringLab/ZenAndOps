package com.zenandops.admin.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for updating an existing role.
 */
public record UpdateRoleRequest(
        String name,
        String description,
        List<String> permissions
) {}
