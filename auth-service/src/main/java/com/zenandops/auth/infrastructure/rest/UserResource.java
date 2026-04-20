package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.application.usecase.CreateUserUseCase;
import com.zenandops.auth.application.usecase.DeleteUserUseCase;
import com.zenandops.auth.application.usecase.GetUserUseCase;
import com.zenandops.auth.application.usecase.ListUsersUseCase;
import com.zenandops.auth.application.usecase.PaginatedResult;
import com.zenandops.auth.application.usecase.UpdateUserUseCase;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.infrastructure.rest.dto.CreateUserRequest;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.auth.infrastructure.rest.dto.PaginatedUsersResponse;
import com.zenandops.auth.infrastructure.rest.dto.UpdateUserRequest;
import com.zenandops.auth.infrastructure.rest.dto.UserResponse;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
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
 * REST resource exposing User CRUD endpoints. All endpoints require ADMIN role.
 */
@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Users", description = "User CRUD operations for user management")
public class UserResource {

    @Inject
    CreateUserUseCase createUserUseCase;

    @Inject
    ListUsersUseCase listUsersUseCase;

    @Inject
    GetUserUseCase getUserUseCase;

    @Inject
    UpdateUserUseCase updateUserUseCase;

    @Inject
    DeleteUserUseCase deleteUserUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @Operation(summary = "Create a user", description = "Creates a new user with the given login, name, email, password, roles, and tags")
    @RequestBody(description = "User creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateUserRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @APIResponse(responseCode = "409", description = "User with this login already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "One or more specified roles do not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createUser(CreateUserRequest request) {
        User user = createUserUseCase.execute(request.login(), request.name(), request.email(),
                request.password(), request.roles(), request.tagIds());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(user))
                .build();
    }

    @GET
    @Operation(summary = "List all users", description = "Retrieves a paginated list of all users")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaginatedUsersResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response listUsers(
            @Parameter(description = "Page number (zero-based)", example = "0")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size", example = "20")
            @QueryParam("size") @DefaultValue("20") int size) {
        PaginatedResult<User> result = listUsersUseCase.execute(page, size);
        List<UserResponse> items = result.items().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(new PaginatedUsersResponse(items, result.page(), result.size(),
                result.totalItems(), result.totalPages())).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a user by ID", description = "Retrieves a single user by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getUser(
            @Parameter(description = "User identifier", required = true)
            @PathParam("id") String id) {
        User user = getUserUseCase.execute(id);
        return Response.ok(toResponse(user)).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a user", description = "Updates the name, email, password, active status, roles, and tags of an existing user")
    @RequestBody(description = "User update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateUserRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateUser(
            @Parameter(description = "User identifier", required = true)
            @PathParam("id") String id,
            UpdateUserRequest request) {
        User user = updateUserUseCase.execute(id, request.name(), request.email(),
                request.password(), request.active(), request.roles(), request.tagIds());
        return Response.ok(toResponse(user)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a user", description = "Deletes a user by its identifier. Fails if the user attempts to delete themselves.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "User deleted successfully"),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Cannot delete your own user account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteUser(
            @Parameter(description = "User identifier", required = true)
            @PathParam("id") String id) {
        String currentUserId = securityContext.getUserPrincipal().getName();
        deleteUserUseCase.execute(id, currentUserId);
        return Response.noContent().build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getLogin(), user.getName(), user.getEmail(),
                user.getRoles(), user.getTagIds(), user.isActive(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
