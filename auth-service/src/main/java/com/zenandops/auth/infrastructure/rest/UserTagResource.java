package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.application.usecase.AssignTagsToUserUseCase;
import com.zenandops.auth.application.usecase.GetUserTagsUseCase;
import com.zenandops.auth.application.usecase.RemoveTagsFromUserUseCase;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.auth.infrastructure.rest.dto.TagResponse;
import com.zenandops.auth.infrastructure.rest.dto.UserTagsRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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

import java.util.List;

/**
 * REST resource exposing User-Tag assignment endpoints. All endpoints require ADMIN role.
 */
@Path("/api/v1/users/{userId}/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "User Tags", description = "User-Tag assignment operations for ABAC attribute management")
public class UserTagResource {

    @Inject
    GetUserTagsUseCase getUserTagsUseCase;

    @Inject
    AssignTagsToUserUseCase assignTagsToUserUseCase;

    @Inject
    RemoveTagsFromUserUseCase removeTagsFromUserUseCase;

    @GET
    @Operation(summary = "Get user tags", description = "Retrieves all tags currently assigned to a user")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tags retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TagResponse[].class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getUserTags(
            @Parameter(description = "User identifier", required = true)
            @PathParam("userId") String userId) {
        List<Tag> tags = getUserTagsUseCase.execute(userId);
        List<TagResponse> response = tags.stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(response).build();
    }

    @POST
    @Operation(summary = "Assign tags to user", description = "Assigns one or more tags to a user. Duplicate assignments are ignored.")
    @RequestBody(description = "Tag identifiers to assign", required = true,
            content = @Content(schema = @Schema(implementation = UserTagsRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tags assigned successfully",
                    content = @Content(schema = @Schema(implementation = TagResponse[].class))),
            @APIResponse(responseCode = "404", description = "User or tag not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response assignTags(
            @Parameter(description = "User identifier", required = true)
            @PathParam("userId") String userId,
            UserTagsRequest request) {
        assignTagsToUserUseCase.execute(userId, request.tagIds());
        List<Tag> tags = getUserTagsUseCase.execute(userId);
        List<TagResponse> response = tags.stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(response).build();
    }

    @DELETE
    @Operation(summary = "Remove tags from user", description = "Removes one or more tags from a user")
    @RequestBody(description = "Tag identifiers to remove", required = true,
            content = @Content(schema = @Schema(implementation = UserTagsRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tags removed successfully",
                    content = @Content(schema = @Schema(implementation = TagResponse[].class))),
            @APIResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response removeTags(
            @Parameter(description = "User identifier", required = true)
            @PathParam("userId") String userId,
            UserTagsRequest request) {
        removeTagsFromUserUseCase.execute(userId, request.tagIds());
        List<Tag> tags = getUserTagsUseCase.execute(userId);
        List<TagResponse> response = tags.stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(response).build();
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getKey(), tag.getValue(),
                tag.getDescription(), tag.getCreatedAt(), tag.getUpdatedAt());
    }
}
