package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.application.usecase.AssignRolesToUserUseCase;
import com.zenandops.auth.application.usecase.RemoveRolesFromUserUseCase;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.auth.infrastructure.rest.dto.UserResponse;
import com.zenandops.auth.infrastructure.rest.dto.UserRolesRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

/**
 * REST resource exposing User-Role assignment endpoints. All endpoints require ADMIN role.
 */
@Path("/api/v1/users/{userId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "User Roles", description = "User-Role assignment operations for RBAC management")
public class UserRoleResource {

    @Inject
    AssignRolesToUserUseCase assignRolesToUserUseCase;

    @Inject
    RemoveRolesFromUserUseCase removeRolesFromUserUseCase;

    @POST
    @Operation(summary = "Assign roles to user", description = "Assigns one or more roles to a user. Duplicate assignments are ignored.")
    @RequestBody(description = "Role names to assign", required = true,
            content = @Content(schema = @Schema(implementation = UserRolesRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Roles assigned successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @APIResponse(responseCode = "404", description = "User or role not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response assignRoles(
            @Parameter(description = "User identifier", required = true)
            @PathParam("userId") String userId,
            UserRolesRequest request) {
        User user = assignRolesToUserUseCase.execute(userId, request.roleNames());
        return Response.ok(toResponse(user)).build();
    }

    @DELETE
    @Operation(summary = "Remove roles from user", description = "Removes one or more roles from a user")
    @RequestBody(description = "Role names to remove", required = true,
            content = @Content(schema = @Schema(implementation = UserRolesRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Roles removed successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response removeRoles(
            @Parameter(description = "User identifier", required = true)
            @PathParam("userId") String userId,
            UserRolesRequest request) {
        User user = removeRolesFromUserUseCase.execute(userId, request.roleNames());
        return Response.ok(toResponse(user)).build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getLogin(), user.getName(), user.getEmail(),
                user.getRoles(), user.getTagIds(), user.isActive(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
