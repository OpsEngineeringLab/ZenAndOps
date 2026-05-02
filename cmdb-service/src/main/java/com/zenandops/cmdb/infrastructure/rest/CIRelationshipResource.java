package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.CreateCIRelationshipUseCase;
import com.zenandops.cmdb.application.usecase.DeleteCIRelationshipUseCase;
import com.zenandops.cmdb.application.usecase.ListCIRelationshipsUseCase;
import com.zenandops.cmdb.domain.entity.CIRelationship;
import com.zenandops.cmdb.infrastructure.rest.dto.CIRelationshipResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateCIRelationshipRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.PaginatedResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST resource exposing CIRelationship CRUD endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/ci-relationships")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "CI Relationships",
        description = "CI relationship management for the CMDB")
public class CIRelationshipResource {

    @Inject
    CreateCIRelationshipUseCase createCIRelationshipUseCase;

    @Inject
    DeleteCIRelationshipUseCase deleteCIRelationshipUseCase;

    @Inject
    ListCIRelationshipsUseCase listCIRelationshipsUseCase;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create a CI relationship",
            description = "Creates a directed relationship between two CIs")
    @RequestBody(description = "CI relationship creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateCIRelationshipRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Relationship created successfully",
                    content = @Content(schema = @Schema(implementation = CIRelationshipResponse.class))),
            @APIResponse(responseCode = "404", description = "Source or target CI not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "400", description = "Self-reference not allowed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Duplicate relationship",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createRelationship(CreateCIRelationshipRequest request) {
        CIRelationship relationship = createCIRelationshipUseCase.execute(
                request.sourceCIId(), request.targetCIId(), request.relationshipType());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(relationship))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List CI relationships",
            description = "Returns all relationships (upstream and downstream) for a given CI")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Relationships retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CIRelationshipResponse[].class)))
    })
    public Response listRelationships(
            @Parameter(description = "Page number (zero-based)")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size")
            @QueryParam("size") @DefaultValue("50") int size,
            @Parameter(description = "CI ID to list relationships for", required = true)
            @QueryParam("ciId") String ciId) {
        if (page < 0 || size < 1 || size > 200) {
            return Response.status(400)
                    .entity(Map.of("error", new ErrorResponse("CMDB_VALIDATION_ERROR",
                            "page must be >= 0, size must be between 1 and 200",
                            Instant.now())))
                    .build();
        }
        var result = listCIRelationshipsUseCase.execute(ciId, page, size);
        List<CIRelationshipResponse> items = result.items().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(PaginatedResponse.of(items, page, size, result.totalItems())).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete a CI relationship",
            description = "Deletes a CI relationship by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Relationship deleted successfully"),
            @APIResponse(responseCode = "404", description = "Relationship not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteRelationship(
            @Parameter(description = "Relationship identifier", required = true)
            @PathParam("id") String id) {
        deleteCIRelationshipUseCase.execute(id);
        return Response.noContent().build();
    }

    private CIRelationshipResponse toResponse(CIRelationship relationship) {
        return new CIRelationshipResponse(
                relationship.getId(),
                relationship.getSourceCIId(),
                relationship.getTargetCIId(),
                relationship.getRelationshipType(),
                relationship.getCreatedAt()
        );
    }
}
