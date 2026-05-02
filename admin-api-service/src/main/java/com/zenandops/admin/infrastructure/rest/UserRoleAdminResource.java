package com.zenandops.admin.infrastructure.rest;

import com.zenandops.admin.application.usecase.AssignUserRolesUseCase;
import com.zenandops.admin.application.usecase.GetRoleUseCase;
import com.zenandops.admin.application.usecase.RemoveUserRolesUseCase;
import com.zenandops.admin.domain.exception.ForbiddenException;
import com.zenandops.admin.infrastructure.rest.dto.RoleAssignmentRequest;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
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
 * Admin proxy resource for user role assignments.
 * Delegates to application-layer use cases which orchestrate calls through
 * port interfaces, keeping this resource decoupled from infrastructure adapters.
 */
@Path("/api/v1/users/{userId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserRoleAdminResource {

    @Inject
    GetRoleUseCase getRoleUseCase;

    @Inject
    AssignUserRolesUseCase assignUserRolesUseCase;

    @Inject
    RemoveUserRolesUseCase removeUserRolesUseCase;

    @Inject
    JsonWebToken jwt;

    @POST
    public Response assignRoles(@PathParam("userId") String userId,
                                RoleAssignmentRequest request) {
        requirePermission("users:write");
        List<Map<String, Object>> roleRepresentations = resolveRoles(request.roles());
        assignUserRolesUseCase.execute(userId, roleRepresentations);
        return Response.noContent().build();
    }

    @DELETE
    public Response removeRoles(@PathParam("userId") String userId,
                                RoleAssignmentRequest request) {
        requirePermission("users:write");
        List<Map<String, Object>> roleRepresentations = resolveRoles(request.roles());
        removeUserRolesUseCase.execute(userId, roleRepresentations);
        return Response.noContent().build();
    }

    // ── Internal helpers ────────────────────────────────────────────────

    /**
     * Resolve role names to Keycloak role representations by looking up each role by name.
     */
    private List<Map<String, Object>> resolveRoles(List<String> roleNames) {
        return roleNames.stream()
                .map(getRoleUseCase::executeByName)
                .toList();
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
