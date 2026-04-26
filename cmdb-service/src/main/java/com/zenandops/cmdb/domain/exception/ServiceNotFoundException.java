package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a Service with the given identifier does not exist.
 */
public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException() {
        super("Service not found");
    }

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
