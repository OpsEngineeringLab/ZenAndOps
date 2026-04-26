package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when an uploaded file format is not supported or is malformed.
 */
public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException() {
        super("Invalid or unsupported file format");
    }

    public InvalidFileFormatException(String message) {
        super(message);
    }
}
