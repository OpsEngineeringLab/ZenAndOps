package com.zenandops.auth.domain.exception;

/**
 * Thrown when a Tag with the same key:value combination already exists.
 */
public class TagAlreadyExistsException extends RuntimeException {

    public TagAlreadyExistsException() {
        super("Tag already exists");
    }

    public TagAlreadyExistsException(String message) {
        super(message);
    }
}
