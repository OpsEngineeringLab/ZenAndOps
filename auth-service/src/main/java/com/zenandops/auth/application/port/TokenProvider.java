package com.zenandops.auth.application.port;

import com.zenandops.auth.domain.entity.User;

import java.util.Map;

/**
 * Outbound port for JWT token generation and validation.
 */
public interface TokenProvider {

    String generateAccessToken(User user);

    String generateRefreshToken();

    Map<String, Object> validateAccessToken(String token);
}
