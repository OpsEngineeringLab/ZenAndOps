# Requirements Document

## Introduction

This specification addresses the gaps identified in an architecture compliance assessment of the ZenAndOps platform against its microservices architecture guidelines (`.kiro/steering/microservices-architecture.md`). The project scored 8.2/10 overall, with the critical gap being resilience patterns (3/10). This feature brings all three backend services (admin-api-service, cmdb-service, dashboard-service) into full compliance with the architecture guidelines across resilience, API conventions, hexagonal architecture, and observability.

## Glossary

- **Admin_API_Service**: The Quarkus microservice that proxies Keycloak Admin REST API calls for user, role, tag, and profile management
- **CMDB_Service**: The Quarkus microservice responsible for asset and configuration item management, backed by MongoDB
- **Dashboard_Service**: The Quarkus microservice that provides operational dashboard metrics and KPIs
- **KeycloakAdminClient**: The infrastructure adapter in Admin_API_Service that makes synchronous HTTP calls to the Keycloak Admin REST API using `java.net.http.HttpClient`
- **Fault_Tolerance_Extension**: The `quarkus-smallrye-fault-tolerance` Quarkus extension that provides MicroProfile Fault Tolerance annotations (`@CircuitBreaker`, `@Retry`, `@Timeout`, `@Bulkhead`, `@Fallback`)
- **Health_Extension**: The `quarkus-smallrye-health` Quarkus extension that exposes `/q/health`, `/q/health/live`, and `/q/health/ready` endpoints
- **Circuit_Breaker**: A resilience pattern that stops calling a failing dependency after a failure threshold is breached, transitioning through Closed, Open, and Half-Open states
- **Bulkhead**: A resilience pattern that isolates resources (e.g., concurrent call limits) to prevent one failing dependency from consuming all available capacity
- **Error_Envelope**: The standard error response format used across all services: `{"error": {"code": "...", "message": "...", "timestamp": "..."}}`
- **Paginated_Response**: A response wrapper containing `items`, `page`, `size`, `totalItems`, and `totalPages` fields
- **Application_Layer**: The middle layer in hexagonal architecture containing use cases and port interfaces, sitting between the domain layer and the infrastructure layer
- **Port**: An interface defined in the application layer that abstracts inbound or outbound communication
- **Docker_Compose**: The container orchestration file (`docker-compose.yml`) used for local development of the full ZenAndOps stack
- **Kong_Gateway**: The API gateway service that routes external traffic to backend services
- **W3C_Trace_Context**: The standard for propagating distributed trace identifiers across service boundaries via `traceparent` and `tracestate` HTTP headers
- **Metrics_Provider**: An application-layer port interface in Dashboard_Service (e.g., `IncidentMetricsProvider`, `TicketMetricsProvider`) whose infrastructure adapter implementations supply dashboard data

## Requirements

### Requirement 1: Fault Tolerance Dependency for All Backend Services

**User Story:** As a platform operator, I want all backend services to include the SmallRye Fault Tolerance extension, so that resilience annotations are available for protecting outbound calls.

#### Acceptance Criteria

1. THE Admin_API_Service SHALL include the `quarkus-smallrye-fault-tolerance` dependency in its Maven POM
2. THE CMDB_Service SHALL include the `quarkus-smallrye-fault-tolerance` dependency in its Maven POM
3. THE Dashboard_Service SHALL include the `quarkus-smallrye-fault-tolerance` dependency in its Maven POM

### Requirement 2: Health Check Extension for All Backend Services

**User Story:** As a platform operator, I want all backend services to expose health check endpoints, so that container orchestrators and the API gateway can probe service readiness.

#### Acceptance Criteria

1. THE CMDB_Service SHALL include the `quarkus-smallrye-health` dependency in its Maven POM
2. THE Dashboard_Service SHALL include the `quarkus-smallrye-health` dependency in its Maven POM
3. WHEN an HTTP GET request is sent to `/q/health` on CMDB_Service, THE CMDB_Service SHALL return a JSON health status response with HTTP status 200 when healthy
4. WHEN an HTTP GET request is sent to `/q/health` on Dashboard_Service, THE Dashboard_Service SHALL return a JSON health status response with HTTP status 200 when healthy

### Requirement 3: Circuit Breaker, Retry, and Timeout on KeycloakAdminClient

**User Story:** As a platform operator, I want the KeycloakAdminClient to implement circuit breaker, retry, and timeout patterns, so that a Keycloak outage does not cascade into Admin_API_Service failures.

#### Acceptance Criteria

1. THE KeycloakAdminClient SHALL configure `HttpClient` with a connection timeout of 5 seconds
2. THE KeycloakAdminClient SHALL configure each `HttpRequest` with a request timeout of 10 seconds
3. WHEN a transient failure occurs on an outbound Keycloak call, THE KeycloakAdminClient SHALL retry the call up to 3 times with exponential backoff starting at 200 milliseconds
4. THE KeycloakAdminClient SHALL apply a circuit breaker that opens after 5 consecutive failures within a 10-second rolling window
5. WHILE the circuit breaker is in the Open state, THE KeycloakAdminClient SHALL reject calls immediately for 5 seconds before transitioning to Half-Open
6. WHEN the circuit breaker is in the Half-Open state and a probe call succeeds, THE KeycloakAdminClient SHALL transition the circuit breaker to the Closed state
7. IF a non-retryable error occurs (HTTP 400, 401, 403, 404, 409), THEN THE KeycloakAdminClient SHALL propagate the error immediately without retrying

### Requirement 4: Bulkhead on KeycloakAdminClient

**User Story:** As a platform operator, I want the KeycloakAdminClient to limit concurrent outbound calls, so that a slow Keycloak instance does not exhaust all available threads in Admin_API_Service.

#### Acceptance Criteria

1. THE KeycloakAdminClient SHALL limit concurrent outbound calls to Keycloak to a maximum of 10 simultaneous requests
2. WHEN the concurrent call limit is reached, THE KeycloakAdminClient SHALL queue up to 10 additional requests
3. IF the queue is full and a new request arrives, THEN THE KeycloakAdminClient SHALL reject the request with an appropriate error

### Requirement 5: Explicit MongoDB Timeout Configuration for CMDB_Service

**User Story:** As a platform operator, I want the CMDB_Service to have explicit timeout configuration for MongoDB operations, so that database slowdowns do not block request processing indefinitely.

#### Acceptance Criteria

1. THE CMDB_Service SHALL configure a MongoDB connection timeout of 5 seconds via Quarkus configuration properties
2. THE CMDB_Service SHALL configure a MongoDB server selection timeout of 5 seconds via Quarkus configuration properties
3. THE CMDB_Service SHALL configure a MongoDB socket read timeout of 10 seconds via Quarkus configuration properties

### Requirement 6: Docker Compose Health Checks for All Backend Services

**User Story:** As a developer, I want all backend services to have Docker Compose health checks, so that dependent services only start after their dependencies are confirmed healthy.

#### Acceptance Criteria

1. THE Docker_Compose SHALL define a health check for Dashboard_Service that probes the `/q/health` endpoint
2. THE Docker_Compose SHALL define a health check for CMDB_Service that probes the `/q/health` endpoint
3. THE Docker_Compose SHALL configure Kong_Gateway to depend on Dashboard_Service with condition `service_healthy`
4. THE Docker_Compose SHALL configure Kong_Gateway to depend on CMDB_Service with condition `service_healthy`
5. WHEN Dashboard_Service health check fails, THE Docker_Compose SHALL prevent Kong_Gateway from starting until Dashboard_Service becomes healthy
6. WHEN CMDB_Service health check fails, THE Docker_Compose SHALL prevent Kong_Gateway from starting until CMDB_Service becomes healthy

### Requirement 7: Pagination for CMDB_Service List Endpoints

**User Story:** As an API consumer, I want CMDB list endpoints to support pagination, so that large datasets are returned in manageable pages instead of unbounded result sets.

#### Acceptance Criteria

1. WHEN a list request is sent to an asset, CI, service, organization, CI relationship, service dependency, or service-CI association endpoint, THE CMDB_Service SHALL accept optional `page` and `size` query parameters
2. WHEN `page` and `size` query parameters are omitted, THE CMDB_Service SHALL default to page 0 and size 50
3. THE CMDB_Service SHALL return a Paginated_Response containing `items`, `page`, `size`, `totalItems`, and `totalPages` fields
4. THE CMDB_Service SHALL calculate `totalPages` as the ceiling of `totalItems` divided by `size`
5. WHEN the requested page exceeds the available pages, THE CMDB_Service SHALL return an empty `items` array with correct `totalItems` and `totalPages` values
6. IF `page` is negative or `size` is less than 1 or greater than 200, THEN THE CMDB_Service SHALL return HTTP 400 with an Error_Envelope describing the validation failure

### Requirement 8: Exception Mapper for Dashboard_Service

**User Story:** As an API consumer, I want the Dashboard_Service to return errors in the standard error envelope format, so that error handling is consistent across all ZenAndOps services.

#### Acceptance Criteria

1. THE Dashboard_Service SHALL implement an exception mapper that converts unhandled runtime exceptions to the Error_Envelope format
2. WHEN an unhandled runtime exception occurs, THE Dashboard_Service SHALL return HTTP 500 with an Error_Envelope containing code `DASHBOARD_INTERNAL_ERROR`, a descriptive message, and an ISO-8601 timestamp
3. WHEN an authentication failure occurs, THE Dashboard_Service SHALL return HTTP 401 with an Error_Envelope containing code `DASHBOARD_UNAUTHORIZED`
4. THE Dashboard_Service Error_Envelope SHALL follow the same JSON structure as Admin_API_Service and CMDB_Service: `{"error": {"code": "...", "message": "...", "timestamp": "..."}}`

### Requirement 9: Application Layer for Admin_API_Service

**User Story:** As a developer, I want the Admin_API_Service to have a proper application layer with port interfaces and use case classes, so that REST resources are decoupled from infrastructure adapters and the service follows hexagonal architecture.

#### Acceptance Criteria

1. THE Admin_API_Service SHALL define a `UserManagementPort` interface in the application layer that abstracts user CRUD and role assignment operations
2. THE Admin_API_Service SHALL define a `RoleManagementPort` interface in the application layer that abstracts role CRUD operations
3. THE Admin_API_Service SHALL define a `TagManagementPort` interface in the application layer that abstracts tag definition CRUD operations
4. THE Admin_API_Service SHALL define a `ProfileManagementPort` interface in the application layer that abstracts user profile retrieval and update operations
5. THE KeycloakAdminClient SHALL implement all port interfaces defined in the application layer
6. THE Admin_API_Service SHALL define use case classes in the application layer that orchestrate calls through port interfaces
7. THE Admin_API_Service REST resources SHALL invoke use case classes instead of calling KeycloakAdminClient directly
8. THE Admin_API_Service application layer SHALL have zero dependencies on infrastructure classes or framework annotations

### Requirement 10: W3C Trace Context Propagation in KeycloakAdminClient

**User Story:** As a platform operator, I want the KeycloakAdminClient to propagate W3C Trace Context headers to Keycloak, so that distributed traces span across the Admin_API_Service-to-Keycloak boundary.

#### Acceptance Criteria

1. WHEN the KeycloakAdminClient sends an HTTP request to Keycloak, THE KeycloakAdminClient SHALL include the `traceparent` header from the current OpenTelemetry span context
2. WHEN a `tracestate` header is present in the current span context, THE KeycloakAdminClient SHALL forward the `tracestate` header to Keycloak
3. IF no active span context exists, THEN THE KeycloakAdminClient SHALL send the request without trace headers

### Requirement 11: Fallback Methods for Dashboard_Service Metrics Providers

**User Story:** As a platform operator, I want the Dashboard_Service metrics providers to have fallback methods, so that the dashboard returns gracefully degraded data instead of failing when a data source is unavailable.

#### Acceptance Criteria

1. WHEN a Metrics_Provider implementation throws an exception, THE Dashboard_Service SHALL invoke a fallback method that returns a safe default value
2. THE fallback value for each Metrics_Provider SHALL contain zeroed or neutral metric values with a `Trend.STABLE` indicator where applicable
3. THE Dashboard_Service SHALL log a warning when a fallback method is invoked, including the name of the failed provider and the exception message
