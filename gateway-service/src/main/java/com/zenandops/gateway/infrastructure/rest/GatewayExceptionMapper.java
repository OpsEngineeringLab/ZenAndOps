package com.zenandops.gateway.infrastructure.rest;

import com.zenandops.gateway.domain.exception.ForbiddenException;
import com.zenandops.gateway.domain.exception.RateLimitExceededException;
import com.zenandops.gateway.domain.exception.RouteNotFoundException;
import com.zenandops.gateway.domain.exception.UnauthorizedException;
import com.zenandops.gateway.infrastructure.adapter.keycloak.KeycloakAdminException;
import com.zenandops.gateway.infrastructure.adapter.proxy.VertxHttpProxyAdapter.BackendServiceUnavailableException;
import com.zenandops.gateway.infrastructure.rest.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

/**
 * JAX-RS exception mapper that converts gateway domain exceptions to
 * appropriate HTTP responses using the standard error envelope format.
 */
@Provider
public class GatewayExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof RateLimitExceededException rateLimited) {
            return Response.status(429)
                    .header("Retry-After", rateLimited.getRetryAfterSeconds())
                    .entity(buildEnvelope("GATEWAY_RATE_LIMITED", rateLimited.getMessage()))
                    .build();
        }
        if (exception instanceof UnauthorizedException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(buildEnvelope("GATEWAY_UNAUTHORIZED", exception.getMessage()))
                    .build();
        }
        if (exception instanceof ForbiddenException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(buildEnvelope("FORBIDDEN", exception.getMessage()))
                    .build();
        }
        if (exception instanceof RouteNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildEnvelope("GATEWAY_ROUTE_NOT_FOUND", exception.getMessage()))
                    .build();
        }
        if (exception instanceof BackendServiceUnavailableException) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(buildEnvelope("GATEWAY_SERVICE_UNAVAILABLE", exception.getMessage()))
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
