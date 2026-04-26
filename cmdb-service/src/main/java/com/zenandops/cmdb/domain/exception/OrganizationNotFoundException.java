package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when an Organization with the given identifier does not exist.
 */
public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException() {
        super("Organization not found");
    }

    public OrganizationNotFoundException(String message) {
        super(message);
    }
}
