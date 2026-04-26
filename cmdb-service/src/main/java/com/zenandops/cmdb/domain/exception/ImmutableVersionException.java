package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when an attempt is made to update or delete an immutable version record.
 */
public class ImmutableVersionException extends RuntimeException {

    public ImmutableVersionException() {
        super("Version records are immutable and cannot be updated or deleted");
    }

    public ImmutableVersionException(String message) {
        super(message);
    }
}
