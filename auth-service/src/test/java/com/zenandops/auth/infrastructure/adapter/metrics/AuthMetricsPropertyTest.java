package com.zenandops.auth.infrastructure.adapter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.jqwik.api.*;

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

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthMetrics authMetrics = new AuthMetrics(registry);

        long expectedSuccesses = attempts.stream().filter(b -> b).count();
        long expectedFailures = attempts.stream().filter(b -> !b).count();

        for (Boolean success : attempts) {
            authMetrics.recordAttempt(success);
        }

        double totalSuccess = findCounter(registry, "success");
        double totalFailure = findCounter(registry, "failure");

        assertEquals(expectedSuccesses, (long) totalSuccess,
                "Success counter should match number of successful attempts");
        assertEquals(expectedFailures, (long) totalFailure,
                "Failure counter should match number of failed attempts");
    }

    private double findCounter(SimpleMeterRegistry registry, String outcome) {
        Counter counter = registry.find("zenandops.auth.login.attempts")
                .tag("outcome", outcome)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }
}
