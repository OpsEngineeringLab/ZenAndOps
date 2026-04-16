# Tasks

- [x] 1. Project scaffolding and monorepo structure
  - [x] 1.1 Create root project directory structure with `auth-service/`, `dashboard-service/`, and `frontend-app/` directories
  - [x] 1.2 Scaffold Auth_Service as a Quarkus Maven project (Java 25) with hexagonal architecture package layout: `domain`, `application`, `infrastructure`
  - [x] 1.3 Scaffold Dashboard_Service as a Quarkus Maven project (Java 25) with hexagonal architecture package layout: `domain`, `application`, `infrastructure`
  - [x] 1.4 Scaffold Frontend_App by copying `.frontend-template` into `frontend-app/` and adapting package.json (rename, update version to `1.0.0-SNAPSHOT`)
  - [x] 1.5 Add Quarkus dependencies to Auth_Service: `quarkus-rest-jackson`, `quarkus-mongodb-panache`, `quarkus-smallrye-jwt`, `quarkus-smallrye-reactive-messaging-kafka`, `quarkus-elytron-security-common`
  - [x] 1.6 Add Quarkus dependencies to Dashboard_Service: `quarkus-rest-jackson`, `quarkus-smallrye-jwt`
  - [x] 1.7 Add frontend dependencies: `axios`, `jwt-decode`, `react-router`, `workbox-webpack-plugin` (or vite-plugin-pwa)
  - [x] 1.8 Create `.gitignore` and `.dockerignore` at root level following the ignore files policy

- [x] 2. Auth_Service domain layer
  - [x] 2.1 Create `User` entity with fields: id, login, name, email, passwordHash, roles, attributes, active, createdAt, updatedAt
  - [x] 2.2 Create `RefreshToken` entity with fields: id, token, userId, expiresAt, revoked, createdAt
  - [x] 2.3 Create `AuthEvent` value object with fields: eventId, eventType (LOGIN, LOGOFF, TOKEN_REFRESH), userId, userLogin, timestamp
  - [x] 2.4 Create `RbacPolicy` and `AbacPolicy` value objects for authorization rules
  - [x] 2.5 Create domain exceptions: `InvalidCredentialsException`, `TokenExpiredException`, `TokenRevokedException`, `AccessDeniedException`

- [x] 3. Auth_Service application layer (ports and use cases)
  - [x] 3.1 Define outbound port interfaces: `UserRepository`, `RefreshTokenRepository`, `TokenProvider`, `PasswordEncoder`, `AuthEventPublisher`, `PolicyEngine`
  - [x] 3.2 Implement `LoginUseCase`: validate credentials, hash comparison, issue Access_Token (15 min) and Refresh_Token (8 hours), publish login event
  - [x] 3.3 Implement `RefreshTokenUseCase`: validate Refresh_Token, rotate tokens (invalidate old, issue new pair), publish refresh event
  - [x] 3.4 Implement `LogoffUseCase`: revoke Refresh_Token, publish logoff event

- [x] 4. Auth_Service infrastructure layer (adapters)
  - [x] 4.1 Implement `MongoUserRepository` adapter using Quarkus MongoDB Panache
  - [x] 4.2 Implement `MongoRefreshTokenRepository` adapter using Quarkus MongoDB Panache
  - [x] 4.3 Implement `JwtTokenProvider` adapter using SmallRye JWT for Access_Token generation and validation with claims (sub, userId, name, email, roles, attributes)
  - [x] 4.4 Implement `BcryptPasswordEncoder` adapter using Quarkus Elytron security
  - [x] 4.5 Implement `KafkaAuthEventPublisher` adapter using SmallRye Reactive Messaging with graceful failure handling (log and continue if Kafka is unavailable)
  - [x] 4.6 Implement `DefaultPolicyEngine` adapter evaluating RBAC (role matching) and ABAC (attribute matching) rules
  - [x] 4.7 Create REST resource `AuthResource` exposing `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logoff` endpoints
  - [x] 4.8 Configure `application.properties` for MongoDB connection, Kafka broker, JWT secret/issuer, and token expiration values

- [x] 5. Dashboard_Service domain and application layers
  - [x] 5.1 Create `DashboardPayload` value object with nested structures: ExecutiveSummary, TicketsByState, SliSloCompliance, IncidentMetrics, ErrorBudget, ChangeManagement, and errors list
  - [x] 5.2 Define outbound port interfaces: `TicketMetricsProvider`, `SliSloMetricsProvider`, `IncidentMetricsProvider`, `ChangeMetricsProvider`
  - [x] 5.3 Implement `GetDashboardPayloadUseCase`: aggregate data from all providers, handle partial failures by populating the errors array

- [x] 6. Dashboard_Service infrastructure layer (adapters)
  - [x] 6.1 Implement `MockTicketMetricsProvider` returning realistic ITIL ticket counts by state (New, Processing Assigned, Processing Planned, Pending, Solved, Closed)
  - [x] 6.2 Implement `MockSliSloMetricsProvider` returning realistic SRE metrics (availability SLI/SLO, latency SLI/SLO percentages)
  - [x] 6.3 Implement `MockIncidentMetricsProvider` returning realistic MTTR and MTTD values with trend indicators
  - [x] 6.4 Implement `MockChangeMetricsProvider` returning realistic change failure rate and error budget consumption data
  - [x] 6.5 Create REST resource `DashboardResource` exposing `/api/v1/dashboard` endpoint (protected, requires valid Access_Token)
  - [x] 6.6 Configure `application.properties` for JWT validation (shared secret/issuer with Auth_Service) and service port

- [x] 7. Frontend login page and authentication module
  - [x] 7.1 Create `AuthContext` with React context providing: login, logoff, token state, isAuthenticated, and auto-refresh logic
  - [x] 7.2 Create `ApiClient` module (axios instance) that attaches Bearer token to requests and intercepts 401 responses to attempt token refresh and retry
  - [x] 7.3 Adapt the `.frontend-template` SignIn page into a `LoginPage` component with login/password fields, form validation (empty field checks), loading indicator, and generic error display
  - [x] 7.4 Implement login form submission: call Auth_Service `/api/v1/auth/login`, store tokens, redirect to dashboard
  - [x] 7.5 Implement logoff: call Auth_Service `/api/v1/auth/logoff`, clear tokens from storage, redirect to login page

- [x] 8. Frontend protected routes and navigation
  - [x] 8.1 Create `ProtectedRoute` component that checks authentication state and redirects unauthenticated users to `/login`
  - [x] 8.2 Update `App.tsx` routing: `/login` (public), `/` dashboard (protected), redirect authenticated users from `/login` to `/`
  - [x] 8.3 Implement client-side RBAC/ABAC checks: hide or disable UI elements based on JWT claims (roles and attributes)
  - [x] 8.4 Update sidebar and header components to show authenticated user info and logoff button

- [-] 9. Frontend dashboard page
  - [-] 9.1 Adapt the `.frontend-template` Dashboard Home layout for the operational dashboard page
  - [-] 9.2 Create executive summary metric cards (total open tickets, critical incidents, overall availability, error budget remaining) derived from the EcommerceMetrics component pattern
  - [-] 9.3 Create ticket-by-state bar chart visualization (New, Processing Assigned, Processing Planned, Pending, Solved, Closed) using the existing chart components
  - [-] 9.4 Create SLI/SLO compliance gauge or percentage indicator component
  - [-] 9.5 Create MTTR and MTTD metric cards with trend indicators (up/down/stable arrows)
  - [-] 9.6 Create error budget consumption progress bar or gauge visualization
  - [-] 9.7 Create change failure rate display component
  - [-] 9.8 Wire dashboard page to fetch Dashboard_Payload from Dashboard_Service `/api/v1/dashboard` on mount

- [ ] 10. Frontend PWA configuration
  - [~] 10.1 Create web app manifest (`manifest.json`) with application name "ZenAndOps", icons, theme color, and `display: standalone`
  - [~] 10.2 Register a service worker that caches static assets for offline access to the login page shell
  - [~] 10.3 Configure Vite PWA plugin (or workbox) for service worker generation and asset precaching

- [ ] 11. Docker and Docker Compose setup
  - [~] 11.1 Create `Dockerfile` for Auth_Service (Java 25 runtime, multi-stage build with Maven)
  - [~] 11.2 Create `Dockerfile` for Dashboard_Service (Java 25 runtime, multi-stage build with Maven)
  - [~] 11.3 Create `Dockerfile` for Frontend_App (multi-stage: Node build + Nginx serve)
  - [~] 11.4 Create `docker-compose.yml` defining services: auth-service, dashboard-service, frontend-app, mongodb, kafka (with zookeeper or KRaft)
  - [~] 11.5 Configure network connectivity: frontend reaches backend services, backend services reach MongoDB and Kafka
  - [~] 11.6 Add health checks for MongoDB and Kafka with `depends_on` conditions for dependent services
  - [~] 11.7 Externalize all configuration via environment variables: database connection strings, Kafka broker addresses, JWT secret keys, service ports
  - [~] 11.8 Create `.env.example` file documenting all required environment variables

- [ ] 12. Version control and release
  - [~] 12.1 Ensure all previous tasks are complete and tests pass
  - [~] 12.2 Remove SNAPSHOT suffix from all version references in the codebase
  - [~] 12.3 Commit the version bump: "release: 1.0.0 - zenandops-mvp"
  - [~] 12.4 Merge branch into main/master
  - [~] 12.5 Apply Git tag: 1.0.0 (without SNAPSHOT)
  - [~] 12.6 Push branch, merge, and tag to remote
