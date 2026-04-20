package com.zenandops.auth.domain.exception;

/**
 * Thrown when a Role deletion is attempted while the Role is still assigned to one or more Users.
 */
public class RoleInUseException extends RuntimeException {

    public RoleInUseException() {
        super("Role is in use");
    }

    public RoleInUseException(String message) {
        super(message);
    }
}
