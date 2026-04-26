package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a Data Source deletion is attempted while it is referenced by asset or CI versions.
 */
public class DataSourceInUseException extends RuntimeException {

    public DataSourceInUseException() {
        super("Data source is in use");
    }

    public DataSourceInUseException(String message) {
        super(message);
    }
}
