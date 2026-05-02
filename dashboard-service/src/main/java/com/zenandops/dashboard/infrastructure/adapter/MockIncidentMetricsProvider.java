package com.zenandops.dashboard.infrastructure.adapter;

import com.zenandops.dashboard.application.port.IncidentMetricsProvider;
import com.zenandops.dashboard.domain.valueobject.IncidentMetrics;
import com.zenandops.dashboard.domain.valueobject.Trend;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

/**
 * Mock implementation of {@link IncidentMetricsProvider} returning realistic
 * MTTR and MTTD values with trend indicators.
 */
@ApplicationScoped
public class MockIncidentMetricsProvider implements IncidentMetricsProvider {

    private static final Logger LOG = Logger.getLogger(MockIncidentMetricsProvider.class);

    @Override
    @Fallback(fallbackMethod = "fallbackGetIncidentMetrics")
    public IncidentMetrics getIncidentMetrics() {
        return new IncidentMetrics(47.3, Trend.DOWN, 8.2, Trend.STABLE);
    }

    IncidentMetrics fallbackGetIncidentMetrics() {
        LOG.warn("Fallback invoked for MockIncidentMetricsProvider.getIncidentMetrics");
        return new IncidentMetrics(0.0, Trend.STABLE, 0.0, Trend.STABLE);
    }
}
