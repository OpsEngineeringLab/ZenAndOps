package com.zenandops.admin.infrastructure.adapter.keycloak;

import com.zenandops.admin.infrastructure.rest.dto.CreateUserRequest;
import com.zenandops.admin.infrastructure.rest.dto.UpdateUserRequest;
import com.zenandops.admin.infrastructure.rest.dto.UserResponse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates between Keycloak UserRepresentation (as {@code Map<String, Object>})
 * and ZenAndOps user DTOs.
 * <p>
 * This is a stateless utility class with static methods — no CDI needed.
 */
public final class UserResponseTranslator {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT;

    private UserResponseTranslator() {
        // utility class
    }

    /**
     * Convert a Keycloak user representation to a ZenAndOps {@link UserResponse}.
     *
     * @param keycloakUser the Keycloak user map (from Admin REST API)
     * @param roleNames    the user's realm role names (fetched separately)
     * @param tagIds       the user's tag IDs (resolved from tag definitions)
     * @return the translated user response
     */
    public static UserResponse toUserResponse(Map<String, Object> keycloakUser,
                                              List<String> roleNames,
                                              List<String> tagIds) {
        String id = stringValue(keycloakUser.get("id"));
        String login = stringValue(keycloakUser.get("username"));
        String name = stringValue(keycloakUser.get("firstName"));
        String email = stringValue(keycloakUser.get("email"));
        boolean active = booleanValue(keycloakUser.get("enabled"));

        String createdAt = epochMillisToIso(keycloakUser.get("createdTimestamp"));
        // Keycloak does not natively track updatedAt — use createdAt as fallback
        String updatedAt = createdAt;

        return new UserResponse(
                id,
                login,
                name,
                email,
                roleNames != null ? roleNames : List.of(),
                tagIds != null ? tagIds : List.of(),
                active,
                createdAt,
                updatedAt
        );
    }

    /**
     * Convert a {@link CreateUserRequest} to a Keycloak user representation map
     * suitable for the Admin REST API {@code POST /users} endpoint.
     *
     * @param request the create user request
     * @return the Keycloak user representation map
     */
    public static Map<String, Object> toKeycloakUser(CreateUserRequest request) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", request.login());
        user.put("firstName", request.name());
        user.put("email", request.email());
        user.put("enabled", request.active());

        if (request.password() != null && !request.password().isBlank()) {
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", request.password());
            credential.put("temporary", false);
            user.put("credentials", List.of(credential));
        }

        return user;
    }

    /**
     * Convert an {@link UpdateUserRequest} to a partial Keycloak user representation map.
     * Only non-null fields are included so that unchanged fields are not overwritten.
     *
     * @param request the update user request
     * @return the partial Keycloak user representation map
     */
    public static Map<String, Object> toKeycloakUserUpdate(UpdateUserRequest request) {
        Map<String, Object> user = new HashMap<>();

        if (request.name() != null) {
            user.put("firstName", request.name());
        }
        if (request.email() != null) {
            user.put("email", request.email());
        }
        if (request.active() != null) {
            user.put("enabled", request.active());
        }

        return user;
    }

    // ── Internal helpers ────────────────────────────────────────────────

    /**
     * Convert an epoch-millis value to an ISO 8601 string.
     */
    static String epochMillisToIso(Object epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        long millis;
        if (epochMillis instanceof Number number) {
            millis = number.longValue();
        } else {
            try {
                millis = Long.parseLong(String.valueOf(epochMillis));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return Instant.ofEpochMilli(millis)
                .atOffset(ZoneOffset.UTC)
                .format(ISO_FORMATTER);
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return false;
    }
}
