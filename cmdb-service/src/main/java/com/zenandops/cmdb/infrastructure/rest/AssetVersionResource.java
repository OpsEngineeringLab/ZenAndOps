package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.CreateAssetVersionUseCase;
import com.zenandops.cmdb.application.usecase.ListAssetVersionsUseCase;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import com.zenandops.cmdb.infrastructure.rest.dto.AssetVersionResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateAssetVersionRequest;
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
 * REST resource exposing AssetVersion creation and listing endpoints.
 * Versions are immutable — no update or delete operations are exposed.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/assets/{assetId}/versions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Asset Versions",
        description = "Immutable asset version history for the CMDB")
public class AssetVersionResource {

    @Inject
    CreateAssetVersionUseCase createAssetVersionUseCase;

    @Inject
    ListAssetVersionsUseCase listAssetVersionsUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create an asset version",
            description = "Creates a new immutable version for the specified asset")
    @RequestBody(description = "Asset version creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateAssetVersionRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Version created successfully",
                    content = @Content(schema = @Schema(implementation = AssetVersionResponse.class))),
            @APIResponse(responseCode = "404", description = "Asset or data source not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createVersion(
            @Parameter(description = "Asset identifier", required = true)
            @PathParam("assetId") String assetId,
            CreateAssetVersionRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        AssetVersion version = createAssetVersionUseCase.execute(
                assetId, request.description(), request.attributes(),
                request.dataOrigin(), request.dataSourceId(),
                request.changeReference(), userId);
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(version))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List asset version history",
            description = "Returns the complete version history for the specified asset, ordered by version number")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Version history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AssetVersionResponse[].class)))
    })
    public Response listVersions(
            @Parameter(description = "Asset identifier", required = true)
            @PathParam("assetId") String assetId) {
        List<AssetVersionResponse> items = listAssetVersionsUseCase.execute(assetId)
                .stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    private AssetVersionResponse toResponse(AssetVersion version) {
        return new AssetVersionResponse(
                version.getId(),
                version.getAssetId(),
                version.getVersionNumber(),
                version.getDescription(),
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
