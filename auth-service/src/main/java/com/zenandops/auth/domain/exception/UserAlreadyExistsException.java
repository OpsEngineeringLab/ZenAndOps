package com.zenandops.auth.domain.exception;

/**
 * Thrown when a User with the same login already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException() {
        super("User already exists");
    }

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
