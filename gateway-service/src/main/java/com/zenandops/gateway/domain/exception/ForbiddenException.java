package com.zenandops.gateway.domain.exception;

/**
 * Thrown when a user lacks the required permission to perform an operation.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
