package com.zenandops.admin.infrastructure.rest;

import com.zenandops.admin.domain.exception.ForbiddenException;
import com.zenandops.admin.infrastructure.adapter.keycloak.KeycloakAdminClient;
import com.zenandops.admin.infrastructure.adapter.keycloak.RoleResponseTranslator;
import com.zenandops.admin.infrastructure.rest.dto.CreateRoleRequest;
import com.zenandops.admin.infrastructure.rest.dto.RoleResponse;
import com.zenandops.admin.infrastructure.rest.dto.UpdateRoleRequest;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin proxy resource for role management.
 * Proxies requests to the Keycloak Admin REST API and translates responses
 * to the ZenAndOps API contract.
 */
@Path("/api/v1/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class RoleAdminResource {

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    JsonWebToken jwt;

    @GET
    public List<RoleResponse> listRoles() {
        requirePermission("roles:read");
        List<Map<String, Object>> keycloakRoles = keycloakAdminClient.listRealmRoles();
        return keycloakRoles.stream()
                .map(RoleResponseTranslator::toRoleResponse)
                .toList();
    }

    @POST
    public Response createRole(CreateRoleRequest request) {
        requirePermission("roles:write");
        Map<String, Object> keycloakRole = RoleResponseTranslator.toKeycloakRole(request);
        keycloakAdminClient.createRealmRole(keycloakRole);

        // Fetch the created role by name to return the full representation
        Map<String, Object> createdRole = keycloakAdminClient.getRealmRoleByName(request.name());
        RoleResponse response = RoleResponseTranslator.toRoleResponse(createdRole);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public RoleResponse getRole(@PathParam("id") String id) {
        requirePermission("roles:read");
        Map<String, Object> keycloakRole = keycloakAdminClient.getRealmRoleById(id);
        return RoleResponseTranslator.toRoleResponse(keycloakRole);
    }

    @PUT
    @Path("/{id}")
    public Response updateRole(@PathParam("id") String id, UpdateRoleRequest request) {
        requirePermission("roles:write");
        Map<String, Object> keycloakUpdate = RoleResponseTranslator.toKeycloakRoleUpdate(request);
        keycloakAdminClient.updateRealmRole(id, keycloakUpdate);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRole(@PathParam("id") String id) {
        requirePermission("roles:write");
        keycloakAdminClient.deleteRealmRole(id);
        return Response.noContent().build();
    }

    // ── Internal helpers ────────────────────────────────────────────────

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
