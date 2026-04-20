package com.zenandops.auth.domain.exception;

/**
 * Thrown when a Role with the given identifier does not exist.
 */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException() {
        super("Role not found");
    }

    public RoleNotFoundException(String message) {
        super(message);
    }
}
