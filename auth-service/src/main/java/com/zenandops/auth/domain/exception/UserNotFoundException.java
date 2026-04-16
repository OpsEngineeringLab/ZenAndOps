package com.zenandops.auth.domain.exception;

/**
 * Thrown when a User with the given identifier does not exist.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User not found");
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
