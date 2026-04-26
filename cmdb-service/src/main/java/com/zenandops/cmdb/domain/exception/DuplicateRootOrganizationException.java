package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when an attempt is made to create a second ROOT organization.
 */
public class DuplicateRootOrganizationException extends RuntimeException {

    public DuplicateRootOrganizationException() {
        super("A ROOT organization already exists");
    }

    public DuplicateRootOrganizationException(String message) {
        super(message);
    }
}
