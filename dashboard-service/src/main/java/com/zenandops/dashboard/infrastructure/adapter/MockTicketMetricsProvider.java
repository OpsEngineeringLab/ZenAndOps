package com.zenandops.dashboard.infrastructure.adapter;

import com.zenandops.dashboard.application.port.TicketMetricsProvider;
import com.zenandops.dashboard.domain.valueobject.TicketsByState;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

/**
 * Mock implementation of {@link TicketMetricsProvider} returning realistic
 * ITIL ticket counts by lifecycle state.
 */
@ApplicationScoped
public class MockTicketMetricsProvider implements TicketMetricsProvider {

    private static final Logger LOG = Logger.getLogger(MockTicketMetricsProvider.class);

    @Override
    @Fallback(fallbackMethod = "fallbackGetTicketsByState")
    public TicketsByState getTicketsByState() {
        return new TicketsByState(28, 35, 22, 18, 31, 8);
    }

    TicketsByState fallbackGetTicketsByState() {
        LOG.warn("Fallback invoked for MockTicketMetricsProvider.getTicketsByState");
        return new TicketsByState(0, 0, 0, 0, 0, 0);
    }
}
