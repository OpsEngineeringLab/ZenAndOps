package com.zenandops.gateway.application.port;

import com.zenandops.gateway.domain.valueobject.RateLimitResult;

/**
 * Port interface for checking rate limits.
 */
public interface RateLimiter {

    /**
     * Checks whether the given client IP is within the allowed request rate.
     *
     * @param clientIp the source IP address of the client
     * @return the rate limit result indicating whether the request is allowed
     */
    RateLimitResult check(String clientIp);
}
