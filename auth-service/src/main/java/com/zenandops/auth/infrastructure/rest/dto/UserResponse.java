package com.zenandops.auth.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO representing a User. Does NOT include passwordHash.
 *
 * @param id        the user identifier
 * @param login     the user login
 * @param name      the user display name
 * @param email     the user email address
 * @param roles     the list of role names
 * @param tagIds    the list of tag identifiers
 * @param active    whether the user is active
 * @param createdAt when the user was created
 * @param updatedAt when the user was last updated
 */
public record UserResponse(String id, String login, String name, String email,
                           List<String> roles, List<String> tagIds, boolean active,
                           Instant createdAt, Instant updatedAt) {
}
