# Requirements Document

## Introduction

This feature replaces the current custom JWT-based authentication and authorization implementation in ZenAndOps with Keycloak as the centralized Identity and Access Management (IAM) provider. The current system uses a custom auth-service that handles login, token generation (SmallRye JWT with RSA 2048-bit), user/role/tag management, and refresh token storage in MongoDB. Each backend service independently validates JWTs using a shared public key. The frontend manages tokens manually via localStorage.

By delegating authentication to Keycloak, ZenAndOps gains industry-standard OIDC/OAuth2 flows, centralized user management, SSO readiness, and eliminates the maintenance burden of custom token generation and validation logic. The auth-service microservice is completely removed from the codebase, and the Gateway_Service assumes responsibility for proxying user, role, tag, and profile management requests to the Keycloak Admin REST API. No thin wrapper or intermediate service is retained. Kafka auth event publishing is preserved through a Keycloak Event Listener SPI.

## Glossary

- **Keycloak**: An open-source Identity and Access Management server that provides OIDC/OAuth2-based authentication, user federation, and centralized session management.
- **OIDC (OpenID Connect)**: An identity layer built on top of OAuth 2.0 that provides authentication and user identity claims in a standardized JWT format.
- **Authorization_Code_Flow_with_PKCE**: An OAuth 2.0 flow designed for public clients (such as SPAs) where the client exchanges an authorization code for tokens, using a Proof Key for Code Exchange to prevent interception attacks.
- **JWKS (JSON Web Key Set)**: A set of public keys published by Keycloak at a well-known endpoint, used by backend services to verify the signature of Keycloak-issued JWTs without sharing private keys.
- **Realm**: A Keycloak namespace that manages a set of users, credentials, roles, and groups. ZenAndOps uses a single realm named `zenandops`.
- **Client**: A Keycloak entity representing an application that can request authentication. ZenAndOps registers a public client for the frontend SPA and a confidential client for backend service-to-service communication.
- **Protocol_Mapper**: A Keycloak mechanism that adds custom claims (such as tags and permissions) to issued tokens by mapping user attributes, roles, or group memberships into JWT claims.
- **Keycloak_Admin_REST_API**: A RESTful API provided by Keycloak for programmatic management of users, roles, groups, and realm configuration.
- **Event_Listener_SPI**: A Keycloak Service Provider Interface that allows custom code to react to authentication events (login, logout, token refresh) and publish them to external systems such as Kafka.
- **Gateway_Service**: The ZenAndOps API gateway (port 8080) that handles request routing, rate limiting, and JWT validation for all backend services.
- **Auth_Service**: The ZenAndOps authentication and user management microservice (port 8081), which is completely removed as part of this migration. All identity and management operations are delegated to Keycloak, with the Gateway_Service proxying administrative requests to the Keycloak_Admin_REST_API.
- **Dashboard_Service**: The ZenAndOps dashboard microservice (port 8082) that serves dashboard data and validates JWTs independently.
- **CMDB_Service**: The ZenAndOps Configuration Management Database microservice (port 8083) that manages assets and CIs, validating JWTs independently.
- **Frontend_App**: The ZenAndOps React 19 + TypeScript single-page application that provides the user interface.
- **Tag**: A key-value pair assigned to users for Attribute-Based Access Control (ABAC). Tags are stored as Keycloak user attributes and mapped into JWT claims via Protocol_Mappers.
- **Permission**: A string representing an allowed action, derived from role memberships. Permissions are mapped into JWT claims via Protocol_Mappers.
- **Access_Token**: A short-lived JWT issued by Keycloak containing user identity, roles, tags, and permissions claims.
- **Refresh_Token**: A longer-lived token issued by Keycloak used to obtain new Access_Tokens without re-authentication.

## Requirements

### Requirement 1: Keycloak Infrastructure Provisioning

**User Story:** As a platform operator, I want Keycloak added to the Docker Compose infrastructure with a pre-configured realm, so that all ZenAndOps services can delegate authentication to a centralized identity provider.

#### Acceptance Criteria

1. THE Docker_Compose_Configuration SHALL include a Keycloak service using the official `quay.io/keycloak/keycloak` image with a health check and connection to the `zenandops-net` network.
2. THE Docker_Compose_Configuration SHALL configure the Keycloak service to start in development mode with an admin user whose credentials are sourced from environment variables `KEYCLOAK_ADMIN` and `KEYCLOAK_ADMIN_PASSWORD`.
3. THE Docker_Compose_Configuration SHALL expose the Keycloak service on a port defined by the `KEYCLOAK_PORT` environment variable, defaulting to `8180`.
4. THE Environment_Configuration SHALL include all Keycloak-related variables (`KEYCLOAK_PORT`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_REALM`, `KEYCLOAK_FRONTEND_CLIENT_ID`, `KEYCLOAK_BACKEND_CLIENT_ID`, `KEYCLOAK_BACKEND_CLIENT_SECRET`, `KEYCLOAK_URL`) in both `.env` and `.env.example` files.
5. THE Keycloak_Realm_Configuration SHALL be exported as a JSON file and imported automatically on Keycloak startup, containing the `zenandops` realm, a public client for the Frontend_App, and a confidential client for backend service-to-service communication.
6. THE Keycloak_Realm_Configuration SHALL define a public client named by `KEYCLOAK_FRONTEND_CLIENT_ID` with Authorization_Code_Flow_with_PKCE enabled, redirect URIs matching the Frontend_App origin, and web origins configured for CORS.
7. THE Keycloak_Realm_Configuration SHALL define a confidential client named by `KEYCLOAK_BACKEND_CLIENT_ID` with service account enabled for backend-to-Keycloak Admin REST API calls.
8. IF the Keycloak service fails its health check, THEN THE Docker_Compose_Configuration SHALL prevent dependent services from starting.

### Requirement 2: Keycloak Token Claim Mapping

**User Story:** As a backend developer, I want Keycloak-issued tokens to contain the same custom claims (userId, name, email, roles, tags, permissions) as the current custom JWTs, so that backend services can authorize requests without code changes to claim parsing logic.

#### Acceptance Criteria

1. THE Keycloak_Realm_Configuration SHALL include a Protocol_Mapper that maps the Keycloak user ID to a `userId` claim in the Access_Token.
2. THE Keycloak_Realm_Configuration SHALL include a Protocol_Mapper that maps the user's full name to a `name` claim in the Access_Token.
3. THE Keycloak_Realm_Configuration SHALL include a Protocol_Mapper that maps the user's email to an `email` claim in the Access_Token.
4. THE Keycloak_Realm_Configuration SHALL include a Protocol_Mapper that maps the user's realm roles to a `roles` claim as a JSON array in the Access_Token.
5. THE Keycloak_Realm_Configuration SHALL include a Protocol_Mapper that maps the user's `tags` attribute to a `tags` claim as a JSON array of objects with `key` and `value` fields in the Access_Token.
6. THE Keycloak_Realm_Configuration SHALL include a Protocol_Mapper that aggregates permissions from all assigned roles and maps them to a `permissions` claim as a JSON array in the Access_Token.
7. THE Keycloak_Realm_Configuration SHALL set the Access_Token lifespan to 15 minutes and the Refresh_Token lifespan to 8 hours, matching the current token expiration configuration.

### Requirement 3: Backend Service OIDC Token Validation

**User Story:** As a backend developer, I want all backend services to validate Keycloak-issued tokens using the JWKS endpoint, so that token verification is automatic and does not require manual public key distribution.

#### Acceptance Criteria

1. THE Gateway_Service SHALL validate incoming JWTs by verifying the token signature against the Keycloak JWKS endpoint URL configured via `quarkus.oidc.jwks.url` or equivalent OIDC discovery.
2. THE Gateway_Service SHALL reject requests with expired, malformed, or incorrectly signed tokens by returning HTTP 401.
3. THE Dashboard_Service SHALL validate incoming JWTs using the Keycloak OIDC configuration, replacing the current inline public key verification.
4. THE CMDB_Service SHALL validate incoming JWTs using the Keycloak OIDC configuration, replacing the current inline public key verification.
5. WHEN a backend service starts, THE Service SHALL retrieve the JWKS key set from Keycloak and cache the keys for subsequent token validations.
6. IF the Keycloak JWKS endpoint is unreachable during token validation, THEN THE Service SHALL use the cached JWKS keys and log a warning.
7. THE Application_Properties for each backend service SHALL replace `mp.jwt.verify.publickey` and `mp.jwt.verify.issuer` with Keycloak OIDC discovery configuration (`quarkus.oidc.auth-server-url`, `quarkus.oidc.client-id`).

### Requirement 4: Frontend OIDC Integration

**User Story:** As a frontend developer, I want the React application to authenticate users via Keycloak using Authorization_Code_Flow_with_PKCE, so that the application uses a standards-based authentication flow instead of custom login forms.

#### Acceptance Criteria

1. THE Frontend_App SHALL use the `keycloak-js` adapter library to manage the OIDC authentication lifecycle (login, logout, token refresh).
2. WHEN a user accesses a protected route without a valid session, THE Frontend_App SHALL redirect the user to the Keycloak login page.
3. WHEN Keycloak redirects back after successful authentication, THE Frontend_App SHALL exchange the authorization code for tokens and store the Access_Token and Refresh_Token in memory (not localStorage).
4. THE Frontend_App SHALL automatically refresh the Access_Token using the Refresh_Token before the Access_Token expires, with a buffer of 60 seconds.
5. WHEN the user clicks the logout button, THE Frontend_App SHALL invoke the Keycloak logout endpoint to terminate the SSO session and clear all local token state.
6. THE Frontend_App SHALL expose the authenticated user's claims (userId, name, email, roles, tags, permissions) through the existing `useAuth` hook interface, maintaining backward compatibility with consuming components.
7. THE Frontend_App SHALL pass the Keycloak-issued Access_Token as a Bearer token in the Authorization header for all API requests to the Gateway_Service.
8. IF the token refresh fails, THEN THE Frontend_App SHALL redirect the user to the Keycloak login page.
9. THE Frontend_App SHALL receive Keycloak configuration (URL, realm, client ID) via build-time environment variables (`VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID`).

### Requirement 5: Auth Service Complete Removal

**User Story:** As a platform operator, I want the auth-service microservice completely removed from the codebase, so that all identity and user management operations are handled directly by Keycloak without an intermediate service layer.

#### Acceptance Criteria

1. THE Codebase SHALL remove the entire `auth-service/` directory, including all source code, configuration files, Dockerfiles, and test resources.
2. THE Docker_Compose_Configuration SHALL remove the `auth-service` service definition, including all associated environment variables, volume mounts, network connections, and health check configurations.
3. THE Docker_Compose_Configuration SHALL remove the `AUTH_SERVICE_PORT`, `AUTH_DB_NAME`, and `GATEWAY_AUTH_SERVICE_URL` environment variables from all service definitions that reference them.
4. THE Environment_Configuration SHALL remove `AUTH_SERVICE_PORT`, `AUTH_DB_NAME`, and `GATEWAY_AUTH_SERVICE_URL` from `.env` and `.env.example` files.
5. THE Docker_Compose_Configuration SHALL retain the MongoDB service because the CMDB_Service depends on it, but SHALL remove the `zenandops-auth` database reference if it is only used by the Auth_Service.
6. THE Codebase SHALL remove all references to the Auth_Service from documentation, README files, and any inter-service configuration that points to the auth-service URL or port.
7. THE Gateway_Service SHALL remove all route definitions that previously forwarded requests to the Auth_Service backend URL.

### Requirement 6: Kafka Auth Event Preservation

**User Story:** As a platform operator, I want authentication events (login, logout, token refresh) to continue being published to the Kafka `auth-events` topic, so that downstream consumers are not disrupted by the migration to Keycloak.

#### Acceptance Criteria

1. THE Keycloak_Event_Listener SHALL publish a LOGIN event to the Kafka `auth-events` topic WHEN a user successfully authenticates via Keycloak.
2. THE Keycloak_Event_Listener SHALL publish a LOGOFF event to the Kafka `auth-events` topic WHEN a user logs out via Keycloak.
3. THE Keycloak_Event_Listener SHALL publish a TOKEN_REFRESH event to the Kafka `auth-events` topic WHEN a token refresh occurs in Keycloak.
4. THE Keycloak_Event_Listener SHALL format each event as a JSON object containing `eventId` (UUID), `eventType` (LOGIN, LOGOFF, TOKEN_REFRESH), `userId`, `userLogin`, and `timestamp` (ISO 8601), matching the current AuthEvent schema.
5. THE Keycloak_Event_Listener SHALL be packaged as a Keycloak SPI JAR and deployed into the Keycloak providers directory via a Docker volume mount.
6. THE Keycloak_Event_Listener SHALL read the Kafka bootstrap servers address from a Keycloak realm attribute or environment variable.
7. IF the Kafka broker is unreachable, THEN THE Keycloak_Event_Listener SHALL log the failure and continue processing authentication events without blocking the login flow.

### Requirement 7: Gateway Service OIDC Adaptation and Admin API Proxy

**User Story:** As a backend developer, I want the gateway service to validate Keycloak-issued tokens, proxy user/role/tag/profile management requests to the Keycloak Admin REST API, and route OIDC-related requests appropriately, so that the gateway remains the single entry point for all API traffic after the auth-service is removed.

#### Acceptance Criteria

1. THE Gateway_Service SHALL validate JWT tokens on protected routes by verifying the signature against the Keycloak JWKS endpoint.
2. THE Gateway_Service SHALL remove the route definitions for `/api/v1/auth/login`, `/api/v1/auth/refresh`, and `/api/v1/auth/logoff`, as these endpoints are replaced by Keycloak OIDC flows.
3. THE Gateway_Service SHALL remove the `MP_JWT_VERIFY_PUBLICKEY` and `MP_JWT_VERIFY_ISSUER` environment variables from its Docker Compose configuration, replacing them with Keycloak OIDC configuration variables.
4. THE Gateway_Service SHALL preserve the existing rate limiting behavior for all routes.
5. THE Gateway_Service SHALL proxy user management requests (`GET /api/v1/users`, `POST /api/v1/users`, `GET /api/v1/users/{id}`, `PUT /api/v1/users/{id}`, `DELETE /api/v1/users/{id}`) to the corresponding Keycloak_Admin_REST_API endpoints under `/admin/realms/{realm}/users`.
6. THE Gateway_Service SHALL proxy user role assignment requests (`POST /api/v1/users/{userId}/roles`, `DELETE /api/v1/users/{userId}/roles`) to the Keycloak_Admin_REST_API endpoint `/admin/realms/{realm}/users/{id}/role-mappings/realm`.
7. THE Gateway_Service SHALL proxy user tag management requests (`GET /api/v1/users/{userId}/tags`, `POST /api/v1/users/{userId}/tags`, `DELETE /api/v1/users/{userId}/tags`) by reading and updating Keycloak user attributes via the Keycloak_Admin_REST_API.
8. THE Gateway_Service SHALL proxy role management requests (`GET /api/v1/roles`, `POST /api/v1/roles`, `GET /api/v1/roles/{id}`, `PUT /api/v1/roles/{id}`, `DELETE /api/v1/roles/{id}`) to the corresponding Keycloak_Admin_REST_API endpoints under `/admin/realms/{realm}/roles` and `/admin/realms/{realm}/roles-by-id/{id}`.
9. THE Gateway_Service SHALL proxy tag management requests (`GET /api/v1/tags`, `POST /api/v1/tags`, `GET /api/v1/tags/{id}`, `PUT /api/v1/tags/{id}`, `DELETE /api/v1/tags/{id}`) using custom adapter logic that stores and retrieves tag definitions via Keycloak realm attributes or a dedicated Keycloak group structure.
10. THE Gateway_Service SHALL proxy profile management requests (`GET /api/v1/profile`, `PUT /api/v1/profile`, `POST /api/v1/profile/password`) to the Keycloak Account API or equivalent Keycloak_Admin_REST_API endpoints.
11. THE Gateway_Service SHALL authenticate to the Keycloak_Admin_REST_API using the confidential client credentials (client ID and secret) configured via environment variables `KEYCLOAK_BACKEND_CLIENT_ID` and `KEYCLOAK_BACKEND_CLIENT_SECRET`.
12. THE Gateway_Service SHALL translate Keycloak_Admin_REST_API response formats to match the existing ZenAndOps REST API response contracts, so that the Frontend_App and other consumers are not affected by the backend change.
13. IF the Keycloak_Admin_REST_API returns an error during a proxied management operation, THEN THE Gateway_Service SHALL return an appropriate HTTP error response with a descriptive message to the caller.

### Requirement 8: Environment and Configuration Cleanup

**User Story:** As a platform operator, I want all custom JWT configuration and auth-service references removed and replaced with Keycloak configuration across the entire codebase, so that there are no remnants of the old authentication mechanism or the removed auth-service.

#### Acceptance Criteria

1. THE Environment_Configuration SHALL remove the `JWT_PUBLIC_KEY`, `JWT_PRIVATE_KEY`, `JWT_ISSUER`, `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`, and `JWT_REFRESH_TOKEN_EXPIRATION_HOURS` variables from `.env` and `.env.example`.
2. THE Environment_Configuration SHALL remove the `AUTH_SERVICE_PORT`, `AUTH_DB_NAME`, and `GATEWAY_AUTH_SERVICE_URL` variables from `.env` and `.env.example`.
3. THE Docker_Compose_Configuration SHALL remove all `MP_JWT_VERIFY_PUBLICKEY`, `MP_JWT_VERIFY_ISSUER`, `SMALLRYE_JWT_SIGN_KEY`, `SMALLRYE_JWT_NEW_TOKEN_ISSUER`, `ZENANDOPS_JWT_ISSUER`, `ZENANDOPS_JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`, and `ZENANDOPS_JWT_REFRESH_TOKEN_EXPIRATION_HOURS` environment variables from all service definitions.
4. THE Docker_Compose_Configuration SHALL remove all `AUTH_SERVICE_PORT`, `AUTH_DB_NAME`, and `GATEWAY_AUTH_SERVICE_URL` environment variables from all service definitions.
5. THE Docker_Compose_Configuration SHALL add Keycloak OIDC configuration variables (`QUARKUS_OIDC_AUTH_SERVER_URL`, `QUARKUS_OIDC_CLIENT_ID`, `QUARKUS_OIDC_CREDENTIALS_SECRET` where applicable) to each backend service definition.
6. THE Frontend_App Docker build SHALL receive `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, and `VITE_KEYCLOAK_CLIENT_ID` as build arguments.
7. THE Application_Properties for each backend service SHALL remove all `mp.jwt.verify.*`, `smallrye.jwt.*`, and `zenandops.jwt.*` configuration entries.
8. THE Application_Properties for each backend service SHALL add `quarkus.oidc.auth-server-url`, `quarkus.oidc.client-id`, and `quarkus.oidc.tls.verification=none` (for development mode) configuration entries.

### Requirement 9: Keycloak Realm Seed Data

**User Story:** As a platform operator, I want the Keycloak realm export JSON to include default users, roles, and tags matching the current SeedDataService data, so that a fresh deployment has the same seed data available without a separate migration script or MongoDB dependency.

#### Acceptance Criteria

1. THE Keycloak_Realm_Configuration SHALL define realm roles `ADMIN` (description: "Full system access"), `USER` (description: "Standard user access"), and `GUEST` (description: "Read-only guest access") with their associated permissions matching the current SeedDataService definitions.
2. THE Keycloak_Realm_Configuration SHALL include a user `admin` with username `admin`, first name `Administrator`, email `admin@zenandops.com`, credential password `admin`, and realm role assignments `ADMIN` and `USER`.
3. THE Keycloak_Realm_Configuration SHALL include a user `user` with username `user`, first name `Default User`, email `user@zenandops.com`, credential password `user`, and realm role assignment `USER`.
4. THE Keycloak_Realm_Configuration SHALL include a user `guest` with username `guest`, first name `Guest User`, email `guest@zenandops.com`, credential password `guest`, and realm role assignment `GUEST`.
5. THE Keycloak_Realm_Configuration SHALL store tag assignments as user attributes on each user in the format `tags` (JSON array of `{"key":"...","value":"..."}`), assigning `[{"key":"department","value":"engineering"},{"key":"location","value":"HQ"}]` to `admin`, `[{"key":"department","value":"operations"}]` to `user`, and an empty array `[]` to `guest`.
6. THE Keycloak_Realm_Configuration SHALL store role permissions as a realm role attribute named `permissions` (JSON array of permission strings), assigning `["users:read","users:write","roles:read","roles:write","tags:read","tags:write","profile:read","profile:write","dashboard:read"]` to `ADMIN`, `["profile:read","profile:write","dashboard:read"]` to `USER`, and `["dashboard:read"]` to `GUEST`.
7. WHEN Keycloak starts with the `--import-realm` flag, THE Keycloak service SHALL automatically import all seed users, roles, and tag attributes from the realm JSON file without requiring a separate migration script.
