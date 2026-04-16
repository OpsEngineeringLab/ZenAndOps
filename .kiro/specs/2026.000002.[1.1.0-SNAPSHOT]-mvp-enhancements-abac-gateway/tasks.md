# Implementation Plan: MVP Enhancements — ABAC & Gateway

## Overview

This plan implements the ZenAndOps 1.1.0 enhancements on top of the completed MVP (1.0.0). Work is organized into: Tag-based ABAC model refactoring in the Auth_Service domain/application/infrastructure layers, a new Gateway_Service (Quarkus) with JWT validation and rate limiting, seed data routine, OpenAPI documentation across all services, frontend Tag management pages and gateway routing updates, Docker Compose integration, and version control release.

## Tasks

- [x] 1. Auth_Service — Tag domain model and ABAC refactoring
  - [x] 1.1 Create `Tag` entity class in `auth-service/.../domain/entity/` with fields: id, key, value, description, createdAt, updatedAt
    - _Requirements: 1.1_
  - [x] 1.2 Create domain exceptions `TagAlreadyExistsException`, `TagInUseException`, `TagNotFoundException`, `UserNotFoundException` in `auth-service/.../domain/exception/`
    - _Requirements: 2.6, 2.7, 3.4, 3.5_
  - [x] 1.3 Refactor `User` entity: replace `Map<String, String> attributes` with `List<String> tagIds`
    - _Requirements: 1.3_
  - [x] 1.4 Update `AbacPolicy` value object: ensure `requiredUserAttributes` semantics align with Tag key-value matching
    - _Requirements: 1.5_

- [x] 2. Auth_Service — Tag application layer (ports and use cases)
  - [x] 2.1 Define `TagRepository` outbound port interface with methods: `save`, `findById`, `findAll(page, size)`, `findByKeyAndValue`, `delete`, `findAllByIds`, `existsAssignedToAnyUser`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.7_
  - [x] 2.2 Update `UserRepository` outbound port: add `findAll()` method for user listing
    - _Requirements: 3.1, 3.2, 3.3_
  - [x] 2.3 Implement `CreateTagUseCase`: create a new Tag, enforce key:value uniqueness via `TagRepository.findByKeyAndValue`
    - _Requirements: 2.1, 2.6_
  - [x] 2.4 Implement `ListTagsUseCase`: retrieve paginated list of all Tags
    - _Requirements: 2.2_
  - [x] 2.5 Implement `GetTagUseCase`: retrieve a single Tag by id, throw `TagNotFoundException` if not found
    - _Requirements: 2.3_
  - [x] 2.6 Implement `UpdateTagUseCase`: update the description of an existing Tag
    - _Requirements: 2.4_
  - [x] 2.7 Implement `DeleteTagUseCase`: delete a Tag, reject with `TagInUseException` if assigned to any User
    - _Requirements: 2.5, 2.7_
  - [x] 2.8 Implement `AssignTagsToUserUseCase`: assign one or more Tags to a User, ignore duplicates
    - _Requirements: 3.1, 3.4, 3.5, 3.6_
  - [x] 2.9 Implement `RemoveTagsFromUserUseCase`: remove one or more Tags from a User
    - _Requirements: 3.2_
  - [x] 2.10 Implement `GetUserTagsUseCase`: retrieve all Tags assigned to a User
    - _Requirements: 3.3_

- [x] 3. Auth_Service — Tag infrastructure layer (adapters and REST)
  - [x] 3.1 Implement `MongoTagRepository` adapter using Quarkus MongoDB Panache with unique compound index on `{ key: 1, value: 1 }`
    - _Requirements: 1.2, 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 3.2 Update `MongoUserRepository` adapter to handle `tagIds` field and implement `findAll()`
    - _Requirements: 1.3, 3.1, 3.2, 3.3_
  - [x] 3.3 Update `JwtTokenProvider` adapter: embed resolved Tag key-value pairs (instead of raw attributes map) in Access_Token JWT claims
    - _Requirements: 1.4_
  - [x] 3.4 Update `DefaultPolicyEngine` adapter: `evaluateAbac()` matches User's resolved Tag key-value pairs against `AbacPolicy.requiredUserAttributes`
    - _Requirements: 1.5, 1.6_
  - [x] 3.5 Create Tag REST DTOs: `CreateTagRequest`, `UpdateTagRequest`, `TagResponse`, `PaginatedTagsResponse`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - [x] 3.6 Create User-Tag REST DTOs: `UserTagsRequest`
    - _Requirements: 3.1, 3.2_
  - [x] 3.7 Create `TagResource` REST resource exposing Tag CRUD endpoints: `POST /api/v1/tags`, `GET /api/v1/tags`, `GET /api/v1/tags/{id}`, `PUT /api/v1/tags/{id}`, `DELETE /api/v1/tags/{id}` — all requiring ADMIN role
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_
  - [x] 3.8 Create `UserTagResource` REST resource exposing User-Tag assignment endpoints: `GET /api/v1/users/{userId}/tags`, `POST /api/v1/users/{userId}/tags`, `DELETE /api/v1/users/{userId}/tags` — all requiring ADMIN role
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - [x] 3.9 Update `AuthExceptionMapper` to handle new domain exceptions (`TagAlreadyExistsException` → 409, `TagInUseException` → 409, `TagNotFoundException` → 404, `UserNotFoundException` → 404)
    - _Requirements: 2.6, 2.7, 3.4, 3.5_

- [x] 4. Auth_Service — Seed data routine
  - [x] 4.1 Implement `SeedDataService` observing Quarkus `StartupEvent`: check if users collection is empty, create default Tags (`department:engineering`, `department:operations`, `location:HQ`, `location:remote`), create default Users (admin/admin with ADMIN+USER roles, user/user with USER role, guest/guest with GUEST role), assign Tags to Users per design
    - Passwords must be bcrypt-hashed
    - Must be idempotent: skip if users collection already has data
    - Must log errors and continue startup without terminating on database failure
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

- [x] 5. Checkpoint — Auth_Service Tag ABAC and seed data
  - Ensure all Auth_Service changes compile and the application starts correctly, ask the user if questions arise.

- [x] 6. Auth_Service — OpenAPI documentation
  - [x] 6.1 Add `quarkus-smallrye-openapi` dependency to Auth_Service `pom.xml`
    - _Requirements: 9.1, 9.4_
  - [x] 6.2 Annotate `AuthResource`, `TagResource`, and `UserTagResource` with OpenAPI annotations (descriptions, request/response schemas, authentication requirements, error response codes)
    - _Requirements: 9.7_
  - [x] 6.3 Configure `application.properties` to enable Swagger UI in dev mode
    - _Requirements: 9.8_

- [-] 7. Dashboard_Service — OpenAPI documentation
  - [~] 7.1 Add `quarkus-smallrye-openapi` dependency to Dashboard_Service `pom.xml`
    - _Requirements: 9.2, 9.5_
  - [~] 7.2 Annotate `DashboardResource` with OpenAPI annotations (descriptions, request/response schemas, authentication requirements, error response codes)
    - _Requirements: 9.7_
  - [~] 7.3 Configure `application.properties` to enable Swagger UI in dev mode
    - _Requirements: 9.8_


- [ ] 8. Gateway_Service — Project scaffolding
  - [~] 8.1 Scaffold `gateway-service/` as a Quarkus Maven project (Java 25) with hexagonal architecture package layout: `domain`, `application`, `infrastructure` under `com.zenandops.gateway`
    - Add dependencies: `quarkus-rest-jackson`, `quarkus-smallrye-jwt`, `quarkus-vertx`, `quarkus-smallrye-openapi`, `quarkus-arc`
    - _Requirements: 6.1, 9.3, 9.6_
  - [~] 8.2 Create domain value objects: `RouteDefinition` (pathPrefix, targetBaseUrl, jwtRequired) and `RateLimitResult` (allowed, retryAfterSeconds)
    - _Requirements: 6.2, 6.3, 6.4, 8.1_
  - [~] 8.3 Define application port interfaces: `RouteResolver` (resolve target backend URL for a given path) and `RateLimiter` (check rate limit for a client IP)
    - _Requirements: 6.2, 6.3, 6.4, 8.1_

- [ ] 9. Gateway_Service — Infrastructure adapters
  - [~] 9.1 Implement `ConfigRouteResolver` adapter: config-driven route resolution mapping path prefixes to backend service URLs with JWT requirement flag
    - Route definitions: `/api/v1/auth/login` → Auth_Service (no JWT), `/api/v1/auth/refresh` → Auth_Service (no JWT), `/api/v1/auth/*` → Auth_Service (JWT), `/api/v1/tags/*` → Auth_Service (JWT), `/api/v1/users/*/tags*` → Auth_Service (JWT), `/api/v1/dashboard/*` → Dashboard_Service (JWT)
    - _Requirements: 6.2, 6.3, 6.4, 7.3_
  - [~] 9.2 Implement `InMemoryRateLimiter` adapter: sliding window rate limiter per IP using `ConcurrentHashMap`, configurable max requests and window seconds via environment variables
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  - [~] 9.3 Implement `VertxHttpProxyAdapter`: Vert.x HTTP client for proxying requests to backend services, preserving original path, headers, query parameters, and body
    - _Requirements: 6.5, 6.7_

- [ ] 10. Gateway_Service — REST layer and request handling
  - [~] 10.1 Create `GatewayResource` catch-all route handler: apply rate limiting → resolve route → validate JWT (if required) → proxy request to backend → return response
    - Rate limiting applied before JWT validation per requirement 8.5
    - Forward original Authorization header on valid JWT per requirement 7.5
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.4, 7.5, 8.5_
  - [~] 10.2 Create `HealthResource` exposing a health check endpoint (e.g., `/q/health`)
    - _Requirements: 10.6_
  - [~] 10.3 Create `GatewayExceptionMapper` mapping errors to the standard error envelope format: 429 `GATEWAY_RATE_LIMITED` with Retry-After header, 401 `GATEWAY_UNAUTHORIZED`, 404 `GATEWAY_ROUTE_NOT_FOUND`, 503 `GATEWAY_SERVICE_UNAVAILABLE`
    - _Requirements: 6.6, 6.7, 7.2, 8.2_
  - [~] 10.4 Annotate `GatewayResource` and `HealthResource` with OpenAPI annotations
    - _Requirements: 9.3, 9.6, 9.7_
  - [~] 10.5 Configure `application.properties` for Gateway_Service: backend service URLs, JWT public key/issuer, rate limit settings, HTTP port, Swagger UI in dev mode
    - _Requirements: 7.4, 8.4, 9.8_

- [ ] 11. Checkpoint — Gateway_Service
  - Ensure Gateway_Service compiles and starts correctly, ask the user if questions arise.

- [ ] 12. Frontend — Tag management pages
  - [~] 12.1 Create `useTagApi` custom hook encapsulating Tag CRUD API calls (`POST /api/v1/tags`, `GET /api/v1/tags`, `GET /api/v1/tags/{id}`, `PUT /api/v1/tags/{id}`, `DELETE /api/v1/tags/{id}`)
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - [~] 12.2 Create `useUserTagApi` custom hook encapsulating User-Tag assignment API calls (`GET /api/v1/users/{userId}/tags`, `POST /api/v1/users/{userId}/tags`, `DELETE /api/v1/users/{userId}/tags`)
    - _Requirements: 4.5, 4.6_
  - [~] 12.3 Create `TagManagementPage` component: table listing all Tags with columns for key, value, description, and action buttons (edit, delete); includes create button
    - _Requirements: 4.1, 4.8, 4.9_
  - [~] 12.4 Create `TagFormModal` component: modal form for creating or editing a Tag with input fields for key, value, and description; displays loading indicator and disables controls during submission
    - _Requirements: 4.2, 4.3, 4.7, 4.9_
  - [~] 12.5 Create `TagDeleteConfirmModal` component: confirmation dialog before Tag deletion; displays error message if Tag is in use (409)
    - _Requirements: 4.4, 4.7_
  - [~] 12.6 Create `UserTagsSection` component: section showing assigned Tags for a User with assign/remove controls; displays loading indicator during operations
    - _Requirements: 4.5, 4.6, 4.7, 4.9_

- [ ] 13. Frontend — Gateway routing and ABAC updates
  - [~] 13.1 Update `ApiClient.ts`: set `baseURL` from `VITE_GATEWAY_URL` environment variable so all API requests route through the Gateway_Service
    - _Requirements: 11.1, 11.2_
  - [~] 13.2 Update `AuthContext.tsx`: replace `JwtClaims.attributes: Record<string, string>` with `tags: Array<{key: string, value: string}>`
    - _Requirements: 1.4_
  - [~] 13.3 Update `useAuthorization.ts`: refactor `useHasAttributes()` and `useIsAuthorized()` to match against Tag key-value pairs from the `tags` array claim
    - _Requirements: 1.5_
  - [~] 13.4 Update `Authorize.tsx` component: adapt `attributes` prop to work with Tag-based claims
    - _Requirements: 1.5_
  - [~] 13.5 Add Gateway error handling to `ApiClient.ts` response interceptor: display notification on 429 (rate limit exceeded) and 503 (service unavailable)
    - _Requirements: 11.3, 11.4_

- [ ] 14. Frontend — Routing and navigation updates
  - [~] 14.1 Update `App.tsx`: add `/tags` route for `TagManagementPage` (protected, ADMIN only)
    - _Requirements: 4.8_
  - [~] 14.2 Update `AppSidebar.tsx`: add sidebar entry for Tag Management visible only to ADMIN users
    - _Requirements: 4.8_
  - [~] 14.3 Update `nginx.conf`: replace individual backend service proxy rules with a single `/api/*` proxy to Gateway_Service
    - _Requirements: 10.4, 11.1_
  - [~] 14.4 Update Vite config and `.env.example` to include `VITE_GATEWAY_URL` environment variable
    - _Requirements: 11.2_

- [ ] 15. Checkpoint — Frontend
  - Ensure frontend compiles and builds successfully, ask the user if questions arise.

- [ ] 16. Docker Compose and containerization
  - [~] 16.1 Create `Dockerfile` for Gateway_Service (Java 25 runtime, multi-stage build with Maven) following the same pattern as Auth_Service and Dashboard_Service Dockerfiles
    - _Requirements: 10.1_
  - [~] 16.2 Update `docker-compose.yml`: add `gateway-service` service with configurable external port, environment variables for backend URLs, JWT config, and rate limit settings; add `depends_on` for auth-service and dashboard-service; add health check
    - _Requirements: 10.2, 10.3, 10.5, 10.6_
  - [~] 16.3 Update `docker-compose.yml`: update `frontend-app` service to depend on `gateway-service` instead of individual backend services; update environment to pass `VITE_GATEWAY_URL`
    - _Requirements: 10.4_
  - [~] 16.4 Update `.env` and `.env.example`: add Gateway_Service environment variables (`GATEWAY_SERVICE_PORT`, `GATEWAY_AUTH_SERVICE_URL`, `GATEWAY_DASHBOARD_SERVICE_URL`, `GATEWAY_RATE_LIMIT_MAX_REQUESTS`, `GATEWAY_RATE_LIMIT_WINDOW_SECONDS`)
    - _Requirements: 10.5_
  - [~] 16.5 Update `.gitignore` and `.dockerignore` to include `gateway-service/` build artifacts and target directories
    - _Requirements: 10.1_

- [ ] 17. Checkpoint — Full stack integration
  - Ensure all services compile, Docker Compose builds successfully, and the full stack starts without errors. Ask the user if questions arise.

- [ ] 18. Version control and release
  - [~] 18.1 Ensure all previous tasks are complete and tests pass
  - [~] 18.2 Remove SNAPSHOT suffix from all version references in the codebase
  - [~] 18.3 Commit the version bump: "release: 1.1.0 - mvp-enhancements-abac-gateway"
  - [~] 18.4 Merge branch into main/master
  - [~] 18.5 Apply Git tag: 1.1.0 (without SNAPSHOT)
  - [~] 18.6 Push branch, merge, and tag to remote

## Notes

- No test implementation is included in this scope, consistent with the MVP precedent
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at logical boundaries
- The Gateway_Service follows the same hexagonal architecture and DDD conventions as existing services
- All services use the standard error envelope format established in the MVP
