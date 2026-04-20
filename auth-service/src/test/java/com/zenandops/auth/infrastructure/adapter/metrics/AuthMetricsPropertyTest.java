package com.zenandops.auth.infrastructure.adapter.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import net.jqwik.api.*;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based tests for AuthMetrics.
 *
 * Validates: Requirements 4.8
 */
@Tag("Feature: observability-opentelemetry-grafana, Property 2: Authentication attempt counter tracks outcomes correctly")
class AuthMetricsPropertyTest {

    /**
     * Property 2: Authentication attempt counter tracks outcomes correctly
     *
     * For any sequence of authentication attempts (each being either success or failure),
     * after recording all attempts, the counter SHALL report a total equal to the number
     * of successes for the "success" outcome and a total equal to the number of failures
     * for the "failure" outcome.
     */
    @Property(tries = 100)
    void counterTotalsMatchExpectedSuccessAndFailureCounts(
            @ForAll List<Boolean> attempts) {

        InMemoryMetricReader reader = InMemoryMetricReader.create();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(reader)
                .build();
        Meter meter = meterProvider.get("test");

        AuthMetrics authMetrics = new AuthMetrics(meter);

        long expectedSuccesses = attempts.stream().filter(b -> b).count();
        long expectedFailures = attempts.stream().filter(b -> !b).count();

        for (Boolean success : attempts) {
            authMetrics.recordAttempt(success);
        }

        Collection<MetricData> metrics = reader.collectAllMetrics();

        long totalSuccess = extractCounterValue(metrics, "success");
        long totalFailure = extractCounterValue(metrics, "failure");

        assertEquals(expectedSuccesses, totalSuccess,
                "Success counter should match number of successful attempts");
        assertEquals(expectedFailures, totalFailure,
                "Failure counter should match number of failed attempts");
    }

    private long extractCounterValue(Collection<MetricData> metrics, String outcome) {
        return metrics.stream()
                .filter(m -> m.getName().equals("zenandops.auth.login.attempts"))
                .flatMap(m -> m.getLongSumData().getPoints().stream())
                .filter(point -> outcome.equals(
                        point.getAttributes().get(AttributeKey.stringKey("outcome"))))
                .mapToLong(LongPointData::getValue)
                .findFirst()
                .orElse(0L);
    }
}
