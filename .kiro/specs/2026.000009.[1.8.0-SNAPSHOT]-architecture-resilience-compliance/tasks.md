# Implementation Plan: Architecture Resilience Compliance

## Overview

This plan brings the ZenAndOps platform into full compliance with its microservices architecture guidelines across resilience, API conventions, hexagonal architecture, observability, and infrastructure. Changes span admin-api-service, cmdb-service, dashboard-service, and Docker Compose orchestration.

Tasks are ordered to respect dependency chains: fault tolerance dependencies first, then application layer refactoring, then annotations and features that depend on them.

## Tasks

- [x] 1. Add fault tolerance and health check dependencies to all services
  - [x] 1.1 Add `quarkus-smallrye-fault-tolerance` dependency to admin-api-service `pom.xml`
    - _Requirements: 1.1_
  - [x] 1.2 Add `quarkus-smallrye-fault-tolerance` dependency to cmdb-service `pom.xml`
    - _Requirements: 1.2_
  - [x] 1.3 Add `quarkus-smallrye-fault-tolerance` and `quarkus-smallrye-health` dependencies to dashboard-service `pom.xml`
    - Also add `jqwik` test dependency for property-based testing
    - _Requirements: 1.3, 2.2_
  - [x] 1.4 Add `quarkus-smallrye-health` dependency to cmdb-service `pom.xml`
    - _Requirements: 2.1_

- [x] 2. Configure MongoDB timeouts for cmdb-service
  - Add `quarkus.mongodb.connect-timeout=5s`, `quarkus.mongodb.server-selection-timeout=5s`, and `quarkus.mongodb.read-timeout=10s` to `cmdb-service/src/main/resources/application.properties`
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 3. Checkpoint — Verify dependency additions compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Create application layer for admin-api-service (ports and use cases)
  - [x] 4.1 Define port interfaces in `com.zenandops.admin.application.port`
    - Create `UserManagementPort`, `RoleManagementPort`, `TagManagementPort`, `ProfileManagementPort` interfaces
    - Methods must use only Java standard library types (`Map<String, Object>`, `List<Map<String, Object>>`, `String`)
    - Port interfaces must have zero imports from `com.zenandops.admin.infrastructure`
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.8_
  - [x] 4.2 Create use case classes in `com.zenandops.admin.application.usecase`
    - Create all 20 use case classes: `ListUsersUseCase`, `GetUserUseCase`, `CreateUserUseCase`, `UpdateUserUseCase`, `DeleteUserUseCase`, `GetUserRolesUseCase`, `AssignUserRolesUseCase`, `RemoveUserRolesUseCase`, `ListRolesUseCase`, `GetRoleUseCase`, `CreateRoleUseCase`, `UpdateRoleUseCase`, `DeleteRoleUseCase`, `ListTagsUseCase`, `CreateTagUseCase`, `UpdateTagUseCase`, `DeleteTagUseCase`, `GetProfileUseCase`, `UpdateProfileUseCase`, `ResetPasswordUseCase`
    - Each use case injects its corresponding port interface via CDI constructor injection
    - Use case classes must have zero imports from `com.zenandops.admin.infrastructure`
    - _Requirements: 9.6, 9.8_
  - [x] 4.3 Make `KeycloakAdminClient` implement all four port interfaces
    - Add `implements UserManagementPort, RoleManagementPort, TagManagementPort, ProfileManagementPort` to the class declaration
    - Verify all port methods are already satisfied by existing public methods
    - _Requirements: 9.5_
  - [x] 4.4 Refactor REST resources to inject use cases instead of `KeycloakAdminClient`
    - Update `UserAdminResource`, `RoleAdminResource`, `TagAdminResource`, `ProfileResource`, `UserRoleAdminResource`, `UserTagAdminResource` to inject use case classes
    - REST resources must no longer import or reference `KeycloakAdminClient` directly
    - _Requirements: 9.7_
  - [ ]* 4.5 Write static analysis test to verify application layer has zero infrastructure dependencies
    - **Property 8: Application layer has zero infrastructure dependencies**
    - Scan all `.java` files in `com.zenandops.admin.application` package and assert zero import statements referencing `com.zenandops.admin.infrastructure`
    - **Validates: Requirements 9.8**

- [x] 5. Checkpoint — Verify application layer refactoring compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Add fault tolerance annotations and trace context to KeycloakAdminClient
  - [x] 6.1 Add `@CircuitBreaker`, `@Retry`, `@Timeout`, and `@Bulkhead` annotations to `KeycloakAdminClient`
    - Apply class-level annotations with parameters from the design: `@CircuitBreaker(requestVolumeThreshold=5, failureRatio=1.0, delay=5000, delayUnit=ChronoUnit.MILLIS, successThreshold=1)`, `@Retry(maxRetries=3, delay=200, delayUnit=ChronoUnit.MILLIS, jitter=100, jitterDelayUnit=ChronoUnit.MILLIS, retryOn=Exception.class, abortOn=KeycloakAdminException.class)`, `@Timeout(value=10, unit=ChronoUnit.SECONDS)`, `@Bulkhead(value=10, waitingTaskQueue=10)`
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7, 4.1, 4.2, 4.3_
  - [x] 6.2 Configure `HttpClient` connection timeout and per-request timeouts
    - Set `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))` in `@PostConstruct init()`
    - Add `.timeout(Duration.ofSeconds(10))` to all `buildGet`, `buildPost`, `buildPut`, `buildDelete`, `buildDeleteWithBody` request builders
    - _Requirements: 3.1, 3.2_
  - [x] 6.3 Inject `OpenTelemetry` and implement W3C Trace Context propagation
    - Inject `OpenTelemetry` via CDI
    - Create `injectTraceContext(HttpRequest.Builder)` method using `openTelemetry.getPropagators().getTextMapPropagator().inject()`
    - Call `injectTraceContext()` in all request builder methods before `.build()`
    - _Requirements: 10.1, 10.2, 10.3_
  - [x] 6.4 Update `AdminExceptionMapper` to handle `CircuitBreakerOpenException` and `BulkheadException`
    - Map both exceptions to HTTP 503 Service Unavailable with the standard error envelope
    - _Requirements: 4.3_
  - [ ]* 6.5 Write property test for non-retryable error propagation
    - **Property 1: Non-retryable errors are never retried**
    - Test that HTTP 400, 401, 403, 404, 409 responses from Keycloak result in immediate `KeycloakAdminException` propagation without retry
    - **Validates: Requirements 3.7**

- [x] 7. Implement pagination for cmdb-service list endpoints
  - [x] 7.1 Create `PaginatedResponse<T>` record in `com.zenandops.cmdb.infrastructure.rest.dto`
    - Define record with fields: `items` (List<T>), `page` (int), `size` (int), `totalItems` (long), `totalPages` (int)
    - Add static factory method `of(List<T> items, int page, int size, long totalItems)` that calculates `totalPages`
    - _Requirements: 7.3, 7.4_
  - [x] 7.2 Add paginated query methods to repository port interfaces
    - Add `findWithFilters(..., int page, int size)` and `countWithFilters(...)` methods to `AssetRepository`, `CIRepository`, `ServiceRepository`, `OrganizationRepository`, `CIRelationshipRepository`, `ServiceDependencyRepository`, `ServiceCIRepository`
    - _Requirements: 7.1_
  - [x] 7.3 Implement paginated queries in MongoDB Panache adapters
    - Use `PanacheQuery.page(Page.of(page, size)).list()` for paginated results
    - Use `PanacheQuery.count()` for total count
    - Update all 7 repository adapters
    - _Requirements: 7.1_
  - [x] 7.4 Update list use cases to accept `page` and `size` parameters
    - Modify `ListAssetsUseCase`, `ListCIsUseCase`, `ListServicesUseCase`, `ListOrganizationsUseCase`, `ListCIRelationshipsUseCase`, `ListServiceDependenciesUseCase`, `ListServicesByCIUseCase`/`ListCIsByServiceUseCase` to pass pagination parameters to repository ports
    - _Requirements: 7.1, 7.2_
  - [x] 7.5 Update REST resources to accept `page`/`size` query parameters and return `PaginatedResponse`
    - Add `@QueryParam("page") @DefaultValue("0") int page` and `@QueryParam("size") @DefaultValue("50") int size` to all 7 list endpoints
    - Add validation: reject `page < 0`, `size < 1`, `size > 200` with HTTP 400 and `CMDB_VALIDATION_ERROR` error code
    - Wrap use case results in `PaginatedResponse.of(...)` for the response
    - Affected resources: `AssetResource`, `CIResource`, `ServiceResource`, `OrganizationResource`, `CIRelationshipResource`, `ServiceDependencyResource`, `ServiceCIResource`
    - _Requirements: 7.1, 7.2, 7.5, 7.6_
  - [ ]* 7.6 Write property test for pagination totalPages calculation
    - **Property 2: Pagination totalPages calculation**
    - For any positive `totalItems` (0–10000) and valid `size` (1–200), verify `totalPages == ceil(totalItems / size)`
    - **Validates: Requirements 7.4**
  - [ ]* 7.7 Write property test for pagination parameter validation
    - **Property 3: Pagination rejects invalid parameters**
    - For any `page < 0` or `size < 1` or `size > 200`, verify HTTP 400 with `CMDB_VALIDATION_ERROR`
    - **Validates: Requirements 7.6**
  - [ ]* 7.8 Write property test for out-of-range page behavior
    - **Property 4: Out-of-range page returns empty items with correct totals**
    - For any `page >= totalPages`, verify empty `items` array with correct `totalItems` and `totalPages`
    - **Validates: Requirements 7.5**
  - [ ]* 7.9 Write property test for paginated response structure
    - **Property 5: Paginated response contains all required fields**
    - For any valid `page` and `size`, verify response contains `items`, `page`, `size`, `totalItems`, `totalPages`
    - **Validates: Requirements 7.1, 7.3**

- [x] 8. Checkpoint — Verify pagination and fault tolerance compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement dashboard-service exception mapper and fallback methods
  - [x] 9.1 Create `ErrorResponse` record in `com.zenandops.dashboard.infrastructure.rest.dto`
    - Define record with fields: `code` (String), `message` (String), `timestamp` (Instant)
    - Follow the same JSON structure as admin-api-service and cmdb-service: `{"error": {"code": "...", "message": "...", "timestamp": "..."}}`
    - _Requirements: 8.4_
  - [x] 9.2 Create `DashboardExceptionMapper` in `com.zenandops.dashboard.infrastructure.rest`
    - Implement `ExceptionMapper<RuntimeException>` annotated with `@Provider`
    - Map `UnauthorizedException` to HTTP 401 with code `DASHBOARD_UNAUTHORIZED`
    - Map all other `RuntimeException` to HTTP 500 with code `DASHBOARD_INTERNAL_ERROR`
    - Return error envelope format: `{"error": {...}}`
    - _Requirements: 8.1, 8.2, 8.3_
  - [x] 9.3 Add `@Fallback` annotations to all mock metrics providers
    - Add `@Fallback(fallbackMethod = "...")` to each provider method in `MockIncidentMetricsProvider`, `MockTicketMetricsProvider`, `MockSliSloMetricsProvider`, `MockChangeMetricsProvider`
    - Implement fallback methods returning zeroed/neutral values with `Trend.STABLE`
    - Add warning log in each fallback method including provider name and exception message
    - _Requirements: 11.1, 11.2, 11.3_
  - [ ]* 9.4 Write property test for dashboard exception mapper error envelope
    - **Property 6: Dashboard exception mapper produces valid Error_Envelope**
    - For any `RuntimeException` with random message, verify HTTP 500 with `{"error": {"code": "DASHBOARD_INTERNAL_ERROR", "message": "...", "timestamp": "..."}}`
    - **Validates: Requirements 8.1, 8.2, 8.4**
  - [ ]* 9.5 Write property test for fallback safe defaults
    - **Property 7: Fallback returns zeroed safe defaults with STABLE trends**
    - For any exception thrown by a `MetricsProvider`, verify fallback returns non-null value with all numeric fields zero and all `Trend` fields `STABLE`
    - **Validates: Requirements 11.1, 11.2**

- [x] 10. Add health checks and Docker Compose configuration
  - [x] 10.1 Update cmdb-service Dockerfile to compile and include `HealthCheck.java` utility
    - Follow the same pattern as admin-api-service Dockerfile: compile `HealthCheck.java` in build stage, copy `.class` to `/app/healthcheck/` in runtime stage
    - _Requirements: 6.2_
  - [x] 10.2 Update dashboard-service Dockerfile to compile and include `HealthCheck.java` utility
    - Follow the same pattern as admin-api-service Dockerfile
    - _Requirements: 6.1_
  - [x] 10.3 Add Docker Compose health checks for cmdb-service and dashboard-service
    - Add `healthcheck` block to `cmdb-service` in `docker-compose.yml` using `java -cp /app/healthcheck HealthCheck http://localhost:8083/q/health`
    - Add `healthcheck` block to `dashboard-service` in `docker-compose.yml` using `java -cp /app/healthcheck HealthCheck http://localhost:8082/q/health`
    - _Requirements: 6.1, 6.2_
  - [x] 10.4 Update Kong Gateway `depends_on` conditions to `service_healthy`
    - Change `dashboard-service` dependency from `condition: service_started` to `condition: service_healthy`
    - Change `cmdb-service` dependency from `condition: service_started` to `condition: service_healthy`
    - Change `admin-api-service` dependency from `condition: service_started` to `condition: service_healthy`
    - _Requirements: 6.3, 6.4, 6.5, 6.6_

- [x] 11. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Version control and release
  - [x] Ensure all previous tasks are complete and tests pass
  - [x] Remove SNAPSHOT suffix from all version references in the codebase
  - [x] Commit the version bump: "release: 1.8.0 - architecture-resilience-compliance"
  - [x] Merge branch into main/master
  - [x] Apply Git tag: 1.8.0 (without SNAPSHOT)
  - [x] Push branch, merge, and tag to remote
  - [x] Prepare next development cycle: bump all version references to 1.8.1-SNAPSHOT and commit with message "chore: prepare next development cycle (1.8.1-SNAPSHOT)"
  - [x] Push the development cycle commit to main

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation after each major phase
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The fault tolerance dependency (Task 1) must be added before annotations can be applied (Task 6)
- The application layer refactoring (Task 4) must happen before fault tolerance annotations on KeycloakAdminClient (Task 6), since the client will implement port interfaces
- Health check extensions (Task 1.4, 1.3) must be added before Docker Compose health checks (Task 10)
- Pagination (Task 7) and dashboard exception mapper/fallbacks (Task 9) are independent of each other
