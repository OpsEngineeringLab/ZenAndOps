package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.AuthEventPublisher;
import com.zenandops.auth.application.port.RefreshTokenRepository;
import com.zenandops.auth.domain.entity.RefreshToken;
import com.zenandops.auth.domain.exception.InvalidCredentialsException;
import com.zenandops.auth.domain.valueobject.AuthEvent;
import com.zenandops.auth.domain.valueobject.EventType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for terminating a user session.
 * Revokes the Refresh_Token and publishes a LOGOFF event.
 */
@ApplicationScoped
public class LogoffUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthEventPublisher authEventPublisher;

    @Inject
    public LogoffUseCase(RefreshTokenRepository refreshTokenRepository,
                         AuthEventPublisher authEventPublisher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authEventPublisher = authEventPublisher;
    }

    /**
     * Execute the logoff flow.
     *
     * @param refreshTokenValue the refresh token string to revoke
     * @throws InvalidCredentialsException if the token is not found
     */
    public void execute(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidCredentialsException::new);

        refreshTokenRepository.revokeByToken(refreshTokenValue);

        authEventPublisher.publish(new AuthEvent(
                UUID.randomUUID().toString(),
                EventType.LOGOFF,
                token.getUserId(),
                null,
                Instant.now()
        ));
    }
}
