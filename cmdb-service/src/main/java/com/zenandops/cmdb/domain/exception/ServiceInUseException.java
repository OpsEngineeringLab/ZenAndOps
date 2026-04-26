package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a Service deletion is attempted while it has children, dependencies, or CI associations.
 */
public class ServiceInUseException extends RuntimeException {

    public ServiceInUseException() {
        super("Service is in use");
    }

    public ServiceInUseException(String message) {
        super(message);
    }
}
