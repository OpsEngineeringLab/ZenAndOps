package com.zenandops.auth.infrastructure.adapter.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Custom application metrics for authentication attempts.
 * Tracks login attempts by outcome (success/failure) using an OpenTelemetry LongCounter.
 */
@ApplicationScoped
public class AuthMetrics {

    private final LongCounter authAttempts;

    @Inject
    public AuthMetrics(Meter meter) {
        this.authAttempts = meter.counterBuilder("zenandops.auth.login.attempts")
            .setDescription("Number of authentication attempts")
            .setUnit("{attempts}")
            .build();
    }

    public void recordAttempt(boolean success) {
        authAttempts.add(1, Attributes.of(
            AttributeKey.stringKey("outcome"), success ? "success" : "failure"));
    }
}
