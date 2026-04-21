# Requirements Document

## Introduction

This specification defines the migration of the ZenAndOps observability stack from an OpenTelemetry Collector-centric architecture to a purpose-built toolchain. The migration replaces the OTel Collector and Promtail with Prometheus (metrics scraping), Fluent Bit (log collection), and Jaeger (trace collection and visualization). The Grafana backend stack (Mimir, Loki, Tempo, Grafana) remains unchanged as the long-term storage and dashboarding layer.

The current architecture routes all telemetry (metrics, logs, traces) through the OTel Collector via OTLP, with Promtail handling infrastructure container logs. The target architecture decouples each telemetry signal into a dedicated collector: Prometheus scrapes metrics endpoints and remote-writes to Mimir, Fluent Bit tails container logs and forwards to Loki, and Jaeger receives OTLP traces from services and forwards to Tempo.

## Glossary

- **Compose_Stack**: The Docker Compose-based deployment of all ZenAndOps services and observability components defined in `docker-compose.yml`
- **Prometheus**: A time-series metrics scraping and storage system that collects metrics from HTTP endpoints and supports remote write to external backends
- **Fluent_Bit**: A lightweight log processor and forwarder that collects, parses, and routes container logs
- **Jaeger**: A distributed tracing platform that receives trace data via OTLP and provides a trace visualization UI
- **Mimir**: Grafana's long-term Prometheus-compatible metrics storage backend
- **Loki**: Grafana's log aggregation and storage backend
- **Tempo**: Grafana's distributed tracing storage backend
- **Grafana**: The dashboarding and visualization platform with datasources for Mimir, Loki, and Tempo
- **OTel_Collector**: The OpenTelemetry Collector (to be removed), currently the central telemetry pipeline
- **Promtail**: The Grafana log shipper (to be removed), currently collecting MongoDB and Kafka container logs
- **Quarkus_Service**: Any of the backend Java/Quarkus microservices (auth-service, dashboard-service, gateway-service)
- **Metrics_Endpoint**: The HTTP endpoint exposed by each Quarkus_Service at `/q/metrics` in Prometheus exposition format via Micrometer
- **OTLP**: OpenTelemetry Protocol, used for transmitting traces from Quarkus_Services to Jaeger
- **Remote_Write**: The Prometheus remote write protocol used to push metrics from Prometheus to Mimir
- **Docker_Socket**: The Unix socket (`/var/run/docker.sock`) used to access Docker container metadata and logs

## Requirements

### Requirement 1: Remove OTel Collector Service

**User Story:** As a platform operator, I want to remove the OTel Collector from the Compose_Stack, so that telemetry routing is handled by purpose-built collectors instead of a monolithic pipeline.

#### Acceptance Criteria

1. WHEN the Compose_Stack is started, THE Compose_Stack SHALL NOT include an OTel_Collector service definition
2. WHEN the Compose_Stack is started, THE Compose_Stack SHALL NOT reference the `otel-collector-config.yaml` configuration file
3. WHEN the Compose_Stack is started, THE Compose_Stack SHALL NOT expose ports 4317 (gRPC) or 4318 (HTTP) for the OTel_Collector
4. THE Compose_Stack SHALL remove all `depends_on` references to the `otel-collector` service from every Quarkus_Service definition

### Requirement 2: Remove Promtail Service

**User Story:** As a platform operator, I want to remove Promtail from the Compose_Stack, so that log collection is unified under Fluent Bit.

#### Acceptance Criteria

1. WHEN the Compose_Stack is started, THE Compose_Stack SHALL NOT include a Promtail service definition
2. WHEN the Compose_Stack is started, THE Compose_Stack SHALL NOT reference the `promtail-config.yaml` configuration file

### Requirement 3: Add Prometheus for Metrics Scraping

**User Story:** As a platform operator, I want Prometheus to scrape metrics from all Quarkus_Services and infrastructure containers, so that metrics are collected via pull-based scraping and forwarded to Mimir for long-term storage.

#### Acceptance Criteria

1. WHEN the Compose_Stack is started, THE Compose_Stack SHALL include a Prometheus service using the `prom/prometheus` image
2. THE Prometheus service SHALL scrape the Metrics_Endpoint of each Quarkus_Service (auth-service on port 8081, dashboard-service on port 8082, gateway-service on port 8080) at the path `/q/metrics`
3. THE Prometheus service SHALL use Remote_Write to forward all scraped metrics to Mimir at `http://mimir:9009/api/v1/push`
4. THE Prometheus service SHALL scrape its own internal metrics
5. THE Prometheus service SHALL define a scrape interval of 15 seconds for all targets
6. THE Prometheus service SHALL persist its data using a named Docker volume
7. THE Prometheus service SHALL expose port 9090 for its web UI and API
8. THE Prometheus service SHALL include a health check to verify readiness
9. WHEN a Quarkus_Service is temporarily unavailable, THE Prometheus service SHALL continue scraping all other available targets without interruption

### Requirement 4: Configure Quarkus Services for Prometheus Metrics

**User Story:** As a platform operator, I want each Quarkus_Service to expose a Prometheus-format metrics endpoint, so that Prometheus can scrape application metrics directly.

#### Acceptance Criteria

1. THE each Quarkus_Service SHALL expose a Metrics_Endpoint at `/q/metrics` in Prometheus exposition format via the Micrometer Prometheus registry
2. WHEN the Compose_Stack is started, THE each Quarkus_Service SHALL NOT include the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable for metrics export
3. THE each Quarkus_Service SHALL retain the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable configured to point to Jaeger for trace export

### Requirement 5: Add Fluent Bit for Log Collection

**User Story:** As a platform operator, I want Fluent Bit to collect logs from all Docker containers, so that both application and infrastructure logs are forwarded to Loki through a single lightweight agent.

#### Acceptance Criteria

1. WHEN the Compose_Stack is started, THE Compose_Stack SHALL include a Fluent_Bit service using the `fluent/fluent-bit` image
2. THE Fluent_Bit service SHALL collect logs from all Docker containers in the Compose_Stack by reading from the Docker_Socket
3. THE Fluent_Bit service SHALL forward collected logs to Loki at `http://loki:3100/loki/api/v1/push`
4. THE Fluent_Bit service SHALL enrich each log entry with the container name as a `container` label
5. THE Fluent_Bit service SHALL enrich each log entry with the compose service name as a `service_name` label
6. THE Fluent_Bit service SHALL parse JSON-formatted log lines from Quarkus_Services to extract structured fields
7. THE Fluent_Bit service SHALL include a health check to verify readiness
8. THE Fluent_Bit service SHALL expose port 2020 for its built-in HTTP monitoring endpoint
9. IF the Docker_Socket is unavailable, THEN THE Fluent_Bit service SHALL log an error and exit with a non-zero status code

### Requirement 6: Add Jaeger for Trace Collection and Visualization

**User Story:** As a platform operator, I want Jaeger to receive traces from Quarkus_Services via OTLP and forward them to Tempo, so that traces are stored in Tempo and also accessible through the Jaeger UI.

#### Acceptance Criteria

1. WHEN the Compose_Stack is started, THE Compose_Stack SHALL include a Jaeger service using the `jaegertracing/jaeger` image (v2 or later)
2. THE Jaeger service SHALL accept traces via OTLP gRPC on port 4317
3. THE Jaeger service SHALL accept traces via OTLP HTTP on port 4318
4. THE Jaeger service SHALL forward all received traces to Tempo via OTLP gRPC at `tempo:4317`
5. THE Jaeger service SHALL expose port 16686 for the Jaeger query UI
6. THE Jaeger service SHALL persist its data using a named Docker volume or use Tempo as its storage backend
7. THE Jaeger service SHALL include a health check to verify readiness
8. WHEN a Quarkus_Service sends a trace via OTLP, THE Jaeger service SHALL accept the trace within 5 seconds under normal operating conditions

### Requirement 7: Reconfigure Quarkus Services for Jaeger Trace Export

**User Story:** As a platform operator, I want each Quarkus_Service to send traces to Jaeger instead of the OTel_Collector, so that trace data flows through the new tracing pipeline.

#### Acceptance Criteria

1. THE each Quarkus_Service SHALL set the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable to `http://jaeger:4317`
2. THE each Quarkus_Service SHALL include a `depends_on` reference to the Jaeger service
3. WHEN a Quarkus_Service starts, THE Quarkus_Service SHALL send traces to Jaeger using the OTLP gRPC protocol

### Requirement 8: Update Environment Variables

**User Story:** As a platform operator, I want the `.env` file to reflect the new observability stack configuration, so that all port mappings and endpoints are correct for the migrated components.

#### Acceptance Criteria

1. THE `.env` file SHALL remove the `OTEL_COLLECTOR_PORT_GRPC` variable
2. THE `.env` file SHALL remove the `OTEL_COLLECTOR_PORT_HTTP` variable
3. THE `.env` file SHALL remove the `OTEL_EXPORTER_OTLP_ENDPOINT` variable referencing `otel-collector`
4. THE `.env` file SHALL add a `PROMETHEUS_PORT` variable with a default value of `9090`
5. THE `.env` file SHALL add a `JAEGER_UI_PORT` variable with a default value of `16686`
6. THE `.env` file SHALL add a `JAEGER_OTLP_GRPC_PORT` variable with a default value of `4317`
7. THE `.env` file SHALL add a `FLUENT_BIT_MONITOR_PORT` variable with a default value of `2020`
8. THE `.env` file SHALL update the `OTEL_EXPORTER_OTLP_ENDPOINT` variable to reference `http://jaeger:4317`

### Requirement 9: Remove Obsolete Configuration Files

**User Story:** As a platform operator, I want obsolete configuration files to be removed from the repository, so that the codebase reflects only the active observability components.

#### Acceptance Criteria

1. THE repository SHALL NOT contain the file `observability/otel-collector-config.yaml`
2. THE repository SHALL NOT contain the file `observability/promtail-config.yaml`

### Requirement 10: Preserve Grafana Backend Stack

**User Story:** As a platform operator, I want the Grafana backend stack (Mimir, Loki, Tempo, Grafana) to remain unchanged, so that long-term storage and dashboarding continue to function without disruption.

#### Acceptance Criteria

1. THE Compose_Stack SHALL retain the Mimir service with its existing configuration
2. THE Compose_Stack SHALL retain the Loki service with its existing configuration
3. THE Compose_Stack SHALL retain the Tempo service with its existing configuration
4. THE Compose_Stack SHALL retain the Grafana service with its existing datasource provisioning for Mimir, Loki, and Tempo
5. THE Grafana datasources configuration SHALL continue to reference Mimir at `http://mimir:9009/prometheus`, Loki at `http://loki:3100`, and Tempo at `http://tempo:3200`

### Requirement 11: Update Grafana Datasources for Jaeger

**User Story:** As a platform operator, I want Grafana to include a Jaeger datasource, so that traces can be queried directly from the Jaeger UI link within Grafana.

#### Acceptance Criteria

1. THE Grafana datasources provisioning SHALL add a Jaeger datasource pointing to `http://jaeger:16686`
2. THE existing Tempo datasource configuration SHALL remain unchanged

### Requirement 12: Maintain Service Dependency Order

**User Story:** As a platform operator, I want Docker Compose service dependencies to reflect the new architecture, so that services start in the correct order.

#### Acceptance Criteria

1. THE Prometheus service SHALL depend on Mimir being started
2. THE Fluent_Bit service SHALL depend on Loki being healthy
3. THE Jaeger service SHALL depend on Tempo being healthy
4. THE each Quarkus_Service SHALL depend on Jaeger being started
5. THE Grafana service SHALL retain its existing dependencies on Loki, Mimir, and Tempo
6. WHEN the Compose_Stack is started, THE all services SHALL start without circular dependency errors

### Requirement 13: Update Tempo Configuration for Jaeger Integration

**User Story:** As a platform operator, I want Tempo to accept traces forwarded from Jaeger, so that all traces are stored in Tempo regardless of the ingestion path.

#### Acceptance Criteria

1. THE Tempo configuration SHALL accept OTLP gRPC traces on port 4317 (existing configuration is sufficient)
2. WHEN Jaeger forwards a trace via OTLP gRPC, THE Tempo service SHALL ingest and store the trace

### Requirement 14: Grafana Dashboard Compatibility

**User Story:** As a platform operator, I want existing Grafana dashboards to remain functional after the migration, so that monitoring continuity is preserved.

#### Acceptance Criteria

1. WHEN Prometheus scrapes Quarkus_Service Metrics_Endpoints, THE metric names SHALL use the same Micrometer-generated names as previously exported via OTLP (e.g., `http_server_requests_seconds`, `jvm_memory_used_bytes`)
2. THE existing Grafana dashboards SHALL continue to display data from Mimir, Loki, and Tempo without modification to datasource references
3. IF a dashboard query references OTel_Collector-specific metric names or labels, THEN THE dashboard SHALL be updated to use the Prometheus-scraped equivalents
