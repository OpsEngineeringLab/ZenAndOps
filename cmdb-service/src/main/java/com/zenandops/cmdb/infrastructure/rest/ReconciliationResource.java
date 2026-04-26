package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.ListReconciliationsUseCase;
import com.zenandops.cmdb.application.usecase.TriggerReconciliationUseCase;
import com.zenandops.cmdb.domain.entity.ReconciliationRecord;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.ReconciliationResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.TriggerReconciliationRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;

/**
 * REST resource exposing reconciliation endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/reconciliations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Reconciliation",
        description = "Data reconciliation operations for the CMDB")
public class ReconciliationResource {

    @Inject
    TriggerReconciliationUseCase triggerReconciliationUseCase;

    @Inject
    ListReconciliationsUseCase listReconciliationsUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Trigger reconciliation",
            description = "Triggers a reconciliation process for the given entity type (ASSET or CI)")
    @RequestBody(description = "Reconciliation trigger data", required = true,
            content = @Content(schema = @Schema(implementation = TriggerReconciliationRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Reconciliation completed",
                    content = @Content(schema = @Schema(implementation = ReconciliationResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid entity type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response triggerReconciliation(TriggerReconciliationRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        ReconciliationRecord result = triggerReconciliationUseCase.execute(
                request.entityType(), userId);
        return Response.ok(toResponse(result)).build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List reconciliation history",
            description = "Retrieves the history of reconciliation operations")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Reconciliation history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ReconciliationResponse[].class)))
    })
    public Response listReconciliations() {
        List<ReconciliationResponse> items = listReconciliationsUseCase.execute().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    private ReconciliationResponse toResponse(ReconciliationRecord record) {
        List<ReconciliationResponse.ReconciliationDetailResponse> detailResponses =
                record.getDetails().stream()
                        .map(d -> new ReconciliationResponse.ReconciliationDetailResponse(
                                d.getEntityId(), d.getEntityName(),
                                d.getConflictType(), d.getResolution(),
                                d.getPreferredSourceId()))
                        .toList();

        return new ReconciliationResponse(
                record.getId(),
                record.getEntityType(),
                record.getRecordsAnalyzed(),
                record.getDuplicatesFound(),
                record.getConflictsResolved(),
                record.getUnresolvedConflicts(),
                detailResponses,
                record.getTriggeredBy(),
                record.getCreatedAt()
        );
    }
}
