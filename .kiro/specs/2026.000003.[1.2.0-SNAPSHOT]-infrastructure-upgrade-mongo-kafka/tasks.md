# Tasks: Infrastructure Upgrade — MongoDB and Kafka

- [x] 1. Upgrade MongoDB image from `mongo:7` to `mongo:8` in `docker-compose.yml`
  - [x] Update the image tag
  - [x] Verify healthcheck compatibility with MongoDB 8

- [x] 2. Upgrade Kafka image from `apache/kafka:3.9.0` to `apache/kafka:4.2.0` in `docker-compose.yml`
  - [x] Update the image tag
  - [x] Review and update KRaft configuration properties for Kafka 4.x compatibility
  - [x] Verify healthcheck command path for Kafka 4.x

- [-] 3. Version control and release
  - [x] Ensure all previous tasks are complete and tests pass
  - [x] Remove SNAPSHOT suffix from all version references in the codebase
  - [ ] Commit the version bump: "release: 1.2.0 - infrastructure-upgrade-mongo-kafka"
  - [ ] Merge branch into main/master
  - [ ] Apply Git tag: 1.2.0 (without SNAPSHOT)
  - [ ] Push branch, merge, and tag to remote
