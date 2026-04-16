package com.zenandops.auth.domain.exception;

/**
 * Thrown when a Tag deletion is attempted while the Tag is still assigned to one or more Users.
 */
public class TagInUseException extends RuntimeException {

    public TagInUseException() {
        super("Tag is in use");
    }

    public TagInUseException(String message) {
        super(message);
    }
}
