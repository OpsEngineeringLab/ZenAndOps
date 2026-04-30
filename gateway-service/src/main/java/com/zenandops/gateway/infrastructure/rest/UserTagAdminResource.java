package com.zenandops.gateway.infrastructure.rest;

import com.zenandops.gateway.domain.exception.ForbiddenException;
import com.zenandops.gateway.infrastructure.adapter.keycloak.KeycloakAdminClient;
import com.zenandops.gateway.infrastructure.adapter.keycloak.TagResponseTranslator;
import com.zenandops.gateway.infrastructure.rest.dto.TagAssignment;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin proxy resource for user tag assignments.
 * Manages the {@code tags} user attribute in Keycloak.
 */
@Path("/api/v1/users/{userId}/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserTagAdminResource {

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    JsonWebToken jwt;

    @GET
    public List<TagAssignment> getUserTags(@PathParam("userId") String userId) {
        requirePermission("users:read");
        Map<String, Object> user = keycloakAdminClient.getUser(userId);
        return TagResponseTranslator.parseUserTags(user);
    }

    @POST
    public Response addUserTag(@PathParam("userId") String userId, TagAssignment tag) {
        requirePermission("users:write");
        Map<String, Object> user = keycloakAdminClient.getUser(userId);
        List<TagAssignment> currentTags = new ArrayList<>(TagResponseTranslator.parseUserTags(user));
        currentTags.add(tag);

        updateUserTags(userId, user, currentTags);
        return Response.status(Response.Status.CREATED).entity(tag).build();
    }

    @DELETE
    public Response removeUserTag(@PathParam("userId") String userId,
                                  @QueryParam("key") String key,
                                  @QueryParam("value") String value) {
        requirePermission("users:write");
        Map<String, Object> user = keycloakAdminClient.getUser(userId);
        List<TagAssignment> currentTags = new ArrayList<>(TagResponseTranslator.parseUserTags(user));
        currentTags.removeIf(t -> t.key().equals(key) && t.value().equals(value));

        updateUserTags(userId, user, currentTags);
        return Response.noContent().build();
    }

    // ── Internal helpers ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void updateUserTags(String userId, Map<String, Object> user, List<TagAssignment> tags) {
        String tagsJson = TagResponseTranslator.serializeUserTags(tags);

        // Build the attributes map preserving existing attributes
        Map<String, Object> attributes;
        Object existingAttrs = user.get("attributes");
        if (existingAttrs instanceof Map<?, ?> m) {
            attributes = new HashMap<>((Map<String, Object>) m);
        } else {
            attributes = new HashMap<>();
        }
        attributes.put("tags", List.of(tagsJson));

        Map<String, Object> update = Map.of("attributes", attributes);
        keycloakAdminClient.updateUser(userId, update);
    }

    @SuppressWarnings("unchecked")
    private void requirePermission(String permission) {
        Object permissionsClaim = jwt.getClaim("permissions");
        Set<String> permissions;
        if (permissionsClaim instanceof Set<?> s) {
            permissions = (Set<String>) s;
        } else if (permissionsClaim instanceof List<?> l) {
            permissions = new java.util.HashSet<>((List<String>) (List<?>) l);
        } else {
            permissions = Set.of();
        }
        if (!permissions.contains(permission)) {
            throw new ForbiddenException("Insufficient permissions");
        }
    }
}
