# Requirements Document

## Introduction

This feature introduces an Asset & Configuration Item (CI) Management System to the ZenAndOps ITSM platform. The system enables organizations to manage the full lifecycle of IT assets (financial perspective) and configuration items (operational perspective), model business services and their dependencies, and maintain organizational structure — all with immutable versioning and traceable data origins. The data model separates financial asset tracking from operational CI management while supporting hierarchical (tree) structures for organizations and services, and graph-based dependency relationships between CIs and services. The system supports hybrid automatic discovery through multiple data sources (API, agent, file import) and aligns with ITIL Configuration Management, COBIT governance, and Site Reliability Engineering principles.

A new backend microservice (`cmdb-service`) will be created following the established hexagonal architecture with DDD, built on Java 25 with Quarkus and MongoDB. The service will be integrated into the existing Docker Compose stack, routed through the Gateway_Service, and instrumented with OpenTelemetry for full observability. The frontend will provide management pages for organizations, services, assets, CIs, and their relationships, derived from the `.frontend-template` design system.

## Glossary

- **CMDB_Service**: The Quarkus-based backend microservice responsible for managing organizations, services, assets, configuration items, versions, relationships, data sources, and file imports
- **Organization**: An entity representing an organizational unit within the company hierarchy (root, business unit, department, or team), used to scope ownership of services and assets
- **Service**: A business or technical service modeled in a hierarchy, representing the value delivered to the organization; each service has a business owner and a technical owner
- **Service_Dependency**: A directed relationship between two services indicating a synchronous, asynchronous, or critical dependency
- **Asset**: A financial entity representing a hardware, software, or cloud resource owned by an organization, tracked for cost management (CAPEX/OPEX)
- **Asset_Version**: An immutable snapshot of an asset's attributes at a point in time, capturing the state, data origin, and optional change reference
- **CI**: A Configuration Item representing an operational component (VM, database, API, storage, or network) managed to deliver IT services
- **CI_Version**: An immutable snapshot of a CI's attributes at a point in time, capturing the state, data origin, and optional change reference
- **CI_Relationship**: A directed relationship between two CIs indicating a dependency, hosting, or connectivity relationship
- **Service_CI**: An association linking a CI to a service, establishing which CIs support which services
- **Data_Source**: A registered origin of data (API, agent, or file) with a reliability rating, used to trace where asset and CI information comes from
- **Hierarchy**: A tree structure where each node has at most one parent, used for organizations and services
- **Dependency_Graph**: A directed graph structure representing dependencies between services or between CIs, distinct from the hierarchy
- **Immutable_Version**: A version record that, once created, is never modified or deleted; new changes produce new version records
- **Impact_Analysis**: The process of traversing the dependency graph to determine which services and CIs are affected when a specific CI or service experiences a change or failure
- **Reconciliation**: The process of comparing data from multiple sources to detect conflicts, duplicates, and inconsistencies in asset and CI records
- **File_Import**: The process of ingesting asset and CI data from structured files (CSV, JSON, XML) representing exports from external systems such as VMware or storage platforms
- **CAPEX**: Capital Expenditure — one-time purchase costs for assets
- **OPEX**: Operational Expenditure — recurring costs for assets (subscriptions, licenses, cloud usage)
- **Gateway_Service**: The existing API Gateway that routes requests to backend services, validates JWT tokens, and enforces rate limiting
- **Frontend_App**: The existing React Progressive Web Application providing the user interface

## Requirements

### Requirement 1: Organization Management

**User Story:** As an administrator, I want to create and manage a hierarchical organizational structure, so that I can scope ownership of services and assets to specific business units, departments, and teams.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, retrieving, updating, and listing Organization entities
2. WHEN an Organization is created, THE CMDB_Service SHALL store the following fields: name, type (ROOT, BUSINESS_UNIT, DEPARTMENT, TEAM), parent organization reference (optional), responsible person, cost center, and creation timestamp
3. WHEN an Organization is created with a parent reference, THE CMDB_Service SHALL validate that the referenced parent Organization exists
4. THE CMDB_Service SHALL enforce that only one Organization of type ROOT exists in the system
5. WHEN an Organization is created with type ROOT, THE CMDB_Service SHALL reject the request if a ROOT Organization already exists
6. IF a request attempts to delete an Organization that has child organizations, services, or assets associated with it, THEN THE CMDB_Service SHALL reject the deletion and return an error indicating the Organization is in use
7. THE CMDB_Service SHALL expose an endpoint that returns the full organizational tree starting from the ROOT Organization
8. WHEN an Organization name is updated, THE CMDB_Service SHALL validate that the new name is unique among siblings under the same parent

### Requirement 2: Service Modeling and Hierarchy

**User Story:** As a service owner, I want to model business and technical services in a hierarchy with ownership and criticality, so that I can understand the service landscape and its relationship to the organization.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, retrieving, updating, and listing Service entities
2. WHEN a Service is created, THE CMDB_Service SHALL store the following fields: name, description, type (DOMAIN, BUSINESS, TECHNICAL), parent service reference (optional), organization reference, business owner, technical owner, criticality level, status, and creation timestamp
3. WHEN a Service is created, THE CMDB_Service SHALL validate that the referenced Organization exists
4. WHEN a Service is created with a parent service reference, THE CMDB_Service SHALL validate that the referenced parent Service exists
5. THE CMDB_Service SHALL enforce that every Service has both a business owner and a technical owner assigned
6. IF a request attempts to delete a Service that has child services, dependencies, or CI associations, THEN THE CMDB_Service SHALL reject the deletion and return an error indicating the Service is in use
7. THE CMDB_Service SHALL expose an endpoint that returns the service hierarchy tree starting from root-level services (services with no parent)
8. THE CMDB_Service SHALL support filtering services by organization, type, criticality, and status

### Requirement 3: Service Dependency Management

**User Story:** As a technical owner, I want to define dependencies between services, so that I can understand how services interact and assess the impact of changes or failures.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, retrieving, deleting, and listing Service_Dependency relationships
2. WHEN a Service_Dependency is created, THE CMDB_Service SHALL store the following fields: source service reference, target service reference, and dependency type (SYNCHRONOUS, ASYNCHRONOUS, CRITICAL)
3. WHEN a Service_Dependency is created, THE CMDB_Service SHALL validate that both the source and target Service entities exist
4. THE CMDB_Service SHALL prevent the creation of a Service_Dependency where the source and target are the same Service
5. THE CMDB_Service SHALL prevent the creation of a duplicate Service_Dependency between the same source and target services
6. THE CMDB_Service SHALL expose an endpoint that returns all direct dependencies (both upstream and downstream) for a given Service
7. WHEN a Service_Dependency of type CRITICAL is created, THE CMDB_Service SHALL log a warning indicating a critical dependency has been established

### Requirement 4: Asset Management

**User Story:** As a financial manager, I want to register and track IT assets with their cost information, so that I can manage the financial lifecycle of hardware, software, and cloud resources.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, retrieving, updating, and listing Asset entities
2. WHEN an Asset is created, THE CMDB_Service SHALL store the following fields: name, type (HARDWARE, SOFTWARE, CLOUD), organization reference, cost, cost type (CAPEX, OPEX), acquisition date, status, supplier, and creation timestamp
3. WHEN an Asset is created, THE CMDB_Service SHALL validate that the referenced Organization exists
4. THE CMDB_Service SHALL support filtering assets by organization, type, cost type, status, and supplier
5. IF a request attempts to delete an Asset that has associated CIs or active versions, THEN THE CMDB_Service SHALL reject the deletion and return an error indicating the Asset is in use
6. THE CMDB_Service SHALL expose an endpoint that returns the total cost of assets grouped by organization and cost type (CAPEX/OPEX)

### Requirement 5: Asset Versioning

**User Story:** As an operations engineer, I want every change to an asset to be recorded as an immutable version, so that I can trace the complete history of asset configurations and their data origins.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating and listing Asset_Version records for a given Asset
2. WHEN an Asset_Version is created, THE CMDB_Service SHALL store the following fields: asset reference, version number, description, attributes (JSON), start date, end date (optional), data origin (API, AGENT, FILE), and optional change reference
3. WHEN an Asset_Version is created, THE CMDB_Service SHALL validate that the referenced Asset exists
4. WHEN an Asset_Version is created, THE CMDB_Service SHALL automatically assign the next sequential version number for that Asset
5. THE CMDB_Service SHALL enforce that Asset_Version records are immutable — update and delete operations on existing versions SHALL be rejected
6. WHEN a new Asset_Version is created, THE CMDB_Service SHALL set the end date of the previous active version to the start date of the new version
7. WHEN an Asset_Version is created, THE CMDB_Service SHALL validate that the data origin references a registered Data_Source
8. THE CMDB_Service SHALL expose an endpoint that returns the complete version history for a given Asset, ordered by version number

### Requirement 6: Configuration Item Management

**User Story:** As an operations engineer, I want to register and manage configuration items representing operational components, so that I can track VMs, databases, APIs, storage, and network devices that deliver IT services.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, retrieving, updating, and listing CI entities
2. WHEN a CI is created, THE CMDB_Service SHALL store the following fields: name, type (VM, DATABASE, API, STORAGE, NETWORK), organization reference, asset reference (optional), status, and creation timestamp
3. WHEN a CI is created, THE CMDB_Service SHALL validate that the referenced Organization exists
4. WHEN a CI is created with an asset reference, THE CMDB_Service SHALL validate that the referenced Asset exists
5. THE CMDB_Service SHALL support filtering CIs by organization, type, status, and associated asset
6. IF a request attempts to delete a CI that has active versions, relationships, or service associations, THEN THE CMDB_Service SHALL reject the deletion and return an error indicating the CI is in use
7. THE CMDB_Service SHALL enforce that every CI is linked to at least one Service, except when the CI has a controlled exception flag set to true

### Requirement 7: CI Versioning

**User Story:** As an operations engineer, I want every change to a CI to be recorded as an immutable version, so that I can trace the complete history of CI configurations and their data origins.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating and listing CI_Version records for a given CI
2. WHEN a CI_Version is created, THE CMDB_Service SHALL store the following fields: CI reference, version number, attributes (JSON), start date, end date (optional), data origin (API, AGENT, FILE), and optional change reference
3. WHEN a CI_Version is created, THE CMDB_Service SHALL validate that the referenced CI exists
4. WHEN a CI_Version is created, THE CMDB_Service SHALL automatically assign the next sequential version number for that CI
5. THE CMDB_Service SHALL enforce that CI_Version records are immutable — update and delete operations on existing versions SHALL be rejected
6. WHEN a new CI_Version is created, THE CMDB_Service SHALL set the end date of the previous active version to the start date of the new version
7. WHEN a CI_Version is created, THE CMDB_Service SHALL validate that the data origin references a registered Data_Source
8. THE CMDB_Service SHALL expose an endpoint that returns the complete version history for a given CI, ordered by version number

### Requirement 8: CI Relationship Management

**User Story:** As an operations engineer, I want to define relationships between configuration items, so that I can model dependencies, hosting, and connectivity in the infrastructure.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, retrieving, deleting, and listing CI_Relationship records
2. WHEN a CI_Relationship is created, THE CMDB_Service SHALL store the following fields: source CI reference, target CI reference, and relationship type (DEPENDS_ON, HOSTS, CONNECTS_TO)
3. WHEN a CI_Relationship is created, THE CMDB_Service SHALL validate that both the source and target CI entities exist
4. THE CMDB_Service SHALL prevent the creation of a CI_Relationship where the source and target are the same CI
5. THE CMDB_Service SHALL prevent the creation of a duplicate CI_Relationship between the same source and target CIs with the same type
6. THE CMDB_Service SHALL expose an endpoint that returns all direct relationships (both upstream and downstream) for a given CI

### Requirement 9: Service-CI Association

**User Story:** As a service owner, I want to associate configuration items with services, so that I can understand which infrastructure components support each business service.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, deleting, and listing Service_CI associations
2. WHEN a Service_CI association is created, THE CMDB_Service SHALL validate that both the referenced Service and CI exist
3. THE CMDB_Service SHALL prevent the creation of a duplicate Service_CI association between the same Service and CI
4. THE CMDB_Service SHALL expose an endpoint that returns all CIs associated with a given Service
5. THE CMDB_Service SHALL expose an endpoint that returns all Services associated with a given CI
6. WHEN a Service_CI association is the last association for a CI and the CI does not have the controlled exception flag, THE CMDB_Service SHALL reject the deletion and return an error indicating the CI must be linked to at least one Service

### Requirement 10: Data Source Management

**User Story:** As an administrator, I want to register and manage data sources, so that every piece of asset and CI data can be traced back to its origin with a known reliability rating.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose REST endpoints for creating, retrieving, updating, and listing Data_Source entities
2. WHEN a Data_Source is created, THE CMDB_Service SHALL store the following fields: name, type (API, AGENT, FILE), and reliability rating
3. THE CMDB_Service SHALL enforce that Data_Source names are unique
4. IF a request attempts to delete a Data_Source that is referenced by any Asset_Version or CI_Version, THEN THE CMDB_Service SHALL reject the deletion and return an error indicating the Data_Source is in use
5. THE CMDB_Service SHALL validate that the reliability rating is a value between 0 and 100

### Requirement 11: File Import for Asset and CI Discovery

**User Story:** As an operations engineer, I want to import asset and CI data from structured files exported by external systems (VMware, storage platforms), so that I can populate the CMDB without manual data entry.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose a REST endpoint that accepts file uploads in CSV, JSON, and XML formats for asset and CI data import
2. WHEN a file is uploaded, THE CMDB_Service SHALL validate the file format and structure before processing
3. WHEN a valid file is processed, THE CMDB_Service SHALL create or update Asset and CI records based on the file contents, creating new Asset_Version or CI_Version records for each imported item
4. WHEN a file is processed, THE CMDB_Service SHALL associate all imported records with the Data_Source of type FILE that corresponds to the import operation
5. IF a file contains records that fail validation, THEN THE CMDB_Service SHALL skip the invalid records, continue processing valid records, and return a summary indicating the count of successful imports, failed imports, and the specific errors for each failed record
6. THE CMDB_Service SHALL expose an endpoint that returns the history of file import operations with their status and summary

### Requirement 12: Reconciliation

**User Story:** As an operations engineer, I want to reconcile data from multiple sources to detect conflicts and duplicates, so that the CMDB maintains accurate and consistent records.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose a REST endpoint that triggers a reconciliation process for a given entity type (Asset or CI)
2. WHEN reconciliation is triggered, THE CMDB_Service SHALL compare records from different Data_Sources and identify duplicates based on matching name and type within the same Organization
3. WHEN reconciliation detects a conflict between two versions of the same entity from different sources, THE CMDB_Service SHALL prefer the version from the Data_Source with the higher reliability rating
4. WHEN reconciliation completes, THE CMDB_Service SHALL return a reconciliation report containing: the number of records analyzed, duplicates found, conflicts resolved, and unresolved conflicts requiring manual review
5. THE CMDB_Service SHALL expose an endpoint that returns the history of reconciliation operations with their reports

### Requirement 13: Impact Analysis

**User Story:** As a service owner, I want to perform impact analysis on a CI or service, so that I can understand the blast radius of a change or failure before it occurs.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose a REST endpoint that accepts a CI identifier and returns all directly and transitively affected CIs and Services
2. THE CMDB_Service SHALL expose a REST endpoint that accepts a Service identifier and returns all directly and transitively affected Services (via the dependency graph) and their associated CIs
3. WHEN performing impact analysis, THE CMDB_Service SHALL traverse the dependency graph (CI_Relationship and Service_Dependency) to determine transitive impact up to a configurable maximum depth
4. THE CMDB_Service SHALL return the impact analysis result as a structured response containing: the root entity, the list of affected entities with their relationship path, and the total count of affected services and CIs
5. IF the impact analysis traversal encounters a circular dependency, THEN THE CMDB_Service SHALL detect the cycle, terminate the traversal for that path, and include a warning in the response indicating the circular dependency

### Requirement 14: Historical Queries

**User Story:** As an auditor, I want to query the state of any asset or CI at a specific point in time, so that I can perform audits and understand the historical configuration of the infrastructure.

#### Acceptance Criteria

1. THE CMDB_Service SHALL expose a REST endpoint that accepts an Asset identifier and a timestamp, and returns the Asset_Version that was active at that point in time
2. THE CMDB_Service SHALL expose a REST endpoint that accepts a CI identifier and a timestamp, and returns the CI_Version that was active at that point in time
3. WHEN a historical query is executed, THE CMDB_Service SHALL determine the active version by finding the version whose start date is on or before the queried timestamp and whose end date is after the queried timestamp or is null
4. IF no version was active at the queried timestamp, THEN THE CMDB_Service SHALL return an HTTP 404 status code with a message indicating no version existed at that time

### Requirement 15: CMDB Service Hexagonal Architecture and DDD Structure

**User Story:** As a developer, I want the CMDB service to follow hexagonal architecture with DDD, so that the domain logic is decoupled from infrastructure concerns and the codebase is maintainable and consistent with existing services.

#### Acceptance Criteria

1. THE CMDB_Service SHALL organize its codebase into domain, application, and infrastructure layers following Hexagonal_Architecture principles consistent with the Auth_Service and Dashboard_Service
2. THE CMDB_Service SHALL define ports (interfaces) for all external interactions including persistence, event publishing, and file processing
3. THE CMDB_Service SHALL implement adapters for MongoDB persistence, REST endpoint exposure, and file import processing
4. THE CMDB_Service SHALL use the package structure `com.zenandops.cmdb` with sub-packages for domain (entities, value objects, exceptions), application (ports, use cases), and infrastructure (adapters, REST resources, DTOs)

### Requirement 16: Gateway Integration and Routing

**User Story:** As a platform operator, I want the CMDB service routed through the existing API Gateway, so that all requests benefit from JWT validation, rate limiting, and centralized access control.

#### Acceptance Criteria

1. THE Gateway_Service SHALL route all requests with the path prefix `/api/v1/cmdb/*` to the CMDB_Service
2. THE Gateway_Service SHALL require a valid JWT Access_Token for all CMDB_Service endpoints
3. THE CMDB_Service SHALL validate that the authenticated user has the appropriate role (ADMIN or OPERATOR) for write operations (create, update, delete)
4. THE CMDB_Service SHALL allow read operations (get, list, search, impact analysis, historical queries) for any authenticated user
5. THE Frontend_App SHALL access all CMDB_Service endpoints exclusively through the Gateway_Service

### Requirement 17: Infrastructure and Containerization

**User Story:** As a developer, I want the CMDB service containerized and integrated into the existing Docker Compose stack, so that the entire platform can be started with a single command.

#### Acceptance Criteria

1. THE CMDB_Service SHALL include a Dockerfile that produces a container image based on a Java 25 runtime, consistent with existing backend services
2. THE Docker_Compose configuration SHALL define a `cmdb-service` service on the existing `zenandops-net` network
3. THE Docker_Compose configuration SHALL configure the CMDB_Service to connect to the existing MongoDB instance
4. THE CMDB_Service SHALL use a dedicated MongoDB database (separate from the auth-service database) for its collections
5. THE Docker_Compose configuration SHALL include the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable for the CMDB_Service pointing to the OTel Collector
6. THE Gateway_Service configuration SHALL include the `GATEWAY_CMDB_SERVICE_URL` environment variable pointing to the CMDB_Service base URL
7. THE `.env.example` file SHALL document all new environment variables introduced for the CMDB_Service

### Requirement 18: Kafka Event Publishing for CMDB Events

**User Story:** As a developer, I want significant CMDB changes published to Kafka, so that other services can react to asset, CI, and service lifecycle events asynchronously.

#### Acceptance Criteria

1. WHEN an Asset is created or updated, THE CMDB_Service SHALL publish an asset change event to a Kafka topic
2. WHEN a CI is created or updated, THE CMDB_Service SHALL publish a CI change event to a Kafka topic
3. WHEN a Service is created or updated, THE CMDB_Service SHALL publish a service change event to a Kafka topic
4. WHEN a new Asset_Version or CI_Version is created, THE CMDB_Service SHALL publish a version created event to a Kafka topic
5. THE CMDB_Service SHALL include the entity identifier, entity type, event type, user identity, and timestamp in every published Kafka event
6. IF the Kafka broker is unavailable, THEN THE CMDB_Service SHALL log the event publishing failure and continue processing the request without blocking

### Requirement 19: Frontend Organization Management Page

**User Story:** As an administrator, I want a frontend page to manage the organizational structure, so that I can create, view, and edit organizations through the UI.

#### Acceptance Criteria

1. THE Frontend_App SHALL display an Organization management page accessible from the sidebar navigation
2. THE Frontend_App SHALL display the organizational hierarchy as a tree view showing parent-child relationships
3. THE Frontend_App SHALL provide a form for creating a new Organization with fields for name, type, parent organization (dropdown), responsible person, and cost center
4. THE Frontend_App SHALL provide inline editing for Organization name, responsible person, and cost center
5. WHEN a user attempts to delete an Organization that is in use, THE Frontend_App SHALL display an error message indicating the Organization cannot be deleted
6. THE Frontend_App SHALL derive its Organization management page layout and styling from the `.frontend-template` design system

### Requirement 20: Frontend Service Management Page

**User Story:** As a service owner, I want a frontend page to manage services and their dependencies, so that I can model the service landscape through the UI.

#### Acceptance Criteria

1. THE Frontend_App SHALL display a Service management page accessible from the sidebar navigation
2. THE Frontend_App SHALL display services in a table view with columns for name, type, organization, criticality, status, business owner, and technical owner
3. THE Frontend_App SHALL provide a form for creating and editing a Service with all required fields
4. THE Frontend_App SHALL display service dependencies as a list showing source, target, and dependency type for each selected service
5. THE Frontend_App SHALL provide a form for adding a new Service_Dependency with dropdowns for source service, target service, and dependency type
6. THE Frontend_App SHALL derive its Service management page layout and styling from the `.frontend-template` design system

### Requirement 21: Frontend Asset and CI Management Pages

**User Story:** As an operations engineer, I want frontend pages to manage assets and configuration items, so that I can register, view, and track assets and CIs through the UI.

#### Acceptance Criteria

1. THE Frontend_App SHALL display an Asset management page accessible from the sidebar navigation with a table listing all assets
2. THE Frontend_App SHALL provide a form for creating and editing an Asset with all required fields including cost, cost type, supplier, and organization
3. THE Frontend_App SHALL display the version history for a selected Asset in a chronological list
4. THE Frontend_App SHALL display a CI management page accessible from the sidebar navigation with a table listing all CIs
5. THE Frontend_App SHALL provide a form for creating and editing a CI with all required fields including type, organization, and optional asset reference
6. THE Frontend_App SHALL display the version history for a selected CI in a chronological list
7. THE Frontend_App SHALL display CI relationships for a selected CI showing source, target, and relationship type
8. THE Frontend_App SHALL derive its Asset and CI management page layouts and styling from the `.frontend-template` design system

### Requirement 22: Frontend Impact Analysis and File Import Pages

**User Story:** As a service owner, I want frontend pages for impact analysis and file import, so that I can assess change impact and bulk-import data through the UI.

#### Acceptance Criteria

1. THE Frontend_App SHALL display an Impact Analysis page accessible from the sidebar navigation
2. THE Frontend_App SHALL provide a search field to select a CI or Service for impact analysis
3. WHEN an impact analysis is executed, THE Frontend_App SHALL display the results as a structured list showing affected entities, their relationship paths, and total counts
4. THE Frontend_App SHALL display a File Import page accessible from the sidebar navigation
5. THE Frontend_App SHALL provide a file upload component that accepts CSV, JSON, and XML files
6. WHEN a file import completes, THE Frontend_App SHALL display the import summary showing successful imports, failed imports, and error details
7. THE Frontend_App SHALL derive its Impact Analysis and File Import page layouts and styling from the `.frontend-template` design system

