package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.application.usecase.CreateTagUseCase;
import com.zenandops.auth.application.usecase.DeleteTagUseCase;
import com.zenandops.auth.application.usecase.GetTagUseCase;
import com.zenandops.auth.application.usecase.ListTagsUseCase;
import com.zenandops.auth.application.usecase.PaginatedResult;
import com.zenandops.auth.application.usecase.UpdateTagUseCase;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.infrastructure.rest.dto.CreateTagRequest;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.auth.infrastructure.rest.dto.PaginatedTagsResponse;
import com.zenandops.auth.infrastructure.rest.dto.TagResponse;
import com.zenandops.auth.infrastructure.rest.dto.UpdateTagRequest;
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
 * REST resource exposing Tag CRUD endpoints. All endpoints require ADMIN role.
 */
@Path("/api/v1/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Tags", description = "Tag CRUD operations for ABAC attribute management")
public class TagResource {

    @Inject
    CreateTagUseCase createTagUseCase;

    @Inject
    ListTagsUseCase listTagsUseCase;

    @Inject
    GetTagUseCase getTagUseCase;

    @Inject
    UpdateTagUseCase updateTagUseCase;

    @Inject
    DeleteTagUseCase deleteTagUseCase;

    @POST
    @Operation(summary = "Create a tag", description = "Creates a new tag with the given key, value, and optional description")
    @RequestBody(description = "Tag creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateTagRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Tag created successfully",
                    content = @Content(schema = @Schema(implementation = TagResponse.class))),
            @APIResponse(responseCode = "409", description = "Tag with this key:value combination already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createTag(CreateTagRequest request) {
        Tag tag = createTagUseCase.execute(request.key(), request.value(), request.description());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(tag))
                .build();
    }

    @GET
    @Operation(summary = "List all tags", description = "Retrieves a paginated list of all tags")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tags retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaginatedTagsResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response listTags(
            @Parameter(description = "Page number (zero-based)", example = "0")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size", example = "20")
            @QueryParam("size") @DefaultValue("20") int size) {
        PaginatedResult<Tag> result = listTagsUseCase.execute(page, size);
        List<TagResponse> items = result.items().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(new PaginatedTagsResponse(items, result.page(), result.size(),
                result.totalItems(), result.totalPages())).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a tag by ID", description = "Retrieves a single tag by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tag retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TagResponse.class))),
            @APIResponse(responseCode = "404", description = "Tag not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getTag(
            @Parameter(description = "Tag identifier", required = true)
            @PathParam("id") String id) {
        Tag tag = getTagUseCase.execute(id);
        return Response.ok(toResponse(tag)).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a tag", description = "Updates the description of an existing tag")
    @RequestBody(description = "Tag update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateTagRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tag updated successfully",
                    content = @Content(schema = @Schema(implementation = TagResponse.class))),
            @APIResponse(responseCode = "404", description = "Tag not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateTag(
            @Parameter(description = "Tag identifier", required = true)
            @PathParam("id") String id,
            UpdateTagRequest request) {
        Tag tag = updateTagUseCase.execute(id, request.description());
        return Response.ok(toResponse(tag)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a tag", description = "Deletes a tag by its identifier. Fails if the tag is assigned to any user.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Tag deleted successfully"),
            @APIResponse(responseCode = "404", description = "Tag not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Tag is in use and cannot be deleted",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions — ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteTag(
            @Parameter(description = "Tag identifier", required = true)
            @PathParam("id") String id) {
        deleteTagUseCase.execute(id);
        return Response.noContent().build();
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getKey(), tag.getValue(),
                tag.getDescription(), tag.getCreatedAt(), tag.getUpdatedAt());
    }
}
