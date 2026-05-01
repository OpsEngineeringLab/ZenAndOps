package com.zenandops.admin.infrastructure.adapter.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * REST client for the Keycloak Admin REST API.
 * <p>
 * Uses {@link OidcClient} (client credentials flow) for automatic service account
 * token management and Java's {@link HttpClient} for HTTP calls. All methods throw
 * {@link KeycloakAdminException} on non-success responses so that the admin-api error
 * mapper can translate them to appropriate client-facing HTTP status codes.
 */
@ApplicationScoped
public class KeycloakAdminClient {

    private static final Logger LOG = Logger.getLogger(KeycloakAdminClient.class);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};

    @Inject
    OidcClient oidcClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "admin.keycloak.admin-url")
    String adminUrl;

    private HttpClient httpClient;

    @PostConstruct
    void init() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // ── User operations ─────────────────────────────────────────────────

    /**
     * List users with optional pagination.
     *
     * @param first index of the first result (nullable)
     * @param max   maximum number of results (nullable)
     * @return list of Keycloak user representations
     */
    public List<Map<String, Object>> listUsers(Integer first, Integer max) {
        var uriBuilder = new StringBuilder(adminUrl).append("/users");
        var separator = '?';
        if (first != null) {
            uriBuilder.append(separator).append("first=").append(first);
            separator = '&';
        }
        if (max != null) {
            uriBuilder.append(separator).append("max=").append(max);
        }
        return executeList(buildGet(uriBuilder.toString()));
    }

    /**
     * Get a single user by ID.
     */
    public Map<String, Object> getUser(String userId) {
        return executeMap(buildGet(adminUrl + "/users/" + userId));
    }

    /**
     * Create a new user. Returns the user ID extracted from the Location header.
     */
    public String createUser(Map<String, Object> userRepresentation) {
        HttpRequest request = buildPost(adminUrl + "/users", userRepresentation);
        HttpResponse<String> response = send(request);
        handleStatus(response, 201);

        String location = response.headers().firstValue("Location").orElse(null);
        if (location == null || location.isBlank()) {
            throw new KeycloakAdminException(500, "Keycloak did not return a Location header for the created user");
        }
        // Location format: .../users/{userId}
        return location.substring(location.lastIndexOf('/') + 1);
    }

    /**
     * Update an existing user.
     */
    public void updateUser(String userId, Map<String, Object> userRepresentation) {
        handleStatus(send(buildPut(adminUrl + "/users/" + userId, userRepresentation)), 204);
    }

    /**
     * Delete a user.
     */
    public void deleteUser(String userId) {
        handleStatus(send(buildDelete(adminUrl + "/users/" + userId)), 204);
    }

    // ── Role mapping operations ─────────────────────────────────────────

    /**
     * Get realm-level role mappings for a user.
     */
    public List<Map<String, Object>> getUserRealmRoles(String userId) {
        return executeList(buildGet(adminUrl + "/users/" + userId + "/role-mappings/realm"));
    }

    /**
     * Assign realm-level roles to a user.
     */
    public void assignRealmRoles(String userId, List<Map<String, Object>> roles) {
        handleStatus(send(buildPostList(adminUrl + "/users/" + userId + "/role-mappings/realm", roles)), 204);
    }

    /**
     * Remove realm-level roles from a user.
     */
    public void removeRealmRoles(String userId, List<Map<String, Object>> roles) {
        handleStatus(send(buildDeleteWithBody(adminUrl + "/users/" + userId + "/role-mappings/realm", roles)), 204);
    }

    // ── Role operations ─────────────────────────────────────────────────

    /**
     * List all realm roles.
     */
    public List<Map<String, Object>> listRealmRoles() {
        return executeList(buildGet(adminUrl + "/roles"));
    }

    /**
     * Get a realm role by name.
     */
    public Map<String, Object> getRealmRoleByName(String roleName) {
        return executeMap(buildGet(adminUrl + "/roles/" + roleName));
    }

    /**
     * Get a realm role by ID.
     */
    public Map<String, Object> getRealmRoleById(String roleId) {
        return executeMap(buildGet(adminUrl + "/roles-by-id/" + roleId));
    }

    /**
     * Create a new realm role.
     */
    public void createRealmRole(Map<String, Object> roleRepresentation) {
        handleStatus(send(buildPost(adminUrl + "/roles", roleRepresentation)), 201);
    }

    /**
     * Update a realm role by ID.
     */
    public void updateRealmRole(String roleId, Map<String, Object> roleRepresentation) {
        handleStatus(send(buildPut(adminUrl + "/roles-by-id/" + roleId, roleRepresentation)), 204);
    }

    /**
     * Delete a realm role by ID.
     */
    public void deleteRealmRole(String roleId) {
        handleStatus(send(buildDelete(adminUrl + "/roles-by-id/" + roleId)), 204);
    }

    // ── Realm attribute operations (tag definitions) ────────────────────

    /**
     * Get the full realm representation (used to read realm attributes such as tag definitions).
     */
    public Map<String, Object> getRealmRepresentation() {
        return executeMap(buildGet(adminUrl));
    }

    /**
     * Update the realm representation (used to write realm attributes such as tag definitions).
     */
    public void updateRealmRepresentation(Map<String, Object> realmRepresentation) {
        handleStatus(send(buildPut(adminUrl, realmRepresentation)), 204);
    }

    // ── Password operations ─────────────────────────────────────────────

    /**
     * Reset a user's password.
     *
     * @param userId      the Keycloak user ID
     * @param newPassword the new password
     * @param temporary   if true, the user must change the password on next login
     */
    public void resetPassword(String userId, String newPassword, boolean temporary) {
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", temporary
        );
        handleStatus(send(buildPut(adminUrl + "/users/" + userId + "/reset-password", credential)), 204);
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private String getAccessToken() {
        try {
            Tokens tokens = oidcClient.getTokens().await().indefinitely();
            return tokens.getAccessToken();
        } catch (Exception e) {
            throw new KeycloakAdminException(502,
                    "Unable to authenticate with identity provider", e);
        }
    }

    private HttpRequest buildGet(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private HttpRequest buildPost(String url, Map<String, Object> body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(bodyPublisher(body))
                .build();
    }

    private HttpRequest buildPostList(String url, List<Map<String, Object>> body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(bodyPublisherList(body))
                .build();
    }

    private HttpRequest buildPut(String url, Object body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(bodyPublisherObject(body))
                .build();
    }

    private HttpRequest buildDelete(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Accept", "application/json")
                .DELETE()
                .build();
    }

    private HttpRequest buildDeleteWithBody(String url, List<Map<String, Object>> body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("DELETE", bodyPublisherList(body))
                .build();
    }

    private HttpRequest.BodyPublisher bodyPublisher(Map<String, Object> body) {
        try {
            return HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body));
        } catch (Exception e) {
            throw new KeycloakAdminException(500, "Failed to serialize request body", e);
        }
    }

    private HttpRequest.BodyPublisher bodyPublisherList(List<Map<String, Object>> body) {
        try {
            return HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body));
        } catch (Exception e) {
            throw new KeycloakAdminException(500, "Failed to serialize request body", e);
        }
    }

    private HttpRequest.BodyPublisher bodyPublisherObject(Object body) {
        try {
            return HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body));
        } catch (Exception e) {
            throw new KeycloakAdminException(500, "Failed to serialize request body", e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeycloakAdminException(502, "Identity provider request interrupted", e);
        } catch (Exception e) {
            throw new KeycloakAdminException(502, "Identity provider unavailable", e);
        }
    }

    private List<Map<String, Object>> executeList(HttpRequest request) {
        HttpResponse<String> response = send(request);
        handleStatus(response, 200);
        try {
            return objectMapper.readValue(response.body(), LIST_MAP_TYPE);
        } catch (Exception e) {
            throw new KeycloakAdminException(502,
                    "Failed to parse identity provider response", e);
        }
    }

    private Map<String, Object> executeMap(HttpRequest request) {
        HttpResponse<String> response = send(request);
        handleStatus(response, 200);
        try {
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (Exception e) {
            throw new KeycloakAdminException(502,
                    "Failed to parse identity provider response", e);
        }
    }

    private void handleStatus(HttpResponse<String> response, int expectedStatus) {
        int status = response.statusCode();
        if (status == expectedStatus) {
            return;
        }

        String errorMessage = extractErrorMessage(response);
        LOG.warnf("Keycloak Admin API returned %d (expected %d): %s", status, expectedStatus, errorMessage);
        throw new KeycloakAdminException(status, errorMessage);
    }

    private String extractErrorMessage(HttpResponse<String> response) {
        String body = response.body();
        if (body == null || body.isBlank()) {
            return "Keycloak Admin API error (HTTP " + response.statusCode() + ")";
        }
        try {
            Map<String, Object> errorBody = objectMapper.readValue(body, MAP_TYPE);
            // Keycloak error responses typically use "errorMessage" or "error"
            if (errorBody.containsKey("errorMessage")) {
                return String.valueOf(errorBody.get("errorMessage"));
            }
            if (errorBody.containsKey("error_description")) {
                return String.valueOf(errorBody.get("error_description"));
            }
            if (errorBody.containsKey("error")) {
                return String.valueOf(errorBody.get("error"));
            }
        } catch (Exception ignored) {
            // Response body is not JSON — use it as-is if short enough
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
