package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.GetAssetVersionAtTimeUseCase;
import com.zenandops.cmdb.application.usecase.GetCIVersionAtTimeUseCase;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import com.zenandops.cmdb.domain.entity.CIVersion;
import com.zenandops.cmdb.infrastructure.rest.dto.AssetVersionResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.CIVersionResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.time.Instant;

/**
 * REST resource exposing historical point-in-time query endpoints.
 * All operations require authentication (any role).
 */
@Path("/api/v1/cmdb/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Historical Queries",
        description = "Point-in-time historical queries for the CMDB")
public class HistoricalQueryResource {

    @Inject
    GetAssetVersionAtTimeUseCase getAssetVersionAtTimeUseCase;

    @Inject
    GetCIVersionAtTimeUseCase getCIVersionAtTimeUseCase;

    @GET
    @Path("/assets/{assetId}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get asset version at time",
            description = "Returns the asset version that was active at the specified timestamp")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Asset version found",
                    content = @Content(schema = @Schema(implementation = AssetVersionResponse.class))),
            @APIResponse(responseCode = "404", description = "Asset or version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAssetVersionAtTime(
            @Parameter(description = "Asset identifier", required = true)
            @PathParam("assetId") String assetId,
            @Parameter(description = "Point-in-time timestamp (ISO 8601)", required = true)
            @QueryParam("at") Instant at) {
        AssetVersion version = getAssetVersionAtTimeUseCase.execute(assetId, at);
        return Response.ok(toAssetVersionResponse(version)).build();
    }

    @GET
    @Path("/cis/{ciId}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get CI version at time",
            description = "Returns the CI version that was active at the specified timestamp")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "CI version found",
                    content = @Content(schema = @Schema(implementation = CIVersionResponse.class))),
            @APIResponse(responseCode = "404", description = "CI or version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getCIVersionAtTime(
            @Parameter(description = "CI identifier", required = true)
            @PathParam("ciId") String ciId,
            @Parameter(description = "Point-in-time timestamp (ISO 8601)", required = true)
            @QueryParam("at") Instant at) {
        CIVersion version = getCIVersionAtTimeUseCase.execute(ciId, at);
        return Response.ok(toCIVersionResponse(version)).build();
    }

    private AssetVersionResponse toAssetVersionResponse(AssetVersion version) {
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

    private CIVersionResponse toCIVersionResponse(CIVersion version) {
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
