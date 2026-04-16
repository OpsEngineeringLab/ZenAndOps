package com.zenandops.gateway.domain.exception;

/**
 * Thrown when a request to a protected route lacks a valid JWT.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
