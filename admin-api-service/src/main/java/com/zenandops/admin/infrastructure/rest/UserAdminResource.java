package com.zenandops.admin.infrastructure.rest;

import com.zenandops.admin.domain.exception.ForbiddenException;
import com.zenandops.admin.infrastructure.adapter.keycloak.KeycloakAdminClient;
import com.zenandops.admin.infrastructure.adapter.keycloak.UserResponseTranslator;
import com.zenandops.admin.infrastructure.rest.dto.CreateUserRequest;
import com.zenandops.admin.infrastructure.rest.dto.UpdateUserRequest;
import com.zenandops.admin.infrastructure.rest.dto.UserResponse;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin proxy resource for user management.
 * Proxies requests to the Keycloak Admin REST API and translates responses
 * to the ZenAndOps API contract.
 */
@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserAdminResource {

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    JsonWebToken jwt;

    @GET
    public List<UserResponse> listUsers(@QueryParam("first") Integer first,
                                        @QueryParam("max") Integer max) {
        requirePermission("users:read");
        List<Map<String, Object>> keycloakUsers = keycloakAdminClient.listUsers(first, max);
        return keycloakUsers.stream()
                .map(this::translateUser)
                .toList();
    }

    @POST
    public Response createUser(CreateUserRequest request) {
        requirePermission("users:write");
        Map<String, Object> keycloakUser = UserResponseTranslator.toKeycloakUser(request);
        String userId = keycloakAdminClient.createUser(keycloakUser);

        Map<String, Object> createdUser = keycloakAdminClient.getUser(userId);
        List<Map<String, Object>> roles = keycloakAdminClient.getUserRealmRoles(userId);
        List<String> roleNames = roles.stream()
                .map(r -> String.valueOf(r.get("name")))
                .toList();

        UserResponse response = UserResponseTranslator.toUserResponse(createdUser, roleNames, List.of());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public UserResponse getUser(@PathParam("id") String id) {
        requirePermission("users:read");
        return translateUser(keycloakAdminClient.getUser(id));
    }

    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") String id, UpdateUserRequest request) {
        requirePermission("users:write");
        Map<String, Object> keycloakUpdate = UserResponseTranslator.toKeycloakUserUpdate(request);
        keycloakAdminClient.updateUser(id, keycloakUpdate);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        requirePermission("users:write");
        keycloakAdminClient.deleteUser(id);
        return Response.noContent().build();
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private UserResponse translateUser(Map<String, Object> keycloakUser) {
        String userId = String.valueOf(keycloakUser.get("id"));
        List<Map<String, Object>> roles = keycloakAdminClient.getUserRealmRoles(userId);
        List<String> roleNames = roles.stream()
                .map(r -> String.valueOf(r.get("name")))
                .toList();
        // Tag IDs are resolved from user attributes; for simplicity return empty list
        // (tags can be fetched via the dedicated /users/{id}/tags endpoint)
        return UserResponseTranslator.toUserResponse(keycloakUser, roleNames, List.of());
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
