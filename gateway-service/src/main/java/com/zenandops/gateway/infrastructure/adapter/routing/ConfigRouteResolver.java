package com.zenandops.gateway.infrastructure.adapter.routing;

import com.zenandops.gateway.application.port.RouteResolver;
import com.zenandops.gateway.domain.valueobject.RouteDefinition;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Config-driven route resolver that maps path prefixes to backend service URLs.
 * Routes are ordered from most specific (longest prefix) to least specific
 * so that the first match wins.
 * <p>
 * Auth-related routes (login, refresh, logoff) and admin proxy routes (users, roles,
 * tags, profile) are no longer routed here — authentication is handled by Keycloak
 * OIDC flows, and admin operations are handled by dedicated JAX-RS resources
 * (UserAdminResource, RoleAdminResource, TagAdminResource, ProfileResource).
 */
@ApplicationScoped
public class ConfigRouteResolver implements RouteResolver {

    @ConfigProperty(name = "gateway.dashboard-service.url")
    String dashboardServiceUrl;

    @ConfigProperty(name = "gateway.cmdb-service.url")
    String cmdbServiceUrl;

    private List<RouteDefinition> routes;

    @PostConstruct
    void init() {
        var definitions = new ArrayList<RouteDefinition>();

        // Protected routes (JWT required)
        definitions.add(new RouteDefinition("/api/v1/dashboard", dashboardServiceUrl, true));
        definitions.add(new RouteDefinition("/api/v1/cmdb", cmdbServiceUrl, true));

        // Sort by path prefix length descending — most specific first
        definitions.sort(Comparator.comparingInt((RouteDefinition r) -> r.pathPrefix().length()).reversed());

        this.routes = List.copyOf(definitions);
    }

    @Override
    public Optional<RouteDefinition> resolve(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        return routes.stream()
                .filter(route -> path.startsWith(route.pathPrefix()))
                .findFirst();
    }
}
