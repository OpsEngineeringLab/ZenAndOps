package com.zenandops.gateway.application.port;

import com.zenandops.gateway.domain.valueobject.RouteDefinition;

import java.util.Optional;

/**
 * Port interface for resolving a request path to a route definition.
 */
public interface RouteResolver {

    /**
     * Resolves the given request path to a route definition.
     *
     * @param path the incoming request path (e.g., "/api/v1/auth/login")
     * @return the matching route definition, or empty if no route matches
     */
    Optional<RouteDefinition> resolve(String path);
}
