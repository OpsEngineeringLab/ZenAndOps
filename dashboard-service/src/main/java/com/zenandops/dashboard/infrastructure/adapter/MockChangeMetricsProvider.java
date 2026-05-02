package com.zenandops.dashboard.infrastructure.adapter;

import com.zenandops.dashboard.application.port.ChangeMetricsProvider;
import com.zenandops.dashboard.domain.valueobject.ChangeManagement;
import com.zenandops.dashboard.domain.valueobject.ErrorBudget;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

/**
 * Mock implementation of {@link ChangeMetricsProvider} returning realistic
 * change failure rate and error budget consumption data.
 */
@ApplicationScoped
public class MockChangeMetricsProvider implements ChangeMetricsProvider {

    private static final Logger LOG = Logger.getLogger(MockChangeMetricsProvider.class);

    @Override
    @Fallback(fallbackMethod = "fallbackGetChangeManagement")
    public ChangeManagement getChangeManagement() {
        return new ChangeManagement(4.8, 62, 3);
    }

    @Override
    @Fallback(fallbackMethod = "fallbackGetErrorBudget")
    public ErrorBudget getErrorBudget() {
        return new ErrorBudget(68.5, 1.2, 30);
    }

    ChangeManagement fallbackGetChangeManagement() {
        LOG.warn("Fallback invoked for MockChangeMetricsProvider.getChangeManagement");
        return new ChangeManagement(0.0, 0, 0);
    }

    ErrorBudget fallbackGetErrorBudget() {
        LOG.warn("Fallback invoked for MockChangeMetricsProvider.getErrorBudget");
        return new ErrorBudget(0.0, 0.0, 0);
    }
}
