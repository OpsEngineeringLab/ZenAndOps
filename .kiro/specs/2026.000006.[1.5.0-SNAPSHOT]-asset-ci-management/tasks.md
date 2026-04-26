# Implementation Plan: Asset & CI Management System

## Overview

This plan implements the CMDB Service (`cmdb-service`) for the ZenAndOps ITSM platform, following hexagonal architecture with DDD on Java 25 / Quarkus 3.33.x / MongoDB. It covers the full backend (domain, application, infrastructure layers), Docker Compose integration, gateway routing, Kafka event publishing, and the frontend pages for organization, service, asset, CI, impact analysis, and file import management.

## Tasks

- [x] 1. Scaffold cmdb-service project and configure dependencies
  - [x] 1.1 Create `cmdb-service/` Maven project with Quarkus 3.33.x, Java 25, and hexagonal package structure: `com.zenandops.cmdb.domain`, `com.zenandops.cmdb.application`, `com.zenandops.cmdb.infrastructure`
    - Add Quarkus dependencies: `quarkus-rest-jackson`, `quarkus-mongodb-panache`, `quarkus-smallrye-jwt`, `quarkus-smallrye-reactive-messaging-kafka`, `quarkus-opentelemetry`, `quarkus-micrometer-opentelemetry`
    - Add test dependency: `jqwik` 1.9.3
    - _Requirements: 15.1, 15.4_
  - [x] 1.2 Configure `application.properties` for MongoDB connection (`zenandops-cmdb` database), Kafka broker, JWT validation (shared secret/issuer with auth-service), OTel exporter, and service port 8083
    - Include `CMDB_IMPACT_ANALYSIS_MAX_DEPTH` configuration property with default value 10
    - _Requirements: 15.3, 17.4, 17.5_
  - [x] 1.3 Create Dockerfile for cmdb-service (Java 25 runtime, multi-stage build with Maven), consistent with existing backend services
    - _Requirements: 17.1_

- [x] 2. Implement domain layer — entities, value objects, and exceptions
  - [x] 2.1 Create all domain entities: `Organization`, `Service`, `ServiceDependency`, `Asset`, `AssetVersion`, `CI`, `CIVersion`, `CIRelationship`, `ServiceCI`, `DataSource`, `FileImportRecord`, `ReconciliationRecord`
    - Each entity with fields as specified in the design data models
    - _Requirements: 1.2, 2.2, 3.2, 4.2, 5.2, 6.2, 7.2, 8.2, 9.1, 10.2, 11.1, 12.1_
  - [x] 2.2 Create all value objects and enums: `OrganizationType`, `ServiceType`, `ServiceStatus`, `DependencyType`, `AssetType`, `AssetStatus`, `CostType`, `CIType`, `CIStatus`, `RelationshipType`, `DataOrigin`, `DataSourceType`, `CriticalityLevel`, `ImportStatus`
    - _Requirements: 15.1_
  - [x] 2.3 Create all domain exceptions: `OrganizationNotFoundException`, `OrganizationInUseException`, `DuplicateRootOrganizationException`, `DuplicateSiblingNameException`, `ServiceNotFoundException`, `ServiceInUseException`, `AssetNotFoundException`, `AssetInUseException`, `CINotFoundException`, `CIInUseException`, `DataSourceNotFoundException`, `DataSourceInUseException`, `DuplicateDataSourceNameException`, `DuplicateDependencyException`, `SelfReferenceException`, `DuplicateServiceCIException`, `LastServiceAssociationException`, `ImmutableVersionException`, `InvalidReliabilityRatingException`, `InvalidFileFormatException`
    - _Requirements: 15.1_

- [x] 3. Implement application layer — repository ports and outbound ports
  - [x] 3.1 Define all 12 repository port interfaces: `OrganizationRepository`, `ServiceRepository`, `ServiceDependencyRepository`, `AssetRepository`, `AssetVersionRepository`, `CIRepository`, `CIVersionRepository`, `CIRelationshipRepository`, `ServiceCIRepository`, `DataSourceRepository`, `FileImportRecordRepository`, `ReconciliationRecordRepository`
    - _Requirements: 15.2_
  - [x] 3.2 Define `CmdbEventPublisher` outbound port interface for Kafka event publishing with methods for asset, CI, service, and version change events
    - _Requirements: 15.2, 18.1, 18.2, 18.3, 18.4_

- [x] 4. Implement Organization use cases and REST resource
  - [x] 4.1 Implement `CreateOrganizationUseCase`, `GetOrganizationUseCase`, `UpdateOrganizationUseCase`, `ListOrganizationsUseCase`, `DeleteOrganizationUseCase`, `GetOrganizationTreeUseCase`
    - Enforce single ROOT invariant, parent existence validation, sibling name uniqueness, deletion protection for entities with children/services/assets
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8_
  - [x] 4.2 Create `OrganizationResource` REST resource at `/api/v1/cmdb/organizations` with all endpoints (POST, GET list, GET by ID, PUT, DELETE, GET tree) and role-based authorization (ADMIN/OPERATOR for writes, authenticated for reads)
    - _Requirements: 1.1, 16.3, 16.4_
  - [ ]* 4.3 Write property test for Organization CRUD round-trip
    - **Property 1: Organization CRUD Round-Trip**
    - **Validates: Requirements 1.1, 1.2**
  - [ ]* 4.4 Write property test for single ROOT Organization invariant
    - **Property 11: Single ROOT Organization Invariant**
    - **Validates: Requirements 1.4, 1.5**
  - [ ]* 4.5 Write property test for hierarchy tree completeness (Organization)
    - **Property 17: Hierarchy Tree Completeness**
    - **Validates: Requirements 1.7**

- [x] 5. Implement Service use cases and REST resource
  - [x] 5.1 Implement `CreateServiceUseCase`, `GetServiceUseCase`, `UpdateServiceUseCase`, `ListServicesUseCase`, `DeleteServiceUseCase`, `GetServiceTreeUseCase`
    - Enforce organization and parent existence validation, business/technical owner requirement, deletion protection, filtering by org/type/criticality/status
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_
  - [x] 5.2 Create `ServiceResource` REST resource at `/api/v1/cmdb/services` with all endpoints and role-based authorization
    - _Requirements: 2.1, 16.3, 16.4_
  - [ ]* 5.3 Write property test for Service CRUD round-trip
    - **Property 2: Service CRUD Round-Trip**
    - **Validates: Requirements 2.1, 2.2**
  - [ ]* 5.4 Write property test for service owner enforcement
    - **Property 12: Service Owner Enforcement**
    - **Validates: Requirements 2.5**

- [x] 6. Implement ServiceDependency use cases and REST resource
  - [x] 6.1 Implement `CreateServiceDependencyUseCase`, `DeleteServiceDependencyUseCase`, `ListServiceDependenciesUseCase`
    - Enforce both services exist, prevent self-reference and duplicates, log CRITICAL warning
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - [x] 6.2 Create `ServiceDependencyResource` REST resource at `/api/v1/cmdb/service-dependencies` with all endpoints and role-based authorization
    - _Requirements: 3.1, 16.3, 16.4_
  - [ ]* 6.3 Write property test for self-reference prevention (ServiceDependency)
    - **Property 7: Self-Reference Prevention**
    - **Validates: Requirements 3.4**
  - [ ]* 6.4 Write property test for relationship uniqueness enforcement (ServiceDependency)
    - **Property 9: Relationship Uniqueness Enforcement**
    - **Validates: Requirements 3.5**

- [x] 7. Checkpoint — Verify organization, service, and dependency functionality
  - Ensure all tests pass, ask the user if questions arise.


- [ ] 8. Implement Asset use cases and REST resource
  - [-] 8.1 Implement `CreateAssetUseCase`, `GetAssetUseCase`, `UpdateAssetUseCase`, `ListAssetsUseCase`, `DeleteAssetUseCase`, `GetAssetCostSummaryUseCase`
    - Enforce organization existence validation, deletion protection for assets with CIs or active versions, filtering by org/type/cost type/status/supplier, cost summary grouped by org and cost type
    - Publish asset change events via `CmdbEventPublisher`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 18.1_
  - [~] 8.2 Create `AssetResource` REST resource at `/api/v1/cmdb/assets` with all endpoints (POST, GET list, GET by ID, PUT, DELETE, GET cost-summary) and role-based authorization
    - _Requirements: 4.1, 16.3, 16.4_
  - [ ]* 8.3 Write property test for Asset CRUD round-trip
    - **Property 3: Asset CRUD Round-Trip**
    - **Validates: Requirements 4.1, 4.2**
  - [ ]* 8.4 Write property test for asset cost summary accuracy
    - **Property 22: Asset Cost Summary Accuracy**
    - **Validates: Requirements 4.6**

- [ ] 9. Implement AssetVersion use cases and REST resource
  - [~] 9.1 Implement `CreateAssetVersionUseCase` and `ListAssetVersionsUseCase`
    - Validate asset and data source exist, auto-assign sequential version number, close previous active version (set endDate), publish version created event
    - Enforce immutability — reject update and delete operations on existing versions
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 18.4_
  - [~] 9.2 Create `AssetVersionResource` REST resource at `/api/v1/cmdb/assets/{assetId}/versions` with POST and GET endpoints and role-based authorization
    - _Requirements: 5.1, 16.3, 16.4_
  - [ ]* 9.3 Write property test for version immutability (AssetVersion)
    - **Property 13: Version Immutability**
    - **Validates: Requirements 5.5**
  - [ ]* 9.4 Write property test for version auto-increment (AssetVersion)
    - **Property 14: Version Auto-Increment**
    - **Validates: Requirements 5.4**
  - [ ]* 9.5 Write property test for previous version closure (AssetVersion)
    - **Property 15: Previous Version Closure**
    - **Validates: Requirements 5.6**
  - [ ]* 9.6 Write property test for version history ordering (AssetVersion)
    - **Property 16: Version History Ordering**
    - **Validates: Requirements 5.8**

- [ ] 10. Implement CI use cases and REST resource
  - [~] 10.1 Implement `CreateCIUseCase`, `GetCIUseCase`, `UpdateCIUseCase`, `ListCIsUseCase`, `DeleteCIUseCase`
    - Validate organization and optional asset exist, enforce deletion protection for CIs with versions/relationships/service associations, enforce service linkage requirement (unless controlled exception flag), filtering by org/type/status/asset
    - Publish CI change events via `CmdbEventPublisher`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 18.2_
  - [~] 10.2 Create `CIResource` REST resource at `/api/v1/cmdb/cis` with all endpoints and role-based authorization
    - _Requirements: 6.1, 16.3, 16.4_
  - [ ]* 10.3 Write property test for CI CRUD round-trip
    - **Property 4: CI CRUD Round-Trip**
    - **Validates: Requirements 6.1, 6.2**

- [ ] 11. Implement CIVersion use cases and REST resource
  - [~] 11.1 Implement `CreateCIVersionUseCase` and `ListCIVersionsUseCase`
    - Validate CI and data source exist, auto-assign sequential version number, close previous active version, publish version created event
    - Enforce immutability — reject update and delete operations on existing versions
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 18.4_
  - [~] 11.2 Create `CIVersionResource` REST resource at `/api/v1/cmdb/cis/{ciId}/versions` with POST and GET endpoints and role-based authorization
    - _Requirements: 7.1, 16.3, 16.4_

- [ ] 12. Implement CIRelationship use cases and REST resource
  - [~] 12.1 Implement `CreateCIRelationshipUseCase`, `DeleteCIRelationshipUseCase`, `ListCIRelationshipsUseCase`
    - Validate both CIs exist, prevent self-reference and duplicates, list upstream and downstream relationships
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_
  - [~] 12.2 Create `CIRelationshipResource` REST resource at `/api/v1/cmdb/ci-relationships` with all endpoints and role-based authorization
    - _Requirements: 8.1, 16.3, 16.4_

- [ ] 13. Implement ServiceCI use cases and REST resource
  - [~] 13.1 Implement `CreateServiceCIUseCase`, `DeleteServiceCIUseCase`, `ListCIsByServiceUseCase`, `ListServicesByCIUseCase`
    - Validate service and CI exist, prevent duplicates, enforce last service association protection (unless controlled exception flag)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_
  - [~] 13.2 Create `ServiceCIResource` REST resource at `/api/v1/cmdb/service-cis` with all endpoints and role-based authorization
    - _Requirements: 9.1, 16.3, 16.4_
  - [ ]* 13.3 Write property test for last service association protection
    - **Property 20: Last Service Association Protection**
    - **Validates: Requirements 9.6, 6.7**
  - [ ]* 13.4 Write property test for bidirectional relationship listing
    - **Property 19: Bidirectional Relationship Listing**
    - **Validates: Requirements 3.6, 8.6, 9.4, 9.5**

- [ ] 14. Checkpoint — Verify all core entity CRUD, versioning, and relationship functionality
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 15. Implement DataSource use cases and REST resource
  - [~] 15.1 Implement `CreateDataSourceUseCase`, `GetDataSourceUseCase`, `UpdateDataSourceUseCase`, `ListDataSourcesUseCase`, `DeleteDataSourceUseCase`
    - Enforce name uniqueness, reliability rating range [0-100], deletion protection for data sources referenced by versions
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  - [~] 15.2 Create `DataSourceResource` REST resource at `/api/v1/cmdb/data-sources` with all endpoints and role-based authorization
    - _Requirements: 10.1, 16.3, 16.4_
  - [ ]* 15.3 Write property test for DataSource CRUD round-trip
    - **Property 5: DataSource CRUD Round-Trip**
    - **Validates: Requirements 10.1, 10.2**
  - [ ]* 15.4 Write property test for reliability rating range validation
    - **Property 21: Reliability Rating Range Validation**
    - **Validates: Requirements 10.5**
  - [ ]* 15.5 Write property test for name uniqueness enforcement
    - **Property 8: Name Uniqueness Enforcement**
    - **Validates: Requirements 1.8, 10.3**

- [ ] 16. Implement cross-cutting validation property tests
  - [ ]* 16.1 Write property test for foreign reference validation
    - **Property 6: Foreign Reference Validation**
    - **Validates: Requirements 1.3, 2.3, 2.4, 3.3, 4.3, 5.3, 5.7, 6.3, 6.4, 7.3, 7.7, 8.3, 9.2**
  - [ ]* 16.2 Write property test for deletion protection for entities with dependents
    - **Property 10: Deletion Protection for Entities with Dependents**
    - **Validates: Requirements 1.6, 2.6, 4.5, 6.6, 10.4**
  - [ ]* 16.3 Write property test for filter result correctness
    - **Property 18: Filter Result Correctness**
    - **Validates: Requirements 2.8, 4.4, 6.5**

- [ ] 17. Implement File Import use cases and REST resource
  - [~] 17.1 Implement `ImportFileUseCase` and `ListFileImportsUseCase`
    - Validate file format (CSV, JSON, XML) and structure, process records creating/updating assets and CIs with new versions, associate imported records with FILE data source, handle partial failures (skip invalid records, continue processing valid ones), return summary with success/failure counts and error details
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
  - [~] 17.2 Create `FileImportResource` REST resource at `/api/v1/cmdb/imports` with POST (upload) and GET (history) endpoints and role-based authorization
    - _Requirements: 11.1, 11.6, 16.3, 16.4_
  - [ ]* 17.3 Write property test for file import partial failure summary
    - **Property 27: File Import Partial Failure Summary**
    - **Validates: Requirements 11.5**

- [ ] 18. Implement Reconciliation use cases and REST resource
  - [~] 18.1 Implement `TriggerReconciliationUseCase` and `ListReconciliationsUseCase`
    - Compare records from different data sources, identify duplicates by name+type+organization, resolve conflicts by preferring higher reliability rating, return reconciliation report with counts and details
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  - [~] 18.2 Create `ReconciliationResource` REST resource at `/api/v1/cmdb/reconciliations` with POST (trigger) and GET (history) endpoints and role-based authorization
    - _Requirements: 12.1, 12.5, 16.3, 16.4_
  - [ ]* 18.3 Write property test for reconciliation conflict resolution by reliability
    - **Property 26: Reconciliation Conflict Resolution by Reliability**
    - **Validates: Requirements 12.2, 12.3**

- [ ] 19. Implement Impact Analysis use cases and REST resource
  - [~] 19.1 Implement `AnalyzeCIImpactUseCase` and `AnalyzeServiceImpactUseCase`
    - BFS traversal of CI relationships and service dependencies with configurable max depth, detect circular dependencies and include warnings, return structured response with root entity, affected entities with relationship paths, and total counts
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_
  - [~] 19.2 Create `ImpactAnalysisResource` REST resource at `/api/v1/cmdb/impact-analysis` with GET endpoints for CI and service impact analysis
    - _Requirements: 13.1, 13.2, 16.4_
  - [ ]* 19.3 Write property test for impact analysis transitive completeness
    - **Property 23: Impact Analysis Transitive Completeness**
    - **Validates: Requirements 13.1, 13.2, 13.3**
  - [ ]* 19.4 Write property test for impact analysis circular dependency detection
    - **Property 24: Impact Analysis Circular Dependency Detection**
    - **Validates: Requirements 13.5**

- [ ] 20. Implement Historical Query use cases and REST resource
  - [~] 20.1 Implement `GetAssetVersionAtTimeUseCase` and `GetCIVersionAtTimeUseCase`
    - Find the version whose startDate is on or before the queried timestamp and whose endDate is after the timestamp or is null; return 404 if no version existed at that time
    - _Requirements: 14.1, 14.2, 14.3, 14.4_
  - [~] 20.2 Create `HistoricalQueryResource` REST resource at `/api/v1/cmdb/history` with GET endpoints for asset and CI point-in-time queries
    - _Requirements: 14.1, 14.2, 16.4_
  - [ ]* 20.3 Write property test for historical point-in-time query
    - **Property 25: Historical Point-in-Time Query**
    - **Validates: Requirements 14.1, 14.2, 14.3, 14.4**

- [ ] 21. Checkpoint — Verify data source, file import, reconciliation, impact analysis, and historical queries
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 22. Implement MongoDB adapters for all repository ports
  - [~] 22.1 Implement MongoDB adapters for `OrganizationRepository`, `ServiceRepository`, `ServiceDependencyRepository`, `AssetRepository`, `AssetVersionRepository` using Quarkus MongoDB Panache with the `zenandops-cmdb` database
    - Create MongoDB indexes as specified in the design (parentId, type, unique sibling name, organizationId, etc.)
    - _Requirements: 15.3, 17.4_
  - [~] 22.2 Implement MongoDB adapters for `CIRepository`, `CIVersionRepository`, `CIRelationshipRepository`, `ServiceCIRepository`, `DataSourceRepository`, `FileImportRecordRepository`, `ReconciliationRecordRepository`
    - Create MongoDB indexes as specified in the design (unique constraints, foreign key indexes)
    - _Requirements: 15.3, 17.4_

- [ ] 23. Implement Kafka event publisher adapter
  - [~] 23.1 Implement `KafkaCmdbEventPublisher` adapter using SmallRye Reactive Messaging publishing to `cmdb-events` topic
    - Include entity identifier, entity type, event type, user identity, and timestamp in every event
    - Handle Kafka unavailability gracefully: log failure and continue processing without blocking
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6_

- [ ] 24. Implement RBAC authorization and error handling
  - [~] 24.1 Implement role-based authorization checks on all REST resources: ADMIN/OPERATOR for write operations, any authenticated user for read operations
    - _Requirements: 16.2, 16.3, 16.4_
  - [~] 24.2 Implement global exception mapper for all domain exceptions, mapping to appropriate HTTP status codes and error response format as specified in the design
    - _Requirements: 15.3_
  - [ ]* 24.3 Write property test for RBAC write operation enforcement
    - **Property 28: RBAC Write Operation Enforcement**
    - **Validates: Requirements 16.2, 16.3, 16.4**

- [ ] 25. Checkpoint — Verify infrastructure adapters, Kafka publishing, and authorization
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 26. Update Docker Compose and Gateway configuration
  - [~] 26.1 Add `cmdb-service` to `docker-compose.yml` on `zenandops-net` network with MongoDB connection, Kafka broker, OTel exporter endpoint, and port 8083
    - Configure `CMDB_DB_NAME=zenandops-cmdb`, `OTEL_EXPORTER_OTLP_ENDPOINT`, and health check
    - _Requirements: 17.2, 17.3, 17.4, 17.5_
  - [~] 26.2 Add `GATEWAY_CMDB_SERVICE_URL` environment variable to gateway-service in Docker Compose and update `ConfigRouteResolver` to route `/api/v1/cmdb` to cmdb-service
    - _Requirements: 16.1, 17.6_
  - [~] 26.3 Update `.env` and `.env.example` with all new environment variables: `CMDB_SERVICE_PORT`, `CMDB_DB_NAME`, `GATEWAY_CMDB_SERVICE_URL`, `CMDB_IMPACT_ANALYSIS_MAX_DEPTH`
    - _Requirements: 17.7_

- [ ] 27. Implement frontend API hooks
  - [~] 27.1 Create all 14 API hooks: `useOrganizationApi`, `useServiceApi`, `useServiceDependencyApi`, `useAssetApi`, `useAssetVersionApi`, `useCIApi`, `useCIVersionApi`, `useCIRelationshipApi`, `useServiceCIApi`, `useDataSourceApi`, `useFileImportApi`, `useReconciliationApi`, `useImpactAnalysisApi`, `useHistoricalQueryApi`
    - All hooks call through the gateway at `/api/v1/cmdb/*` with JWT Bearer token
    - _Requirements: 16.5, 19.1, 20.1, 21.1, 22.1_

- [ ] 28. Implement frontend Organization Management page
  - [~] 28.1 Create Organization Management page at `/cmdb/organizations` with tree view displaying organizational hierarchy, create form (name, type dropdown, parent dropdown, responsible person, cost center), inline editing, and delete with in-use error feedback
    - Derive layout and styling from `.frontend-template` design system
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6_

- [ ] 29. Implement frontend Service Management page
  - [~] 29.1 Create Service Management page at `/cmdb/services` with table view (name, type, organization, criticality, status, business owner, technical owner), create/edit form, dependency list per service, add dependency form, and filtering
    - Derive layout and styling from `.frontend-template` design system
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6_

- [ ] 30. Implement frontend Asset Management page
  - [~] 30.1 Create Asset Management page at `/cmdb/assets` with table view (name, type, organization, cost, cost type, status, supplier), create/edit form, version history panel for selected asset, and filtering
    - Derive layout and styling from `.frontend-template` design system
    - _Requirements: 21.1, 21.2, 21.3, 21.8_

- [ ] 31. Implement frontend CI Management page
  - [~] 31.1 Create CI Management page at `/cmdb/cis` with table view (name, type, organization, status, associated asset), create/edit form, version history panel, relationship list, and filtering
    - Derive layout and styling from `.frontend-template` design system
    - _Requirements: 21.4, 21.5, 21.6, 21.7, 21.8_

- [ ] 32. Implement frontend Impact Analysis and File Import pages
  - [~] 32.1 Create Impact Analysis page at `/cmdb/impact-analysis` with search field to select CI or Service, structured result list showing affected entities with relationship paths and total counts
    - Derive layout and styling from `.frontend-template` design system
    - _Requirements: 22.1, 22.2, 22.3, 22.7_
  - [~] 32.2 Create File Import page at `/cmdb/imports` with file upload component (CSV, JSON, XML), import summary display (successful, failed, error details), and import history list
    - Derive layout and styling from `.frontend-template` design system
    - _Requirements: 22.4, 22.5, 22.6, 22.7_

- [ ] 33. Update frontend sidebar navigation and routing
  - [~] 33.1 Add CMDB navigation group to `AppSidebar.tsx` with sub-items: Organizations, Services, Assets, Configuration Items, Impact Analysis, File Import
    - _Requirements: 19.1, 20.1, 21.1, 21.4, 22.1, 22.4_
  - [~] 33.2 Add routes in `App.tsx` for all 6 CMDB pages: `/cmdb/organizations`, `/cmdb/services`, `/cmdb/assets`, `/cmdb/cis`, `/cmdb/impact-analysis`, `/cmdb/imports`
    - _Requirements: 19.1, 20.1, 21.1, 21.4, 22.1, 22.4_

- [ ] 34. Checkpoint — Verify full frontend integration and end-to-end flows
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 35. Version control and release
  - [~] 35.1 Ensure all previous tasks are complete and tests pass
  - [~] 35.2 Remove SNAPSHOT suffix from all version references in the codebase
  - [~] 35.3 Commit the version bump: "release: 1.5.0 - asset-ci-management"
  - [~] 35.4 Merge branch into main/master
  - [~] 35.5 Apply Git tag: 1.5.0 (without SNAPSHOT)
  - [~] 35.6 Push branch, merge, and tag to remote

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using jqwik (28 properties defined in design)
- Unit tests validate specific examples and edge cases
- The implementation language is Java 25 with Quarkus 3.33.x, consistent with existing backend services
- Frontend uses React 19 + TypeScript, consistent with existing frontend-app
