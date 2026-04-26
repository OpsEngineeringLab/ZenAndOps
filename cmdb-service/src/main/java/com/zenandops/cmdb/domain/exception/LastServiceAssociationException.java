package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when attempting to remove the last service association from a CI
 * that does not have the controlled exception flag set.
 */
public class LastServiceAssociationException extends RuntimeException {

    public LastServiceAssociationException() {
        super("Cannot remove last service association from CI");
    }

    public LastServiceAssociationException(String message) {
        super(message);
    }
}
