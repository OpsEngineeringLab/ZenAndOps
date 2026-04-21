# Implementation Plan: Observability Stack Migration

## Overview

Migrate the ZenAndOps observability stack from an OTel Collector-centric architecture to purpose-built collectors: Prometheus (metrics), Fluent Bit (logs), and Jaeger (traces). The Grafana backend stack (Mimir, Loki, Tempo, Grafana) remains unchanged. All changes are infrastructure configuration — no application code modifications required.

## Tasks

- [ ] 1. Create Prometheus configuration and add service to Docker Compose
  - Create `observability/prometheus.yml` with scrape configs for auth-service (port 8081), dashboard-service (port 8082), gateway-service (port 8080) at `/q/metrics`, self-scrape, 15s interval, and remote_write to Mimir at `http://mimir:9009/api/v1/push`
  - Add `prometheus` service to `docker-compose.yml` using `prom/prometheus:v3.4.1` image, container name `zenandops-prometheus`, port `${PROMETHEUS_PORT:-9090}:9090`, volume `prometheus-data:/prometheus`, config mount `./observability/prometheus.yml:/etc/prometheus/prometheus.yml`, health check via `wget --no-verbose --tries=1 --spider http://localhost:9090/-/ready`, depends_on `mimir` (started)
  - Add `prometheus-data` to the named volumes section
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 12.1_

- [ ] 2. Create Fluent Bit configuration and add service to Docker Compose
  - Create `observability/fluent-bit.conf` with SERVICE (flush 1s, HTTP server on port 2020, health check on), INPUT (tail Docker container JSON logs), FILTER (JSON parser for structured Quarkus logs), OUTPUT (Loki at `loki:3100`, labels `job=fluent-bit`, line format JSON)
  - Create `observability/fluent-bit-parsers.conf` with `docker` parser (JSON format) and `json_parser` parser (JSON format for Quarkus structured logs)
  - Add `fluent-bit` service to `docker-compose.yml` using `fluent/fluent-bit:4.0` image, container name `zenandops-fluent-bit`, port `${FLUENT_BIT_MONITOR_PORT:-2020}:2020`, volume mounts for Docker socket (`/var/run/docker.sock:/var/run/docker.sock:ro`) and config files, health check via `wget --no-verbose --tries=1 --spider http://localhost:2020/api/v1/health`, depends_on `loki` (healthy)
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 12.2_

- [ ] 3. Create Jaeger configuration and add service to Docker Compose
  - Create `observability/jaeger-config.yaml` with OTLP gRPC receiver (port 4317), OTLP HTTP receiver (port 4318), batch processor, `otlp/tempo` exporter to `tempo:4317` (insecure TLS), `jaeger_storage` extension with in-memory backend (10,000 traces), `jaeger_query` extension, and `healthcheckv2` extension on port 13133
  - Add `jaeger` service to `docker-compose.yml` using `jaegertracing/jaeger:2.6.0` image, container name `zenandops-jaeger`, ports `${JAEGER_UI_PORT:-16686}:16686`, `${JAEGER_OTLP_GRPC_PORT:-4317}:4317`, `4318:4318`, config mount `./observability/jaeger-config.yaml:/etc/jaeger/config.yaml`, command `["--config", "/etc/jaeger/config.yaml"]`, health check via `wget --no-verbose --tries=1 --spider http://localhost:13133/`, depends_on `tempo` (healthy)
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 12.3_

- [ ] 4. Checkpoint — Validate new services configuration
  - Ensure all new config files are syntactically valid
  - Ensure `docker-compose.yml` has no YAML syntax errors
  - Ask the user if questions arise

- [ ] 5. Reconfigure Quarkus services and remove OTel Collector and Promtail
  - [ ] 5.1 Update Quarkus service definitions in `docker-compose.yml`
    - Change `OTEL_EXPORTER_OTLP_ENDPOINT` from `http://otel-collector:4317` to `http://jaeger:4317` for auth-service, dashboard-service, and gateway-service
    - Replace `depends_on: otel-collector` with `depends_on: jaeger` (condition: `service_started`) for all three Quarkus services
    - _Requirements: 4.2, 4.3, 7.1, 7.2, 7.3, 12.4_
  - [ ] 5.2 Remove OTel Collector service from `docker-compose.yml`
    - Remove the entire `otel-collector` service definition including image, ports, volumes, depends_on, and networks
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [ ] 5.3 Remove Promtail service from `docker-compose.yml`
    - Remove the entire `promtail` service definition including image, volumes, command, depends_on, and networks
    - _Requirements: 2.1, 2.2_

- [ ] 6. Update environment variables and Grafana datasources
  - [ ] 6.1 Update `.env` and `.env.example` files
    - Remove `OTEL_COLLECTOR_PORT_GRPC`, `OTEL_COLLECTOR_PORT_HTTP` variables and their comments
    - Update `OTEL_EXPORTER_OTLP_ENDPOINT` from `http://otel-collector:4317` to `http://jaeger:4317`
    - Add `PROMETHEUS_PORT=9090`, `JAEGER_UI_PORT=16686`, `JAEGER_OTLP_GRPC_PORT=4317`, `FLUENT_BIT_MONITOR_PORT=2020` with descriptive comments
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8_
  - [ ] 6.2 Add Jaeger datasource to Grafana provisioning
    - Add a Jaeger datasource entry to `observability/grafana/provisioning/datasources/datasources.yaml` with name `Jaeger`, type `jaeger`, access `proxy`, uid `jaeger`, url `http://jaeger:16686`
    - Verify existing Mimir, Loki, and Tempo datasources remain unchanged
    - _Requirements: 11.1, 11.2, 10.4, 10.5_

- [ ] 7. Remove obsolete configuration files
  - Delete `observability/otel-collector-config.yaml`
  - Delete `observability/promtail-config.yaml`
  - _Requirements: 9.1, 9.2_

- [ ] 8. Final checkpoint — Validate complete migration
  - Ensure `docker-compose.yml` contains no references to `otel-collector` or `promtail`
  - Ensure all Quarkus services point `OTEL_EXPORTER_OTLP_ENDPOINT` to `http://jaeger:4317`
  - Ensure Grafana datasources include Mimir, Loki, Tempo, and Jaeger
  - Ensure Mimir, Loki, Tempo, and Grafana service definitions are unchanged
  - Ensure all tests pass, ask the user if questions arise
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 14.1, 14.2, 14.3_

- [ ] 9. Version control and release
  - [ ] Ensure all previous tasks are complete and tests pass
  - [ ] Remove SNAPSHOT suffix from all version references in the codebase
  - [ ] Commit the version bump: "release: 1.5.0 - observability-stack-migration"
  - [ ] Merge branch into main/master
  - [ ] Apply Git tag: 1.5.0 (without SNAPSHOT)
  - [ ] Push branch, merge, and tag to remote

## Notes

- No property-based tests — this is a declarative infrastructure configuration migration with no application logic to test
- No application code changes needed — Quarkus services already expose `/q/metrics` via Micrometer Prometheus registry
- The Grafana backend stack (Mimir, Loki, Tempo, Grafana) remains completely unchanged
- Each task references specific requirement acceptance criteria for traceability
- Checkpoints ensure incremental validation of the migration
