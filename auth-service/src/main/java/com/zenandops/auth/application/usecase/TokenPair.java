package com.zenandops.auth.application.usecase;

/**
 * DTO representing a pair of access and refresh tokens returned by authentication use cases.
 *
 * @param accessToken  the short-lived JWT access token
 * @param refreshToken the long-lived refresh token
 */
public record TokenPair(String accessToken, String refreshToken) {
}
