package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a CI with the given identifier does not exist.
 */
public class CINotFoundException extends RuntimeException {

    public CINotFoundException() {
        super("CI not found");
    }

    public CINotFoundException(String message) {
        super(message);
    }
}
