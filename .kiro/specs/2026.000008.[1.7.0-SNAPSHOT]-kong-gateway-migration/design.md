# Design Document

## Overview

This design replaces the custom Quarkus-based `gateway-service` with Kong API Gateway (OSS) running in DB-less (declarative) mode. Kong handles request routing, rate limiting, CORS, and observability via its native plugin ecosystem. A new lightweight `admin-api-service` (Quarkus) hosts the Keycloak Admin REST API proxy endpoints (users, roles, tags, profile) that were previously embedded in the gateway. JWT/OIDC validation is delegated to the backend services (which already have Quarkus OIDC configured) and to the new admin-api-service, since Kong OSS does not include a native OIDC/JWKS discovery plugin — the built-in JWT plugin requires pre-provisioned consumer credentials, which is incompatible with Keycloak's dynamic token issuance model.

## Architecture

### High-Level Request Flow

```
Frontend SPA (nginx:80)
       │
       ▼
Kong Gateway (port 8080)
  ├── CORS plugin (global)
  ├── Rate-limiting plugin (global)
  ├── OpenTelemetry plugin (global)
  ├── Prometheus plugin (global)
  │
  ├── /api/v1/dashboard/* ──► dashboard-service:8082
  ├── /api/v1/cmdb/*      ──► cmdb-service:8083
  └── /api/v1/users/*     ┐
      /api/v1/roles/*     ├──► admin-api-service:8084
      /api/v1/tags/*      │
      /api/v1/profile/*   ┘
```

### Component Responsibilities

| Component | Responsibility |
|---|---|
| **Kong Gateway** | Request routing, rate limiting, CORS, observability (metrics, traces, logs). No JWT validation — delegates to upstreams. |
| **admin-api-service** | Keycloak Admin REST API proxy (users, roles, tags, profile). JWT validation via Quarkus OIDC. Permission-based authorization. |
| **dashboard-service** | Dashboard business logic. Already validates JWT via Quarkus OIDC. |
| **cmdb-service** | CMDB business logic. Already validates JWT via Quarkus OIDC. |

### JWT/OIDC Strategy Decision

**Decision:** Delegate JWT validation to upstream services rather than using Kong's built-in JWT plugin.

**Rationale:**
1. Kong OSS's JWT plugin requires pre-provisioned consumer/credential pairs — it cannot dynamically discover JWKS from Keycloak
2. The Enterprise-only OIDC plugin supports JWKS discovery but requires a paid license
3. Community plugins (e.g., `nokia/kong-oidc`) add maintenance burden and version compatibility risk
4. All backend services (dashboard-service, cmdb-service) already validate JWT tokens via Quarkus OIDC
5. The new admin-api-service will also validate JWT tokens via Quarkus OIDC
6. This approach is simpler, more maintainable, and avoids introducing third-party Lua plugins

**Trade-off:** Unauthenticated requests reach upstream services before being rejected (HTTP 401). This is acceptable because:
- Rate limiting at Kong prevents abuse
- Backend services reject invalid tokens immediately with minimal resource consumption
- This is a common pattern in Kong OSS deployments with Keycloak

## Detailed Design

### 1. Kong Gateway Docker Compose Service

**Image:** `kong:3` (latest stable 3.x OSS image from Docker Hub)

**Mode:** DB-less (declarative) via `KONG_DATABASE=off`

**Port mapping:** `${GATEWAY_SERVICE_PORT}:8000` (Kong's default proxy port is 8000)

**Admin API:** Disabled in production (`KONG_ADMIN_LISTEN=off`) — all config is declarative

**Health check:** Kong exposes a status endpoint at `/status` on port 8100 when `KONG_STATUS_LISTEN` is configured

**Environment variables:**
```yaml
environment:
  KONG_DATABASE: "off"
  KONG_DECLARATIVE_CONFIG: /kong/kong.yml
  KONG_PROXY_LISTEN: "0.0.0.0:8000"
  KONG_ADMIN_LISTEN: "off"
  KONG_STATUS_LISTEN: "0.0.0.0:8100"
  KONG_LOG_LEVEL: info
  KONG_PROXY_ACCESS_LOG: /dev/stdout
  KONG_PROXY_ERROR_LOG: /dev/stderr
  KONG_PLUGINS: "bundled"
```

**Volume mount:** `./kong/kong.yml:/kong/kong.yml:ro`

### 2. Kong Declarative Configuration (`kong/kong.yml`)

```yaml
_format_version: "3.0"
_transform: true

services:
  - name: dashboard-service
    url: http://dashboard-service:8082
    routes:
      - name: dashboard-route
        paths:
          - /api/v1/dashboard
        strip_path: false
        preserve_host: true

  - name: cmdb-service
    url: http://cmdb-service:8083
    routes:
      - name: cmdb-route
        paths:
          - /api/v1/cmdb
        strip_path: false
        preserve_host: true

  - name: admin-api-service
    url: http://admin-api-service:8084
    routes:
      - name: admin-users-route
        paths:
          - /api/v1/users
        strip_path: false
        preserve_host: true
      - name: admin-roles-route
        paths:
          - /api/v1/roles
        strip_path: false
        preserve_host: true
      - name: admin-tags-route
        paths:
          - /api/v1/tags
        strip_path: false
        preserve_host: true
      - name: admin-profile-route
        paths:
          - /api/v1/profile
        strip_path: false
        preserve_host: true

plugins:
  - name: rate-limiting
    config:
      minute: ${GATEWAY_RATE_LIMIT_MAX_REQUESTS:-100}
      policy: local
      limit_by: ip
      fault_tolerant: true
      hide_client_headers: false

  - name: cors
    config:
      origins:
        - http://localhost:${FRONTEND_PORT:-3000}
      methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
        - OPTIONS
        - HEAD
      headers:
        - Authorization
        - Content-Type
        - Accept
      exposed_headers:
        - X-RateLimit-Remaining
        - X-RateLimit-Reset
      max_age: 86400
      credentials: true
      preflight_continue: false

  - name: prometheus

  - name: opentelemetry
    config:
      endpoint: http://tempo:4317
      resource_attributes:
        service.name: zenandops-kong-gateway
```

**Note on environment variable interpolation:** Kong's declarative config does not natively support environment variable interpolation. The rate-limiting `minute` value and CORS `origins` will be set to fixed values matching the defaults from `.env.example`. If dynamic configuration is needed, a startup script using `envsubst` can be used to template the `kong.yml` file before Kong loads it.

### 3. Admin API Service (`admin-api-service/`)

The admin-api-service is a new Quarkus microservice extracted from the current gateway-service. It contains all Keycloak Admin REST API proxy logic.

#### Project Structure

```
admin-api-service/
├── Dockerfile
├── pom.xml
└── src/main/
    ├── java/com/zenandops/admin/
    │   ├── application/port/          # (empty — no domain ports needed)
    │   ├── domain/exception/
    │   │   ├── ForbiddenException.java
    │   │   └── UnauthorizedException.java
    │   ├── infrastructure/
    │   │   ├── adapter/keycloak/
    │   │   │   ├── KeycloakAdminClient.java
    │   │   │   ├── KeycloakAdminException.java
    │   │   │   ├── RoleResponseTranslator.java
    │   │   │   ├── TagResponseTranslator.java
    │   │   │   └── UserResponseTranslator.java
    │   │   └── rest/
    │   │       ├── AdminExceptionMapper.java
    │   │       ├── HealthResource.java
    │   │       ├── ProfileResource.java
    │   │       ├── RoleAdminResource.java
    │   │       ├── TagAdminResource.java
    │   │       ├── UserAdminResource.java
    │   │       ├── UserRoleAdminResource.java
    │   │       ├── UserTagAdminResource.java
    │   │       └── dto/
    │   │           ├── CreateRoleRequest.java
    │   │           ├── CreateTagRequest.java
    │   │           ├── CreateUserRequest.java
    │   │           ├── ErrorResponse.java
    │   │           ├── PasswordChangeRequest.java
    │   │           ├── RoleAssignmentRequest.java
    │   │           ├── RoleResponse.java
    │   │           ├── TagAssignment.java
    │   │           ├── TagResponse.java
    │   │           ├── UpdateRoleRequest.java
    │   │           ├── UpdateTagRequest.java
    │   │           ├── UpdateUserRequest.java
    │   │           └── UserResponse.java
    └── resources/
        └── application.properties
```

#### Key Design Decisions

1. **Package rename:** `com.zenandops.gateway` → `com.zenandops.admin` to reflect the service's focused responsibility
2. **Removed components:** `RouteResolver`, `RateLimiter`, `VertxHttpProxyAdapter`, `GatewayResource`, `RouteDefinition`, `RateLimitResult`, `RateLimitMetrics`, `RouteNotFoundException`, `RateLimitExceededException` — all routing/proxying/rate-limiting logic is now handled by Kong
3. **Preserved components:** All Keycloak admin client code, response translators, admin REST resources, DTOs, and the exception mapper
4. **Port:** 8084 (internal only, not exposed to host)
5. **Java version:** 25 (same as existing services)
6. **Quarkus version:** 3.33.1 (same as existing services)

#### Quarkus Dependencies (pom.xml)

```xml
<!-- REST with Jackson serialization -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>

<!-- OIDC token validation (Keycloak JWKS) -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc</artifactId>
</dependency>

<!-- OIDC client for service account token management -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc-client</artifactId>
</dependency>

<!-- Health checks -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>

<!-- OpenTelemetry -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry</artifactId>
</dependency>

<!-- Micrometer Prometheus registry -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Arc (CDI) -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-arc</artifactId>
</dependency>
```

**Removed dependencies** (compared to gateway-service): `quarkus-rest-client-jackson`, `quarkus-vertx`, `smallrye-mutiny-vertx-web-client`, `quarkus-smallrye-openapi` — these were only needed for the proxy adapter and Swagger UI.

#### application.properties

```properties
# HTTP
quarkus.http.port=8084

# OIDC token validation (Keycloak)
quarkus.oidc.auth-server-url=http://keycloak:8080/realms/zenandops
quarkus.oidc.client-id=zenandops-backend
quarkus.oidc.credentials.secret=${KEYCLOAK_BACKEND_CLIENT_SECRET}
quarkus.oidc.tls.verification=none
quarkus.oidc.token.issuer=http://localhost:${KEYCLOAK_EXTERNAL_PORT:8180}/realms/zenandops

# OIDC client for Admin REST API calls (service account)
quarkus.oidc-client.auth-server-url=http://keycloak:8080/realms/zenandops
quarkus.oidc-client.client-id=zenandops-backend
quarkus.oidc-client.credentials.secret=${KEYCLOAK_BACKEND_CLIENT_SECRET}
quarkus.oidc-client.grant.type=client

# Keycloak Admin REST API base URL
admin.keycloak.admin-url=http://keycloak:8080/admin/realms/zenandops

# OpenTelemetry
quarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
quarkus.application.name=zenandops-admin-api

# Tracing
quarkus.otel.traces.enabled=true
quarkus.otel.traces.sampler=parentbased_traceidratio
quarkus.otel.traces.sampler.arg=${OTEL_TRACES_SAMPLER_ARG:1.0}
quarkus.otel.logs.enabled=false
quarkus.otel.metrics.enabled=false

# Metrics
quarkus.micrometer.binder.jvm=true
quarkus.micrometer.binder.http-server.enabled=true

# Structured JSON logging
quarkus.log.console.json=true
```

#### Dockerfile

Same multi-stage build pattern as existing services (Maven build + Java 25 JRE runtime), with the health check utility compiled in the build stage.

### 4. Docker Compose Changes

#### New services

```yaml
kong-gateway:
  image: kong:3
  container_name: zenandops-kong-gateway
  ports:
    - "${GATEWAY_SERVICE_PORT}:8000"
  environment:
    KONG_DATABASE: "off"
    KONG_DECLARATIVE_CONFIG: /kong/kong.yml
    KONG_PROXY_LISTEN: "0.0.0.0:8000"
    KONG_ADMIN_LISTEN: "off"
    KONG_STATUS_LISTEN: "0.0.0.0:8100"
    KONG_LOG_LEVEL: info
    KONG_PROXY_ACCESS_LOG: /dev/stdout
    KONG_PROXY_ERROR_LOG: /dev/stderr
    KONG_PLUGINS: "bundled"
  volumes:
    - ./kong/kong.yml:/kong/kong.yml:ro
  depends_on:
    keycloak:
      condition: service_healthy
    dashboard-service:
      condition: service_started
    cmdb-service:
      condition: service_started
    admin-api-service:
      condition: service_started
    tempo:
      condition: service_started
  healthcheck:
    test: ["CMD-SHELL", "kong health"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 15s
  networks:
    - zenandops-net

admin-api-service:
  build:
    context: ./admin-api-service
    dockerfile: Dockerfile
  image: zenandops/admin-api-service:${ZENANDOPS_VERSION}
  container_name: zenandops-admin-api-service
  environment:
    QUARKUS_HTTP_PORT: 8084
    QUARKUS_OIDC_AUTH_SERVER_URL: http://keycloak:8080/realms/zenandops
    QUARKUS_OIDC_CLIENT_ID: ${KEYCLOAK_BACKEND_CLIENT_ID}
    QUARKUS_OIDC_CREDENTIALS_SECRET: ${KEYCLOAK_BACKEND_CLIENT_SECRET}
    QUARKUS_OIDC_TOKEN_ISSUER: http://localhost:${KEYCLOAK_PORT:-8180}/realms/zenandops
    QUARKUS_OIDC_CLIENT_AUTH_SERVER_URL: http://keycloak:8080/realms/zenandops
    QUARKUS_OIDC_CLIENT_CLIENT_ID: ${KEYCLOAK_BACKEND_CLIENT_ID}
    QUARKUS_OIDC_CLIENT_CREDENTIALS_SECRET: ${KEYCLOAK_BACKEND_CLIENT_SECRET}
    ADMIN_KEYCLOAK_ADMIN_URL: http://keycloak:8080/admin/realms/zenandops
    OTEL_EXPORTER_OTLP_ENDPOINT: http://tempo:4317
  depends_on:
    keycloak:
      condition: service_healthy
    tempo:
      condition: service_started
  healthcheck:
    test: ["CMD", "java", "-cp", "/app/healthcheck", "HealthCheck", "http://localhost:8084/q/health"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
  networks:
    - zenandops-net
```

#### Removed services

- `gateway-service` — entirely replaced by `kong-gateway` + `admin-api-service`

#### Updated services

- `frontend-app` — `depends_on` changes from `gateway-service` to `kong-gateway`

### 5. Observability Changes

#### Prometheus Configuration (`observability/prometheus.yaml`)

**Remove:**
```yaml
- job_name: zenandops-gateway
  metrics_path: /q/metrics
  static_configs:
    - targets: ["gateway-service:8080"]
      labels:
        service: zenandops-gateway
```

**Add:**
```yaml
- job_name: zenandops-kong-gateway
  metrics_path: /metrics
  static_configs:
    - targets: ["kong-gateway:8100"]
      labels:
        service: zenandops-kong-gateway

- job_name: zenandops-admin-api
  metrics_path: /q/metrics
  static_configs:
    - targets: ["admin-api-service:8084"]
      labels:
        service: zenandops-admin-api
```

Kong's Prometheus plugin exposes metrics on the status port (8100) at `/metrics`.

### 6. Environment Variable Changes

#### New variables in `.env.example`

```env
# --- Admin API Service ---
ADMIN_API_SERVICE_PORT=8084
```

#### Removed variables

None — all existing gateway variables (`GATEWAY_SERVICE_PORT`, `GATEWAY_RATE_LIMIT_MAX_REQUESTS`, `GATEWAY_RATE_LIMIT_WINDOW_SECONDS`, `GATEWAY_DASHBOARD_SERVICE_URL`, `GATEWAY_CMDB_SERVICE_URL`) are either reused by Kong config or can be removed. The `GATEWAY_DASHBOARD_SERVICE_URL` and `GATEWAY_CMDB_SERVICE_URL` variables are no longer needed since Kong's declarative config uses hardcoded Docker network hostnames.

#### Updated variables

- `GATEWAY_DASHBOARD_SERVICE_URL` and `GATEWAY_CMDB_SERVICE_URL` — removed (Kong uses Docker DNS names directly in `kong.yml`)
- `GATEWAY_RATE_LIMIT_MAX_REQUESTS` and `GATEWAY_RATE_LIMIT_WINDOW_SECONDS` — retained for documentation but values are baked into `kong.yml`

### 7. File System Changes

#### New files/directories

| Path | Description |
|---|---|
| `kong/kong.yml` | Kong declarative configuration |
| `admin-api-service/` | New Quarkus microservice (full project) |
| `admin-api-service/Dockerfile` | Multi-stage Docker build |
| `admin-api-service/pom.xml` | Maven project descriptor |
| `admin-api-service/src/` | Java source code (extracted from gateway-service) |

#### Removed files/directories

| Path | Description |
|---|---|
| `gateway-service/` | Entire old gateway service directory |

## Correctness Properties

### Property 1: Kong Route Resolution Preserves Path

**Requirement:** 2.4 — Preserve original request attributes

**Type:** Property (metamorphic)

**Description:** For all valid request paths matching a configured route prefix, Kong must forward the complete original path to the upstream service without modification (since `strip_path: false`).

**Testable via:** Integration test verifying that requests to `/api/v1/dashboard/some/path?q=1` arrive at the upstream with the same path and query string.

### Property 2: Admin API Service Permission Enforcement

**Requirement:** 6.5 — Permission-based authorization

**Type:** Property

**Description:** For all requests to admin endpoints, if the JWT token's `permissions` claim does not contain the required permission string, the Admin_API_Service must return HTTP 403. This property holds for all combinations of endpoints and permission sets.

**Testable via:** Property-based test generating random permission sets and verifying that access is denied when the required permission is absent and granted when present.

### Property 3: Admin API Service Error Envelope Consistency

**Requirement:** 6.6 — Preserve existing API contract

**Type:** Property (round-trip)

**Description:** For all error responses from the Admin_API_Service, the response body must follow the envelope format `{"error": {"code": "<CODE>", "message": "<MSG>", "timestamp": "<ISO>"}}`. This ensures the frontend error handling logic continues to work unchanged.

**Testable via:** Property-based test generating various error conditions and verifying the response body structure matches the envelope schema.

### Property 4: Rate Limiting Enforcement

**Requirement:** 4.1, 4.2 — Rate limit enforcement and 429 response

**Type:** Edge case

**Description:** When a client IP sends more than the configured maximum requests within the rate limit window, Kong must return HTTP 429 with a `Retry-After` header. Subsequent requests within the window must also be rejected.

**Testable via:** Integration test sending N+1 requests rapidly and verifying the (N+1)th returns 429.

### Property 5: CORS Preflight Handling

**Requirement:** 5.6 — CORS preflight handling

**Type:** Edge case

**Description:** When an OPTIONS request with `Origin` and `Access-Control-Request-Method` headers is sent, Kong must respond with appropriate CORS headers (Access-Control-Allow-Origin, Access-Control-Allow-Methods, Access-Control-Allow-Headers, Access-Control-Max-Age) and HTTP 200, without proxying to any upstream service.

**Testable via:** Integration test sending a CORS preflight request and verifying response headers.

### Property 6: Kong Declarative Config Validity

**Requirement:** 8.1, 8.2, 8.3, 8.4, 8.5 — Declarative config completeness

**Type:** Example

**Description:** The `kong.yml` file must be valid Kong declarative configuration that passes `kong config parse` validation. It must define exactly 3 services, 6 routes, and 4 global plugins (rate-limiting, cors, prometheus, opentelemetry).

**Testable via:** Running `kong config parse kong.yml` in a Kong container and verifying exit code 0.
