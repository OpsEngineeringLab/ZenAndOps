package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.dto.CostSummaryEntry;
import com.zenandops.cmdb.application.usecase.CreateAssetUseCase;
import com.zenandops.cmdb.application.usecase.DeleteAssetUseCase;
import com.zenandops.cmdb.application.usecase.GetAssetCostSummaryUseCase;
import com.zenandops.cmdb.application.usecase.GetAssetUseCase;
import com.zenandops.cmdb.application.usecase.ListAssetsUseCase;
import com.zenandops.cmdb.application.usecase.UpdateAssetUseCase;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;
import com.zenandops.cmdb.infrastructure.rest.dto.AssetResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateAssetRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.UpdateAssetRequest;
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
 * REST resource exposing Asset CRUD, filtering, and cost summary endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Assets",
        description = "Asset lifecycle and cost management for the CMDB")
public class AssetResource {

    @Inject
    CreateAssetUseCase createAssetUseCase;

    @Inject
    GetAssetUseCase getAssetUseCase;

    @Inject
    UpdateAssetUseCase updateAssetUseCase;

    @Inject
    ListAssetsUseCase listAssetsUseCase;

    @Inject
    DeleteAssetUseCase deleteAssetUseCase;

    @Inject
    GetAssetCostSummaryUseCase getAssetCostSummaryUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create an asset",
            description = "Creates a new asset in the CMDB")
    @RequestBody(description = "Asset creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateAssetRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Asset created successfully",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @APIResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createAsset(CreateAssetRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        Asset asset = createAssetUseCase.execute(
                request.name(), request.type(), request.organizationId(),
                request.cost(), request.costType(), request.acquisitionDate(),
                request.status(), request.supplier(), userId);
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(asset))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List assets",
            description = "Retrieves assets with optional filtering by organizationId, type, costType, status, and supplier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Assets retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AssetResponse[].class)))
    })
    public Response listAssets(
            @Parameter(description = "Filter by organization ID")
            @QueryParam("organizationId") String organizationId,
            @Parameter(description = "Filter by asset type")
            @QueryParam("type") AssetType type,
            @Parameter(description = "Filter by cost type")
            @QueryParam("costType") CostType costType,
            @Parameter(description = "Filter by asset status")
            @QueryParam("status") AssetStatus status,
            @Parameter(description = "Filter by supplier")
            @QueryParam("supplier") String supplier) {
        List<AssetResponse> items = listAssetsUseCase.execute(
                organizationId, type, costType, status, supplier).stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get an asset by ID",
            description = "Retrieves a single asset by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Asset retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @APIResponse(responseCode = "404", description = "Asset not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAsset(
            @Parameter(description = "Asset identifier", required = true)
            @PathParam("id") String id) {
        Asset asset = getAssetUseCase.execute(id);
        return Response.ok(toResponse(asset)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Update an asset",
            description = "Updates the mutable fields of an existing asset")
    @RequestBody(description = "Asset update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateAssetRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Asset updated successfully",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @APIResponse(responseCode = "404", description = "Asset not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateAsset(
            @Parameter(description = "Asset identifier", required = true)
            @PathParam("id") String id,
            UpdateAssetRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        Asset asset = updateAssetUseCase.execute(
                id, request.name(), request.cost(), request.costType(),
                request.status(), request.supplier(), userId);
        return Response.ok(toResponse(asset)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete an asset",
            description = "Deletes an asset. Fails if the asset has CIs or active versions.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Asset deleted successfully"),
            @APIResponse(responseCode = "404", description = "Asset not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Asset is in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteAsset(
            @Parameter(description = "Asset identifier", required = true)
            @PathParam("id") String id) {
        deleteAssetUseCase.execute(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/cost-summary")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get asset cost summary",
            description = "Returns total cost of assets grouped by organization and cost type")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cost summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CostSummaryEntry[].class)))
    })
    public Response getCostSummary() {
        List<CostSummaryEntry> summary = getAssetCostSummaryUseCase.execute();
        return Response.ok(summary).build();
    }

    private AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getType(),
                asset.getOrganizationId(),
                asset.getCost(),
                asset.getCostType(),
                asset.getAcquisitionDate(),
                asset.getStatus(),
                asset.getSupplier(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
