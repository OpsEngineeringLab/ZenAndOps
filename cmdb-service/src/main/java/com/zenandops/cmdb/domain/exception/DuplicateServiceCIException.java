package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a duplicate Service-CI association already exists.
 */
public class DuplicateServiceCIException extends RuntimeException {

    public DuplicateServiceCIException() {
        super("Service-CI association already exists");
    }

    public DuplicateServiceCIException(String message) {
        super(message);
    }
}
