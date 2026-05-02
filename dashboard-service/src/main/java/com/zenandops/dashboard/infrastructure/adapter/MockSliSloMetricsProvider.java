package com.zenandops.dashboard.infrastructure.adapter;

import com.zenandops.dashboard.application.port.SliSloMetricsProvider;
import com.zenandops.dashboard.domain.valueobject.SliSloCompliance;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

/**
 * Mock implementation of {@link SliSloMetricsProvider} returning realistic
 * SRE metrics for availability and latency SLI/SLO compliance.
 */
@ApplicationScoped
public class MockSliSloMetricsProvider implements SliSloMetricsProvider {

    private static final Logger LOG = Logger.getLogger(MockSliSloMetricsProvider.class);

    @Override
    @Fallback(fallbackMethod = "fallbackGetSliSloCompliance")
    public SliSloCompliance getSliSloCompliance() {
        return new SliSloCompliance(99.92, 99.9, 96.5, 95.0);
    }

    SliSloCompliance fallbackGetSliSloCompliance() {
        LOG.warn("Fallback invoked for MockSliSloMetricsProvider.getSliSloCompliance");
        return new SliSloCompliance(0.0, 0.0, 0.0, 0.0);
    }
}
