package com.zenandops.auth.application.port;

import com.zenandops.auth.domain.valueobject.AuthEvent;

/**
 * Outbound port for publishing authentication events to a message broker.
 */
public interface AuthEventPublisher {

    void publish(AuthEvent event);
}
