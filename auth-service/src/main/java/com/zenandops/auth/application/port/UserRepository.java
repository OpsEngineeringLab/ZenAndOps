package com.zenandops.auth.application.port;

import com.zenandops.auth.domain.entity.User;

import java.util.Optional;

/**
 * Outbound port for User persistence operations.
 */
public interface UserRepository {

    Optional<User> findByLogin(String login);

    Optional<User> findById(String id);

    void save(User user);
}
