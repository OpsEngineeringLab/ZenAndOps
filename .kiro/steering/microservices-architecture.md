# Microservices Architecture Guide

## Overview

This project follows a microservices architecture with hexagonal (ports & adapters) design and Domain-Driven Design (DDD) principles. Each microservice is an independently deployable unit with its own database, bounded context, and well-defined API contracts.

---

## Architectural Principles

### Hexagonal Architecture (Per Service)

Every microservice must follow the hexagonal architecture pattern with three distinct layers:

```
Domain Layer (innermost)
├── Entities
├── Value Objects
├── Domain Services
└── Domain Exceptions

Application Layer (middle)
├── Use Cases (orchestrate domain logic)
└── Ports (interfaces for inbound and outbound communication)

Infrastructure Layer (outermost)
├── Inbound Adapters (REST Resources, File Import, Message Consumers)
├── Outbound Adapters (Database, Messaging, External APIs)
└── Configuration
```

**Rules:**
- Domain layer has ZERO dependencies on infrastructure or frameworks
- Application layer depends only on the domain layer
- Infrastructure layer implements ports defined in the application layer
- Dependencies always point inward (infrastructure → application → domain)
- Framework annotations (JAX-RS, CDI, MongoDB) belong exclusively in the infrastructure layer

### Domain-Driven Design (DDD)

| Concept | Rule |
|---|---|
| **Bounded Context** | Each microservice owns exactly one bounded context |
| **Entities** | Have identity, lifecycle, and business rules |
| **Value Objects** | Immutable, defined by their attributes, no identity |
| **Aggregates** | Consistency boundaries; only the aggregate root is referenced externally |
| **Domain Events** | Published when significant state changes occur |
| **Ubiquitous Language** | Code uses the same terminology as the business domain |

---

## Database Strategy

### Database Per Service (Mandatory)

Each microservice MUST have its own dedicated database instance or logical database. No two services may share the same database.

| Rule | Description |
|---|---|
| **Isolation** | Each service owns its data exclusively — no shared tables, no shared schemas |
| **Independence** | A service can change its database schema without affecting other services |
| **Technology freedom** | Each service chooses the database technology that best fits its domain |
| **No cross-service queries** | Services communicate via APIs or events, never via direct database access |
| **Naming convention** | Database name follows the pattern: `{project}-{service-name}` (e.g., `zenandops-auth`, `zenandops-cmdb`) |

### SQL vs NoSQL Decision Matrix

Use the following criteria to decide between SQL (relational) and NoSQL (document/key-value) databases for each microservice:

#### Choose SQL (PostgreSQL, MySQL) When:

| Criterion | Description |
|---|---|
| **Strong consistency required** | ACID transactions are critical for data integrity |
| **Complex relationships** | Data has many-to-many relationships requiring JOINs |
| **Structured, predictable schema** | Data model is well-defined and unlikely to change frequently |
| **Reporting and analytics** | Complex aggregation queries, GROUP BY, window functions are needed |
| **Referential integrity** | Foreign key constraints are essential for correctness |
| **Financial data** | Monetary calculations, ledgers, billing — where precision and consistency are non-negotiable |
| **Audit compliance** | Regulatory requirements demand strict schema enforcement |
| **Multi-entity transactions** | Operations that must atomically update multiple tables |

**Examples of SQL-appropriate domains:**
- Billing and invoicing services
- Order management with inventory
- Financial ledger services
- Compliance and audit trail services
- Scheduling and calendar services with complex constraints

#### Choose NoSQL (MongoDB, DynamoDB, Redis) When:

| Criterion | Description |
|---|---|
| **Flexible/evolving schema** | Data structure varies between records or changes frequently |
| **Document-oriented data** | Data is naturally hierarchical or nested (JSON-like) |
| **High write throughput** | Write-heavy workloads requiring horizontal scalability |
| **Denormalized reads** | Read patterns benefit from pre-joined, embedded documents |
| **Catalog/inventory with variable attributes** | Items have different fields depending on type |
| **Event sourcing / append-only** | Immutable event logs, version histories |
| **Session/token storage** | Short-lived, schema-less data with TTL requirements |
| **Configuration/metadata storage** | Key-value or document data with variable structure |

**Examples of NoSQL-appropriate domains:**
- User profiles with variable attributes and tags
- Configuration item (CI) management with type-dependent attributes
- Session and token management
- Event logs and audit trails (append-only)
- Content management with flexible schemas
- IoT telemetry and time-series data

#### Decision Flowchart

```
Does the data require multi-entity ACID transactions?
├── YES → SQL
└── NO ↓

Does the schema vary significantly between records of the same type?
├── YES → NoSQL (Document)
└── NO ↓

Are complex JOINs across multiple entities a primary access pattern?
├── YES → SQL
└── NO ↓

Is the primary access pattern key-based lookups or document retrieval?
├── YES → NoSQL (Document or Key-Value)
└── NO ↓

Does the data require strict referential integrity with cascading deletes?
├── YES → SQL
└── NO ↓

Is horizontal write scalability a primary concern?
├── YES → NoSQL
└── NO → SQL (default safe choice)
```

### Database Technology Defaults

| Database | Use Case | Default For |
|---|---|---|
| **MongoDB** | Document storage, flexible schemas, hierarchical data | Services with variable-attribute entities, CMDB, user profiles |
| **PostgreSQL** | Relational data, complex queries, ACID transactions | Services with strict schemas, financial data, reporting |
| **Redis** | Caching, session storage, rate limiting, pub/sub | Ephemeral data, hot caches, distributed locks |

### Database Configuration Rules

- Each service declares its database connection via environment variables
- Connection strings follow the pattern: `{SERVICE}_DB_HOST`, `{SERVICE}_DB_PORT`, `{SERVICE}_DB_NAME`
- Credentials are injected via environment variables, never hardcoded
- Each service's database runs as a separate Docker Compose service OR uses a shared database server with isolated logical databases
- Database migrations (SQL) use a versioned migration tool (Flyway, Liquibase)
- Database indexes must be defined in code or migration scripts, not applied manually

---

## Service Communication

### Synchronous Communication (REST)

| Rule | Description |
|---|---|
| **API Gateway** | All external traffic enters through the API Gateway (Kong) |
| **Versioned APIs** | All REST endpoints are versioned: `/api/v{version}/{resource}` |
| **JSON format** | Request and response bodies use JSON |
| **Standard error envelope** | All errors follow: `{"error": {"code": "...", "message": "...", "timestamp": "..."}}` |
| **Idempotency** | PUT and DELETE operations must be idempotent |
| **Pagination** | List endpoints support pagination via `page` and `size` query parameters |

### Asynchronous Communication (Events)

| Rule | Description |
|---|---|
| **Event broker** | Kafka is the default event broker |
| **Topic naming** | Topics follow: `{service-name}-events` (e.g., `auth-events`, `cmdb-events`) |
| **Event schema** | Events include: `eventId` (UUID), `eventType`, `entityId`, `entityType`, `userId`, `timestamp`, `metadata` |
| **At-least-once delivery** | Consumers must be idempotent — handle duplicate events gracefully |
| **Fire-and-forget publishing** | Event publishing failures must NOT block the primary operation |
| **Domain events only** | Only publish events for significant domain state changes |

### Inter-Service Communication Rules

| Rule | Description |
|---|---|
| **No direct database access** | Services NEVER access another service's database |
| **API contracts** | Services communicate only through well-defined API contracts |
| **Loose coupling** | Services should be deployable independently |
| **Eventual consistency** | Accept eventual consistency between services; use events for synchronization |
| **Circuit breaker** | Implement circuit breaker pattern for synchronous inter-service calls |
| **Timeout and retry** | All outbound HTTP calls must have timeouts and retry policies with exponential backoff |

---

## Authentication and Authorization

### JWT-Based Authentication

| Rule | Description |
|---|---|
| **Identity provider** | Keycloak is the centralized IAM provider |
| **Token validation** | Each service validates JWT tokens independently via OIDC/JWKS |
| **No shared sessions** | Services are stateless — no server-side session storage |
| **Token propagation** | Incoming JWT is forwarded to downstream services when needed |
| **Service accounts** | Inter-service communication uses client credentials flow |

### Authorization Model

| Layer | Mechanism |
|---|---|
| **RBAC** | Role-Based Access Control — roles grant permissions |
| **ABAC** | Attribute-Based Access Control — tags/attributes refine access |
| **Permissions** | Fine-grained permission strings embedded in JWT claims |
| **Resource-level** | Each endpoint declares required roles or permissions |

---

## Observability

### Three Pillars (Mandatory)

Every microservice MUST implement all three observability pillars:

| Pillar | Technology | Rule |
|---|---|---|
| **Traces** | OpenTelemetry + Tempo | All HTTP requests, database operations, and message publishing generate spans |
| **Metrics** | Micrometer + Mimir/Prometheus | JVM metrics, HTTP server metrics, and custom business metrics are exported |
| **Logs** | Structured JSON + Loki | All logs are structured JSON with `traceId` and `spanId` correlation |

### Observability Rules

- Trace context (W3C Trace Context) must be propagated across all service boundaries
- Every log entry must include `traceId` and `spanId` when available
- Custom business metrics use the naming convention: `{project}.{service}.{metric_name}`
- Health checks expose liveness (`/q/health/live`) and readiness (`/q/health/ready`) endpoints
- Observability infrastructure failures must NEVER impact service availability

---

## Infrastructure and Deployment

### Containerization

| Rule | Description |
|---|---|
| **Docker** | Every service has a `Dockerfile` with multi-stage build |
| **Docker Compose** | Local development uses Docker Compose for the full stack |
| **Network isolation** | All services communicate on a shared Docker network |
| **Health checks** | Every container defines a health check |
| **Environment variables** | All configuration is injected via environment variables |
| **No secrets in images** | Secrets are never baked into Docker images |

### Service Port Convention

| Service Type | Port Range | Example |
|---|---|---|
| API Gateway | 8080 | Kong Gateway |
| Backend services | 8081–8099 | auth:8081, dashboard:8082, cmdb:8083, admin-api:8084 |
| Identity provider | 8180 | Keycloak |
| Databases | 27017, 5432, 6379 | MongoDB, PostgreSQL, Redis |
| Message brokers | 9092 | Kafka |
| Observability | 3000, 3100, 3200, 4317, 9009 | Grafana, Loki, Tempo, OTel Collector, Mimir |
| Frontend | 3000 (dev), 80 (prod/nginx) | React SPA |

### Resilience Patterns

Every service-to-service communication must implement:

| Pattern | Description |
|---|---|
| **Circuit Breaker** | Stop calling a failing service after threshold breaches |
| **Retry with Backoff** | Retry transient failures with exponential backoff and jitter |
| **Timeout** | All outbound calls have explicit timeouts |
| **Bulkhead** | Isolate resources to prevent cascading failures |
| **Graceful Degradation** | Return partial results or cached data when dependencies fail |
| **Health Checks** | Expose health endpoints for orchestrator probing |

---

## New Service Checklist

When creating a new microservice, ensure the following:

- [ ] Define the bounded context and domain model
- [ ] Choose database technology (SQL vs NoSQL) using the decision matrix above
- [ ] Create dedicated database with naming convention `{project}-{service-name}`
- [ ] Implement hexagonal architecture (domain → application → infrastructure)
- [ ] Define REST API with versioned endpoints (`/api/v{n}/...`)
- [ ] Implement JWT validation via Quarkus OIDC
- [ ] Add Kafka event publishing for domain events
- [ ] Add OpenTelemetry instrumentation (traces, metrics, logs)
- [ ] Create Dockerfile with multi-stage build
- [ ] Add service to Docker Compose with health check
- [ ] Register routes in API Gateway (Kong declarative config)
- [ ] Add Prometheus scrape target in observability config
- [ ] Update `.env.example` with new environment variables
- [ ] Update `.gitignore` and `.dockerignore` if needed

---

## Anti-Patterns (Forbidden)

| Anti-Pattern | Why It's Forbidden |
|---|---|
| **Shared database** | Breaks service independence; creates hidden coupling |
| **Synchronous chains** | A → B → C → D creates fragile, high-latency call chains |
| **Distributed transactions** | 2PC across services is complex and fragile; use sagas instead |
| **God service** | One service doing everything defeats the purpose of microservices |
| **Chatty communication** | Too many inter-service calls per request indicates wrong boundaries |
| **Hardcoded URLs** | Service discovery must use environment variables or DNS |
| **Shared libraries with domain logic** | Shared code should be limited to utilities, not business rules |
| **Direct database access across services** | Always communicate through APIs or events |
| **Ignoring eventual consistency** | Design for it; don't pretend distributed systems are synchronous |
