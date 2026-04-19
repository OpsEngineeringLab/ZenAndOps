# Design: Infrastructure Upgrade — MongoDB and Kafka

## Scope

This change is limited to `docker-compose.yml`. No application code changes are required.

## MongoDB 7 → 8

- Image tag change: `mongo:7` → `mongo:8`
- The `mongosh` healthcheck command (`db.adminCommand('ping')`) is fully supported in MongoDB 8
- Volume mount (`mongodb-data:/data/db`) remains compatible
- Environment variables (`MONGO_INITDB_ROOT_USERNAME`, `MONGO_INITDB_ROOT_PASSWORD`) remain the same

### Key Benefits of MongoDB 8
- ~32% higher throughput
- ~56% faster bulk writes
- Vector search capabilities for AI workloads
- Improved sharding and scalability

## Apache Kafka 3.9.0 → 4.2.0

- Image tag change: `apache/kafka:3.9.0` → `apache/kafka:4.2.0`
- Kafka 4.x fully removes ZooKeeper — KRaft is the only mode (project already uses KRaft)
- KRaft configuration properties remain compatible
- The `kafka-broker-api-versions.sh` healthcheck script path may differ in the new image

### Kafka Configuration Review

All current KRaft properties are forward-compatible with Kafka 4.x:
- `KAFKA_NODE_ID`, `KAFKA_PROCESS_ROLES`, `KAFKA_CONTROLLER_QUORUM_VOTERS` — unchanged
- `KAFKA_LISTENERS`, `KAFKA_ADVERTISED_LISTENERS` — unchanged
- `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP`, `KAFKA_CONTROLLER_LISTENER_NAMES` — unchanged
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` and transaction log settings — unchanged
- `CLUSTER_ID` — unchanged

### Kafka Healthcheck Path

In the `apache/kafka` Docker image 4.x, the Kafka binaries are located at `/opt/kafka/bin/`. The healthcheck command path should be verified but is expected to remain the same.

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| MongoDB data format incompatibility | Low | Docker volume can be recreated for dev environments |
| Kafka config property deprecation | Low | All KRaft properties are stable in 4.x |
| Healthcheck script path change | Medium | Verify path in Kafka 4.x image |

## Files Changed

- `docker-compose.yml` — image tags and potentially healthcheck paths
