package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when a reliability rating is outside the valid range of 0-100.
 */
public class InvalidReliabilityRatingException extends RuntimeException {

    public InvalidReliabilityRatingException() {
        super("Reliability rating must be between 0 and 100");
    }

    public InvalidReliabilityRatingException(String message) {
        super(message);
    }
}
