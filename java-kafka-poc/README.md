# Spring Boot Kafka PoC — IoT Telemetry Pipeline

A proof-of-concept demonstrating event-driven microservices using Spring Boot and Apache Kafka, with an IoT/telemetry domain.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Docker Compose                                │
│  ┌──────────┐  ┌─────────────────┐  ┌────────────┐  ┌──────────┐  │
│  │  Kafka   │  │ Schema Registry │  │ PostgreSQL │  │ Kafka UI │  │
│  │ (KRaft)  │  │                 │  │            │  │          │  │
│  └──────────┘  └─────────────────┘  └────────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐        ┌──────────────────────┐       ┌────────────┐
│  sensor-ingest   │──────▶ │  telemetry-processor │─────▶ │ PostgreSQL │
│   (Producer)     │  topic │    (Consumer/        │  JPA  │            │
│                  │ sensor │     Producer)         │       │            │
│ REST API to      │ .data  │                      │       └────────────┘
│ simulate sensors │        │ Persists readings,   │
│                  │        │ publishes alerts      │
│ Avro serialized  │        │                      │
│ Transactional    │        │ DLQ on failure       │
│                  │        │ Consumer groups      │
└──────────────────┘        └──────────────────────┘
                                     │
                                     ▼  topic: sensor.alerts
                            ┌──────────────────────┐
                            │  Alert consumers /   │
                            │  monitor group       │
                            └──────────────────────┘
```

## Kafka Features Demonstrated

| Feature | Implementation |
|---------|---------------|
| **Avro + Schema Registry** | `common-avro` module with generated classes, Confluent Schema Registry |
| **Schema Evolution** | Optional `batteryLevel` field added (backward-compatible), v2 endpoint |
| **Exactly-Once Semantics** | Transactional producer (`executeInTransaction`), `read_committed` consumer |
| **Dead Letter Queue (DLQ)** | `DeadLetterPublishingRecoverer` → `sensor.data.DLT` after 3 retries |
| **Multiple Partitions** | `sensor.data` topic with 3 partitions, `sensorId` as partition key |
| **Consumer Groups** | `telemetry-processor-group` (persist) + `telemetry-monitor-group` (log/count) |
| **Partition Ordering** | Same `sensorId` always routes to same partition |
| **End-to-End Tracing** | Trace ID generated at ingest, propagated via Kafka headers, visible in all consumers |

## Prerequisites

- Java 21 (JDK)
- Docker & Docker Compose
- Gradle (wrapper included)

## Project Structure

```
java-kafka-poc/
├── common-avro/             # Avro schema + generated classes
│   └── src/main/avro/       # .avsc schema files
├── sensor-ingest/           # REST producer service (port 8080)
├── telemetry-processor/     # Consumer + DB writer service (port 8082)
├── scripts/                 # Management and test scripts
│   ├── run.sh / run.ps1 / run.bat
│   ├── generate-data.sh / generate-data.ps1 / generate-data.bat
│   └── check-db.sh / check-db.ps1 / check-db.bat
├── docker-compose.yml       # Kafka KRaft + Schema Registry + PostgreSQL + Kafka UI
├── build.gradle             # Root Gradle config
└── settings.gradle          # Multi-module settings
```

## Quick Start

### 1. Build the Project

You **must** build before running `./scripts/run.sh up` — Docker needs the fat JARs to exist in each module's `build/libs/` directory.

```bash
./gradlew build
```

Or to skip tests and just produce the JARs:
```bash
./gradlew bootJar -x test
```

### 2. Start Infrastructure

```bash
# Infrastructure only (Kafka, Schema Registry, PostgreSQL, Kafka UI)
./scripts/run.sh infra

# Full stack including Spring Boot apps in Docker (requires build first — see step 1)
./scripts/run.sh up
```

Services started:
| Service | URL | Description |
|---------|-----|-------------|
| Kafka | `localhost:29092` | Broker (KRaft mode, no Zookeeper) |
| Schema Registry | `http://localhost:8081` | Avro schema management |
| PostgreSQL | `localhost:5432` | Data sink (db: `telemetry`) |
| Kafka UI | `http://localhost:8090` | Visual topic/message browser |

### 3. Run the Apps Locally (if using `infra` mode)

Terminal 1 — Telemetry Processor (consumer):
```bash
./gradlew :telemetry-processor:bootRun
```

Terminal 2 — Sensor Ingest (producer):
```bash
./gradlew :sensor-ingest:bootRun
```

### 4. Generate Sensor Data

```bash
# Infinite stream — 5 sensors, 1 reading/sec
./scripts/generate-data.sh

# 50 readings, 200ms apart, 10 sensors
./scripts/generate-data.sh --count 50 --interval 200 --sensors 10
```

Or via curl:
```bash
# Single reading
curl -X POST http://localhost:8080/api/sensors/readings \
  -H "Content-Type: application/json" \
  -d '{"sensorId":"sensor-001","temperature":23.5,"humidity":65.0,"location":"warehouse-A"}'

# Bulk simulation
curl -X POST "http://localhost:8080/api/sensors/simulate?sensorId=sensor-001&location=warehouse-A&count=20&intervalMs=200"

# v2 message (with battery level — schema evolution)
curl -X POST http://localhost:8080/api/sensors/readings/v2 \
  -H "Content-Type: application/json" \
  -d '{"sensorId":"mobile-001","temperature":28.0,"humidity":70.0,"location":"field","batteryLevel":85.5}'

# Trigger a temperature alert (threshold is 35°C)
curl -X POST http://localhost:8080/api/sensors/readings \
  -H "Content-Type: application/json" \
  -d '{"sensorId":"furnace-001","temperature":42.0,"humidity":20.0,"location":"boiler-room"}'
```

### 5. Validate the Pipeline

```bash
# Database summary + latest readings
./scripts/check-db.sh

# Watch records arrive in real-time
./scripts/check-db.sh --watch

# Per-sensor breakdown with averages
./scripts/check-db.sh --sensors

# Show alert-level readings (>35°C)
./scripts/check-db.sh --alerts

# Just the total count
./scripts/check-db.sh --count
```

Check Schema Registry:
```bash
curl http://localhost:8081/subjects
curl http://localhost:8081/subjects/sensor.data-value/versions
```

Browse topics and messages visually at [http://localhost:8090](http://localhost:8090) (Kafka UI).

## Scripts

All scripts are in `scripts/` with Windows (`.ps1`, `.bat`) and macOS/Linux (`.sh`) versions.

| Script | Description |
|--------|-------------|
| `run` | Manage Docker Compose stack (`up`, `infra`, `down`, `build`, `logs`, `status`) |
| `generate-data` | Send randomized sensor readings to the ingest service |
| `check-db` | Validate PostgreSQL is receiving records (`--summary`, `--count`, `--watch`, `--sensors`, `--alerts`) |

## End-to-End Tracing

Every request gets a unique `traceId` that flows through the entire pipeline:

```
[INGEST]  → REST endpoint generates traceId, returns it in response
[PRODUCE] → traceId attached as Kafka header (X-Trace-Id)
[CONSUME] → traceId extracted from header, set in MDC
[PERSIST] → traceId visible in DB persistence log
[ALERT]   → traceId visible in alert forwarding log
[MONITOR] → traceId visible in monitor consumer log
```

Example log output:
```
sensor-ingest:
09:15:32.001 [http-nio-8080-exec-1] INFO  SensorIngestController [traceId=a3f7c2d1] - [INGEST] traceId=a3f7c2d1 | Received reading: sensor=sensor-001 temp=42.0C
09:15:32.045 [kafka-producer-1]     INFO  SensorProducerService  [traceId=]         - [PRODUCE] traceId=a3f7c2d1 | SENT sensor=sensor-001 partition=2 offset=47

telemetry-processor:
09:15:32.112 [consumer-0-C-1]       INFO  TelemetryConsumer      [traceId=a3f7c2d1] - [CONSUME] traceId=a3f7c2d1 | Received: sensor=sensor-001 partition=2 offset=47 temp=42.0C
09:15:32.135 [consumer-0-C-1]       INFO  TelemetryConsumer      [traceId=a3f7c2d1] - [PERSIST] traceId=a3f7c2d1 | Saved to DB: id=12 sensor=sensor-001
09:15:32.140 [consumer-0-C-1]       WARN  TelemetryConsumer      [traceId=a3f7c2d1] - [ALERT] traceId=a3f7c2d1 | Temperature threshold exceeded! sensor=sensor-001 temp=42.0C
```

The `traceId` is also returned in the REST response:
```json
{"status": "accepted", "traceId": "a3f7c2d1", "sensorId": "sensor-001", ...}
```

## Running Tests

```bash
./gradlew test
```

Tests use **EmbeddedKafka** and **H2 in-memory database** — no external dependencies required.

## Key Design Decisions

1. **KRaft mode** — Eliminates Zookeeper dependency, simpler deployment (Kafka 3.3+ production-ready)
2. **Transactional producer** — `KafkaTemplate.executeInTransaction()` ensures atomic sends; if any message in a batch fails, all are rolled back
3. **`read_committed` isolation** — Consumers only see committed messages, preventing phantom reads during producer rollback
4. **`sensorId` as message key** — Guarantees ordering per sensor (all readings from one sensor go to the same partition)
5. **DLQ with retry** — 3 retries with 1-second backoff before routing to `sensor.data.DLT`, preventing poison messages from blocking the pipeline
6. **Two consumer groups** — `telemetry-processor-group` (writes to DB) and `telemetry-monitor-group` (logs/counts) demonstrate independent offset tracking
7. **Schema evolution** — Optional `batteryLevel` field with null default ensures backward compatibility; old consumers safely ignore the new field
8. **End-to-end tracing** — Correlation ID propagated via Kafka headers and MDC for cross-service observability

## Topics

| Topic | Partitions | Purpose |
|-------|-----------|---------|
| `sensor.data` | 3 | Main telemetry data flow |
| `sensor.alerts` | 3 | Temperature threshold alerts |
| `sensor.data.DLT` | 3 | Dead letter topic for failed messages |

## Stopping

```bash
./scripts/run.sh down
```
