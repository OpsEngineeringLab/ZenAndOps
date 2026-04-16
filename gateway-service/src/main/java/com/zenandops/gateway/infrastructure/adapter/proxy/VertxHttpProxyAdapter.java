package com.zenandops.gateway.infrastructure.adapter.proxy;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Vert.x HTTP client adapter for proxying requests to backend services.
 * Preserves original path, headers, query parameters, and body.
 */
@ApplicationScoped
public class VertxHttpProxyAdapter {

    private static final int PROXY_TIMEOUT_SECONDS = 30;

    @Inject
    io.vertx.core.Vertx vertx;

    private WebClient webClient;

    @PostConstruct
    void init() {
        var options = new WebClientOptions()
                .setConnectTimeout(5_000)
                .setIdleTimeout(PROXY_TIMEOUT_SECONDS);
        this.webClient = WebClient.create(vertx, options);
    }

    /**
     * Proxies an HTTP request to the target backend service.
     *
     * @param targetBaseUrl the base URL of the backend service (e.g., "http://localhost:8081")
     * @param path          the request path to forward (e.g., "/api/v1/auth/login")
     * @param method        the HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param headers       the original request headers to forward
     * @param queryParams   the original query parameters to forward
     * @param body          the request body (may be null for GET/DELETE)
     * @return the proxy response containing status code, headers, and body
     */
    public ProxyResponse proxy(String targetBaseUrl, String path, String method,
                               Map<String, String> headers, Map<String, String> queryParams,
                               byte[] body) {
        try {
            URI uri = URI.create(targetBaseUrl);
            int port = uri.getPort() != -1 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);

            var request = webClient.request(
                    HttpMethod.valueOf(method.toUpperCase()),
                    port,
                    uri.getHost(),
                    path
            );

            // Forward headers
            if (headers != null) {
                headers.forEach((key, value) -> {
                    // Skip hop-by-hop headers
                    if (!isHopByHopHeader(key)) {
                        request.putHeader(key, value);
                    }
                });
            }

            // Forward query parameters
            if (queryParams != null) {
                queryParams.forEach(request::addQueryParam);
            }

            // Send request and block for the response
            CompletableFuture<HttpResponse<Buffer>> future = new CompletableFuture<>();

            if (body != null && body.length > 0) {
                request.sendBuffer(Buffer.buffer(body))
                        .onSuccess(future::complete)
                        .onFailure(future::completeExceptionally);
            } else {
                request.send()
                        .onSuccess(future::complete)
                        .onFailure(future::completeExceptionally);
            }

            HttpResponse<Buffer> response = future.get(PROXY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Build response headers map
            Map<String, String> responseHeaders = new java.util.HashMap<>();
            response.headers().forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));

            byte[] responseBody = response.body() != null ? response.body().getBytes() : new byte[0];

            return new ProxyResponse(response.statusCode(), responseHeaders, responseBody);

        } catch (ExecutionException e) {
            throw new BackendServiceUnavailableException(
                    "Backend service unavailable: " + targetBaseUrl, e.getCause());
        } catch (TimeoutException e) {
            throw new BackendServiceUnavailableException(
                    "Backend service timed out: " + targetBaseUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackendServiceUnavailableException(
                    "Request interrupted while proxying to: " + targetBaseUrl, e);
        }
    }

    private boolean isHopByHopHeader(String header) {
        String lower = header.toLowerCase();
        return lower.equals("connection") || lower.equals("keep-alive")
                || lower.equals("transfer-encoding") || lower.equals("te")
                || lower.equals("trailer") || lower.equals("upgrade")
                || lower.equals("proxy-authorization") || lower.equals("proxy-authenticate");
    }

    /**
     * Represents the response received from a proxied backend service.
     */
    public record ProxyResponse(int statusCode, Map<String, String> headers, byte[] body) {
    }

    /**
     * Thrown when a backend service is unreachable or times out.
     */
    public static class BackendServiceUnavailableException extends RuntimeException {
        public BackendServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
