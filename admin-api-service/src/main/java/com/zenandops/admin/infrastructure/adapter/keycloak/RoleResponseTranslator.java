package com.zenandops.admin.infrastructure.adapter.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenandops.admin.infrastructure.rest.dto.CreateRoleRequest;
import com.zenandops.admin.infrastructure.rest.dto.RoleResponse;
import com.zenandops.admin.infrastructure.rest.dto.UpdateRoleRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates between Keycloak RoleRepresentation (as {@code Map<String, Object>})
 * and ZenAndOps role DTOs.
 * <p>
 * This is a stateless utility class with static methods — no CDI needed.
 * An {@link ObjectMapper} is passed explicitly to avoid hidden global state.
 */
public final class RoleResponseTranslator {

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private RoleResponseTranslator() {
        // utility class
    }

    /**
     * Convert a Keycloak role representation to a ZenAndOps {@link RoleResponse}.
     *
     * @param keycloakRole the Keycloak role map (from Admin REST API)
     * @return the translated role response
     */
    public static RoleResponse toRoleResponse(Map<String, Object> keycloakRole) {
        return toRoleResponse(keycloakRole, DEFAULT_MAPPER);
    }

    /**
     * Convert a Keycloak role representation to a ZenAndOps {@link RoleResponse}
     * using the provided {@link ObjectMapper}.
     *
     * @param keycloakRole the Keycloak role map (from Admin REST API)
     * @param objectMapper the Jackson ObjectMapper for JSON parsing
     * @return the translated role response
     */
    public static RoleResponse toRoleResponse(Map<String, Object> keycloakRole,
                                              ObjectMapper objectMapper) {
        String id = stringValue(keycloakRole.get("id"));
        String name = stringValue(keycloakRole.get("name"));
        String description = stringValue(keycloakRole.get("description"));
        List<String> permissions = extractPermissions(keycloakRole, objectMapper);

        // Keycloak roles do not have native createdAt/updatedAt timestamps
        return new RoleResponse(id, name, description, permissions, null, null);
    }

    /**
     * Convert a {@link CreateRoleRequest} to a Keycloak role representation map
     * suitable for the Admin REST API {@code POST /roles} endpoint.
     *
     * @param request the create role request
     * @return the Keycloak role representation map
     */
    public static Map<String, Object> toKeycloakRole(CreateRoleRequest request) {
        return toKeycloakRole(request, DEFAULT_MAPPER);
    }

    /**
     * Convert a {@link CreateRoleRequest} to a Keycloak role representation map
     * using the provided {@link ObjectMapper}.
     *
     * @param request      the create role request
     * @param objectMapper the Jackson ObjectMapper for JSON serialization
     * @return the Keycloak role representation map
     */
    public static Map<String, Object> toKeycloakRole(CreateRoleRequest request,
                                                     ObjectMapper objectMapper) {
        Map<String, Object> role = new HashMap<>();
        role.put("name", request.name());
        role.put("description", request.description());

        if (request.permissions() != null) {
            role.put("attributes", buildPermissionsAttribute(request.permissions(), objectMapper));
        }

        return role;
    }

    /**
     * Convert an {@link UpdateRoleRequest} to a Keycloak role representation map.
     *
     * @param request the update role request
     * @return the Keycloak role representation map
     */
    public static Map<String, Object> toKeycloakRoleUpdate(UpdateRoleRequest request) {
        return toKeycloakRoleUpdate(request, DEFAULT_MAPPER);
    }

    /**
     * Convert an {@link UpdateRoleRequest} to a Keycloak role representation map
     * using the provided {@link ObjectMapper}.
     *
     * @param request      the update role request
     * @param objectMapper the Jackson ObjectMapper for JSON serialization
     * @return the Keycloak role representation map
     */
    public static Map<String, Object> toKeycloakRoleUpdate(UpdateRoleRequest request,
                                                           ObjectMapper objectMapper) {
        Map<String, Object> role = new HashMap<>();

        if (request.name() != null) {
            role.put("name", request.name());
        }
        if (request.description() != null) {
            role.put("description", request.description());
        }
        if (request.permissions() != null) {
            role.put("attributes", buildPermissionsAttribute(request.permissions(), objectMapper));
        }

        return role;
    }

    // ── Internal helpers ────────────────────────────────────────────────

    /**
     * Extract permissions from the Keycloak role's attributes.
     * Keycloak stores attributes as {@code Map<String, List<String>>}, where
     * the permissions attribute contains a single-element list with a JSON array string.
     */
    @SuppressWarnings("unchecked")
    static List<String> extractPermissions(Map<String, Object> keycloakRole,
                                           ObjectMapper objectMapper) {
        Object attributesObj = keycloakRole.get("attributes");
        if (!(attributesObj instanceof Map<?, ?> attributes)) {
            return List.of();
        }

        Object permissionsObj = attributes.get("permissions");
        if (permissionsObj == null) {
            return List.of();
        }

        // Keycloak attributes are Map<String, List<String>>
        // The permissions value is a list with a single JSON array string
        String permissionsJson;
        if (permissionsObj instanceof List<?> permList && !permList.isEmpty()) {
            permissionsJson = String.valueOf(permList.getFirst());
        } else if (permissionsObj instanceof String s) {
            permissionsJson = s;
        } else {
            return List.of();
        }

        try {
            return objectMapper.readValue(permissionsJson, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Build the Keycloak attributes map with permissions serialized as a JSON array
     * in a single-element list (Keycloak attribute format).
     */
    private static Map<String, List<String>> buildPermissionsAttribute(List<String> permissions,
                                                                       ObjectMapper objectMapper) {
        try {
            String json = objectMapper.writeValueAsString(permissions);
            return Map.of("permissions", List.of(json));
        } catch (Exception e) {
            return Map.of("permissions", List.of("[]"));
        }
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
