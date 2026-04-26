package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a duplicate service dependency or CI relationship already exists.
 */
public class DuplicateDependencyException extends RuntimeException {

    public DuplicateDependencyException() {
        super("Dependency already exists");
    }

    public DuplicateDependencyException(String message) {
        super(message);
    }
}
