package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for assigning or removing roles from a user.
 *
 * @param roleNames the list of role names to assign or remove
 */
public record UserRolesRequest(List<String> roleNames) {
}
