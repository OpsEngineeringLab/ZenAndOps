package com.zenandops.auth.application.port;

/**
 * Outbound port for password hashing and verification.
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
