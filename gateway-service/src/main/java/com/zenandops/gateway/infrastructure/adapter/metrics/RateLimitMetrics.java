package com.zenandops.gateway.infrastructure.adapter.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Custom application metrics for rate limiting.
 * Exposes gauges reporting the current rate-limit bucket count per client IP.
 */
@ApplicationScoped
public class RateLimitMetrics {

    private final MeterRegistry registry;
    private final Map<String, Long> currentBuckets = new ConcurrentHashMap<>();

    @Inject
    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void registerBucketGauge(Supplier<Map<String, Long>> bucketSupplier) {
        // Periodically refresh the internal map and register gauges for new IPs
        registry.gauge("zenandops.gateway.ratelimit.bucket_count",
            Tags.of("type", "total"),
            this,
            self -> {
                Map<String, Long> snapshot = bucketSupplier.get();
                self.currentBuckets.clear();
                self.currentBuckets.putAll(snapshot);
                // Register individual IP gauges on first appearance
                for (String ip : snapshot.keySet()) {
                    Gauge.builder("zenandops.gateway.ratelimit.bucket_count",
                            self.currentBuckets, m -> m.getOrDefault(ip, 0L))
                        .tags("client.ip", ip)
                        .description("Current rate-limit bucket count per client IP")
                        .baseUnit("requests")
                        .register(self.registry);
                }
                return snapshot.values().stream().mapToLong(Long::longValue).sum();
            });
    }
}
