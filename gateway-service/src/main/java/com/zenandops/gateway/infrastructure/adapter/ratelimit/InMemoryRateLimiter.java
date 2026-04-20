package com.zenandops.gateway.infrastructure.adapter.ratelimit;

import com.zenandops.gateway.application.port.RateLimiter;
import com.zenandops.gateway.domain.valueobject.RateLimitResult;
import com.zenandops.gateway.infrastructure.adapter.metrics.RateLimitMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory sliding window rate limiter per client IP.
 * Tracks request timestamps in a {@link ConcurrentHashMap} and enforces
 * a configurable maximum number of requests within a rolling time window.
 */
@ApplicationScoped
public class InMemoryRateLimiter implements RateLimiter {

    @ConfigProperty(name = "gateway.rate-limit.max-requests", defaultValue = "100")
    int maxRequests;

    @ConfigProperty(name = "gateway.rate-limit.window-seconds", defaultValue = "60")
    int windowSeconds;

    private final ConcurrentHashMap<String, List<Long>> requestLog = new ConcurrentHashMap<>();

    @Inject
    RateLimitMetrics rateLimitMetrics;

    @PostConstruct
    void init() {
        rateLimitMetrics.registerBucketGauge(this::getBucketCounts);
    }

    @Override
    public RateLimitResult check(String clientIp) {
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1_000L);

        List<Long> timestamps = requestLog.computeIfAbsent(clientIp, _ -> new CopyOnWriteArrayList<>());

        // Remove expired timestamps outside the sliding window
        timestamps.removeIf(ts -> ts < windowStart);

        if (timestamps.size() < maxRequests) {
            timestamps.add(now);
            return new RateLimitResult(true, 0);
        }

        // Denied — calculate retry-after from the oldest timestamp in the window
        long oldestInWindow = timestamps.getFirst();
        long retryAfterMs = (oldestInWindow + (windowSeconds * 1_000L)) - now;
        long retryAfterSeconds = Math.max(1, (retryAfterMs + 999) / 1_000);

        return new RateLimitResult(false, retryAfterSeconds);
    }

    /**
     * Returns the current bucket count (number of requests in the active window) per client IP.
     */
    private Map<String, Long> getBucketCounts() {
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1_000L);

        return requestLog.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .filter(ts -> ts >= windowStart)
                    .count()
            ));
    }
}
