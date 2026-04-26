package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.CreateCIUseCase;
import com.zenandops.cmdb.application.usecase.DeleteCIUseCase;
import com.zenandops.cmdb.application.usecase.GetCIUseCase;
import com.zenandops.cmdb.application.usecase.ListCIsUseCase;
import com.zenandops.cmdb.application.usecase.UpdateCIUseCase;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import com.zenandops.cmdb.infrastructure.rest.dto.CIResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateCIRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.UpdateCIRequest;
import jakarta.annotation.security.RolesAllowed;
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
 * REST resource exposing CI CRUD, filtering endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/cis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Configuration Items",
        description = "CI lifecycle management for the CMDB")
public class CIResource {

    @Inject
    CreateCIUseCase createCIUseCase;

    @Inject
    GetCIUseCase getCIUseCase;

    @Inject
    UpdateCIUseCase updateCIUseCase;

    @Inject
    ListCIsUseCase listCIsUseCase;

    @Inject
    DeleteCIUseCase deleteCIUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create a CI",
            description = "Creates a new configuration item in the CMDB")
    @RequestBody(description = "CI creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateCIRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "CI created successfully",
                    content = @Content(schema = @Schema(implementation = CIResponse.class))),
            @APIResponse(responseCode = "404", description = "Organization or asset not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createCI(CreateCIRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        CI ci = createCIUseCase.execute(
                request.name(), request.type(), request.organizationId(),
                request.assetId(), request.status(),
                request.controlledExceptionFlag(), userId);
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(ci))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List CIs",
            description = "Retrieves CIs with optional filtering by organizationId, type, status, and assetId")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "CIs retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CIResponse[].class)))
    })
    public Response listCIs(
            @Parameter(description = "Filter by organization ID")
            @QueryParam("organizationId") String organizationId,
            @Parameter(description = "Filter by CI type")
            @QueryParam("type") CIType type,
            @Parameter(description = "Filter by CI status")
            @QueryParam("status") CIStatus status,
            @Parameter(description = "Filter by asset ID")
            @QueryParam("assetId") String assetId) {
        List<CIResponse> items = listCIsUseCase.execute(organizationId, type, status, assetId)
                .stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get a CI by ID",
            description = "Retrieves a single CI by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "CI retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CIResponse.class))),
            @APIResponse(responseCode = "404", description = "CI not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getCI(
            @Parameter(description = "CI identifier", required = true)
            @PathParam("id") String id) {
        CI ci = getCIUseCase.execute(id);
        return Response.ok(toResponse(ci)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Update a CI",
            description = "Updates the mutable fields of an existing CI")
    @RequestBody(description = "CI update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateCIRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "CI updated successfully",
                    content = @Content(schema = @Schema(implementation = CIResponse.class))),
            @APIResponse(responseCode = "404", description = "CI not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateCI(
            @Parameter(description = "CI identifier", required = true)
            @PathParam("id") String id,
            UpdateCIRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        CI ci = updateCIUseCase.execute(
                id, request.name(), request.status(),
                request.controlledExceptionFlag(), userId);
        return Response.ok(toResponse(ci)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete a CI",
            description = "Deletes a CI. Fails if the CI has versions, relationships, or service associations.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "CI deleted successfully"),
            @APIResponse(responseCode = "404", description = "CI not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "CI is in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteCI(
            @Parameter(description = "CI identifier", required = true)
            @PathParam("id") String id) {
        deleteCIUseCase.execute(id);
        return Response.noContent().build();
    }

    private CIResponse toResponse(CI ci) {
        return new CIResponse(
                ci.getId(),
                ci.getName(),
                ci.getType(),
                ci.getOrganizationId(),
                ci.getAssetId(),
                ci.getStatus(),
                ci.isControlledExceptionFlag(),
                ci.getCreatedAt(),
                ci.getUpdatedAt()
        );
    }
}
