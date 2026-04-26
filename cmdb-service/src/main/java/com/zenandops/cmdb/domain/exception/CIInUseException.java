package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a CI deletion is attempted while it has versions, relationships, or service associations.
 */
public class CIInUseException extends RuntimeException {

    public CIInUseException() {
        super("CI is in use");
    }

    public CIInUseException(String message) {
        super(message);
    }
}
