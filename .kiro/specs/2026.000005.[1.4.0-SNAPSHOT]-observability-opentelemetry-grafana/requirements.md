# Requirements Document

## Introduction

This feature introduces full-stack observability to the ZenAndOps ITSM platform by instrumenting all three backend microservices (auth-service, dashboard-service, gateway-service) with OpenTelemetry. Telemetry data — logs, metrics, and traces — will be collected and exported to the Grafana Labs stack: Loki for log aggregation, Mimir for metrics storage, and Tempo for distributed tracing. Grafana dashboards will provide unified visualization across all three observability pillars, enabling incident detection, SLA monitoring, and performance analysis aligned with SRE best practices.

## Glossary

- **Auth_Service**: The Quarkus-based authentication microservice running on port 8081, responsible for JWT generation, user authentication, and Kafka event publishing
- **Dashboard_Service**: The Quarkus-based operational dashboard microservice running on port 8082, responsible for serving dashboard data with JWT validation
- **Gateway_Service**: The Quarkus-based API Gateway microservice running on port 8080, responsible for request routing, JWT validation, and rate limiting
- **OTel_Collector**: The OpenTelemetry Collector instance that receives, processes, and exports telemetry data from all backend services to the Grafana stack
- **Grafana**: The visualization platform used to query and display logs, metrics, and traces from Loki, Mimir, and Tempo
- **Loki**: The Grafana Labs log aggregation system that stores and indexes structured log data
- **Mimir**: The Grafana Labs metrics storage backend compatible with Prometheus remote-write protocol
- **Tempo**: The Grafana Labs distributed tracing backend that stores and queries trace data
- **Trace_Context**: The W3C Trace Context propagation headers (traceparent, tracestate) used to correlate requests across service boundaries
- **Span**: A single unit of work within a distributed trace, representing an operation such as an HTTP request or database query
- **Correlation_ID**: A unique identifier (trace ID) propagated across all services for a single user request, enabling end-to-end request tracking
- **Structured_Log**: A log entry formatted as JSON with standardized fields including timestamp, severity, service name, trace ID, and span ID
- **RED_Metrics**: The Rate, Errors, Duration method for monitoring request-driven services
- **USE_Metrics**: The Utilization, Saturation, Errors method for monitoring infrastructure resources
- **SLI**: Service Level Indicator — a quantitative measure of a specific aspect of the level of service provided
- **SLO**: Service Level Objective — a target value or range for an SLI over a time window

## Requirements

### Requirement 1: OpenTelemetry SDK Integration

**User Story:** As a platform operator, I want all backend microservices instrumented with OpenTelemetry, so that telemetry data is collected in a vendor-neutral standard.

#### Acceptance Criteria

1. THE Auth_Service SHALL include the `quarkus-opentelemetry` extension as a compile-time dependency
2. THE Dashboard_Service SHALL include the `quarkus-opentelemetry` extension as a compile-time dependency
3. THE Gateway_Service SHALL include the `quarkus-opentelemetry` extension as a compile-time dependency
4. WHEN a backend service starts, THE backend service SHALL export its service name as an OpenTelemetry resource attribute matching the pattern `zenandops-{service}`
5. WHEN a backend service starts, THE backend service SHALL connect to the OTel_Collector endpoint defined by the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable
6. IF the OTel_Collector is unreachable at startup, THEN THE backend service SHALL continue operating and retry the connection using exponential backoff

### Requirement 2: Distributed Tracing

**User Story:** As a platform operator, I want end-to-end distributed tracing across all microservices, so that I can visualize the full request path and diagnose latency issues.

#### Acceptance Criteria

1. WHEN the Gateway_Service receives an inbound HTTP request, THE Gateway_Service SHALL create a root Span containing the HTTP method, URL path, and response status code
2. WHEN the Gateway_Service proxies a request to Auth_Service or Dashboard_Service, THE Gateway_Service SHALL propagate Trace_Context headers (traceparent, tracestate) in the outbound request
3. WHEN Auth_Service or Dashboard_Service receives a request with Trace_Context headers, THE receiving service SHALL create a child Span linked to the parent trace
4. WHEN Auth_Service executes a MongoDB query, THE Auth_Service SHALL create a child Span capturing the database operation name and collection name
5. WHEN Auth_Service publishes a message to Kafka, THE Auth_Service SHALL create a child Span capturing the Kafka topic name and propagate Trace_Context in the message headers
6. THE Gateway_Service SHALL record the total request duration as a Span attribute for every proxied request
7. IF a downstream service returns an error response, THEN THE calling service SHALL mark the corresponding Span status as ERROR and record the HTTP status code

### Requirement 3: Structured Logging with Trace Correlation

**User Story:** As a platform operator, I want all log output in structured JSON format with trace correlation, so that I can search and filter logs by request across all services.

#### Acceptance Criteria

1. THE Auth_Service SHALL emit all log entries as Structured_Log in JSON format
2. THE Dashboard_Service SHALL emit all log entries as Structured_Log in JSON format
3. THE Gateway_Service SHALL emit all log entries as Structured_Log in JSON format
4. WHEN a log entry is emitted within a traced request context, THE logging framework SHALL include the `traceId` and `spanId` fields from the active Trace_Context in the Structured_Log
5. THE Structured_Log format SHALL include the following fields: `timestamp`, `level`, `message`, `service.name`, `traceId`, `spanId`, `logger.name`
6. WHEN a log entry is emitted outside of a traced request context, THE logging framework SHALL set the `traceId` and `spanId` fields to empty strings
7. IF a request results in an error, THEN THE handling service SHALL log the error with severity level ERROR including the exception type and message

### Requirement 4: Application Metrics Collection

**User Story:** As a platform operator, I want application-level metrics collected from all services following the RED method, so that I can monitor request rate, error rate, and latency.

#### Acceptance Criteria

1. THE Gateway_Service SHALL expose HTTP request count metrics labeled by method, path, and response status code
2. THE Gateway_Service SHALL expose HTTP request duration histogram metrics labeled by method and path
3. THE Auth_Service SHALL expose HTTP request count metrics labeled by method, path, and response status code
4. THE Auth_Service SHALL expose HTTP request duration histogram metrics labeled by method and path
5. THE Dashboard_Service SHALL expose HTTP request count metrics labeled by method, path, and response status code
6. THE Dashboard_Service SHALL expose HTTP request duration histogram metrics labeled by method and path
7. THE Gateway_Service SHALL expose a gauge metric representing the current rate-limit bucket count per client IP
8. THE Auth_Service SHALL expose a counter metric tracking the number of successful and failed authentication attempts

### Requirement 5: JVM and Runtime Metrics

**User Story:** As a platform operator, I want JVM and runtime metrics from all services, so that I can monitor resource utilization and detect memory or thread pool issues.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose JVM heap memory usage metrics (used, committed, max) via OpenTelemetry
2. THE Dashboard_Service SHALL expose JVM heap memory usage metrics (used, committed, max) via OpenTelemetry
3. THE Gateway_Service SHALL expose JVM heap memory usage metrics (used, committed, max) via OpenTelemetry
4. THE Auth_Service SHALL expose JVM garbage collection count and duration metrics via OpenTelemetry
5. THE Dashboard_Service SHALL expose JVM garbage collection count and duration metrics via OpenTelemetry
6. THE Gateway_Service SHALL expose JVM garbage collection count and duration metrics via OpenTelemetry
7. THE Auth_Service SHALL expose JVM active thread count metrics via OpenTelemetry
8. THE Dashboard_Service SHALL expose JVM active thread count metrics via OpenTelemetry
9. THE Gateway_Service SHALL expose JVM active thread count metrics via OpenTelemetry

### Requirement 6: OpenTelemetry Collector Deployment

**User Story:** As a platform operator, I want a centralized OpenTelemetry Collector in the Docker Compose stack, so that all telemetry data is received, processed, and exported to the Grafana stack from a single point.

#### Acceptance Criteria

1. THE Docker Compose configuration SHALL define an OTel_Collector service using the `otel/opentelemetry-collector-contrib` image
2. THE OTel_Collector SHALL accept telemetry data via the OTLP gRPC receiver on port 4317
3. THE OTel_Collector SHALL accept telemetry data via the OTLP HTTP receiver on port 4318
4. THE OTel_Collector SHALL export trace data to Tempo using the OTLP exporter
5. THE OTel_Collector SHALL export metrics data to Mimir using the Prometheus remote-write exporter
6. THE OTel_Collector SHALL export log data to Loki using the loki exporter
7. THE OTel_Collector SHALL apply a batch processor to buffer telemetry data before export
8. IF the OTel_Collector loses connectivity to a backend (Loki, Mimir, or Tempo), THEN THE OTel_Collector SHALL buffer data in memory and retry delivery using exponential backoff

### Requirement 7: Grafana Loki Deployment for Log Aggregation

**User Story:** As a platform operator, I want Grafana Loki deployed in the Docker Compose stack, so that structured logs from all services are aggregated and queryable.

#### Acceptance Criteria

1. THE Docker Compose configuration SHALL define a Loki service using the `grafana/loki` image
2. THE Loki service SHALL accept log data pushed by the OTel_Collector
3. THE Loki service SHALL store log data in a local filesystem volume for persistence across container restarts
4. THE Loki service SHALL index logs by the labels: `service_name`, `level`, and `traceId`
5. WHEN a user queries Loki via Grafana using a traceId, THE Loki service SHALL return all log entries across all services matching that traceId

### Requirement 8: Grafana Mimir Deployment for Metrics Storage

**User Story:** As a platform operator, I want Grafana Mimir deployed in the Docker Compose stack, so that metrics from all services are stored and queryable using PromQL.

#### Acceptance Criteria

1. THE Docker Compose configuration SHALL define a Mimir service using the `grafana/mimir` image
2. THE Mimir service SHALL accept metrics data via the Prometheus remote-write endpoint
3. THE Mimir service SHALL store metrics data in a local filesystem volume for persistence across container restarts
4. WHEN a user queries Mimir via Grafana using PromQL, THE Mimir service SHALL return time-series data matching the query expression

### Requirement 9: Grafana Tempo Deployment for Distributed Tracing

**User Story:** As a platform operator, I want Grafana Tempo deployed in the Docker Compose stack, so that distributed traces from all services are stored and queryable.

#### Acceptance Criteria

1. THE Docker Compose configuration SHALL define a Tempo service using the `grafana/tempo` image
2. THE Tempo service SHALL accept trace data via the OTLP gRPC protocol from the OTel_Collector
3. THE Tempo service SHALL store trace data in a local filesystem volume for persistence across container restarts
4. WHEN a user queries Tempo via Grafana using a traceId, THE Tempo service SHALL return the complete trace with all Spans across all services

### Requirement 10: Grafana Dashboard Deployment and Data Source Configuration

**User Story:** As a platform operator, I want Grafana deployed with pre-configured data sources for Loki, Mimir, and Tempo, so that I can visualize all observability data from a single interface.

#### Acceptance Criteria

1. THE Docker Compose configuration SHALL define a Grafana service using the `grafana/grafana` image exposed on a configurable host port
2. THE Grafana service SHALL be provisioned with a Loki data source pointing to the Loki service URL
3. THE Grafana service SHALL be provisioned with a Mimir data source (Prometheus type) pointing to the Mimir service URL
4. THE Grafana service SHALL be provisioned with a Tempo data source pointing to the Tempo service URL
5. THE Grafana Tempo data source configuration SHALL enable trace-to-logs correlation linking to the Loki data source using the `service_name` label
6. THE Grafana Tempo data source configuration SHALL enable trace-to-metrics correlation linking to the Mimir data source

### Requirement 11: Grafana Observability Dashboards

**User Story:** As a platform operator, I want pre-built Grafana dashboards for logs, metrics, and traces, so that I can monitor the health and performance of all services immediately after deployment.

#### Acceptance Criteria

1. THE Grafana service SHALL be provisioned with a "Service Overview" dashboard displaying RED_Metrics (request rate, error rate, p50/p95/p99 latency) for all three backend services
2. THE Grafana service SHALL be provisioned with a "JVM Metrics" dashboard displaying heap memory usage, garbage collection activity, and thread count for all three backend services
3. THE Grafana service SHALL be provisioned with a "Logs Explorer" dashboard providing a filterable log stream view with service name, log level, and traceId filters
4. THE Grafana service SHALL be provisioned with a "Gateway Performance" dashboard displaying rate-limit metrics, upstream response times, and error breakdown by downstream service
5. THE Grafana service SHALL be provisioned with an "Authentication Metrics" dashboard displaying login success rate, login failure rate, and token generation latency from Auth_Service
6. WHEN a user clicks a traceId in the Logs Explorer dashboard, THE Grafana service SHALL navigate to the corresponding trace view in Tempo


### Requirement 12: Docker Compose Infrastructure Orchestration

**User Story:** As a platform operator, I want all observability infrastructure services orchestrated in Docker Compose with proper dependency ordering, so that the full stack starts reliably with a single command.

#### Acceptance Criteria

1. THE Docker Compose configuration SHALL place all observability services (OTel_Collector, Loki, Mimir, Tempo, Grafana) on the existing `zenandops-net` network
2. THE OTel_Collector service SHALL depend on Loki, Mimir, and Tempo services being started
3. THE Grafana service SHALL depend on Loki, Mimir, and Tempo services being started
4. THE Auth_Service, Dashboard_Service, and Gateway_Service containers SHALL include the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable pointing to the OTel_Collector gRPC endpoint
5. THE Docker Compose configuration SHALL define named volumes for Loki, Mimir, Tempo, and Grafana data persistence
6. THE `.env.example` file SHALL document all new environment variables introduced for the observability stack with descriptive comments

### Requirement 13: Environment Configuration for Observability

**User Story:** As a platform operator, I want all observability settings externalized via environment variables, so that I can configure endpoints, ports, and sampling rates without modifying source code.

#### Acceptance Criteria

1. THE backend services SHALL read the OpenTelemetry exporter endpoint from the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable
2. THE backend services SHALL read the trace sampling ratio from the `OTEL_TRACES_SAMPLER_ARG` environment variable with a default value of `1.0` (sample all traces)
3. THE Grafana service SHALL read its exposed port from a `GRAFANA_PORT` environment variable defined in the `.env` file
4. THE OTel_Collector configuration SHALL use environment variable substitution for backend endpoint URLs (Loki, Mimir, Tempo)
5. WHEN the `OTEL_TRACES_SAMPLER_ARG` environment variable is set to a value between `0.0` and `1.0`, THE backend services SHALL sample traces at the specified ratio

### Requirement 14: Health Check Integration for Observability Services

**User Story:** As a platform operator, I want health checks on all observability infrastructure services, so that Docker Compose can detect and report unhealthy containers.

#### Acceptance Criteria

1. THE Loki service definition in Docker Compose SHALL include a health check that verifies the Loki readiness endpoint
2. THE Mimir service definition in Docker Compose SHALL include a health check that verifies the Mimir readiness endpoint
3. THE Tempo service definition in Docker Compose SHALL include a health check that verifies the Tempo readiness endpoint
4. THE Grafana service definition in Docker Compose SHALL include a health check that verifies the Grafana health API endpoint
5. THE OTel_Collector service definition in Docker Compose SHALL include a health check that verifies the collector health extension endpoint
