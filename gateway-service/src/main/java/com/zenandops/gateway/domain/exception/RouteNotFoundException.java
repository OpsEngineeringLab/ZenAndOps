package com.zenandops.gateway.domain.exception;

/**
 * Thrown when no route matches the incoming request path.
 */
public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String path) {
        super("No route found for path: " + path);
    }
}
