package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.application.usecase.CreateRoleUseCase;
import com.zenandops.auth.application.usecase.DeleteRoleUseCase;
import com.zenandops.auth.application.usecase.GetRoleUseCase;
import com.zenandops.auth.application.usecase.ListRolesUseCase;
import com.zenandops.auth.application.usecase.PaginatedResult;
import com.zenandops.auth.application.usecase.UpdateRoleUseCase;
import com.zenandops.auth.domain.entity.Role;
import com.zenandops.auth.infrastructure.rest.dto.CreateRoleRequest;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.auth.infrastructure.rest.dto.PaginatedRolesResponse;
import com.zenandops.auth.infrastructure.rest.dto.RoleResponse;
import com.zenandops.auth.infrastructure.rest.dto.UpdateRoleRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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

import java.util.List;

/**
 * REST resource exposing Role CRUD endpoints. All endpoints require ADMIN role.
 */
@Path("/api/v1/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Roles", description = "Role CRUD operations for RBAC permission management")
public class RoleResource {

    @Inject
    CreateRoleUseCase createRoleUseCase;

    @Inject
    ListRolesUseCase listRolesUseCase;

    @Inject
    GetRoleUseCase getRoleUseCase;

    @Inject
    UpdateRoleUseCase updateRoleUseCase;

    @Inject
    DeleteRoleUseCase deleteRoleUseCase;

    @POST
    @Operation(summary = "Create a role", description = "Creates a new role with the given name, description, and permissions")
    @RequestBody(description = "Role creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateRoleRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Role created successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @APIResponse(responseCode = "409", description = "Role with this name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createRole(CreateRoleRequest request) {
        Role role = createRoleUseCase.execute(request.name(), request.description(), request.permissions());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(role))
                .build();
    }

    @GET
    @Operation(summary = "List all roles", description = "Retrieves a paginated list of all roles")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Roles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaginatedRolesResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response listRoles(
            @Parameter(description = "Page number (zero-based)", example = "0")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size", example = "20")
            @QueryParam("size") @DefaultValue("20") int size) {
        PaginatedResult<Role> result = listRolesUseCase.execute(page, size);
        List<RoleResponse> items = result.items().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(new PaginatedRolesResponse(items, result.page(), result.size(),
                result.totalItems(), result.totalPages())).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a role by ID", description = "Retrieves a single role by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Role retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @APIResponse(responseCode = "404", description = "Role not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getRole(
            @Parameter(description = "Role identifier", required = true)
            @PathParam("id") String id) {
        Role role = getRoleUseCase.execute(id);
        return Response.ok(toResponse(role)).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a role", description = "Updates the name, description, and permissions of an existing role")
    @RequestBody(description = "Role update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateRoleRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Role updated successfully",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @APIResponse(responseCode = "404", description = "Role not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Role with this name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateRole(
            @Parameter(description = "Role identifier", required = true)
            @PathParam("id") String id,
            UpdateRoleRequest request) {
        Role role = updateRoleUseCase.execute(id, request.name(), request.description(), request.permissions());
        return Response.ok(toResponse(role)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a role", description = "Deletes a role by its identifier. Fails if the role is assigned to any user.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Role deleted successfully"),
            @APIResponse(responseCode = "404", description = "Role not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Role is in use and cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteRole(
            @Parameter(description = "Role identifier", required = true)
            @PathParam("id") String id) {
        deleteRoleUseCase.execute(id);
        return Response.noContent().build();
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(),
                role.getPermissions(), role.getCreatedAt(), role.getUpdatedAt());
    }
}
