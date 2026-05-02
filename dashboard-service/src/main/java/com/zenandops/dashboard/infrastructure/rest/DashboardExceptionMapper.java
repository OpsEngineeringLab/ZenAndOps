package com.zenandops.dashboard.infrastructure.rest;

import com.zenandops.dashboard.infrastructure.rest.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

/**
 * JAX-RS exception mapper that converts unhandled runtime exceptions to
 * the standard error envelope format for the Dashboard service.
 */
@Provider
public class DashboardExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof io.quarkus.security.UnauthorizedException) {
            return buildResponse(Response.Status.UNAUTHORIZED,
                    "DASHBOARD_UNAUTHORIZED", "Authentication required");
        }
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "DASHBOARD_INTERNAL_ERROR", exception.getMessage());
    }

    private Response buildResponse(Response.Status status, String code, String message) {
        ErrorResponse error = new ErrorResponse(code, message, Instant.now());
        return Response.status(status)
                .entity(Map.of("error", error))
                .build();
    }
}
