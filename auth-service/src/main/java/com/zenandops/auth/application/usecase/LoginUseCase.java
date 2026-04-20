package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.AuthEventPublisher;
import com.zenandops.auth.application.port.PasswordEncoder;
import com.zenandops.auth.application.port.RefreshTokenRepository;
import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.application.port.TokenProvider;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.RefreshToken;
import com.zenandops.auth.domain.entity.Role;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.InvalidCredentialsException;
import com.zenandops.auth.domain.valueobject.AuthEvent;
import com.zenandops.auth.domain.valueobject.EventType;
import com.zenandops.auth.infrastructure.adapter.metrics.AuthMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for authenticating a user with login and password credentials.
 * Validates credentials, issues an Access_Token and Refresh_Token,
 * stores the refresh token, and publishes a LOGIN event.
 */
@ApplicationScoped
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthEventPublisher authEventPublisher;
    private final TagRepository tagRepository;
    private final RoleRepository roleRepository;
    private final AuthMetrics authMetrics;
    private final int refreshTokenExpirationHours;

    @Inject
    public LoginUseCase(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        TokenProvider tokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        AuthEventPublisher authEventPublisher,
                        TagRepository tagRepository,
                        RoleRepository roleRepository,
                        AuthMetrics authMetrics,
                        @ConfigProperty(name = "zenandops.jwt.refresh-token-expiration-hours", defaultValue = "8")
                        int refreshTokenExpirationHours) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authEventPublisher = authEventPublisher;
        this.tagRepository = tagRepository;
        this.roleRepository = roleRepository;
        this.authMetrics = authMetrics;
        this.refreshTokenExpirationHours = refreshTokenExpirationHours;
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
                .orElseThrow(() -> {
                    authMetrics.recordAttempt(false);
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            authMetrics.recordAttempt(false);
            throw new InvalidCredentialsException();
        }

        authMetrics.recordAttempt(true);

        String accessToken = tokenProvider.generateAccessToken(user, resolveTags(user), resolvePermissions(user));
        String refreshTokenValue = tokenProvider.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUserId(user.getId());
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenExpirationHours, ChronoUnit.HOURS));
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

    private List<Tag> resolveTags(User user) {
        if (user.getTagIds() == null || user.getTagIds().isEmpty()) {
            return List.of();
        }
        return tagRepository.findAllByIds(user.getTagIds());
    }

    private List<String> resolvePermissions(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        List<Role> roles = roleRepository.findAllByNames(user.getRoles());
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .distinct()
                .collect(Collectors.toList());
    }
}
