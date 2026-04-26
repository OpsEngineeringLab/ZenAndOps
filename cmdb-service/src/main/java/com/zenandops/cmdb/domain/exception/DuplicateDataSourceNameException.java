package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a Data Source with the same name already exists.
 */
public class DuplicateDataSourceNameException extends RuntimeException {

    public DuplicateDataSourceNameException() {
        super("Data source name already exists");
    }

    public DuplicateDataSourceNameException(String message) {
        super(message);
    }
}
