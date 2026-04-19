# Requirements: Infrastructure Upgrade — MongoDB and Kafka

## Overview

Upgrade the MongoDB and Apache Kafka infrastructure components to their latest stable versions to benefit from performance improvements, security patches, and new features.

## Current State

| Component | Current Version | Target Version |
|---|---|---|
| MongoDB (Docker image) | `mongo:7` | `mongo:8` |
| Apache Kafka (Docker image) | `apache/kafka:3.9.0` | `apache/kafka:4.2.0` |

## Functional Requirements

1. **FR-01**: Update the MongoDB Docker image from `mongo:7` to `mongo:8` in `docker-compose.yml`
2. **FR-02**: Update the Apache Kafka Docker image from `apache/kafka:3.9.0` to `apache/kafka:4.2.0` in `docker-compose.yml`
3. **FR-03**: Review and update any Kafka configuration properties that changed between 3.9 and 4.x
4. **FR-04**: Ensure MongoDB healthcheck command remains compatible with MongoDB 8
5. **FR-05**: Ensure Kafka healthcheck command remains compatible with Kafka 4.x

## Non-Functional Requirements

1. **NFR-01**: No downtime impact — this is a Docker image version bump for development/staging environments
2. **NFR-02**: All existing environment variable references must continue to work
3. **NFR-03**: No changes to application code — only infrastructure configuration

## Acceptance Criteria

- [ ] `docker-compose.yml` references `mongo:8` instead of `mongo:7`
- [ ] `docker-compose.yml` references `apache/kafka:4.2.0` instead of `apache/kafka:3.9.0`
- [ ] Kafka KRaft configuration is compatible with Kafka 4.x
- [ ] Healthchecks for both services are valid for the new versions
