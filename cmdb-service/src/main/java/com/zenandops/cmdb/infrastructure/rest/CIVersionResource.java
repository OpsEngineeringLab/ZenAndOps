package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.CreateCIVersionUseCase;
import com.zenandops.cmdb.application.usecase.ListCIVersionsUseCase;
import com.zenandops.cmdb.domain.entity.CIVersion;
import com.zenandops.cmdb.infrastructure.rest.dto.CIVersionResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateCIVersionRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
 * REST resource exposing CIVersion creation and listing endpoints.
 * Versions are immutable — no update or delete operations are exposed.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/cis/{ciId}/versions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "CI Versions",
        description = "Immutable CI version history for the CMDB")
public class CIVersionResource {

    @Inject
    CreateCIVersionUseCase createCIVersionUseCase;

    @Inject
    ListCIVersionsUseCase listCIVersionsUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create a CI version",
            description = "Creates a new immutable version for the specified CI")
    @RequestBody(description = "CI version creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateCIVersionRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Version created successfully",
                    content = @Content(schema = @Schema(implementation = CIVersionResponse.class))),
            @APIResponse(responseCode = "404", description = "CI or data source not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createVersion(
            @Parameter(description = "CI identifier", required = true)
            @PathParam("ciId") String ciId,
            CreateCIVersionRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        CIVersion version = createCIVersionUseCase.execute(
                ciId, request.attributes(), request.dataOrigin(),
                request.dataSourceId(), request.changeReference(), userId);
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(version))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List CI version history",
            description = "Returns the complete version history for the specified CI, ordered by version number")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Version history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CIVersionResponse[].class)))
    })
    public Response listVersions(
            @Parameter(description = "CI identifier", required = true)
            @PathParam("ciId") String ciId) {
        List<CIVersionResponse> items = listCIVersionsUseCase.execute(ciId)
                .stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    private CIVersionResponse toResponse(CIVersion version) {
        return new CIVersionResponse(
                version.getId(),
                version.getCiId(),
                version.getVersionNumber(),
                version.getAttributes(),
                version.getStartDate(),
                version.getEndDate(),
                version.getDataOrigin(),
                version.getDataSourceId(),
                version.getChangeReference(),
                version.getCreatedAt()
        );
    }
}
