# Implementation Plan — Spring Boot Kafka PoC (IoT Telemetry)

## Problem Statement

Build a proof-of-concept demonstrating event-driven microservices using Spring Boot and Kafka, with an IoT/telemetry domain. Sensor data is ingested, processed/aggregated, and persisted to a database — showcasing Avro + Schema Registry, DLQ error handling, exactly-once semantics, and partitioned consumer groups.

## Requirements

- Domain: IoT telemetry (sensors emit readings → processor aggregates → stores in DB)
- Multi-module Gradle (Groovy DSL) project with separate services
- Kafka in KRaft mode via Docker Compose (no Zookeeper)
- Confluent Schema Registry + Avro serialization
- Dead Letter Queue (DLQ) for poison messages
- Exactly-once semantics (transactional producer, idempotent consumer)
- Multiple partitions & consumer groups to demonstrate scaling
- PostgreSQL as the data sink via Spring Data JPA
- Java 21, Spring Boot 4.1.0

## Background

- The existing workspace already has a basic Spring Boot 4.1.0 / Gradle Groovy scaffold at `java-kafka-poc`
- Spring Kafka 4.0.x aligns with Spring Boot 4.x and kafka-clients 4.1.2
- Confluent Schema Registry provides REST API and Avro serde integration
- KRaft mode eliminates the Zookeeper dependency (Kafka 3.3+ production-ready, 4.x default)

## Proposed Solution

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Docker Compose                                │
│  ┌──────────┐  ┌─────────────────┐  ┌────────────┐                 │
│  │  Kafka   │  │ Schema Registry │  │ PostgreSQL │                 │
│  │ (KRaft)  │  │                 │  │            │                 │
│  └──────────┘  └─────────────────┘  └────────────┘                 │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐        ┌──────────────────────┐       ┌────────────┐
│  sensor-ingest   │──────▶ │  telemetry-processor │─────▶ │ PostgreSQL │
│   (Producer)     │  topic │    (Consumer/        │  JPA  │            │
│                  │ sensor │     Producer)         │       │            │
│ REST API to      │ .data  │                      │       └────────────┘
│ simulate sensors │        │ Aggregates, writes   │
│                  │        │ to DB & downstream   │
│ Avro serialized  │        │ topic                │
│ Transactional    │        │                      │
└──────────────────┘        │ DLQ on failure       │
                            │ Exactly-once         │
                            │ Consumer groups      │
                            └──────────────────────┘
                                     │
                                     ▼  topic: sensor.alerts
                            ┌──────────────────────┐
                            │  (Future consumer /  │
                            │   demo via console)  │
                            └──────────────────────┘
```

### Modules

1. `common-avro` — Avro schema definitions & generated classes
2. `sensor-ingest` — Spring Boot app (REST producer)
3. `telemetry-processor` — Spring Boot app (consumer + processor + DB writer)
4. Root project — Docker Compose, shared Gradle config

## Task Breakdown

### Task 1: Restructure into a multi-module Gradle project with Docker Compose

**Objective:** Convert the existing single-module project into a multi-module layout and stand up the Kafka/infrastructure stack.

**Implementation guidance:**

- Update `settings.gradle` to include subprojects: `common-avro`, `sensor-ingest`, `telemetry-processor`
- Root `build.gradle` defines shared plugins (Spring Boot, dependency management) and common dependencies
- Each submodule gets its own `build.gradle`
- Create `docker-compose.yml` at root with:
  - Kafka in KRaft mode (single broker, 3 partitions for `sensor.data` topic)
  - Confluent Schema Registry
  - PostgreSQL 16
- Remove the existing `src/` directory (replaced by submodule sources)

**Test requirements:**

- `docker compose up -d` starts all services cleanly
- `./gradlew build` compiles without errors

**Demo:** Running `docker compose up` brings up Kafka, Schema Registry, and PostgreSQL. The Gradle multi-module build compiles successfully.

---

### Task 2: Define Avro schemas and generate Java classes in `common-avro`

**Objective:** Create the shared Avro schema for sensor telemetry events and configure the Gradle Avro plugin to generate Java classes.

**Implementation guidance:**

- Add `com.github.davidmc24.gradle.plugin.avro` plugin to `common-avro/build.gradle`
- Define `src/main/avro/SensorReading.avsc`:
  ```json
  {
    "type": "record",
    "name": "SensorReading",
    "namespace": "com.example.telemetry.avro",
    "fields": [
      {"name": "sensorId", "type": "string"},
      {"name": "temperature", "type": "double"},
      {"name": "humidity", "type": "double"},
      {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
      {"name": "location", "type": "string"}
    ]
  }
  ```
- Configure generated sources output so other modules can depend on `common-avro`

**Test requirements:**

- `./gradlew :common-avro:build` generates `SensorReading.java` without errors
- Unit test verifying serialization/deserialization roundtrip of a `SensorReading` instance

**Demo:** The build generates Avro classes and a test proves they serialize/deserialize correctly.

---

### Task 3: Implement `sensor-ingest` producer service with REST API

**Objective:** Create a Spring Boot app that exposes a REST endpoint to simulate sensor data, serializes it as Avro, and publishes to the `sensor.data` topic transactionally.

**Implementation guidance:**

- Depends on `common-avro`
- Add dependencies: `spring-boot-starter-web`, `spring-kafka`, `io.confluent:kafka-avro-serializer`
- Configure `application.yml`:
  - Kafka bootstrap servers
  - Schema Registry URL
  - Transactional ID prefix (enables exactly-once on producer side)
  - Key serializer: String, Value serializer: KafkaAvro
- Create `KafkaProducerConfig` with `KafkaTransactionManager` bean
- Create `SensorIngestController` with:
  - `POST /api/sensors/readings` — accepts JSON, converts to Avro, sends transactionally
  - `POST /api/sensors/simulate?count=100&intervalMs=500` — bulk simulation endpoint
- Use `KafkaTemplate.executeInTransaction()` for atomic sends

**Test requirements:**

- Integration test using Testcontainers (Kafka + Schema Registry) verifying messages land on the topic
- Unit test for the controller logic

**Demo:** Start the service, POST sensor data via curl/Postman, and confirm messages appear on the `sensor.data` topic (use `kafka-console-consumer` or a quick `@KafkaListener` log).

---

### Task 4: Implement `telemetry-processor` consumer with exactly-once and DLQ

**Objective:** Create a Spring Boot app that consumes from `sensor.data`, processes readings, persists to PostgreSQL, and handles errors via a Dead Letter Topic.

**Implementation guidance:**

- Depends on `common-avro`
- Add dependencies: `spring-boot-starter-data-jpa`, `spring-kafka`, `io.confluent:kafka-avro-deserializer`, `postgresql` driver
- Configure `application.yml`:
  - Consumer group: `telemetry-processor-group`
  - Isolation level: `read_committed` (exactly-once consumer side)
  - Auto-offset-reset: `earliest`
  - Concurrency: 3 (matches partition count)
  - Schema Registry URL
- Create JPA entity `SensorReadingEntity` and `SensorReadingRepository`
- Create `TelemetryConsumer` with `@KafkaListener`:
  - Deserializes Avro `SensorReading`
  - Persists to PostgreSQL
  - If temperature exceeds threshold → publish alert to `sensor.alerts` topic
- Configure `DefaultErrorHandler` with `DeadLetterPublishingRecoverer`:
  - Failed messages route to `sensor.data.DLT` after 3 retries
- Enable exactly-once with `spring.kafka.consumer.properties.isolation.level=read_committed` and `spring.kafka.listener.ack-mode=record`

**Test requirements:**

- Integration test (Testcontainers: Kafka + PostgreSQL) verifying:
  - Valid messages are persisted to the DB
  - Poison messages (malformed) end up in the DLT
  - Alert messages published to `sensor.alerts` when threshold exceeded

**Demo:** Send sensor readings via `sensor-ingest`, observe them persisted in PostgreSQL, deliberately send a bad message and see it routed to the DLQ topic.

---

### Task 5: Demonstrate multiple partitions and consumer groups

**Objective:** Configure the `sensor.data` topic with multiple partitions and demonstrate parallel consumption and independent consumer groups.

**Implementation guidance:**

- Ensure `sensor.data` topic has 3 partitions (configured via Docker Compose topic auto-creation or a startup script)
- `telemetry-processor` uses concurrency=3, so each thread handles one partition
- Add a second consumer group `telemetry-monitor-group` (a lightweight `@KafkaListener` in the processor app that only logs/counts messages) to demonstrate independent offset tracking
- Add partition key strategy in `sensor-ingest`: use `sensorId` as the message key so readings from the same sensor always land on the same partition (ordering guarantee)
- Add actuator or logging to show which partition each consumer instance is assigned

**Test requirements:**

- Integration test verifying messages with the same key always land on the same partition
- Test showing both consumer groups receive all messages independently

**Demo:** Send multiple sensor readings, observe logs showing partition assignments, and confirm the monitor group has independent offsets from the processor group.

---

### Task 6: Schema evolution demonstration

**Objective:** Evolve the Avro schema to demonstrate backward-compatible changes working with Schema Registry.

**Implementation guidance:**

- Add a new optional field to `SensorReading.avsc`: `{"name": "batteryLevel", "type": ["null", "double"], "default": null}`
- Register the new schema version (Schema Registry auto-registers on produce)
- Show that the existing consumer (compiled against v1) can still read v2 messages (backward compatibility)
- Add a REST endpoint or test that produces v2 messages
- Verify compatibility mode is set to `BACKWARD` in Schema Registry

**Test requirements:**

- Integration test: produce v2 message, consume with v1-compiled consumer, verify no errors
- Test Schema Registry API confirms two schema versions registered

**Demo:** Produce messages with the new field, show old consumers still work, query Schema Registry REST API to see both versions.

---

### Task 7: End-to-end integration test and documentation

**Objective:** Wire everything together with a full end-to-end test and provide a README with run instructions.

**Implementation guidance:**

- Create an end-to-end test (Testcontainers) that:
  1. Starts Kafka, Schema Registry, PostgreSQL
  2. Boots both Spring apps (or uses `@EmbeddedKafka` alternative)
  3. Produces sensor readings via REST call to `sensor-ingest`
  4. Asserts data appears in PostgreSQL via `telemetry-processor`
  5. Asserts DLQ receives poison messages
  6. Asserts alert topic receives threshold-exceeded events
- Write `README.md` covering:
  - Architecture diagram
  - Prerequisites (Docker, Java 21)
  - How to run (`docker compose up`, `./gradlew :sensor-ingest:bootRun`, etc.)
  - How to test (`./gradlew test`)
  - Key design decisions and Kafka feature demonstrations

**Test requirements:**

- Full E2E test passes in CI-like fashion (Testcontainers, no external dependencies)

**Demo:** Clone the repo, run `docker compose up -d && ./gradlew bootRun --parallel`, hit the simulate endpoint, and observe the full pipeline in action — data in Postgres, alerts on the topic, DLQ handling poison pills.
