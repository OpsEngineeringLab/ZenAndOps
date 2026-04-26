package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a sibling organization with the same name already exists under the same parent.
 */
public class DuplicateSiblingNameException extends RuntimeException {

    public DuplicateSiblingNameException() {
        super("A sibling with the same name already exists under this parent");
    }

    public DuplicateSiblingNameException(String message) {
        super(message);
    }
}
