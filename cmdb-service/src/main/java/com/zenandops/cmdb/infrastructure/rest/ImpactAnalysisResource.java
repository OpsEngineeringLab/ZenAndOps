package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.dto.AffectedEntity;
import com.zenandops.cmdb.application.dto.ImpactAnalysisResult;
import com.zenandops.cmdb.application.usecase.AnalyzeCIImpactUseCase;
import com.zenandops.cmdb.application.usecase.AnalyzeServiceImpactUseCase;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;

/**
 * REST resource exposing impact analysis endpoints.
 * All operations require authentication (any role).
 */
@Path("/api/v1/cmdb/impact-analysis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Impact Analysis",
        description = "Impact analysis operations for the CMDB")
public class ImpactAnalysisResource {

    @Inject
    AnalyzeCIImpactUseCase analyzeCIImpactUseCase;

    @Inject
    AnalyzeServiceImpactUseCase analyzeServiceImpactUseCase;

    @GET
    @Path("/ci/{ciId}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Analyze CI impact",
            description = "Performs impact analysis starting from a CI, traversing relationships and service associations")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Impact analysis completed",
                    content = @Content(schema = @Schema(implementation = ImpactAnalysisResponseDto.class))),
            @APIResponse(responseCode = "404", description = "CI not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response analyzeCIImpact(
            @Parameter(description = "CI identifier", required = true)
            @PathParam("ciId") String ciId) {
        ImpactAnalysisResult result = analyzeCIImpactUseCase.execute(ciId);
        return Response.ok(toResponseDto(result)).build();
    }

    @GET
    @Path("/service/{serviceId}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Analyze service impact",
            description = "Performs impact analysis starting from a Service, traversing dependencies and associated CIs")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Impact analysis completed",
                    content = @Content(schema = @Schema(implementation = ImpactAnalysisResponseDto.class))),
            @APIResponse(responseCode = "404", description = "Service not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response analyzeServiceImpact(
            @Parameter(description = "Service identifier", required = true)
            @PathParam("serviceId") String serviceId) {
        ImpactAnalysisResult result = analyzeServiceImpactUseCase.execute(serviceId);
        return Response.ok(toResponseDto(result)).build();
    }

    private ImpactAnalysisResponseDto toResponseDto(ImpactAnalysisResult result) {
        RootEntityDto rootEntity = new RootEntityDto(
                result.rootEntityId(), result.rootEntityName(), result.rootEntityType());

        List<AffectedEntityDto> affected = result.affectedEntities().stream()
                .map(e -> new AffectedEntityDto(
                        e.id(), e.name(), e.entityType(), e.relationshipPath(), e.depth()))
                .toList();

        return new ImpactAnalysisResponseDto(
                rootEntity, affected,
                result.totalAffectedServices(), result.totalAffectedCIs(),
                result.circularDependencyWarnings(), result.maxDepthReached());
    }

    /**
     * Response DTO matching the design spec Impact Analysis Response format.
     */
    public record ImpactAnalysisResponseDto(
            RootEntityDto rootEntity,
            List<AffectedEntityDto> affectedEntities,
            int totalAffectedServices,
            int totalAffectedCIs,
            List<String> circularDependencyWarnings,
            boolean maxDepthReached
    ) {
    }

    public record RootEntityDto(String id, String name, String type) {
    }

    public record AffectedEntityDto(
            String id,
            String name,
            String entityType,
            List<String> relationshipPath,
            int depth
    ) {
    }
}
