package com.zenandops.auth.infrastructure.rest.dto;

import java.util.List;

/**
 * Response DTO representing the current user's profile.
 *
 * @param login the user login
 * @param name  the user display name
 * @param email the user email address
 * @param roles the list of role names
 * @param tags  the list of resolved tags
 */
public record ProfileResponse(String login, String name, String email,
                               List<String> roles, List<TagResponse> tags) {
}
