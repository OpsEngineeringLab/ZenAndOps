package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a dependency or relationship is created where the source and target are the same entity.
 */
public class SelfReferenceException extends RuntimeException {

    public SelfReferenceException() {
        super("Source and target cannot be the same entity");
    }

    public SelfReferenceException(String message) {
        super(message);
    }
}
