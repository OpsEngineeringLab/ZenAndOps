# ZenAndOps — Architecture Design Document

> **Version:** 1.8.2-SNAPSHOT
> **Last updated:** 2026-05-01

---

## 1. Introduction

ZenAndOps is a modern **IT Service Management (ITSM)** platform that bridges **ITIL best practices** with **Site Reliability Engineering (SRE)** principles. It provides operational dashboards, Configuration Management Database (CMDB), identity management, and a full observability stack — all orchestrated as containerized microservices.

The platform is designed for **incremental growth**: each capability is delivered as an independent, loosely coupled service that communicates through well-defined APIs and asynchronous events. New services can be added without modifying existing ones.

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Browser / PWA                              │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP :3000
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    frontend-app (Nginx)                              │
│              React 19 + TypeScript + Tailwind CSS                    │
│                     Vite build, PWA enabled                         │
│                                                                     │
│   /           → SPA (client-side routing)                           │
│   /api/*      → reverse proxy to Kong Gateway                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP :8000
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Kong Gateway (DB-less)                           │
│                                                                     │
│   Global plugins: rate-limiting, CORS, Prometheus, OpenTelemetry    │
│                                                                     │
│   /api/v1/dashboard/*        → dashboard-service:8082               │
│   /api/v1/cmdb/*             → cmdb-service:8083                    │
│   /api/v1/users/*            ┐                                      │
│   /api/v1/roles/*            ├→ admin-api-service:8084              │
│   /api/v1/tags/*             │                                      │
│   /api/v1/profile/*          ┘                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
     ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
     │  dashboard-  │ │    cmdb-     │ │  admin-api-  │
     │   service    │ │   service    │ │   service    │
     │  (Quarkus)   │ │  (Quarkus)   │ │  (Quarkus)   │
     │   :8082      │ │   :8083      │ │   :8084      │
     └──────┬───────┘ └──┬───┬───────┘ └──────┬───────┘
            │            │   │                 │
            │ OIDC       │   │ OIDC            │ OIDC + Client
            ▼            │   ▼                 ▼
     ┌──────────────┐   │  ┌──────────────────────────┐
     │   Keycloak   │◄──┘  │        MongoDB            │
     │   :8080      │      │        :27017              │
     └──────┬───────┘      └──────────────────────────┘
            │
            │ Kafka Event Listener (SPI)
            ▼
     ┌──────────────┐
     │    Kafka      │
     │   (KRaft)     │
     │    :9092      │
     └──────────────┘
```

---

## 3. Service Catalog

### 3.1 Application Services

| Service | Technology | Port | Responsibility |
|---|---|---|---|
| **frontend-app** | React 19, TypeScript, Tailwind CSS, Vite, Nginx | 3000 | Single Page Application with PWA support. Served by Nginx with reverse proxy to Kong. |
| **kong-gateway** | Kong 3 (DB-less) | 8000 | API Gateway — request routing, rate limiting (100 req/min per IP), CORS, Prometheus metrics, OpenTelemetry traces. |
| **dashboard-service** | Java 25, Quarkus 3.33.1 | 8082 | Operational dashboard with ITIL ticket metrics and SRE indicators (MTTR, MTTD, SLI/SLO, error budget). |
| **cmdb-service** | Java 25, Quarkus 3.33.1 | 8083 | CMDB — organizations, services, assets, configuration items, relationships, and impact analysis. |
| **admin-api-service** | Java 25, Quarkus 3.33.1 | 8084 | Keycloak Admin REST API proxy — user, role, tag, and profile management with permission-based authorization. |
| **keycloak-event-listener** | Java 17, Keycloak SPI | — | Keycloak provider JAR that publishes authentication events (login, logout, token refresh) to Kafka. |

### 3.2 Infrastructure Services

| Service | Technology | Port | Responsibility |
|---|---|---|---|
| **keycloak** | Keycloak 26.6.1 | 8180 | Identity and Access Management — OIDC/JWT provider, RBAC, custom login theme, tracing enabled. |
| **mongodb** | MongoDB 8 | 27017 | Document database for CMDB data. |
| **kafka** | Apache Kafka 4.2.0 (KRaft) | 9092 | Event streaming broker — no Zookeeper dependency. Receives auth events and CMDB events. |

### 3.3 Observability Stack

| Service | Technology | Port | Responsibility |
|---|---|---|---|
| **prometheus** | Prometheus 3.4.0 | 9090 | Metrics scraping from all services. Remote-writes to Mimir for long-term storage. |
| **mimir** | Grafana Mimir 2.16.0 | 9009 | Long-term metrics storage backend (TSDB). |
| **loki** | Grafana Loki 3.4.3 | 3100 | Log aggregation with TSDB schema. |
| **tempo** | Grafana Tempo 2.10.5 | 3200 | Distributed tracing — receives OTLP gRPC (:4317) and HTTP (:4318). Generates span metrics and service graphs, remote-writes to Mimir. |
| **fluent-bit** | Fluent Bit 4.0 | 2020 | Log collector — tails Docker container logs, parses JSON, resolves service names via Lua, forwards to Loki. |
| **grafana** | Grafana 13.0.1 | 3001 | Dashboards and visualization — pre-provisioned datasources for Mimir, Loki, and Tempo with trace-to-logs and trace-to-metrics correlation. |

---

## 4. Architecture Rules and Principles

### 4.1 Hexagonal Architecture with DDD

Every backend service follows **hexagonal architecture** (ports and adapters) with **Domain-Driven Design**:

```
┌─────────────────────────────────────────────┐
│              Infrastructure Layer            │
│  REST Adapters (in) │ MongoDB/Kafka (out)    │
├─────────────────────────────────────────────┤
│              Application Layer               │
│         Use Cases │ Port Interfaces          │
├─────────────────────────────────────────────┤
│                Domain Layer                  │
│     Entities │ Value Objects │ Services      │
└─────────────────────────────────────────────┘
```

- **Domain layer** has zero external dependencies
- **Application layer** defines ports (interfaces) that infrastructure adapters implement
- **Infrastructure layer** contains REST endpoints (inbound), database clients, and messaging adapters (outbound)

### 4.2 Authentication and Authorization

**Identity Provider:** Keycloak with a dedicated `zenandops` realm.

**Clients:**
- `zenandops-frontend` — public client (PKCE with S256) for the SPA
- `zenandops-backend` — confidential client with service account for backend-to-Keycloak Admin API calls

**RBAC Model:**

| Role | Permissions |
|---|---|
| **ADMIN** | `users:read`, `users:write`, `roles:read`, `roles:write`, `tags:read`, `tags:write`, `profile:read`, `profile:write`, `dashboard:read` |
| **USER** | `profile:read`, `profile:write`, `dashboard:read` |
| **GUEST** | `dashboard:read` |

**JWT Token Claims:** `userId`, `name`, `email`, `roles`, `tags`, `permissions` (resolved from role attributes via a custom script mapper).

**JWT Validation Strategy:** Delegated to upstream Quarkus services via `quarkus-oidc`. Kong OSS does not validate JWT — this is intentional because Kong OSS lacks native JWKS discovery, and all backend services already validate tokens. Rate limiting at Kong prevents abuse from unauthenticated requests.

**Security Features:**
- Brute force protection enabled
- Registration disabled (admin-provisioned users only)
- Password reset disabled
- PKCE required for the frontend client
- Access token lifetime: 15 minutes
- SSO session max lifetime: 8 hours

### 4.3 API Gateway Rules

Kong runs in **DB-less declarative mode** — all configuration lives in `kong/kong.yml`:

- **No database dependency** — configuration is a mounted YAML file
- **Admin API disabled** — no runtime config changes
- **Rate limiting:** 100 requests/minute per IP (local policy)
- **CORS:** restricted to `http://localhost:3000` with explicit method and header allowlists
- **Path routing:** `strip_path: false` and `preserve_host: true` — backend services receive the full original path
- **Observability:** Prometheus plugin exposes `/metrics` on port 8100; OpenTelemetry plugin sends traces to Tempo via HTTP (W3C propagation)

### 4.4 Asynchronous Communication

Services communicate asynchronously through **Apache Kafka** (KRaft mode, no Zookeeper):

| Topic | Producer | Content |
|---|---|---|
| `auth-events` | keycloak-event-listener (SPI) | Login, logout, token refresh events |
| `cmdb-events` | cmdb-service | CMDB entity lifecycle events (create, update, delete) |

The Keycloak Event Listener is a custom SPI JAR that shades the Kafka client and is deployed as a Keycloak provider. It publishes events directly from the Keycloak runtime.

### 4.5 Data Storage

| Store | Engine | Usage |
|---|---|---|
| **MongoDB** | Document (BSON) | CMDB entities — organizations, services, assets, CIs, relationships |
| **Keycloak internal** | H2 (dev mode) | Users, roles, clients, realm configuration, tags (stored as realm attributes) |

MongoDB is accessed via **Quarkus MongoDB Panache** with metrics enabled for Prometheus scraping.

### 4.6 Observability Rules

The observability stack implements the **three pillars** — metrics, logs, and traces — with full correlation:

**Metrics Pipeline:**
```
Services (/q/metrics) ──► Prometheus (scrape) ──► Mimir (remote-write, long-term)
Kong (/metrics:8100)   ──► Prometheus (scrape) ──► Mimir
Keycloak (/metrics)    ──► Prometheus (scrape) ──► Mimir
Tempo (span metrics)   ──────────────────────────► Mimir
```

**Logs Pipeline:**
```
Docker containers ──► Fluent Bit (tail + JSON parse + Lua service name) ──► Loki
```

**Traces Pipeline:**
```
Quarkus services (OTLP gRPC :4317) ──► Tempo
Keycloak (OTLP gRPC :4317)         ──► Tempo
Kong (OTLP HTTP :4318)             ──► Tempo
```

**Grafana Correlation:**
- Tempo → Loki: trace-to-logs via `service.name` label
- Tempo → Mimir: trace-to-metrics via `service.name` label
- Service graph and node graph visualization enabled

**Structured Logging:** All Quarkus services output JSON logs (`quarkus.log.console.json=true`). Fluent Bit parses JSON and enriches records with `service_name` resolved from Docker container names via a Lua script with an in-memory cache.

**Trace Sampling:** All services are configured with `parentbased_traceidratio` sampler at 1.0 (100% sampling in development). Kong also samples at 1.0 with full instrumentation.

---

## 5. Growth Model

ZenAndOps is designed for **incremental, spec-driven growth**. Each new capability follows a structured lifecycle:

### 5.1 Evolution History

| Version | Spec | Description |
|---|---|---|
| 1.0.0 | `2026.000001` | MVP — Auth service, Dashboard service, Frontend SPA |
| 1.1.0 | `2026.000002` | ABAC enhancements, Gateway service |
| 1.2.0 | `2026.000003` | Infrastructure upgrade — MongoDB, Kafka |
| 1.3.0 | `2026.000004` | Admin API — tags, roles, users management |
| 1.4.0 | `2026.000005` | Observability — OpenTelemetry, Grafana stack |
| 1.5.0 | `2026.000006` | CMDB — asset and CI management; observability stack migration |
| 1.6.0 | `2026.000007` | Keycloak auth delegation (replaced custom auth service) |
| 1.7.0 | `2026.000008` | Kong Gateway migration (replaced custom gateway service) |
| 1.8.0 | `2026.000009` | Architecture resilience compliance |

### 5.2 How to Add a New Service

1. **Create a Quarkus project** with `quarkus-rest-jackson`, `quarkus-oidc`, `quarkus-opentelemetry`, and `quarkus-micrometer-registry-prometheus`
2. **Add a Dockerfile** following the multi-stage pattern (Maven build → JRE runtime)
3. **Add the service to `docker-compose.yml`** with OIDC and OTEL environment variables
4. **Add a route in `kong/kong.yml`** under the appropriate path prefix
5. **Add a Prometheus scrape target** in `observability/prometheus.yaml`
6. **Add the container name mapping** in `observability/fluent-bit-service-name.lua`
7. **Update `.env.example`** with any new environment variables

No existing service needs to be modified. The gateway, observability, and log collection automatically pick up the new service.

### 5.3 Scaling Strategy

The current architecture runs as a **single Docker Compose stack** for development. The path to production scaling:

| Concern | Current (Dev) | Production Path |
|---|---|---|
| **Orchestration** | Docker Compose | Kubernetes (Helm charts) |
| **API Gateway** | Kong DB-less (single instance) | Kong Ingress Controller or Kong + PostgreSQL |
| **Database** | MongoDB single node | MongoDB Replica Set or Atlas |
| **Messaging** | Kafka single broker (KRaft) | Kafka cluster (3+ brokers) |
| **Identity** | Keycloak dev mode (H2) | Keycloak with PostgreSQL, clustered |
| **Metrics** | Prometheus → Mimir (single node) | Mimir distributed mode |
| **Logs** | Loki single node | Loki distributed mode |
| **Traces** | Tempo single node | Tempo distributed mode |
| **Service instances** | 1 per service | Horizontal scaling with load balancing |

Each backend service is **stateless** (JWT validation, no server-side sessions), making horizontal scaling straightforward. MongoDB handles state, and Kafka decouples producers from consumers.

---

## 6. Build and Deployment

### 6.1 Build Pipeline

**Backend services (Quarkus):**
```
Maven 3.9 + Java 25 → multi-stage Docker build
  Stage 1: maven:3.9-eclipse-temurin-25 (build)
  Stage 2: eclipse-temurin:25-jre (runtime)
```

**Frontend (React):**
```
Node 22 + Vite → multi-stage Docker build
  Stage 1: node:22-alpine (npm ci + vite build)
  Stage 2: nginx:alpine (serve static assets)
```

**Keycloak Event Listener:**
```
Maven + Java 17 → shaded JAR (includes Kafka client)
  Deployed as a Keycloak provider via volume mount
```

### 6.2 Running the Stack

```bash
# Copy and configure environment
cp .env.example .env

# Build the Keycloak event listener
cd keycloak-event-listener && mvn package -DskipTests && cd ..

# Start everything
docker compose up --build -d
```

### 6.3 Port Map

| Port | Service |
|---|---|
| 3000 | Frontend (Nginx) |
| 3001 | Grafana |
| 3100 | Loki |
| 3200 | Tempo |
| 8000 | Kong Gateway (proxy) |
| 8082 | Dashboard Service |
| 8083 | CMDB Service |
| 8084 | Admin API Service |
| 8180 | Keycloak |
| 9009 | Mimir |
| 9090 | Prometheus |
| 27017 | MongoDB |
| 9092 | Kafka |

---

## 7. Advantages

### 7.1 Architectural

- **Loose coupling** — services communicate through Kong (sync) and Kafka (async); no direct service-to-service calls
- **Independent deployability** — each service has its own Dockerfile, build, and lifecycle
- **Technology flexibility** — hexagonal architecture allows swapping adapters (e.g., MongoDB → PostgreSQL) without touching domain logic
- **Native compilation ready** — all Quarkus services include a native profile for GraalVM compilation

### 7.2 Operational

- **Full observability from day one** — metrics, logs, and traces are correlated in Grafana with zero application code changes needed for new services
- **Declarative infrastructure** — Kong config, Grafana datasources, Prometheus targets, and Keycloak realm are all version-controlled YAML/JSON files
- **Health checks everywhere** — every service in Docker Compose has a health check; dependency ordering uses `condition: service_healthy`
- **Zero-Zookeeper Kafka** — KRaft mode simplifies the messaging infrastructure

### 7.3 Security

- **PKCE for SPA** — prevents authorization code interception
- **Short-lived tokens** — 15-minute access tokens with refresh rotation
- **Permission-based authorization** — fine-grained permissions resolved at token issuance, enforced at each service
- **No secrets in images** — all credentials are injected via environment variables
- **Rate limiting at the edge** — Kong protects all backend services from abuse

### 7.4 Developer Experience

- **Single command startup** — `docker compose up --build` brings up the entire platform (14 services)
- **Hot reload in dev** — Vite dev server with proxy for frontend; Quarkus dev mode for backend
- **Swagger UI** — available on services that expose OpenAPI (`/q/swagger-ui`)
- **Pre-provisioned users** — admin, user, and guest accounts are imported with the Keycloak realm

---

## 8. Network Topology

All services run on a single Docker bridge network (`zenandops-net`). Service discovery is handled by Docker DNS — services reference each other by container name.

```
zenandops-net (bridge)
├── zenandops-frontend          (frontend-app)
├── zenandops-kong-gateway      (kong-gateway)
├── zenandops-dashboard-service (dashboard-service)
├── zenandops-cmdb-service      (cmdb-service)
├── zenandops-admin-api-service (admin-api-service)
├── zenandops-keycloak          (keycloak)
├── zenandops-mongodb           (mongodb)
├── zenandops-kafka             (kafka)
├── zenandops-prometheus        (prometheus)
├── zenandops-mimir             (mimir)
├── zenandops-loki              (loki)
├── zenandops-tempo             (tempo)
├── zenandops-fluent-bit        (fluent-bit)
└── zenandops-grafana           (grafana)
```

---

## 9. Version Control and Release Process

The project follows a strict spec-driven workflow:

1. **Branch creation** — `feature-{year}.{sequential}` or `bugfix-{year}.{sequential}`
2. **SNAPSHOT version** — all version references are bumped to `X.Y.Z-SNAPSHOT` immediately
3. **One commit per task** — `{year}.{sequential}.{task}: description`
4. **SNAPSHOT removal** — all references updated to the release version before merge
5. **Merge with `--no-ff`** — preserves branch history
6. **Annotated tag** — `{version}` applied after merge

All version references are synchronized across: `pom.xml` (all services), `package.json` (frontend), `.env` / `.env.example`, and `docker-compose.yml` (image tags and JAR paths).
