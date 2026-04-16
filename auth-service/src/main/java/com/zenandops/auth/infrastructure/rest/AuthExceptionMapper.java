package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.domain.exception.AccessDeniedException;
import com.zenandops.auth.domain.exception.InvalidCredentialsException;
import com.zenandops.auth.domain.exception.TagAlreadyExistsException;
import com.zenandops.auth.domain.exception.TagInUseException;
import com.zenandops.auth.domain.exception.TagNotFoundException;
import com.zenandops.auth.domain.exception.TokenExpiredException;
import com.zenandops.auth.domain.exception.TokenRevokedException;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

/**
 * JAX-RS exception mapper that converts domain exceptions to appropriate HTTP responses.
 */
@Provider
public class AuthExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof InvalidCredentialsException) {
            return buildResponse(Response.Status.UNAUTHORIZED,
                    "AUTH_INVALID_CREDENTIALS", "Authentication failed");
        }
        if (exception instanceof TokenExpiredException) {
            return buildResponse(Response.Status.UNAUTHORIZED,
                    "AUTH_TOKEN_EXPIRED", "Token has expired");
        }
        if (exception instanceof TokenRevokedException) {
            return buildResponse(Response.Status.UNAUTHORIZED,
                    "AUTH_REFRESH_REVOKED", "Token has been revoked");
        }
        if (exception instanceof AccessDeniedException) {
            return buildResponse(Response.Status.FORBIDDEN,
                    "AUTH_FORBIDDEN", "Access denied");
        }
        if (exception instanceof TagAlreadyExistsException) {
            return buildResponse(Response.Status.CONFLICT,
                    "TAG_ALREADY_EXISTS", exception.getMessage());
        }
        if (exception instanceof TagInUseException) {
            return buildResponse(Response.Status.CONFLICT,
                    "TAG_IN_USE", exception.getMessage());
        }
        if (exception instanceof TagNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "TAG_NOT_FOUND", exception.getMessage());
        }
        if (exception instanceof UserNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "USER_NOT_FOUND", exception.getMessage());
        }
        // Let other exceptions propagate
        throw exception;
    }

    private Response buildResponse(Response.Status status, String code, String message) {
        ErrorResponse error = new ErrorResponse(code, message, Instant.now());
        return Response.status(status)
                .entity(Map.of("error", error))
                .build();
    }
}
