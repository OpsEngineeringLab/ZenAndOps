package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.AuthEventPublisher;
import com.zenandops.auth.application.port.RefreshTokenRepository;
import com.zenandops.auth.application.port.TokenProvider;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.RefreshToken;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.InvalidCredentialsException;
import com.zenandops.auth.domain.exception.TokenExpiredException;
import com.zenandops.auth.domain.exception.TokenRevokedException;
import com.zenandops.auth.domain.valueobject.AuthEvent;
import com.zenandops.auth.domain.valueobject.EventType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Use case for refreshing authentication tokens.
 * Validates the existing Refresh_Token, rotates tokens (invalidates old, issues new pair),
 * and publishes a TOKEN_REFRESH event.
 */
@ApplicationScoped
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final AuthEventPublisher authEventPublisher;

    @Inject
    public RefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               TokenProvider tokenProvider,
                               AuthEventPublisher authEventPublisher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.authEventPublisher = authEventPublisher;
    }

    /**
     * Execute the token refresh flow.
     *
     * @param refreshTokenValue the current refresh token string
     * @return a new {@link TokenPair} containing fresh access and refresh tokens
     * @throws InvalidCredentialsException if the token is not found or the user does not exist
     * @throws TokenRevokedException       if the refresh token has been revoked
     * @throws TokenExpiredException       if the refresh token has expired
     */
    public TokenPair execute(String refreshTokenValue) {
        RefreshToken existingToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidCredentialsException::new);

        if (existingToken.isRevoked()) {
            throw new TokenRevokedException();
        }

        if (existingToken.isExpired()) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        User user = userRepository.findById(existingToken.getUserId())
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        // Revoke the old refresh token (rotation)
        refreshTokenRepository.revokeByToken(refreshTokenValue);

        // Issue new token pair
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshTokenValue = tokenProvider.generateRefreshToken();

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setId(UUID.randomUUID().toString());
        newRefreshToken.setToken(newRefreshTokenValue);
        newRefreshToken.setUserId(user.getId());
        newRefreshToken.setExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS));
        newRefreshToken.setCreatedAt(Instant.now());

        refreshTokenRepository.save(newRefreshToken);

        authEventPublisher.publish(new AuthEvent(
                UUID.randomUUID().toString(),
                EventType.TOKEN_REFRESH,
                user.getId(),
                user.getLogin(),
                Instant.now()
        ));

        return new TokenPair(newAccessToken, newRefreshTokenValue);
    }
}
