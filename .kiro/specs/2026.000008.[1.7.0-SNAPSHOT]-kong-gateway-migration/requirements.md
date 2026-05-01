# Requirements Document

## Introduction

This specification defines the requirements for replacing the current custom Quarkus-based `gateway-service` with Kong API Gateway (latest version). The migration must preserve all existing gateway capabilities — JWT/OIDC validation, request routing/proxying, rate limiting, CORS, and observability — while leveraging Kong's native plugin ecosystem. The Keycloak Admin API proxy functionality (user, role, tag, and profile management) must be extracted into a dedicated lightweight microservice since Kong does not provide custom business logic execution. The migration must be transparent to the frontend SPA and backend services, maintaining the same API contract and port mapping.

## Glossary

- **Kong_Gateway**: Kong API Gateway, an open-source API gateway that handles request routing, authentication, rate limiting, and observability via a plugin architecture
- **Kong_Declarative_Config**: A YAML file (`kong.yml`) that defines Kong services, routes, plugins, and consumers in a declarative (DB-less) format
- **Kong_Service**: A Kong entity representing an upstream backend API that Kong proxies requests to
- **Kong_Route**: A Kong entity that maps incoming request paths and methods to a Kong_Service
- **Kong_Plugin**: A Kong extension module that adds cross-cutting functionality (e.g., authentication, rate limiting, CORS) to services or routes
- **Admin_API_Service**: A new lightweight Quarkus microservice that hosts the Keycloak Admin REST API proxy endpoints (users, roles, tags, profile management) previously embedded in the gateway-service
- **Dashboard_Service**: The existing backend service for dashboard operations, accessible at `http://dashboard-service:8082`
- **CMDB_Service**: The existing backend service for CMDB operations, accessible at `http://cmdb-service:8083`
- **Keycloak**: The identity provider (Keycloak 26.6.1) used for OIDC authentication in the `zenandops` realm
- **Frontend_SPA**: The React single-page application served by nginx, which communicates with backend services through the gateway
- **Prometheus**: The metrics collection system that scrapes service endpoints for monitoring
- **Tempo**: The distributed tracing backend that receives OpenTelemetry traces via OTLP
- **Rate_Limit_Window**: The sliding time window (in seconds) during which request counts are tracked per client IP
- **OIDC_Introspection**: The process of validating a JWT/OIDC token against the Keycloak token endpoint to verify authenticity and claims

## Requirements

### Requirement 1: Kong Gateway Docker Compose Integration

**User Story:** As a platform operator, I want Kong API Gateway to replace the custom Quarkus gateway-service in the Docker Compose stack, so that the platform uses a production-grade, plugin-based API gateway.

#### Acceptance Criteria

1. THE Kong_Gateway SHALL be defined as a Docker Compose service using the official `kong` Docker image (latest stable version)
2. THE Kong_Gateway SHALL listen on port 8080 inside the container, mapped to `${GATEWAY_SERVICE_PORT}` on the host
3. THE Kong_Gateway SHALL operate in DB-less (declarative) mode using a mounted `kong.yml` configuration file
4. THE Kong_Gateway SHALL depend on Keycloak (healthy), Dashboard_Service (started), CMDB_Service (started), Admin_API_Service (started), and Tempo (started)
5. THE Kong_Gateway SHALL expose a health check endpoint that Docker Compose uses to determine service readiness
6. WHEN the Kong_Gateway starts, THE Kong_Gateway SHALL load all services, routes, and plugins from the Kong_Declarative_Config file
7. THE Docker Compose stack SHALL remove the old `gateway-service` build definition and replace it with the Kong_Gateway service definition

### Requirement 2: Request Routing and Proxying

**User Story:** As a frontend developer, I want Kong to route API requests to the correct backend services, so that the existing API contract is preserved without frontend changes.

#### Acceptance Criteria

1. WHEN a request matches the path prefix `/api/v1/dashboard`, THE Kong_Gateway SHALL proxy the request to the Dashboard_Service at `http://dashboard-service:8082`
2. WHEN a request matches the path prefix `/api/v1/cmdb`, THE Kong_Gateway SHALL proxy the request to the CMDB_Service at `http://cmdb-service:8083`
3. WHEN a request matches the path prefix `/api/v1/users`, `/api/v1/roles`, `/api/v1/tags`, or `/api/v1/profile`, THE Kong_Gateway SHALL proxy the request to the Admin_API_Service
4. THE Kong_Gateway SHALL preserve the original request path, headers, query parameters, and body when proxying to upstream services
5. THE Kong_Gateway SHALL forward the HTTP methods GET, POST, PUT, DELETE, PATCH, OPTIONS, and HEAD to upstream services
6. IF a request path does not match any configured Kong_Route, THEN THE Kong_Gateway SHALL return HTTP 404 with a JSON error response
7. IF an upstream service is unavailable, THEN THE Kong_Gateway SHALL return HTTP 503 with a JSON error response

### Requirement 3: JWT/OIDC Authentication

**User Story:** As a security engineer, I want Kong to validate JWT tokens issued by Keycloak, so that only authenticated requests reach backend services.

#### Acceptance Criteria

1. THE Kong_Gateway SHALL validate JWT tokens on all protected routes using the Kong JWT or OIDC plugin configured against the Keycloak `zenandops` realm
2. THE Kong_Gateway SHALL retrieve the JWKS (JSON Web Key Set) from Keycloak to validate token signatures
3. WHEN a request to a protected route lacks a valid `Authorization: Bearer <token>` header, THE Kong_Gateway SHALL return HTTP 401 with a JSON error response
4. WHEN a JWT token is expired, THE Kong_Gateway SHALL return HTTP 401 with a JSON error response
5. WHEN a JWT token has an invalid signature, THE Kong_Gateway SHALL return HTTP 401 with a JSON error response
6. THE Kong_Gateway SHALL accept tokens issued by `http://localhost:${KEYCLOAK_PORT}/realms/zenandops` as a valid issuer
7. THE Kong_Gateway SHALL forward the original `Authorization` header to upstream services after successful validation

### Requirement 4: Rate Limiting

**User Story:** As a platform operator, I want Kong to enforce rate limits per client IP, so that no single client can overwhelm the backend services.

#### Acceptance Criteria

1. THE Kong_Gateway SHALL enforce a rate limit of `${GATEWAY_RATE_LIMIT_MAX_REQUESTS}` requests per `${GATEWAY_RATE_LIMIT_WINDOW_SECONDS}` seconds per client IP address
2. WHEN a client exceeds the rate limit, THE Kong_Gateway SHALL return HTTP 429 with a `Retry-After` header indicating when the client may retry
3. THE Kong_Gateway SHALL use the Kong rate-limiting plugin configured in local (DB-less compatible) mode
4. THE Kong_Gateway SHALL identify clients by their source IP address, using the `X-Forwarded-For` header when present
5. WHILE a client is within the rate limit, THE Kong_Gateway SHALL include `X-RateLimit-Remaining` and `X-RateLimit-Limit` headers in the response

### Requirement 5: CORS Configuration

**User Story:** As a frontend developer, I want Kong to handle CORS headers, so that the React SPA can make cross-origin requests to the gateway.

#### Acceptance Criteria

1. THE Kong_Gateway SHALL allow cross-origin requests from `http://localhost:${FRONTEND_PORT}`
2. THE Kong_Gateway SHALL allow the HTTP methods GET, POST, PUT, DELETE, PATCH, OPTIONS, and HEAD in CORS preflight responses
3. THE Kong_Gateway SHALL allow the request headers `Authorization`, `Content-Type`, and `Accept` in CORS preflight responses
4. THE Kong_Gateway SHALL expose the response headers `X-RateLimit-Remaining` and `X-RateLimit-Reset` to the frontend
5. THE Kong_Gateway SHALL set the CORS `Access-Control-Max-Age` to 86400 seconds (24 hours)
6. WHEN a CORS preflight (OPTIONS) request is received, THE Kong_Gateway SHALL respond with the appropriate CORS headers and HTTP 200 without proxying to upstream services

### Requirement 6: Admin API Service Extraction

**User Story:** As a platform architect, I want the Keycloak Admin API proxy logic extracted into a dedicated microservice, so that business logic is decoupled from the API gateway layer.

#### Acceptance Criteria

1. THE Admin_API_Service SHALL be a Quarkus-based microservice that hosts the user, role, tag, and profile management REST endpoints
2. THE Admin_API_Service SHALL expose the following endpoint groups: `/api/v1/users`, `/api/v1/roles`, `/api/v1/tags`, `/api/v1/profile`, and `/api/v1/users/{id}/roles`, `/api/v1/users/{id}/tags`
3. THE Admin_API_Service SHALL authenticate requests using Quarkus OIDC (Keycloak) for JWT validation
4. THE Admin_API_Service SHALL use the OIDC client credentials flow (service account) to call the Keycloak Admin REST API
5. THE Admin_API_Service SHALL enforce permission-based authorization using the `permissions` claim from the JWT token
6. THE Admin_API_Service SHALL preserve the existing API contract (request/response DTOs, HTTP status codes, error envelope format) from the current gateway-service
7. THE Admin_API_Service SHALL be defined as a Docker Compose service with its own Dockerfile, listening on a dedicated internal port
8. THE Admin_API_Service SHALL include SmallRye Health endpoints at `/q/health` for Docker Compose health checks
9. IF the Keycloak Admin REST API is unavailable, THEN THE Admin_API_Service SHALL return HTTP 502 with a JSON error response

### Requirement 7: Observability Integration

**User Story:** As an SRE engineer, I want Kong and the Admin API Service to integrate with the existing observability stack, so that metrics, logs, and traces remain available for monitoring and troubleshooting.

#### Acceptance Criteria

1. THE Kong_Gateway SHALL expose Prometheus-compatible metrics for scraping by the Prometheus service
2. THE Kong_Gateway SHALL emit structured logs (JSON format) compatible with the Fluent Bit log pipeline
3. THE Kong_Gateway SHALL propagate OpenTelemetry trace context headers (`traceparent`, `tracestate`) to upstream services
4. THE Admin_API_Service SHALL expose Micrometer Prometheus metrics at `/q/metrics` for scraping by Prometheus
5. THE Admin_API_Service SHALL send OpenTelemetry traces to Tempo via the OTLP endpoint
6. THE Admin_API_Service SHALL emit structured JSON logs compatible with the Fluent Bit log pipeline
7. THE Prometheus configuration SHALL be updated to scrape the Kong_Gateway metrics endpoint and the Admin_API_Service metrics endpoint
8. THE Prometheus configuration SHALL remove the scrape target for the old `gateway-service`

### Requirement 8: Kong Declarative Configuration

**User Story:** As a DevOps engineer, I want all Kong configuration defined in a single declarative YAML file, so that the gateway setup is version-controlled and reproducible.

#### Acceptance Criteria

1. THE Kong_Declarative_Config file SHALL define Kong_Service entries for Dashboard_Service, CMDB_Service, and Admin_API_Service
2. THE Kong_Declarative_Config file SHALL define Kong_Route entries that map path prefixes to the corresponding Kong_Service entries
3. THE Kong_Declarative_Config file SHALL configure the JWT/OIDC plugin for protected routes
4. THE Kong_Declarative_Config file SHALL configure the rate-limiting plugin with environment-variable-driven parameters
5. THE Kong_Declarative_Config file SHALL configure the CORS plugin matching the current CORS policy
6. THE Kong_Declarative_Config file SHALL be mounted into the Kong_Gateway container via a Docker Compose volume
7. THE Kong_Declarative_Config file SHALL be stored in the repository under a `kong/` directory at the project root

### Requirement 9: Environment Variable Compatibility

**User Story:** As a DevOps engineer, I want the existing environment variables to continue working with the new Kong-based setup, so that deployment configuration requires minimal changes.

#### Acceptance Criteria

1. THE Kong_Gateway SHALL use `${GATEWAY_SERVICE_PORT}` for the host port mapping
2. THE Kong_Gateway SHALL use `${GATEWAY_RATE_LIMIT_MAX_REQUESTS}` and `${GATEWAY_RATE_LIMIT_WINDOW_SECONDS}` for rate limiting configuration
3. THE Kong_Gateway SHALL use `${FRONTEND_PORT}` to derive the CORS allowed origin
4. THE `.env.example` file SHALL be updated to include any new environment variables required by the Kong_Gateway or Admin_API_Service
5. THE `.env.example` file SHALL retain all existing environment variables that are still in use
6. WHEN a new environment variable is introduced, THE `.env.example` file SHALL include a descriptive comment explaining its purpose

### Requirement 10: Frontend Dependency Preservation

**User Story:** As a frontend developer, I want the frontend application to continue working without any code changes after the gateway migration, so that the migration is transparent to the frontend team.

#### Acceptance Criteria

1. THE Kong_Gateway SHALL be accessible on the same host port (`${GATEWAY_SERVICE_PORT}`) as the previous gateway-service
2. THE Frontend_SPA Docker Compose service SHALL depend on the Kong_Gateway service health check instead of the old gateway-service
3. THE Kong_Gateway SHALL return the same HTTP status codes for error conditions (401, 403, 404, 429, 502, 503) as the previous gateway-service
4. THE Kong_Gateway SHALL preserve the `X-RateLimit-Remaining` and `X-RateLimit-Reset` response headers that the Frontend_SPA reads

### Requirement 11: Old Gateway Service Removal

**User Story:** As a platform maintainer, I want the old Quarkus gateway-service code and configuration removed, so that the codebase does not contain dead code.

#### Acceptance Criteria

1. WHEN the Kong_Gateway and Admin_API_Service are fully operational, THE migration process SHALL remove the `gateway-service/` directory from the repository
2. THE migration process SHALL remove the old `gateway-service` Docker Compose service definition
3. THE migration process SHALL update `.dockerignore` and `.gitignore` if entries referenced the old gateway-service
4. THE migration process SHALL remove the Prometheus scrape target for the old `gateway-service` from the Prometheus configuration
