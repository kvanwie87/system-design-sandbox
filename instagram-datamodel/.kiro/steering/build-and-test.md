---
inclusion: always
---

# Build and Test

## Build

Run the Gradle build from the project root:

```shell
./gradlew build
```

On Windows:

```shell
.\gradlew.bat build
```

## Run

```shell
./gradlew bootRun
```

## Test

```shell
./gradlew test
```

Tests use JUnit 5 (JUnit Platform) configured in `build.gradle`.

## Dependencies

Key Spring Data starters in use:

- `spring-boot-starter-data-jpa` — JPA entities and repositories (H2 in-memory for PoC)
- `spring-boot-starter-data-neo4j` — Neo4j graph nodes and relationships
- `spring-boot-starter-data-elasticsearch` — Elasticsearch documents and repositories

Test dependencies include the corresponding test starters which provide embedded/mock instances:

- `spring-boot-starter-data-jpa-test`
- `spring-boot-starter-data-neo4j-test`
- `spring-boot-starter-data-elasticsearch-test`

## Notes

- No external database infrastructure is required — all data stores use in-memory or embedded alternatives
- The H2 console can be enabled in `application.yaml` for debugging JPA entities during development
- If adding Redis support, use an embedded Redis mock or a simple in-memory stub
