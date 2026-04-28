package com.zenandops.auth.infrastructure.adapter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Custom application metrics for authentication attempts.
 * Tracks login attempts by outcome (success/failure) using a Micrometer Counter.
 */
@ApplicationScoped
public class AuthMetrics {

    private final Counter successCounter;
    private final Counter failureCounter;

    @Inject
    public AuthMetrics(MeterRegistry registry) {
        this.successCounter = Counter.builder("zenandops.auth.login.attempts")
            .description("Number of authentication attempts")
            .baseUnit("attempts")
            .tag("outcome", "success")
            .register(registry);

        this.failureCounter = Counter.builder("zenandops.auth.login.attempts")
            .description("Number of authentication attempts")
            .baseUnit("attempts")
            .tag("outcome", "failure")
            .register(registry);
    }

    public void recordAttempt(boolean success) {
        if (success) {
            successCounter.increment();
        } else {
            failureCounter.increment();
        }
    }
}
