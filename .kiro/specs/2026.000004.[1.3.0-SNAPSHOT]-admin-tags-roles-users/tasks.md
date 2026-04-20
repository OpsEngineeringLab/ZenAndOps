# Implementation Plan: Admin Tags, Roles & Users

## Overview

Implement full CRUD administration for Roles and Users, a self-service profile page, a permission-based policy system (RBAC + ABAC), and corresponding frontend pages. The authorization module uses a dual model: RBAC (Roles with permissions) for action-based access control, and ABAC (Tags with key-value attributes) for contextual attribute-based access control. Both are evaluated by the PolicyEngine and carried in the JWT for client-side evaluation. The implementation follows the existing hexagonal architecture and builds incrementally: domain → ports → use cases → adapters → REST → gateway → frontend.

## Tasks

- [x] 1. Create Role domain entity and domain exceptions
  - Create `Role.java` in `domain/entity/` with fields: id, name, description, permissions (List\<String\>), createdAt, updatedAt, getters and setters
  - Create domain exceptions in `domain/exception/`: `RoleAlreadyExistsException`, `RoleNotFoundException`, `RoleInUseException`, `UserAlreadyExistsException`, `SelfDeletionException`, `InvalidPasswordException`
  - Follow the same mutable class pattern as `Tag.java` and `User.java`
  - _Requirements: 1.1_

- [x] 2. Create RoleRepository port and extend UserRepository port
  - Create `RoleRepository.java` in `application/port/` with methods: save, findById, findByName, findAllByNames, findAll(page, size), count, delete, existsAssignedToAnyUser
  - Extend `UserRepository.java` with new methods: findAll(int page, int size), count(), delete(String id), existsByLogin(String login)
  - _Requirements: 1.1, 2.1, 2.3, 2.8_

- [x] 3. Implement Role CRUD use cases
  - Create `CreateRoleUseCase.java` — validate name uniqueness via RoleRepository.findByName, create and save Role
  - Create `ListRolesUseCase.java` — return PaginatedResult\<Role\> using RoleRepository.findAll(page, size) and count()
  - Create `GetRoleUseCase.java` — find by id or throw RoleNotFoundException
  - Create `UpdateRoleUseCase.java` — find by id, validate name uniqueness if changed, update fields and save
  - Create `DeleteRoleUseCase.java` — check existsAssignedToAnyUser, throw RoleInUseException if assigned, otherwise delete
  - All use cases in `application/usecase/` following existing patterns (constructor injection, @ApplicationScoped)
  - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9_

- [-] 4. Implement User CRUD use cases
  - Create `CreateUserUseCase.java` — validate login uniqueness via existsByLogin, validate role names exist via RoleRepository.findAllByNames, hash password via PasswordEncoder, create and save User
  - Create `ListUsersUseCase.java` — return PaginatedResult\<User\> using UserRepository.findAll(page, size) and count()
  - Create `GetUserUseCase.java` — find by id or throw UserNotFoundException
  - Create `UpdateUserUseCase.java` — find by id, update permitted fields (name, email, active, roles, tagIds), hash password if provided, validate role names exist, save
  - Create `DeleteUserUseCase.java` — accept id and currentUserId, throw SelfDeletionException if equal, find by id or throw UserNotFoundException, delete
  - All use cases in `application/usecase/`
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9_

- [~] 5. Implement Role assignment and Profile use cases
  - Create `AssignRolesToUserUseCase.java` — validate role names exist as Role entities via RoleRepository.findAllByNames (throw RoleNotFoundException if any missing), add to user's roles list ignoring duplicates, save
  - Create `RemoveRolesFromUserUseCase.java` — find user, remove specified role names from user's roles list, save
  - Create `GetProfileUseCase.java` — find user by login (from JWT sub claim), return user data
  - Create `UpdateProfileUseCase.java` — find user by login, update only name and email, save
  - Create `ChangePasswordUseCase.java` — find user by login, verify current password via PasswordEncoder, hash new password, save. Throw InvalidPasswordException if current password is wrong
  - All use cases in `application/usecase/`
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5_

- [~] 6. Checkpoint — Verify domain, ports, and use case layers
  - Ensure all use cases compile and follow existing patterns. Ask the user if questions arise.

- [~] 7. Implement MongoRoleRepository adapter and extend MongoUserRepository
  - Create `RolePanacheEntity.java` in `infrastructure/adapter/persistence/` with MongoDB Panache entity fields matching Role domain entity
  - Create `MongoRoleRepository.java` implementing RoleRepository port, following MongoTagRepository patterns (toEntity/toDomain mappers, ObjectId handling, unique index on `name` at startup)
  - Implement `existsAssignedToAnyUser(roleName)` by querying `UserPanacheEntity.count("roles", roleName) > 0`
  - Extend `MongoUserRepository.java` with: findAll(int page, int size) using Panache pagination, count() via UserPanacheEntity.count(), delete(String id) via UserPanacheEntity.deleteById, existsByLogin(String login) via UserPanacheEntity.count("login", login) > 0
  - _Requirements: 1.1, 1.9, 2.1, 2.3, 2.8_

- [~] 8. Modify JwtTokenProvider to include RBAC permissions claim
  - Update `JwtTokenProvider.generateAccessToken` signature to accept `List<Role> resolvedRoles` (or resolve internally)
  - Aggregate unique RBAC permission strings from all resolved Role entities
  - Add `.claim("permissions", permissionsList)` to the JWT builder (RBAC layer)
  - Ensure existing `tags` claim continues to be included (ABAC layer) — the JWT carries both RBAC permissions and ABAC tag attributes for client-side dual evaluation
  - Update `TokenProvider` port interface if signature changes
  - Update all callers of generateAccessToken (LoginUseCase, RefreshTokenUseCase) to resolve roles via RoleRepository.findAllByNames and pass them
  - _Requirements: 5.2, 5.5_

- [~] 9. Update SeedDataService with default Role entities
  - Inject RoleRepository into SeedDataService
  - Create default Role entities before creating users: ADMIN (all permissions), USER (profile:read, profile:write, dashboard:read), GUEST (dashboard:read)
  - Use idempotent creation pattern (check findByName before creating)
  - Keep existing user creation logic unchanged — users already reference role names as strings
  - _Requirements: 5.6_

- [~] 10. Extend AuthExceptionMapper with new exception mappings
  - Add mappings: RoleAlreadyExistsException → 409 (ROLE_ALREADY_EXISTS), RoleNotFoundException → 404 (ROLE_NOT_FOUND), RoleInUseException → 409 (ROLE_IN_USE), UserAlreadyExistsException → 409 (USER_ALREADY_EXISTS), SelfDeletionException → 409 (USER_SELF_DELETION), InvalidPasswordException → 401 (AUTH_INVALID_PASSWORD)
  - Follow existing if-instanceof pattern in AuthExceptionMapper
  - _Requirements: 1.3, 1.6, 1.9, 2.2, 2.5, 2.9, 4.5_

- [~] 11. Create REST DTOs for Role, User, Profile, and Role assignment
  - Create in `infrastructure/rest/dto/`: CreateRoleRequest, UpdateRoleRequest, RoleResponse, PaginatedRolesResponse, CreateUserRequest, UpdateUserRequest, UserResponse, PaginatedUsersResponse, ProfileResponse, UpdateProfileRequest, ChangePasswordRequest, UserRolesRequest
  - Follow existing record pattern (e.g., TagResponse, CreateTagRequest)
  - UserResponse must NOT include passwordHash field
  - _Requirements: 2.1, 2.3, 2.4, 2.6, 4.1_

- [~] 12. Create RoleResource REST endpoint
  - Create `RoleResource.java` at `/api/v1/roles` with @RolesAllowed("ADMIN")
  - Implement: POST (create, 201), GET (list paginated), GET /{id} (get by id), PUT /{id} (update), DELETE /{id} (delete, 204)
  - Follow TagResource patterns for OpenAPI annotations, pagination query params, and response mapping
  - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_

- [~] 13. Create UserResource REST endpoint
  - Create `UserResource.java` at `/api/v1/users` with @RolesAllowed("ADMIN")
  - Implement: POST (create, 201), GET (list paginated), GET /{id} (get by id), PUT /{id} (update), DELETE /{id} (delete, 204)
  - Pass SecurityContext principal name as currentUserId to DeleteUserUseCase for self-deletion prevention
  - Never expose passwordHash in responses
  - Follow TagResource patterns for OpenAPI annotations
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10_

- [~] 14. Create UserRoleResource and ProfileResource REST endpoints
  - Create `UserRoleResource.java` at `/api/v1/users/{userId}/roles` with @RolesAllowed("ADMIN"): POST (assign roles), DELETE (remove roles)
  - Create `ProfileResource.java` at `/api/v1/profile` with @Authenticated: GET (get profile), PUT (update name/email), POST /password (change password)
  - ProfileResource extracts current user login from SecurityContext/JWT sub claim
  - Follow existing UserTagResource pattern for sub-resource structure
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [~] 15. Add gateway routes for /api/v1/roles and /api/v1/profile
  - Add `new RouteDefinition("/api/v1/roles", authServiceUrl, true)` to ConfigRouteResolver.init()
  - Add `new RouteDefinition("/api/v1/profile", authServiceUrl, true)` to ConfigRouteResolver.init()
  - /api/v1/users route already exists — no change needed
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [~] 16. Checkpoint — Verify full backend and gateway compilation
  - Ensure all backend services compile. Ensure gateway routes are correctly defined. Ask the user if questions arise.

- [~] 17. Update frontend AuthContext with RBAC permissions claim
  - Add `permissions: string[]` to the `JwtClaims` interface in `AuthContext.tsx` (RBAC layer)
  - The existing `tags` field already carries ABAC attributes — both are now available for client-side dual RBAC + ABAC evaluation via `useIsAuthorized({ roles, attributes })`
  - _Requirements: 5.5, 5.8, 9.4_

- [~] 18. Create frontend Role Management page with hooks and modals
  - Create `hooks/useRoleApi.ts` with: createRole, listRoles, getRole, updateRole, deleteRole — following useTagApi pattern
  - Create `pages/RoleManagement.tsx` with paginated table (name, description, permission count), create/edit modal (name, description, permission multi-select), delete confirmation modal
  - Create `components/roles/RoleFormModal.tsx` and `components/roles/RoleDeleteConfirmModal.tsx` following TagFormModal/TagDeleteConfirmModal patterns
  - Follow `.frontend-template` design patterns for table, modals, buttons, and page structure
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.8_

- [~] 19. Create frontend User Management page with hooks and modals
  - Create `hooks/useUserApi.ts` with: createUser, listUsers, getUser, updateUser, deleteUser, assignRoles, removeRoles — following useTagApi pattern
  - Create `pages/UserManagement.tsx` with paginated table (name, login, email, roles as badges, active status badge, creation date), create modal (login, name, email, password, role multi-select, tag multi-select), edit modal (name, email, password optional, roles, tags, active toggle), delete confirmation modal
  - Create `components/users/UserFormModal.tsx` and `components/users/UserDeleteConfirmModal.tsx` following existing modal patterns
  - Follow `.frontend-template` design patterns
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.8_

- [~] 20. Create frontend Profile page with hooks
  - Create `hooks/useProfileApi.ts` with: getProfile, updateProfile, changePassword — following useTagApi pattern
  - Create `pages/Profile.tsx` displaying: login (read-only), name (editable), email (editable), roles (read-only badges), tags (read-only badges)
  - Add password change section: current password, new password, confirm password with client-side match validation
  - Follow `.frontend-template` design patterns for form layout
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.8_

- [~] 21. Update frontend sidebar navigation and App routing
  - Add new nav items to `AppSidebar.tsx` navItems array: "Role Management" (/roles, adminOnly: true), "User Management" (/users, adminOnly: true), "Profile" (/profile, no adminOnly)
  - Add appropriate icons (check `.frontend-template/src/icons/` for ShieldIcon, UsersIcon, UserIcon or create them)
  - Add new routes in `App.tsx`: /roles with Authorize roles={["ADMIN"]}, /users with Authorize roles={["ADMIN"]}, /profile (no role restriction, just ProtectedRoute)
  - Ensure non-ADMIN users are redirected to home when accessing /roles or /users
  - _Requirements: 6.1, 6.7, 7.1, 7.7, 8.1, 8.7, 9.1, 9.2, 9.3, 9.4, 9.5_

- [~] 22. Checkpoint — Verify full frontend compilation and routing
  - Ensure frontend builds without errors. Verify all new routes, pages, hooks, and components are wired together. Ask the user if questions arise.

- [~] 23. Version control and release
  - [ ] Ensure all previous tasks are complete and tests pass
  - [ ] Remove SNAPSHOT suffix from all version references in the codebase
  - [ ] Commit the version bump: "release: 1.3.0 - admin-tags-roles-users"
  - [ ] Merge branch into main/master
  - [ ] Apply Git tag: 1.3.0 (without SNAPSHOT)
  - [ ] Push branch, merge, and tag to remote

## Notes

- No tests or observability are required for this delivery per project constraints
- All frontend pages follow `.frontend-template` design patterns
- Existing Tag CRUD and User-Tag assignment functionality remains unchanged
- The authorization module uses a dual RBAC + ABAC model: RBAC (Roles with permissions) for action-based access, ABAC (Tags with key-value attributes) for contextual access. Both are evaluated by the PolicyEngine and carried in the JWT (permissions + tags claims)
- The permission system is additive — existing `@RolesAllowed("ADMIN")` annotations remain in place as part of the RBAC layer
- The ABAC layer (Tag-based evaluation via PolicyEngine.evaluateAbac) is already implemented and unchanged by this feature
- The frontend `useIsAuthorized` hook supports combined RBAC + ABAC checks via `{ roles, attributes }` parameters
- Checkpoints ensure incremental validation across backend and frontend layers
