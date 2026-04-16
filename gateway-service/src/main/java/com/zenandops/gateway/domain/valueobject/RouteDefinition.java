package com.zenandops.gateway.domain.valueobject;

/**
 * Value object representing a route mapping from a path prefix to a backend service URL.
 *
 * @param pathPrefix     the URL path prefix to match (e.g., "/api/v1/auth")
 * @param targetBaseUrl  the base URL of the target backend service
 * @param jwtRequired    whether JWT validation is required for this route
 */
public record RouteDefinition(String pathPrefix, String targetBaseUrl, boolean jwtRequired) {
}
