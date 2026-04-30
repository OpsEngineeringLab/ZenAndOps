package com.zenandops.gateway.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for assigning or removing roles from a user.
 */
public record RoleAssignmentRequest(
        List<String> roles
) {}
