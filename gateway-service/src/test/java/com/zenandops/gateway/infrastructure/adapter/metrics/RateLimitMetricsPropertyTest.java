package com.zenandops.gateway.infrastructure.adapter.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.jqwik.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based tests for RateLimitMetrics.
 *
 * Validates: Requirements 4.7
 */
@Tag("Feature: observability-opentelemetry-grafana, Property 1: Rate-limit gauge reports correct bucket counts")
class RateLimitMetricsPropertyTest {

    /**
     * Property 1: Rate-limit gauge reports correct bucket counts
     *
     * For any map of client IP addresses to request counts, when the rate-limit gauge
     * callback is invoked, it SHALL report the exact count value for each IP address
     * present in the map.
     */
    @Property(tries = 100)
    void gaugeReportsCorrectCountForEachIp(
            @ForAll("ipToCountMaps") Map<String, Long> ipCounts) {

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitMetrics rateLimitMetrics = new RateLimitMetrics(registry);

        // Supply a snapshot of the map via the gauge callback
        Map<String, Long> snapshot = new HashMap<>(ipCounts);
        rateLimitMetrics.registerBucketGauge(() -> snapshot);

        // Trigger gauge evaluation by reading the total gauge
        Gauge totalGauge = registry.find("zenandops.gateway.ratelimit.bucket_count")
                .tag("type", "total")
                .gauge();

        if (totalGauge != null) {
            // Force evaluation
            totalGauge.value();
        }

        for (Map.Entry<String, Long> entry : ipCounts.entrySet()) {
            String ip = entry.getKey();
            long expectedCount = entry.getValue();

            Gauge ipGauge = registry.find("zenandops.gateway.ratelimit.bucket_count")
                    .tag("client.ip", ip)
                    .gauge();

            long reportedCount = ipGauge != null ? (long) ipGauge.value() : Long.MIN_VALUE;

            assertEquals(expectedCount, reportedCount,
                    "Gauge value for IP " + ip + " should match expected count");
        }
    }

    @Provide
    Arbitrary<Map<String, Long>> ipToCountMaps() {
        Arbitrary<String> ips = Arbitraries.integers().between(1, 255)
                .tuple4()
                .map(t -> t.get1() + "." + t.get2() + "." + t.get3() + "." + t.get4());
        Arbitrary<Long> counts = Arbitraries.longs().between(0, 10000);

        return Arbitraries.maps(ips, counts)
                .ofMinSize(0)
                .ofMaxSize(20);
    }
}
