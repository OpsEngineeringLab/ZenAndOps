package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for updating an existing User.
 *
 * @param name     the user display name
 * @param email    the user email address
 * @param password the user password (optional, null means no change)
 * @param active   whether the user is active
 * @param roles    the list of role names to assign
 * @param tagIds   the list of tag identifiers to assign
 */
public record UpdateUserRequest(String name, String email, String password, Boolean active,
                                List<String> roles, List<String> tagIds) {
}
