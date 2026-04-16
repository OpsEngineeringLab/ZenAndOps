package com.zenandops.auth.domain.exception;

/**
 * Thrown when a Tag with the given identifier does not exist.
 */
public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException() {
        super("Tag not found");
    }

    public TagNotFoundException(String message) {
        super(message);
    }
}
