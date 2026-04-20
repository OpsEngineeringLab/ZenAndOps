package com.zenandops.auth.domain.exception;

/**
 * Thrown when a Role with the same name already exists.
 */
public class RoleAlreadyExistsException extends RuntimeException {

    public RoleAlreadyExistsException() {
        super("Role already exists");
    }

    public RoleAlreadyExistsException(String message) {
        super(message);
    }
}
