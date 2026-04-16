package com.zenandops.dashboard.infrastructure.rest;

import com.zenandops.dashboard.application.usecase.GetDashboardPayloadUseCase;
import com.zenandops.dashboard.domain.valueobject.DashboardPayload;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource exposing the operational dashboard endpoint.
 * Requires a valid Access_Token (JWT) for access.
 */
@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Dashboard", description = "Operational dashboard metrics and KPIs")
@SecurityRequirement(name = "bearerAuth")
public class DashboardResource {

    @Inject
    GetDashboardPayloadUseCase getDashboardPayloadUseCase;

    @GET
    @Operation(summary = "Get dashboard payload", description = "Retrieves the complete operational dashboard including executive summary, ticket metrics, SLI/SLO compliance, incident metrics, error budget, and change management data")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Dashboard payload retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DashboardPayload.class))),
            @APIResponse(responseCode = "401", description = "Not authenticated — missing or invalid Access_Token")
    })
    public DashboardPayload getDashboard() {
        return getDashboardPayloadUseCase.execute();
    }
}
