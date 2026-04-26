package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a Data Source with the given identifier does not exist.
 */
public class DataSourceNotFoundException extends RuntimeException {

    public DataSourceNotFoundException() {
        super("Data source not found");
    }

    public DataSourceNotFoundException(String message) {
        super(message);
    }
}
