# Requirements Document

## Introduction

This spec covers the second iteration of the ZenAndOps ITSM platform (version 1.1.0), building on the completed MVP (1.0.0). The enhancements introduce a formal Tag-based ABAC authorization model replacing the generic `User.attributes` map, a full CRUD API and frontend for managing tags, an idempotent seed data routine for MongoDB, an API Gateway service as a single entry point for all microservices, and dynamic API documentation (OpenAPI/Swagger) for all services and the gateway.

The existing architecture remains: hexagonal architecture with DDD, Java 25 with Quarkus, MongoDB, Kafka, React PWA frontend, Docker Compose orchestration. This iteration adds a new `gateway-service` to the monorepo and extends the `auth-service` and `frontend-app`.

## Glossary

- **Auth_Service**: The backend microservice responsible for user authentication, token issuance, authorization, and tag management
- **Dashboard_Service**: The backend microservice responsible for aggregating operational metrics from mocked data sources
- **Frontend_App**: The React Progressive Web Application providing the user interface for login, dashboard, and tag management
- **Gateway_Service**: The API Gateway microservice that serves as the single entry point for all backend services, handling JWT validation, request routing, and rate limiting
- **Tag**: A key-value pair entity (e.g., `department:engineering`, `location:HQ`) assigned to users for Attribute-Based Access Control decisions
- **Tag_Key**: The identifier portion of a Tag (e.g., `department`, `location`, `clearance-level`)
- **Tag_Value**: The value portion of a Tag (e.g., `engineering`, `HQ`, `top-secret`)
- **ABAC**: Attribute-Based Access Control, an authorization model where access decisions are based on Tag key-value pairs assigned to users, resources, and environment conditions
- **Policy_Engine**: The component within the Auth_Service that evaluates RBAC and ABAC rules to determine authorization decisions
- **User**: A person who interacts with the ZenAndOps platform through the Frontend_App
- **JWT**: JSON Web Token, a compact token format used for stateless authentication between the Frontend_App and backend services
- **Access_Token**: A short-lived JWT issued by the Auth_Service that grants access to protected resources
- **Refresh_Token**: A long-lived token issued by the Auth_Service that allows obtaining a new Access_Token without re-authentication
- **Seed_Data_Routine**: An idempotent startup routine that populates MongoDB with default users, roles, and tags when the database is empty
- **Rate_Limiter**: A component within the Gateway_Service that restricts the number of requests a client can make within a time window
- **OpenAPI_Specification**: A machine-readable API description format (formerly Swagger) used to document REST endpoints
- **App_Token**: An application-level token used by the Gateway_Service to identify and authenticate client applications (as opposed to user-level JWT tokens)

## Requirements

### Requirement 1: Tag Entity Model Replacing User Attributes

**User Story:** As a developer, I want the ABAC model to use formal Tag entities (key:value pairs) instead of the generic `User.attributes` map, so that authorization policies are based on a well-defined, manageable data structure.

#### Acceptance Criteria

1. THE Auth_Service SHALL define a Tag entity with the following fields: id, key (Tag_Key), value (Tag_Value), description, createdAt, and updatedAt
2. THE Auth_Service SHALL enforce uniqueness on the combination of Tag_Key and Tag_Value across all Tag records in MongoDB
3. THE Auth_Service SHALL replace the `Map<String, String> attributes` field on the User entity with a collection of Tag references
4. WHEN the Auth_Service issues an Access_Token, THE Access_Token SHALL contain the assigned Tag key-value pairs in the JWT claims instead of the former attributes map
5. THE Policy_Engine SHALL evaluate ABAC rules by matching User Tag key-value pairs against the required tags defined in AbacPolicy records
6. WHEN a User has no tags assigned, THE Policy_Engine SHALL deny access for any ABAC policy that requires tag matching

### Requirement 2: Tag CRUD API

**User Story:** As an administrator, I want a complete API for creating, reading, updating, and deleting tags, so that I can manage the available tags in the system.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose a REST endpoint that creates a new Tag given a Tag_Key, Tag_Value, and optional description
2. THE Auth_Service SHALL expose a REST endpoint that retrieves all existing Tags as a paginated list
3. THE Auth_Service SHALL expose a REST endpoint that retrieves a single Tag by its identifier
4. THE Auth_Service SHALL expose a REST endpoint that updates the description of an existing Tag
5. THE Auth_Service SHALL expose a REST endpoint that deletes a Tag by its identifier
6. WHEN a Tag creation request contains a Tag_Key and Tag_Value combination that already exists, THE Auth_Service SHALL return an HTTP 409 status code with a descriptive error message
7. WHEN a Tag deletion request targets a Tag that is currently assigned to one or more Users, THE Auth_Service SHALL return an HTTP 409 status code indicating the Tag is in use
8. WHEN a Tag operation request is received without a valid Access_Token with the ADMIN role, THE Auth_Service SHALL return an HTTP 403 status code

### Requirement 3: User-Tag Assignment API

**User Story:** As an administrator, I want to assign and remove tags from users via the API, so that I can control user authorization attributes.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose a REST endpoint that assigns one or more Tags to a specified User
2. THE Auth_Service SHALL expose a REST endpoint that removes one or more Tags from a specified User
3. THE Auth_Service SHALL expose a REST endpoint that retrieves all Tags currently assigned to a specified User
4. WHEN a Tag assignment request references a Tag identifier that does not exist, THE Auth_Service SHALL return an HTTP 404 status code
5. WHEN a Tag assignment request references a User identifier that does not exist, THE Auth_Service SHALL return an HTTP 404 status code
6. WHEN a Tag that is already assigned to a User is assigned again, THE Auth_Service SHALL ignore the duplicate assignment and return a successful response
7. WHEN a User-Tag assignment operation request is received without a valid Access_Token with the ADMIN role, THE Auth_Service SHALL return an HTTP 403 status code

### Requirement 4: Frontend Tag Management

**User Story:** As an administrator, I want a frontend interface for managing tags and assigning them to users, so that I can perform tag operations without using the API directly.

#### Acceptance Criteria

1. THE Frontend_App SHALL display a tag management page listing all existing Tags in a table with columns for Tag_Key, Tag_Value, description, and action buttons
2. THE Frontend_App SHALL provide a form for creating a new Tag with input fields for Tag_Key, Tag_Value, and description
3. THE Frontend_App SHALL provide an inline or modal form for editing the description of an existing Tag
4. THE Frontend_App SHALL provide a delete action for each Tag with a confirmation dialog before deletion
5. THE Frontend_App SHALL display a user detail view that shows all Tags assigned to a specific User
6. THE Frontend_App SHALL provide controls on the user detail view for assigning new Tags to the User and removing existing Tag assignments
7. WHEN the Auth_Service returns an error during a tag operation, THE Frontend_App SHALL display the error message to the administrator
8. THE Frontend_App SHALL restrict access to tag management pages to users with the ADMIN role
9. WHILE a tag operation request is in progress, THE Frontend_App SHALL display a loading indicator and disable the action controls to prevent duplicate submissions

### Requirement 5: Idempotent Seed Data Routine

**User Story:** As a developer, I want an idempotent seed data routine that populates MongoDB with default users, roles, and tags when the database is empty, so that the system is ready to use immediately after a fresh deployment.

#### Acceptance Criteria

1. WHEN the Auth_Service starts and the MongoDB users collection is empty, THE Seed_Data_Routine SHALL create three default users: one with login "user", one with login "admin", and one with login "guest"
2. THE Seed_Data_Routine SHALL set each default user password to match the user login (e.g., user "admin" has password "admin")
3. THE Seed_Data_Routine SHALL hash all default user passwords using the bcrypt algorithm before storing them
4. THE Seed_Data_Routine SHALL assign the role "USER" to the "user" account, the roles "ADMIN" and "USER" to the "admin" account, and the role "GUEST" to the "guest" account
5. THE Seed_Data_Routine SHALL create a set of default Tags including at minimum: `department:engineering`, `department:operations`, `location:HQ`, and `location:remote`
6. THE Seed_Data_Routine SHALL assign the tags `department:engineering` and `location:HQ` to the "admin" user, and the tag `department:operations` to the "user" account
7. WHEN the Auth_Service starts and the MongoDB users collection already contains data, THE Seed_Data_Routine SHALL skip execution without modifying existing records
8. THE Seed_Data_Routine SHALL execute during application startup before the Auth_Service begins accepting HTTP requests
9. IF the Seed_Data_Routine encounters a database error during execution, THEN THE Auth_Service SHALL log the error and continue startup without terminating

### Requirement 6: API Gateway Service — Request Routing

**User Story:** As a developer, I want an API Gateway service that routes all client requests to the appropriate backend microservice, so that the frontend has a single entry point and backend services are not exposed directly.

#### Acceptance Criteria

1. THE Gateway_Service SHALL be a Quarkus-based microservice following the same hexagonal architecture and DDD conventions as the existing services
2. THE Gateway_Service SHALL route requests with the path prefix `/api/v1/auth` to the Auth_Service
3. THE Gateway_Service SHALL route requests with the path prefix `/api/v1/dashboard` to the Dashboard_Service
4. THE Gateway_Service SHALL route requests with the path prefix `/api/v1/tags` to the Auth_Service
5. THE Gateway_Service SHALL preserve the original request path, headers, query parameters, and body when forwarding requests to backend services
6. WHEN the Gateway_Service receives a request for a path that does not match any configured route, THE Gateway_Service SHALL return an HTTP 404 status code with a descriptive error message
7. WHEN a target backend service is unavailable, THE Gateway_Service SHALL return an HTTP 503 status code with a descriptive error message
8. THE Docker_Compose configuration SHALL include the Gateway_Service and configure it as the single externally exposed entry point, replacing direct access to backend services from the Frontend_App

### Requirement 7: API Gateway Service — JWT Validation

**User Story:** As a developer, I want the API Gateway to validate JWT tokens on incoming requests, so that unauthenticated requests are rejected before reaching backend services.

#### Acceptance Criteria

1. WHEN the Gateway_Service receives a request to a protected route, THE Gateway_Service SHALL validate the Access_Token in the Authorization header before forwarding the request
2. WHEN the Gateway_Service receives a request with an expired or invalid Access_Token, THE Gateway_Service SHALL return an HTTP 401 status code without forwarding the request to the backend service
3. WHEN the Gateway_Service receives a request to a public route (login, token refresh), THE Gateway_Service SHALL forward the request without requiring an Access_Token
4. THE Gateway_Service SHALL use the same JWT public key and issuer configuration as the Auth_Service and Dashboard_Service for token validation
5. WHEN the Gateway_Service successfully validates an Access_Token, THE Gateway_Service SHALL forward the original Authorization header to the backend service

### Requirement 8: API Gateway Service — Rate Limiting

**User Story:** As a developer, I want the API Gateway to enforce rate limiting on incoming requests, so that backend services are protected from excessive traffic and abuse.

#### Acceptance Criteria

1. THE Gateway_Service SHALL enforce a configurable maximum number of requests per client per time window
2. WHEN a client exceeds the configured rate limit, THE Gateway_Service SHALL return an HTTP 429 status code with a Retry-After header indicating when the client may retry
3. THE Gateway_Service SHALL identify clients by their source IP address for rate limiting purposes
4. THE Gateway_Service SHALL allow rate limit thresholds and time windows to be configured via environment variables
5. THE Gateway_Service SHALL apply rate limiting before JWT validation and request routing to minimize resource consumption on abusive requests

### Requirement 9: Dynamic API Documentation

**User Story:** As a developer, I want all API services and the API Gateway to expose dynamic OpenAPI documentation, so that API consumers can discover and understand available endpoints without reading source code.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose an OpenAPI_Specification document at a standard documentation endpoint (e.g., `/q/openapi`)
2. THE Dashboard_Service SHALL expose an OpenAPI_Specification document at a standard documentation endpoint
3. THE Gateway_Service SHALL expose an OpenAPI_Specification document at a standard documentation endpoint
4. THE Auth_Service SHALL expose a Swagger UI page for interactive API exploration at a standard UI endpoint (e.g., `/q/swagger-ui`)
5. THE Dashboard_Service SHALL expose a Swagger UI page for interactive API exploration at a standard UI endpoint
6. THE Gateway_Service SHALL expose a Swagger UI page for interactive API exploration at a standard UI endpoint
7. THE OpenAPI_Specification documents SHALL include descriptions, request/response schemas, authentication requirements, and error response codes for all endpoints
8. WHILE the application is running in development mode, THE Swagger UI pages SHALL be accessible without authentication

### Requirement 10: Gateway Service Infrastructure and Containerization

**User Story:** As a developer, I want the Gateway Service containerized and integrated into the Docker Compose stack, so that the entire platform including the gateway can be started with a single command.

#### Acceptance Criteria

1. THE Gateway_Service SHALL include a Dockerfile that produces a container image based on a Java 25 runtime using a multi-stage Maven build
2. THE Docker_Compose configuration SHALL define the Gateway_Service as a service with a configurable external port
3. THE Docker_Compose configuration SHALL configure the Gateway_Service to depend on the Auth_Service and Dashboard_Service
4. THE Docker_Compose configuration SHALL update the Frontend_App to route API requests through the Gateway_Service instead of directly to backend services
5. THE Docker_Compose configuration SHALL use environment variables for all Gateway_Service configurable values including backend service URLs, JWT configuration, and rate limit settings
6. THE Gateway_Service SHALL include a health check endpoint that the Docker_Compose configuration uses for readiness verification

### Requirement 11: Frontend Routing Update for API Gateway

**User Story:** As a developer, I want the frontend application to send all API requests through the API Gateway, so that the frontend communicates with a single backend endpoint.

#### Acceptance Criteria

1. THE Frontend_App SHALL configure the ApiClient to send all API requests to the Gateway_Service base URL instead of directly to individual backend services
2. THE Frontend_App SHALL use a single configurable environment variable for the Gateway_Service base URL
3. WHEN the Gateway_Service returns an HTTP 429 status code, THE Frontend_App SHALL display a notification informing the user that the request rate has been exceeded
4. WHEN the Gateway_Service returns an HTTP 503 status code, THE Frontend_App SHALL display a notification informing the user that the service is temporarily unavailable
