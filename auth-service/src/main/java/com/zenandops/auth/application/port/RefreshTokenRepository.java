package com.zenandops.auth.application.port;

import com.zenandops.auth.domain.entity.RefreshToken;

import java.util.Optional;

/**
 * Outbound port for RefreshToken persistence operations.
 */
public interface RefreshTokenRepository {

    void save(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void revokeByUserId(String userId);

    void revokeByToken(String token);
}
