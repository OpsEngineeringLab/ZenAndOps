package com.zenandops.gateway.domain.valueobject;

/**
 * Value object representing the result of a rate limit check.
 *
 * @param allowed           whether the request is allowed
 * @param retryAfterSeconds seconds until the client may retry (0 if allowed)
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
}
