# Implementation Plan: Keycloak Auth Delegation

## Overview

This plan migrates ZenAndOps authentication from a custom auth-service to Keycloak as the centralized IAM provider. Tasks are ordered for incremental buildability: infrastructure first, then the Event Listener SPI, backend OIDC migration, gateway adapter layer, frontend integration, auth-service removal, and finally environment cleanup. Each task builds on the previous, ensuring no orphaned code.

## Tasks

- [x] 1. Provision Keycloak infrastructure and realm configuration
  - [x] 1.1 Add Keycloak service to Docker Compose
    - Add `keycloak` service using `quay.io/keycloak/keycloak:26.2` image
    - Configure dev mode startup with `--import-realm` flag
    - Expose port via `KEYCLOAK_PORT` env var (default 8180)
    - Add health check and connect to `zenandops-net` network
    - Mount realm JSON from `./keycloak/zenandops-realm.json:/opt/keycloak/data/import/zenandops-realm.json`
    - Mount SPI JAR from `./keycloak-event-listener/target/keycloak-event-listener.jar:/opt/keycloak/providers/keycloak-event-listener.jar`
    - Pass `KAFKA_BOOTSTRAP_SERVERS` environment variable for the Event Listener SPI
    - _Requirements: 1.1, 1.2, 1.3, 1.8_

  - [x] 1.2 Create Keycloak realm JSON with clients, mappers, roles, and seed data
    - Create `keycloak/zenandops-realm.json` defining the `zenandops` realm
    - Register `zenandops-frontend` public client with Authorization Code Flow + PKCE (S256), redirect URIs, and web origins
    - Register `zenandops-backend` confidential client with service account enabled
    - Define protocol mappers: `userId`, `name`, `email`, `roles`, `tags`, `permissions` (script-based aggregation)
    - Set access token lifespan to 900s (15 min) and SSO session max lifespan to 28800s (8 hours)
    - Define realm roles `ADMIN`, `USER`, `GUEST` with `permissions` attributes
    - Seed users `admin`, `user`, `guest` with credentials, role assignments, and tag attributes
    - Store tag definitions in realm attribute `_zenandops_tags`
    - _Requirements: 1.5, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [x] 1.3 Add Keycloak environment variables to `.env` and `.env.example`
    - Add `KEYCLOAK_PORT`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_REALM`, `KEYCLOAK_FRONTEND_CLIENT_ID`, `KEYCLOAK_BACKEND_CLIENT_ID`, `KEYCLOAK_BACKEND_CLIENT_SECRET`, `KEYCLOAK_URL`
    - Add `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID` for frontend build
    - _Requirements: 1.4, 8.5, 8.6_

- [x] 2. Implement Keycloak Event Listener SPI
  - [x] 2.1 Scaffold the `keycloak-event-listener` Maven module
    - Create `keycloak-event-listener/pom.xml` with Keycloak SPI and Kafka client dependencies (shaded JAR)
    - Create directory structure: `src/main/java/com/zenandops/keycloak/events/`
    - Create `src/main/resources/META-INF/services/org.keycloak.events.EventListenerProviderFactory` service file
    - _Requirements: 6.5_

  - [x] 2.2 Implement `KafkaEventListenerProvider` and `KafkaEventListenerProviderFactory`
    - Implement `KafkaEventListenerProviderFactory` with `getId()` returning `"zenandops-kafka-event-listener"`
    - Initialize Kafka producer in `init()` reading `KAFKA_BOOTSTRAP_SERVERS` from environment
    - Implement `KafkaEventListenerProvider.onEvent(Event)` to intercept LOGIN, LOGOUT, REFRESH_TOKEN events
    - Map Keycloak event types: LOGIN→LOGIN, LOGOUT→LOGOFF, REFRESH_TOKEN→TOKEN_REFRESH
    - Serialize events as JSON with fields: `eventId`, `eventType`, `userId`, `userLogin`, `timestamp` (ISO 8601)
    - Publish to `auth-events` Kafka topic
    - Handle Kafka unavailability gracefully (log warning, continue)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.6, 6.7_

  - [ ]* 2.3 Write property test for auth event serialization
    - **Property 5: Auth event serialization contains all required fields**
    - **Validates: Requirements 6.4**

- [x] 3. Migrate backend services to OIDC token validation
  - [x] 3.1 Migrate Gateway Service from `quarkus-smallrye-jwt` to `quarkus-oidc`
    - Replace `quarkus-smallrye-jwt` with `quarkus-oidc`, `quarkus-oidc-client`, and `quarkus-rest-client-jackson` in `gateway-service/pom.xml`
    - Remove `mp.jwt.verify.publickey` and `mp.jwt.verify.issuer` from `application.properties`
    - Add `quarkus.oidc.auth-server-url`, `quarkus.oidc.client-id`, `quarkus.oidc.credentials.secret`, `quarkus.oidc.tls.verification=none`
    - Add `quarkus.oidc-client.*` configuration for service account token management
    - Add `gateway.keycloak.admin-url` property
    - Remove `gateway.auth-service.url` property
    - _Requirements: 3.1, 3.2, 3.5, 3.6, 3.7, 7.1, 7.3_

  - [x] 3.2 Migrate Dashboard Service from `quarkus-smallrye-jwt` to `quarkus-oidc`
    - Replace `quarkus-smallrye-jwt` with `quarkus-oidc` in `dashboard-service/pom.xml`
    - Remove `mp.jwt.verify.*` from `application.properties`
    - Add `quarkus.oidc.auth-server-url`, `quarkus.oidc.client-id`, `quarkus.oidc.tls.verification=none`
    - _Requirements: 3.3, 3.7_

  - [x] 3.3 Migrate CMDB Service from `quarkus-smallrye-jwt` to `quarkus-oidc`
    - Replace `quarkus-smallrye-jwt` with `quarkus-oidc` in `cmdb-service/pom.xml`
    - Remove `mp.jwt.verify.*` from `application.properties`
    - Add `quarkus.oidc.auth-server-url`, `quarkus.oidc.client-id`, `quarkus.oidc.tls.verification=none`
    - _Requirements: 3.4, 3.7_

- [x] 4. Checkpoint — Ensure all backend OIDC migrations compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement Gateway Service admin proxy adapter layer
  - [x] 5.1 Implement `KeycloakAdminClient` REST client
    - Create `gateway-service/src/main/java/com/zenandops/gateway/infrastructure/adapter/keycloak/KeycloakAdminClient.java`
    - Implement user CRUD operations against Keycloak Admin REST API
    - Implement role CRUD and role-mapping operations
    - Implement realm attribute operations (for tag definitions)
    - Implement user attribute operations (for tag assignments)
    - Implement password reset operation
    - Use `quarkus-oidc-client` for automatic service account token management
    - _Requirements: 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11_

  - [x] 5.2 Implement response translators
    - Create `UserResponseTranslator` — convert Keycloak `UserRepresentation` ↔ ZenAndOps `UserResponse`/`CreateUserRequest`/`UpdateUserRequest`
    - Create `RoleResponseTranslator` — convert Keycloak `RoleRepresentation` ↔ ZenAndOps `RoleResponse`/`CreateRoleRequest`/`UpdateRoleRequest`
    - Create `TagResponseTranslator` — convert realm attribute JSON ↔ ZenAndOps `TagResponse`/`CreateTagRequest`/`UpdateTagRequest`
    - _Requirements: 7.12_

  - [ ]* 5.3 Write property tests for response translators
    - **Property 1: Response translation preserves all user fields**
    - **Property 2: Response translation preserves all role fields**
    - **Property 3: Tag definition storage round-trip**
    - **Property 4: User tag assignment round-trip**
    - **Validates: Requirements 7.5, 7.7, 7.8, 7.9, 7.12**

  - [x] 5.4 Implement admin proxy JAX-RS resources
    - Create `UserAdminResource` — handles `GET/POST /api/v1/users`, `GET/PUT/DELETE /api/v1/users/{id}`
    - Create `UserRoleAdminResource` — handles `POST/DELETE /api/v1/users/{userId}/roles`
    - Create `UserTagAdminResource` — handles `GET/POST/DELETE /api/v1/users/{userId}/tags`
    - Create `RoleAdminResource` — handles `GET/POST /api/v1/roles`, `GET/PUT/DELETE /api/v1/roles/{id}`
    - Create `TagAdminResource` — handles `GET/POST /api/v1/tags`, `GET/PUT/DELETE /api/v1/tags/{id}`
    - Create `ProfileResource` — handles `GET/PUT /api/v1/profile`, `POST /api/v1/profile/password`
    - Each resource validates JWT, checks permissions, calls `KeycloakAdminClient`, and translates responses
    - _Requirements: 7.5, 7.6, 7.7, 7.8, 7.9, 7.10_

  - [x] 5.5 Implement Keycloak error mapping
    - Map Keycloak Admin REST API error responses to appropriate HTTP status codes (400→400, 404→404, 409→409, 401/403→403, 500→502)
    - Return descriptive error messages in ZenAndOps error response format
    - _Requirements: 7.13_

  - [ ]* 5.6 Write property test for Keycloak error status mapping
    - **Property 6: Keycloak error status mapping**
    - **Validates: Requirements 7.13**

  - [x] 5.7 Update Gateway route resolver
    - Remove all routes pointing to `auth-service.url` from `ConfigRouteResolver`
    - Remove `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logoff` route definitions
    - Keep dashboard and CMDB routes unchanged
    - Preserve existing rate limiting behavior for all routes
    - _Requirements: 7.2, 7.4, 5.7_

- [x] 6. Checkpoint — Ensure gateway adapter layer compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement frontend keycloak-js integration
  - [x] 7.1 Install `keycloak-js` and create Keycloak initialization module
    - Add `keycloak-js` dependency to `frontend-app/package.json`
    - Create `frontend-app/src/lib/keycloak.ts` with Keycloak instance configured from `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID`
    - Add environment variables to `frontend-app/.env.example`
    - _Requirements: 4.1, 4.9_

  - [x] 7.2 Rewrite `AuthContext` to use keycloak-js
    - Replace manual token management with `keycloak-js` lifecycle (init, login, logout, updateToken)
    - Implement `login()` → `keycloak.login()` (redirect to Keycloak)
    - Implement `logoff()` → `keycloak.logout()` (terminate SSO session)
    - Implement automatic token refresh with 60-second buffer via `keycloak.updateToken(60)`
    - Parse user claims from `keycloak.tokenParsed` (userId, name, email, roles, tags, permissions)
    - Maintain backward-compatible `useAuth` hook interface
    - Redirect to Keycloak login on token refresh failure
    - Store tokens in memory only (keycloak-js default)
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6, 4.8_

  - [x] 7.3 Update axios interceptor to use keycloak-js token
    - Replace localStorage token read with `keycloak.token` in the request interceptor
    - Ensure Bearer token is attached to all API requests
    - _Requirements: 4.7_

  - [x] 7.4 Remove custom login page and related components
    - Remove the custom login page component and its route
    - Remove any localStorage-based token utilities
    - Redirect unauthenticated users to Keycloak login page
    - _Requirements: 4.2_

- [x] 8. Remove auth-service from the codebase
  - [x] 8.1 Delete the `auth-service/` directory
    - Remove the entire `auth-service/` directory including all source code, Dockerfile, pom.xml, and test resources
    - _Requirements: 5.1_

  - [x] 8.2 Remove auth-service from Docker Compose
    - Remove the `auth-service` service definition from `docker-compose.yml`
    - Remove `AUTH_SERVICE_PORT`, `AUTH_DB_NAME`, `GATEWAY_AUTH_SERVICE_URL` from all service environment blocks
    - Update `gateway-service` depends_on to reference `keycloak` instead of `auth-service`
    - Retain MongoDB service (used by CMDB) but remove `zenandops-auth` database reference if only used by auth-service
    - _Requirements: 5.2, 5.3, 5.5_

  - [x] 8.3 Remove auth-service references from documentation and configuration
    - Remove references from README, documentation, and inter-service configuration
    - _Requirements: 5.6_

- [x] 9. Environment and configuration cleanup
  - [x] 9.1 Remove old JWT and auth-service variables from `.env` and `.env.example`
    - Remove `JWT_PUBLIC_KEY`, `JWT_PRIVATE_KEY`, `JWT_ISSUER`, `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`, `JWT_REFRESH_TOKEN_EXPIRATION_HOURS`
    - Remove `AUTH_SERVICE_PORT`, `AUTH_DB_NAME`, `GATEWAY_AUTH_SERVICE_URL`
    - _Requirements: 8.1, 8.2_

  - [x] 9.2 Clean up Docker Compose environment variables
    - Remove all `MP_JWT_VERIFY_PUBLICKEY`, `MP_JWT_VERIFY_ISSUER`, `SMALLRYE_JWT_SIGN_KEY`, `SMALLRYE_JWT_NEW_TOKEN_ISSUER`, `ZENANDOPS_JWT_ISSUER`, `ZENANDOPS_JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`, `ZENANDOPS_JWT_REFRESH_TOKEN_EXPIRATION_HOURS` from all service definitions
    - Add `QUARKUS_OIDC_AUTH_SERVER_URL`, `QUARKUS_OIDC_CLIENT_ID`, `QUARKUS_OIDC_CREDENTIALS_SECRET` to each backend service
    - Add `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID` as build args to `frontend-app`
    - _Requirements: 8.3, 8.4, 8.5, 8.6_

  - [x] 9.3 Clean up application.properties for all backend services
    - Remove all `mp.jwt.verify.*`, `smallrye.jwt.*`, and `zenandops.jwt.*` entries from all services
    - Verify `quarkus.oidc.*` entries are present in all backend services
    - _Requirements: 8.7, 8.8_

- [~] 10. Final checkpoint — Ensure all tests pass and full stack is functional
  - Ensure all tests pass, ask the user if questions arise.

- [~] 11. Version control and release
  - [ ] Ensure all previous tasks are complete and tests pass
  - [ ] Remove SNAPSHOT suffix from all version references in the codebase
  - [ ] Commit the version bump: "release: 1.6.0 - keycloak-auth-delegation"
  - [ ] Merge branch into main/master
  - [ ] Apply Git tag: 1.6.0 (without SNAPSHOT)
  - [ ] Push branch, merge, and tag to remote

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key integration points
- Property tests validate universal correctness properties defined in the design document
- The Event Listener SPI must be built before Keycloak starts (JAR is volume-mounted)
- The auth-service removal (task 8) is intentionally placed after the gateway adapter layer is complete, ensuring no functionality gap
- Commit convention: `2026.000007.{task-number}: <short description>`
