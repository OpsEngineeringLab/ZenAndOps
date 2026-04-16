package com.zenandops.gateway.infrastructure.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * Health check endpoint for the Gateway_Service.
 * Used by Docker Compose and orchestration tools for readiness verification.
 */
@Path("/q/health")
@Tag(name = "Health", description = "Gateway health check endpoints")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health check", description = "Returns the health status of the Gateway_Service")
    @APIResponse(responseCode = "200", description = "Service is healthy")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
