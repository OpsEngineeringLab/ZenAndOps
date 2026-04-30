package com.zenandops.gateway.infrastructure.adapter.keycloak;

/**
 * Exception thrown when a Keycloak Admin REST API call fails.
 * Wraps the HTTP status code and error message from the Keycloak response
 * so that the gateway error mapper can translate it to an appropriate
 * client-facing HTTP response.
 */
public class KeycloakAdminException extends RuntimeException {

    private final int statusCode;

    public KeycloakAdminException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public KeycloakAdminException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
