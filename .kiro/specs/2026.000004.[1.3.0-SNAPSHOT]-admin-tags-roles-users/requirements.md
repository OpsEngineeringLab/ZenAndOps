# Requirements Document

## Introduction

This feature introduces full CRUD administration for Tags, Roles, and Users in the ZenAndOps platform. The platform's authorization module is built on a **dual RBAC + ABAC model**: Role-Based Access Control (RBAC) governs what actions a user can perform based on their assigned Roles and the permissions those Roles carry, while Attribute-Based Access Control (ABAC) governs access based on Tag key-value attributes assigned to users and matched against policy rules. Both models coexist and are evaluated by the Policy_Engine — RBAC for permission-based endpoint access, and ABAC for attribute-based contextual access decisions.

Currently, Tag CRUD and User-Tag assignment exist but are restricted to ADMIN role. Roles are stored as plain strings in the User entity with no dedicated Role entity or management capability. No User management REST endpoints or UI exist beyond authentication (login/logoff/refresh). This feature adds a proper Role entity with CRUD (strengthening the RBAC layer), User management endpoints and UI, a self-service profile page for non-admin users, and a flexible permission-based policy system that replaces hardcoded role checks with configurable permissions on Role entities. Tag management (the ABAC layer) is already implemented and remains unchanged. All frontend pages follow the `.frontend-template` design patterns.

## Glossary

- **Auth_Service**: The backend microservice responsible for authentication, authorization (RBAC + ABAC), user management, tag management, and role management. Runs on Quarkus with MongoDB (Panache).
- **Frontend_App**: The React 19 + TypeScript + Tailwind CSS 4 single-page application that provides the user interface for the platform.
- **Gateway**: The API Gateway service that validates JWT tokens and routes requests to backend services.
- **RBAC (Role-Based Access Control)**: Authorization model where access is determined by the Roles assigned to a User and the Permissions those Roles carry. Evaluated by the Policy_Engine via RbacPolicy rules.
- **ABAC (Attribute-Based Access Control)**: Authorization model where access is determined by Tag key-value attributes assigned to a User, matched against policy rules that define required user attributes, resource attributes, and environment conditions. Evaluated by the Policy_Engine via AbacPolicy rules.
- **User**: A domain entity representing an authenticated person in the system, with fields: id, login, name, email, passwordHash, roles (for RBAC), tagIds (for ABAC), active, createdAt, updatedAt.
- **Role**: A domain entity representing a named authorization role with associated permissions (RBAC layer). Contains: id, name, description, permissions, createdAt, updatedAt.
- **Tag**: A domain entity representing a key-value pair used for Attribute-Based Access Control (ABAC layer). Contains: id, key, value, description, createdAt, updatedAt. Tags are assigned to Users and matched against AbacPolicy rules.
- **Permission**: A string identifier representing a granular action that can be performed on a resource (e.g., "users:read", "roles:write"). Permissions are stored on Role entities and aggregated into the JWT for RBAC evaluation.
- **Policy_Engine**: The component that evaluates both RBAC and ABAC authorization rules. For RBAC, it checks the User's Roles against RbacPolicy rules. For ABAC, it resolves the User's Tags and matches them against AbacPolicy rules. Both evaluations can be combined for fine-grained access control.
- **ADMIN**: A role granting full access to administer Tags, Roles, and Users.
- **USER**: A role granting access to view and edit the own user profile, including password change.
- **GUEST**: A role granting read-only access with no ability to modify data.
- **JWT**: JSON Web Token used for authentication, containing claims for roles, tags, and user identity.
- **Profile_Page**: A frontend page where authenticated users can view and edit their own profile information.
- **CRUD**: Create, Read, Update, Delete operations on a resource.

## Requirements

### Requirement 1: Role Entity and CRUD

**User Story:** As an administrator, I want to manage roles as first-class entities with names, descriptions, and permissions, so that I can define granular access control beyond hardcoded role strings.

#### Acceptance Criteria

1. THE Auth_Service SHALL store Role entities in MongoDB with the fields: id, name, description, permissions (list of permission strings), createdAt, and updatedAt.
2. WHEN an ADMIN user sends a POST request to the Role creation endpoint with a valid name and description, THE Auth_Service SHALL create a new Role entity and return it with HTTP status 201.
3. WHEN an ADMIN user sends a POST request to the Role creation endpoint with a name that already exists, THE Auth_Service SHALL reject the request with HTTP status 409 and a descriptive error message.
4. WHEN an ADMIN user sends a GET request to the Role list endpoint, THE Auth_Service SHALL return a paginated list of all Role entities.
5. WHEN an ADMIN user sends a GET request to the Role detail endpoint with a valid Role id, THE Auth_Service SHALL return the corresponding Role entity.
6. WHEN an ADMIN user sends a GET request to the Role detail endpoint with a non-existent Role id, THE Auth_Service SHALL return HTTP status 404 with a descriptive error message.
7. WHEN an ADMIN user sends a PUT request to the Role update endpoint with a valid Role id and updated fields, THE Auth_Service SHALL update the Role entity and return the updated version.
8. WHEN an ADMIN user sends a DELETE request to the Role delete endpoint with a valid Role id, THE Auth_Service SHALL delete the Role entity and return HTTP status 204.
9. WHEN an ADMIN user sends a DELETE request to the Role delete endpoint for a Role that is assigned to at least one User, THE Auth_Service SHALL reject the request with HTTP status 409 and a descriptive error message.
10. WHEN a non-ADMIN user sends any request to the Role CRUD endpoints, THE Auth_Service SHALL reject the request with HTTP status 403.

### Requirement 2: User CRUD Administration

**User Story:** As an administrator, I want to create, list, view, update, and delete users, so that I can manage the platform's user base.

#### Acceptance Criteria

1. WHEN an ADMIN user sends a POST request to the User creation endpoint with valid login, name, email, password, roles, and tagIds, THE Auth_Service SHALL create a new User entity with a hashed password and return it (without passwordHash) with HTTP status 201.
2. WHEN an ADMIN user sends a POST request to the User creation endpoint with a login that already exists, THE Auth_Service SHALL reject the request with HTTP status 409 and a descriptive error message.
3. WHEN an ADMIN user sends a GET request to the User list endpoint, THE Auth_Service SHALL return a paginated list of all User entities without exposing passwordHash fields.
4. WHEN an ADMIN user sends a GET request to the User detail endpoint with a valid User id, THE Auth_Service SHALL return the corresponding User entity without the passwordHash field.
5. WHEN an ADMIN user sends a GET request to the User detail endpoint with a non-existent User id, THE Auth_Service SHALL return HTTP status 404 with a descriptive error message.
6. WHEN an ADMIN user sends a PUT request to the User update endpoint with a valid User id and updated fields (name, email, active, roles, tagIds), THE Auth_Service SHALL update the User entity and return the updated version without the passwordHash field.
7. WHEN an ADMIN user sends a PUT request to the User update endpoint with a password field, THE Auth_Service SHALL hash the new password before storing it.
8. WHEN an ADMIN user sends a DELETE request to the User delete endpoint with a valid User id, THE Auth_Service SHALL delete the User entity and return HTTP status 204.
9. WHEN an ADMIN user sends a DELETE request to the User delete endpoint for the currently authenticated User, THE Auth_Service SHALL reject the request with HTTP status 409 and a descriptive error message indicating self-deletion is not allowed.
10. WHEN a non-ADMIN user sends any request to the User CRUD endpoints, THE Auth_Service SHALL reject the request with HTTP status 403.

### Requirement 3: Role Assignment to Users

**User Story:** As an administrator, I want to assign and remove roles from users, so that I can control what each user is authorized to do.

#### Acceptance Criteria

1. WHEN an ADMIN user sends a POST request to the User Role assignment endpoint with a valid User id and a list of Role names, THE Auth_Service SHALL add the specified roles to the User's roles list and return the updated User.
2. WHEN an ADMIN user sends a DELETE request to the User Role removal endpoint with a valid User id and a list of Role names, THE Auth_Service SHALL remove the specified roles from the User's roles list and return the updated User.
3. WHEN an ADMIN user assigns a Role name that does not correspond to an existing Role entity, THE Auth_Service SHALL reject the request with HTTP status 404 and a descriptive error message.
4. WHEN an ADMIN user assigns a Role that is already assigned to the User, THE Auth_Service SHALL ignore the duplicate and complete the operation without error.
5. WHEN a non-ADMIN user sends any request to the User Role assignment endpoints, THE Auth_Service SHALL reject the request with HTTP status 403.

### Requirement 4: User Self-Service Profile

**User Story:** As a regular user, I want to view and edit my own profile and change my password, so that I can keep my information up to date without needing an administrator.

#### Acceptance Criteria

1. WHEN an authenticated user sends a GET request to the profile endpoint, THE Auth_Service SHALL return the current User's profile data (name, email, login, roles, tags) without the passwordHash field.
2. WHEN an authenticated user sends a PUT request to the profile endpoint with updated name and email fields, THE Auth_Service SHALL update only the name and email of the current User and return the updated profile.
3. WHEN an authenticated user sends a PUT request to the profile endpoint attempting to change roles, tagIds, login, or active status, THE Auth_Service SHALL ignore those fields and update only the permitted fields (name, email).
4. WHEN an authenticated user sends a POST request to the password change endpoint with a valid current password and a new password, THE Auth_Service SHALL verify the current password, hash the new password, update the User entity, and return HTTP status 204.
5. WHEN an authenticated user sends a POST request to the password change endpoint with an incorrect current password, THE Auth_Service SHALL reject the request with HTTP status 401 and a descriptive error message.
6. WHEN an unauthenticated request is sent to the profile or password change endpoints, THE Auth_Service SHALL reject the request with HTTP status 401.

### Requirement 5: Permission-Based Policy System (RBAC + ABAC)

**User Story:** As an administrator, I want a flexible permission-based policy system that combines RBAC and ABAC, so that access control can be configured through Role permissions (RBAC) and Tag attributes (ABAC) rather than hardcoded role name checks.

#### Acceptance Criteria

1. THE Auth_Service SHALL define a standard set of RBAC permission strings following the pattern "resource:action" (e.g., "users:read", "users:write", "roles:read", "roles:write", "tags:read", "tags:write").
2. THE Auth_Service SHALL evaluate endpoint access using RBAC by checking whether the authenticated User holds at least one Role that contains the required permission for the requested resource and action.
3. WHEN a User holds a Role with the required permission (RBAC), THE Auth_Service SHALL allow the request to proceed.
4. WHEN a User holds no Role with the required permission (RBAC), THE Auth_Service SHALL reject the request with HTTP status 403.
5. THE Auth_Service SHALL include the User's aggregated RBAC permissions and ABAC tag attributes in the JWT access token claims so that the Frontend_App can evaluate both RBAC and ABAC policies client-side.
6. WHILE the seed data routine runs on a fresh deployment, THE Auth_Service SHALL create default Role entities (ADMIN with all permissions, USER with "profile:read" and "profile:write" permissions, GUEST with read-only permissions) and assign them to the corresponding seed users.
7. THE Policy_Engine SHALL continue to support ABAC evaluation based on Tag attributes in addition to the permission-based RBAC evaluation. Both RBAC and ABAC evaluations are independent and can be combined: RBAC determines what actions are allowed based on Roles, while ABAC determines contextual access based on Tag attributes.
8. THE Frontend_App SHALL use the existing useIsAuthorized hook to evaluate both RBAC (via roles/permissions) and ABAC (via tag attributes) for conditional rendering and route protection.

### Requirement 6: Frontend Role Management Page

**User Story:** As an administrator, I want a Role management page in the frontend, so that I can create, view, edit, and delete roles through the UI.

#### Acceptance Criteria

1. THE Frontend_App SHALL provide a Role management page accessible at the "/roles" route.
2. WHEN an ADMIN user navigates to the Role management page, THE Frontend_App SHALL display a paginated table listing all roles with their name, description, and permission count.
3. WHEN an ADMIN user clicks the create button on the Role management page, THE Frontend_App SHALL display a modal form with fields for name, description, and a permission selector.
4. WHEN an ADMIN user submits the Role creation form with valid data, THE Frontend_App SHALL send a creation request to the Auth_Service and refresh the role list upon success.
5. WHEN an ADMIN user clicks the edit button for a role, THE Frontend_App SHALL display a modal form pre-filled with the role's current data.
6. WHEN an ADMIN user clicks the delete button for a role, THE Frontend_App SHALL display a confirmation modal before sending the delete request.
7. WHEN a non-ADMIN user attempts to access the "/roles" route, THE Frontend_App SHALL redirect the user to the home page.
8. THE Frontend_App SHALL follow the `.frontend-template` design patterns for the Role management page, including table layout, modal forms, buttons, and page structure.

### Requirement 7: Frontend User Management Page

**User Story:** As an administrator, I want a User management page in the frontend, so that I can create, view, edit, and delete users through the UI.

#### Acceptance Criteria

1. THE Frontend_App SHALL provide a User management page accessible at the "/users" route.
2. WHEN an ADMIN user navigates to the User management page, THE Frontend_App SHALL display a paginated table listing all users with their name, login, email, roles, active status, and creation date.
3. WHEN an ADMIN user clicks the create button on the User management page, THE Frontend_App SHALL display a modal form with fields for login, name, email, password, role selection, and tag selection.
4. WHEN an ADMIN user submits the User creation form with valid data, THE Frontend_App SHALL send a creation request to the Auth_Service and refresh the user list upon success.
5. WHEN an ADMIN user clicks the edit button for a user, THE Frontend_App SHALL display a modal form pre-filled with the user's current data, allowing modification of name, email, password (optional), roles, tags, and active status.
6. WHEN an ADMIN user clicks the delete button for a user, THE Frontend_App SHALL display a confirmation modal before sending the delete request.
7. WHEN a non-ADMIN user attempts to access the "/users" route, THE Frontend_App SHALL redirect the user to the home page.
8. THE Frontend_App SHALL follow the `.frontend-template` design patterns for the User management page, including table layout, modal forms, buttons, and page structure.

### Requirement 8: Frontend Profile Page

**User Story:** As an authenticated user, I want a profile page where I can view my information and change my password, so that I can manage my own account without administrator help.

#### Acceptance Criteria

1. THE Frontend_App SHALL provide a Profile page accessible at the "/profile" route.
2. WHEN an authenticated user navigates to the Profile page, THE Frontend_App SHALL display the user's current name, email, login, assigned roles (read-only), and assigned tags (read-only).
3. WHEN an authenticated user edits the name or email fields and submits the form, THE Frontend_App SHALL send an update request to the Auth_Service profile endpoint and display a success notification upon completion.
4. THE Frontend_App SHALL provide a password change section on the Profile page with fields for current password, new password, and password confirmation.
5. WHEN an authenticated user submits the password change form with matching new password and confirmation, THE Frontend_App SHALL send a password change request to the Auth_Service and display a success notification upon completion.
6. WHEN the new password and confirmation fields do not match, THE Frontend_App SHALL display a client-side validation error and prevent form submission.
7. WHEN an unauthenticated user attempts to access the "/profile" route, THE Frontend_App SHALL redirect the user to the login page.
8. THE Frontend_App SHALL follow the `.frontend-template` design patterns for the Profile page layout and form elements.

### Requirement 9: Frontend Navigation and Authorization

**User Story:** As a user, I want the sidebar navigation to reflect my permissions, so that I only see menu items I am authorized to access.

#### Acceptance Criteria

1. THE Frontend_App SHALL display "Tags", "Roles", and "Users" menu items in the sidebar navigation for users with the ADMIN role.
2. THE Frontend_App SHALL display a "Profile" menu item in the sidebar navigation for all authenticated users.
3. WHEN a user with the GUEST role is authenticated, THE Frontend_App SHALL display only the Dashboard and Profile menu items in the sidebar.
4. THE Frontend_App SHALL use the existing Authorize component and useAuthorization hooks to conditionally render navigation items based on the user's JWT claims.
5. WHEN a user navigates directly to a route for which the user lacks authorization, THE Frontend_App SHALL redirect the user to the home page.

### Requirement 10: Gateway Routing for New Endpoints

**User Story:** As a developer, I want the API Gateway to route requests for the new Role and User management endpoints to the Auth_Service, so that the frontend can access all backend functionality through a single entry point.

#### Acceptance Criteria

1. WHEN the Gateway receives a request matching the path pattern "/api/v1/roles/**", THE Gateway SHALL forward the request to the Auth_Service.
2. WHEN the Gateway receives a request matching the path pattern "/api/v1/users/**", THE Gateway SHALL forward the request to the Auth_Service.
3. WHEN the Gateway receives a request matching the path pattern "/api/v1/profile/**", THE Gateway SHALL forward the request to the Auth_Service.
4. THE Gateway SHALL validate the JWT token on all new routes before forwarding requests to the Auth_Service.
