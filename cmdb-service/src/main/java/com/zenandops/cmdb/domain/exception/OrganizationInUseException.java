package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when an Organization deletion is attempted while it has children, services, or assets.
 */
public class OrganizationInUseException extends RuntimeException {

    public OrganizationInUseException() {
        super("Organization is in use");
    }

    public OrganizationInUseException(String message) {
        super(message);
    }
}
