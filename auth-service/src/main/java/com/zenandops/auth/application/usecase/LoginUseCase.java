package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.AuthEventPublisher;
import com.zenandops.auth.application.port.PasswordEncoder;
import com.zenandops.auth.application.port.RefreshTokenRepository;
import com.zenandops.auth.application.port.TokenProvider;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.RefreshToken;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.InvalidCredentialsException;
import com.zenandops.auth.domain.valueobject.AuthEvent;
import com.zenandops.auth.domain.valueobject.EventType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Use case for authenticating a user with login and password credentials.
 * Validates credentials, issues an Access_Token (15 min) and Refresh_Token (8 hours),
 * stores the refresh token, and publishes a LOGIN event.
 */
@ApplicationScoped
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthEventPublisher authEventPublisher;

    @Inject
    public LoginUseCase(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        TokenProvider tokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        AuthEventPublisher authEventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authEventPublisher = authEventPublisher;
    }

    /**
     * Execute the login flow.
     *
     * @param login    the user's login identifier
     * @param password the user's plain-text password
     * @return a {@link TokenPair} containing the access and refresh tokens
     * @throws InvalidCredentialsException if the user is not found, inactive, or the password does not match
     */
    public TokenPair execute(String login, String password) {
        User user = userRepository.findByLogin(login)
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshTokenValue = tokenProvider.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUserId(user.getId());
        refreshToken.setExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS));
        refreshToken.setCreatedAt(Instant.now());

        refreshTokenRepository.save(refreshToken);

        authEventPublisher.publish(new AuthEvent(
                UUID.randomUUID().toString(),
                EventType.LOGIN,
                user.getId(),
                user.getLogin(),
                Instant.now()
        ));

        return new TokenPair(accessToken, refreshTokenValue);
    }
}
