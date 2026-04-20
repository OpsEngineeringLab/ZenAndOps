package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Request DTO for creating a new User.
 *
 * @param login    the user login
 * @param name     the user display name
 * @param email    the user email address
 * @param password the user password
 * @param roles    the list of role names to assign
 * @param tagIds   the list of tag identifiers to assign
 */
public record CreateUserRequest(String login, String name, String email, String password,
                                List<String> roles, List<String> tagIds) {
}
