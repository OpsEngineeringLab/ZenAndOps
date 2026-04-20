package com.zenandops.auth.domain.exception;

/**
 * Thrown when an administrator attempts to delete their own user account.
 */
public class SelfDeletionException extends RuntimeException {

    public SelfDeletionException() {
        super("Self-deletion is not allowed");
    }

    public SelfDeletionException(String message) {
        super(message);
    }
}
