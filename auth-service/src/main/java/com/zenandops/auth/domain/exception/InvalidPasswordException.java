package com.zenandops.auth.domain.exception;

/**
 * Thrown when the provided current password does not match during a password change operation.
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Invalid password");
    }

    public InvalidPasswordException(String message) {
        super(message);
    }
}
