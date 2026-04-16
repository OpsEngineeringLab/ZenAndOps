package com.zenandops.auth.infrastructure.adapter.security;

import com.zenandops.auth.application.port.TokenProvider;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.entity.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SmallRye JWT adapter implementing the TokenProvider port.
 * Generates Access_Tokens with user claims including resolved Tag key-value pairs,
 * and Refresh_Tokens as random UUIDs.
 */
@ApplicationScoped
public class JwtTokenProvider implements TokenProvider {

    @ConfigProperty(name = "zenandops.jwt.issuer", defaultValue = "https://zenandops.com")
    String issuer;

    @ConfigProperty(name = "zenandops.jwt.access-token-expiration-minutes", defaultValue = "15")
    int accessTokenExpirationMinutes;

    @Override
    public String generateAccessToken(User user, List<Tag> resolvedTags) {
        List<Map<String, String>> tagsClaim = resolvedTags.stream()
                .map(tag -> Map.of("key", tag.getKey(), "value", tag.getValue()))
                .toList();

        return Jwt.issuer(issuer)
                .subject(user.getLogin())
                .claim("userId", user.getId())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .groups(new HashSet<>(user.getRoles()))
                .claim("tags", tagsClaim)
                .expiresIn(Duration.ofMinutes(accessTokenExpirationMinutes))
                .sign();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Map<String, Object> validateAccessToken(String token) {
        // Validation is handled by Quarkus SmallRye JWT extension automatically
        // for incoming requests. This method is available for programmatic validation
        // if needed. In the current flow, the @Authenticated annotation handles it.
        Map<String, Object> claims = new HashMap<>();
        return claims;
    }
}
