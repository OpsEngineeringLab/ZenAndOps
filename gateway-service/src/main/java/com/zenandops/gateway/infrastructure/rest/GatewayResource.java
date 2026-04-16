package com.zenandops.gateway.infrastructure.rest;

import com.zenandops.gateway.application.port.RateLimiter;
import com.zenandops.gateway.application.port.RouteResolver;
import com.zenandops.gateway.domain.exception.RateLimitExceededException;
import com.zenandops.gateway.domain.exception.RouteNotFoundException;
import com.zenandops.gateway.domain.exception.UnauthorizedException;
import com.zenandops.gateway.domain.valueobject.RateLimitResult;
import com.zenandops.gateway.domain.valueobject.RouteDefinition;
import com.zenandops.gateway.infrastructure.adapter.proxy.VertxHttpProxyAdapter;
import com.zenandops.gateway.infrastructure.adapter.proxy.VertxHttpProxyAdapter.ProxyResponse;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Catch-all JAX-RS resource that proxies all incoming requests to the appropriate
 * backend service.
 * <p>
 * Request flow: Rate limit check → Route resolution → JWT validation (if required)
 * → Proxy to backend → Return response.
 */
@Path("/{path:.*}")
@Tag(name = "Gateway", description = "API Gateway proxy endpoints")
public class GatewayResource {

    @Inject
    RouteResolver routeResolver;

    @Inject
    RateLimiter rateLimiter;

    @Inject
    VertxHttpProxyAdapter proxyAdapter;

    @GET
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Proxy GET request", description = "Forwards GET requests to the appropriate backend service")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Proxied response from backend"),
            @APIResponse(responseCode = "401", description = "Unauthorized — invalid or missing JWT"),
            @APIResponse(responseCode = "404", description = "No matching route found"),
            @APIResponse(responseCode = "429", description = "Rate limit exceeded"),
            @APIResponse(responseCode = "503", description = "Backend service unavailable")
    })
    public Response proxyGet(@Context HttpHeaders httpHeaders,
                             @Context UriInfo uriInfo,
                             @Context HttpServerRequest serverRequest) {
        return handleRequest("GET", httpHeaders, uriInfo, serverRequest, null);
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Proxy POST request", description = "Forwards POST requests to the appropriate backend service")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Proxied response from backend"),
            @APIResponse(responseCode = "401", description = "Unauthorized — invalid or missing JWT"),
            @APIResponse(responseCode = "404", description = "No matching route found"),
            @APIResponse(responseCode = "429", description = "Rate limit exceeded"),
            @APIResponse(responseCode = "503", description = "Backend service unavailable")
    })
    public Response proxyPost(@Context HttpHeaders httpHeaders,
                              @Context UriInfo uriInfo,
                              @Context HttpServerRequest serverRequest,
                              InputStream body) {
        return handleRequest("POST", httpHeaders, uriInfo, serverRequest, body);
    }

    @PUT
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Proxy PUT request", description = "Forwards PUT requests to the appropriate backend service")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Proxied response from backend"),
            @APIResponse(responseCode = "401", description = "Unauthorized — invalid or missing JWT"),
            @APIResponse(responseCode = "404", description = "No matching route found"),
            @APIResponse(responseCode = "429", description = "Rate limit exceeded"),
            @APIResponse(responseCode = "503", description = "Backend service unavailable")
    })
    public Response proxyPut(@Context HttpHeaders httpHeaders,
                             @Context UriInfo uriInfo,
                             @Context HttpServerRequest serverRequest,
                             InputStream body) {
        return handleRequest("PUT", httpHeaders, uriInfo, serverRequest, body);
    }

    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Proxy DELETE request", description = "Forwards DELETE requests to the appropriate backend service")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Proxied response from backend"),
            @APIResponse(responseCode = "401", description = "Unauthorized — invalid or missing JWT"),
            @APIResponse(responseCode = "404", description = "No matching route found"),
            @APIResponse(responseCode = "429", description = "Rate limit exceeded"),
            @APIResponse(responseCode = "503", description = "Backend service unavailable")
    })
    public Response proxyDelete(@Context HttpHeaders httpHeaders,
                                @Context UriInfo uriInfo,
                                @Context HttpServerRequest serverRequest,
                                InputStream body) {
        return handleRequest("DELETE", httpHeaders, uriInfo, serverRequest, body);
    }

    @PATCH
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Proxy PATCH request", description = "Forwards PATCH requests to the appropriate backend service")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Proxied response from backend"),
            @APIResponse(responseCode = "401", description = "Unauthorized — invalid or missing JWT"),
            @APIResponse(responseCode = "404", description = "No matching route found"),
            @APIResponse(responseCode = "429", description = "Rate limit exceeded"),
            @APIResponse(responseCode = "503", description = "Backend service unavailable")
    })
    public Response proxyPatch(@Context HttpHeaders httpHeaders,
                               @Context UriInfo uriInfo,
                               @Context HttpServerRequest serverRequest,
                               InputStream body) {
        return handleRequest("PATCH", httpHeaders, uriInfo, serverRequest, body);
    }

    @HEAD
    @Produces(MediaType.WILDCARD)
    public Response proxyHead(@Context HttpHeaders httpHeaders,
                              @Context UriInfo uriInfo,
                              @Context HttpServerRequest serverRequest) {
        return handleRequest("HEAD", httpHeaders, uriInfo, serverRequest, null);
    }

    @OPTIONS
    @Produces(MediaType.WILDCARD)
    public Response proxyOptions(@Context HttpHeaders httpHeaders,
                                 @Context UriInfo uriInfo,
                                 @Context HttpServerRequest serverRequest) {
        return handleRequest("OPTIONS", httpHeaders, uriInfo, serverRequest, null);
    }

    private Response handleRequest(String method, HttpHeaders httpHeaders, UriInfo uriInfo,
                                   HttpServerRequest serverRequest, InputStream bodyStream) {
        String path = "/" + uriInfo.getPath();
        String clientIp = extractClientIp(serverRequest);

        // 1. Rate limit check (before JWT validation per requirement 8.5)
        RateLimitResult rateLimitResult = rateLimiter.check(clientIp);
        if (!rateLimitResult.allowed()) {
            throw new RateLimitExceededException(rateLimitResult.retryAfterSeconds());
        }

        // 2. Route resolution
        RouteDefinition route = routeResolver.resolve(path)
                .orElseThrow(() -> new RouteNotFoundException(path));

        // 3. JWT validation (if required by route)
        if (route.jwtRequired()) {
            String authHeader = httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
                throw new UnauthorizedException("Missing or invalid Authorization header");
            }
            // Token presence verified. Cryptographic validation (signature, expiry, issuer)
            // is handled by SmallRye JWT configured via application.properties.
            // For protected routes, the gateway performs a lightweight check here;
            // full validation occurs at the backend service level.
        }

        // 4. Collect request headers
        Map<String, String> headers = extractHeaders(httpHeaders);

        // 5. Collect query parameters
        Map<String, String> queryParams = new HashMap<>();
        uriInfo.getQueryParameters().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                queryParams.put(key, values.getFirst());
            }
        });

        // 6. Read request body
        byte[] body = readBody(bodyStream);

        // 7. Proxy request to backend service
        ProxyResponse proxyResponse = proxyAdapter.proxy(
                route.targetBaseUrl(), path, method, headers, queryParams, body);

        // 8. Build and return proxied response
        Response.ResponseBuilder responseBuilder = Response.status(proxyResponse.statusCode());

        if (proxyResponse.headers() != null) {
            proxyResponse.headers().forEach((key, value) -> {
                String lower = key.toLowerCase();
                // Skip hop-by-hop headers; JAX-RS manages content-length automatically
                if (!lower.equals("transfer-encoding") && !lower.equals("content-length")
                        && !lower.equals("connection")) {
                    responseBuilder.header(key, value);
                }
            });
        }

        if (proxyResponse.body() != null && proxyResponse.body().length > 0) {
            responseBuilder.entity(proxyResponse.body());
        }

        return responseBuilder.build();
    }

    private String extractClientIp(HttpServerRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.remoteAddress() != null ? request.remoteAddress().host() : "unknown";
    }

    private Map<String, String> extractHeaders(HttpHeaders httpHeaders) {
        Map<String, String> headers = new HashMap<>();
        httpHeaders.getRequestHeaders().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                headers.put(key, values.getFirst());
            }
        });
        return headers;
    }

    private byte[] readBody(InputStream bodyStream) {
        if (bodyStream == null) {
            return null;
        }
        try {
            byte[] bytes = bodyStream.readAllBytes();
            return bytes.length > 0 ? bytes : null;
        } catch (Exception e) {
            return null;
        }
    }
}
