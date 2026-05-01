package com.zenandops.admin.infrastructure.rest;

import com.zenandops.admin.domain.exception.ForbiddenException;
import com.zenandops.admin.infrastructure.adapter.keycloak.KeycloakAdminClient;
import com.zenandops.admin.infrastructure.adapter.keycloak.KeycloakAdminException;
import com.zenandops.admin.infrastructure.adapter.keycloak.TagResponseTranslator;
import com.zenandops.admin.infrastructure.rest.dto.CreateTagRequest;
import com.zenandops.admin.infrastructure.rest.dto.TagResponse;
import com.zenandops.admin.infrastructure.rest.dto.UpdateTagRequest;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin proxy resource for tag definition management.
 * Tag definitions are stored as a JSON array in the Keycloak realm attribute
 * {@code _zenandops_tags}.
 */
@Path("/api/v1/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class TagAdminResource {

    private static final String TAGS_ATTRIBUTE = "_zenandops_tags";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    JsonWebToken jwt;

    @GET
    public List<TagResponse> listTags() {
        requirePermission("tags:read");
        return loadTagDefinitions();
    }

    @POST
    public Response createTag(CreateTagRequest request) {
        requirePermission("tags:write");
        List<TagResponse> tags = new ArrayList<>(loadTagDefinitions());

        Map<String, Object> newTagMap = TagResponseTranslator.createTagDefinition(request);
        TagResponse newTag = TagResponseTranslator.toTagResponse(newTagMap);
        tags.add(newTag);

        saveTagDefinitions(tags);
        return Response.status(Response.Status.CREATED).entity(newTag).build();
    }

    @GET
    @Path("/{id}")
    public TagResponse getTag(@PathParam("id") String id) {
        requirePermission("tags:read");
        return loadTagDefinitions().stream()
                .filter(t -> id.equals(t.id()))
                .findFirst()
                .orElseThrow(() -> new KeycloakAdminException(404, "Tag not found"));
    }

    @PUT
    @Path("/{id}")
    public Response updateTag(@PathParam("id") String id, UpdateTagRequest request) {
        requirePermission("tags:write");
        List<TagResponse> tags = new ArrayList<>(loadTagDefinitions());
        String now = Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);

        boolean found = false;
        for (int i = 0; i < tags.size(); i++) {
            TagResponse existing = tags.get(i);
            if (id.equals(existing.id())) {
                tags.set(i, new TagResponse(
                        existing.id(),
                        request.key() != null ? request.key() : existing.key(),
                        request.value() != null ? request.value() : existing.value(),
                        request.description() != null ? request.description() : existing.description(),
                        existing.createdAt(),
                        now
                ));
                found = true;
                break;
            }
        }

        if (!found) {
            throw new KeycloakAdminException(404, "Tag not found");
        }

        saveTagDefinitions(tags);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTag(@PathParam("id") String id) {
        requirePermission("tags:write");
        List<TagResponse> tags = new ArrayList<>(loadTagDefinitions());
        boolean removed = tags.removeIf(t -> id.equals(t.id()));

        if (!removed) {
            throw new KeycloakAdminException(404, "Tag not found");
        }

        saveTagDefinitions(tags);
        return Response.noContent().build();
    }

    // ── Internal helpers ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<TagResponse> loadTagDefinitions() {
        Map<String, Object> realm = keycloakAdminClient.getRealmRepresentation();
        Object attributesObj = realm.get("attributes");
        if (!(attributesObj instanceof Map<?, ?> attributes)) {
            return List.of();
        }
        String tagsJson = String.valueOf(attributes.get(TAGS_ATTRIBUTE));
        return TagResponseTranslator.parseTagDefinitions(tagsJson);
    }

    @SuppressWarnings("unchecked")
    private void saveTagDefinitions(List<TagResponse> tags) {
        String tagsJson = TagResponseTranslator.serializeTagDefinitions(tags);

        // Read current realm to preserve other attributes
        Map<String, Object> realm = keycloakAdminClient.getRealmRepresentation();
        Map<String, Object> attributes;
        Object existingAttrs = realm.get("attributes");
        if (existingAttrs instanceof Map<?, ?> m) {
            attributes = new HashMap<>((Map<String, Object>) m);
        } else {
            attributes = new HashMap<>();
        }
        attributes.put(TAGS_ATTRIBUTE, tagsJson);

        Map<String, Object> realmUpdate = Map.of("attributes", attributes);
        keycloakAdminClient.updateRealmRepresentation(realmUpdate);
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
