package com.zenandops.admin.infrastructure.rest;

import com.zenandops.admin.domain.exception.ForbiddenException;
import com.zenandops.admin.infrastructure.adapter.keycloak.KeycloakAdminException;
import com.zenandops.admin.infrastructure.rest.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import java.time.Instant;
import java.util.Map;

/**
 * JAX-RS exception mapper that converts admin domain exceptions to
 * appropriate HTTP responses using the standard error envelope format.
 */
@Provider
public class AdminExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof CircuitBreakerOpenException) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(buildEnvelope("ADMIN_SERVICE_UNAVAILABLE",
                            "Service temporarily unavailable due to circuit breaker"))
                    .build();
        }
        if (exception instanceof BulkheadException) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(buildEnvelope("ADMIN_SERVICE_UNAVAILABLE",
                            "Service temporarily unavailable due to capacity limits"))
                    .build();
        }
        if (exception instanceof ForbiddenException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(buildEnvelope("FORBIDDEN", exception.getMessage()))
                    .build();
        }
        if (exception instanceof KeycloakAdminException keycloakEx) {
            return mapKeycloakException(keycloakEx);
        }
        // Let other exceptions propagate to the default handler
        throw exception;
    }

    private Response mapKeycloakException(KeycloakAdminException exception) {
        int keycloakStatus = exception.getStatusCode();
        return switch (keycloakStatus) {
            case 400 -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(buildEnvelope("BAD_REQUEST", exception.getMessage()))
                    .build();
            case 401, 403 -> Response.status(Response.Status.FORBIDDEN)
                    .entity(buildEnvelope("FORBIDDEN", exception.getMessage()))
                    .build();
            case 404 -> Response.status(Response.Status.NOT_FOUND)
                    .entity(buildEnvelope("NOT_FOUND", exception.getMessage()))
                    .build();
            case 409 -> Response.status(409)
                    .entity(buildEnvelope("CONFLICT", exception.getMessage()))
                    .build();
            default -> Response.status(502)
                    .entity(buildEnvelope("SERVICE_UNAVAILABLE", exception.getMessage()))
                    .build();
        };
    }

    private Map<String, ErrorResponse> buildEnvelope(String code, String message) {
        return Map.of("error", new ErrorResponse(code, message, Instant.now()));
    }
}
