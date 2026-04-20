package com.zenandops.gateway.infrastructure.adapter.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Custom application metrics for rate limiting.
 * Exposes an observable gauge reporting the current rate-limit bucket count per client IP.
 */
@ApplicationScoped
public class RateLimitMetrics {

    private final Meter meter;

    @Inject
    public RateLimitMetrics(Meter meter) {
        this.meter = meter;
    }

    public void registerBucketGauge(Supplier<Map<String, Long>> bucketSupplier) {
        meter.gaugeBuilder("zenandops.gateway.ratelimit.bucket_count")
            .setDescription("Current rate-limit bucket count per client IP")
            .setUnit("{requests}")
            .ofLongs()
            .buildWithCallback(measurement -> {
                bucketSupplier.get().forEach((ip, count) ->
                    measurement.record(count, Attributes.of(
                        AttributeKey.stringKey("client.ip"), ip)));
            });
    }
}
