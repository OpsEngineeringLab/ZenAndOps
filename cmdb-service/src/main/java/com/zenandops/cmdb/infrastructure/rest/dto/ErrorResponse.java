package com.zenandops.cmdb.infrastructure.rest.dto;

import java.time.Instant;

/**
 * Standard error response DTO following the design spec format.
 *
 * @param code      the error code
 * @param message   the error message
 * @param timestamp when the error occurred
 */
public record ErrorResponse(String code, String message, Instant timestamp) {
}
