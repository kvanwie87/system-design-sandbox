# Requirements Document

## Introduction

This document defines the requirements for a Redis Proof-of-Concept (PoC) application built with Spring Boot 4.1.1 and Java 21. The PoC demonstrates core Redis data types (Strings, Hashes, Lists, Sets, Sorted Sets) and common application patterns (Caching, Session Store, Pub/Sub Messaging, Rate Limiting, Leaderboard) through a REST API. The application uses RedisTemplate for data-type and pattern demonstrations, and the Spring Cache abstraction (@Cacheable/@CacheEvict) for the caching demo.

## Glossary

- **Application**: The Spring Boot 4.1.1 Redis PoC application running on Java 21 with Gradle build system
- **REST_API**: The set of HTTP endpoints exposed by the Application for interacting with Redis features
- **RedisTemplate**: The Spring Data Redis class used to perform Redis operations for data-type and pattern demos
- **Spring_Cache**: The Spring Cache abstraction using @Cacheable and @CacheEvict annotations for the caching demo
- **String_Endpoint**: REST endpoint that demonstrates Redis String data type operations
- **Hash_Endpoint**: REST endpoint that demonstrates Redis Hash data type operations
- **List_Endpoint**: REST endpoint that demonstrates Redis List data type operations
- **Set_Endpoint**: REST endpoint that demonstrates Redis Set data type operations
- **Sorted_Set_Endpoint**: REST endpoint that demonstrates Redis Sorted Set data type operations
- **Cache_Endpoint**: REST endpoint that demonstrates Spring Cache abstraction with Redis as backing store
- **Session_Endpoint**: REST endpoint that demonstrates Redis-backed session storage
- **PubSub_Endpoint**: REST endpoint that demonstrates Redis Pub/Sub messaging
- **Rate_Limiter**: Component that implements rate limiting using Redis
- **Leaderboard_Endpoint**: REST endpoint that demonstrates a leaderboard using Redis Sorted Sets
- **Docker_Compose**: The docker-compose.yml file that orchestrates Redis and the Application containers for local development
- **Dockerfile**: The multi-stage Dockerfile that builds and packages the Application as a container image
- **Scripts_Folder**: The `scripts/` directory at the project root containing shell scripts for development and testing workflows
- **E2E_Tests**: End-to-end test scripts that validate all REST API endpoints against a running application with Redis

## Requirements

### Requirement 1: Redis String Operations

**User Story:** As a developer, I want REST endpoints for Redis String operations, so that I can test basic key-value storage and retrieval via curl or Postman.

#### Acceptance Criteria

1. WHEN a PUT request with a key and value is received, THE String_Endpoint SHALL store the value in Redis as a String using RedisTemplate
2. WHEN a GET request with a key is received, THE String_Endpoint SHALL retrieve and return the corresponding String value from Redis using RedisTemplate
3. WHEN a DELETE request with a key is received, THE String_Endpoint SHALL remove the key from Redis and return a confirmation response
4. IF a GET request references a key that does not exist, THEN THE String_Endpoint SHALL return an HTTP 404 response with a descriptive message

### Requirement 2: Redis Hash Operations

**User Story:** As a developer, I want REST endpoints for Redis Hash operations, so that I can test storing and retrieving field-value pairs within a hash.

#### Acceptance Criteria

1. WHEN a PUT request with a hash key, field name, and field value is received, THE Hash_Endpoint SHALL store the field-value pair in the specified Redis Hash using RedisTemplate
2. WHEN a GET request with a hash key is received, THE Hash_Endpoint SHALL retrieve and return all field-value pairs from the specified Redis Hash
3. WHEN a GET request with a hash key and field name is received, THE Hash_Endpoint SHALL retrieve and return the value of the specified field from the Redis Hash
4. WHEN a DELETE request with a hash key and field name is received, THE Hash_Endpoint SHALL remove the specified field from the Redis Hash
5. IF a GET request references a hash key that does not exist, THEN THE Hash_Endpoint SHALL return an HTTP 404 response with a descriptive message

### Requirement 3: Redis List Operations

**User Story:** As a developer, I want REST endpoints for Redis List operations, so that I can test push, pop, and range retrieval on ordered collections.

#### Acceptance Criteria

1. WHEN a POST request with a list key and value is received, THE List_Endpoint SHALL push the value to the left (head) of the specified Redis List using RedisTemplate
2. WHEN a POST request with a list key, value, and direction "right" is received, THE List_Endpoint SHALL push the value to the right (tail) of the specified Redis List using RedisTemplate
3. WHEN a GET request with a list key is received, THE List_Endpoint SHALL retrieve and return all elements from the specified Redis List
4. WHEN a DELETE request with a list key and direction "left" is received, THE List_Endpoint SHALL pop and return the element from the left (head) of the Redis List
5. WHEN a DELETE request with a list key and direction "right" is received, THE List_Endpoint SHALL pop and return the element from the right (tail) of the Redis List
6. IF a GET request references a list key that does not exist, THEN THE List_Endpoint SHALL return an empty list response

### Requirement 4: Redis Set Operations

**User Story:** As a developer, I want REST endpoints for Redis Set operations, so that I can test adding, removing, and querying unique members in a set.

#### Acceptance Criteria

1. WHEN a POST request with a set key and member value is received, THE Set_Endpoint SHALL add the member to the specified Redis Set using RedisTemplate
2. WHEN a GET request with a set key is received, THE Set_Endpoint SHALL retrieve and return all members of the specified Redis Set
3. WHEN a DELETE request with a set key and member value is received, THE Set_Endpoint SHALL remove the specified member from the Redis Set
4. WHEN a GET request for set membership with a set key and member value is received, THE Set_Endpoint SHALL return a boolean indicating whether the member exists in the set
5. IF a GET request references a set key that does not exist, THEN THE Set_Endpoint SHALL return an empty set response

### Requirement 5: Redis Sorted Set Operations

**User Story:** As a developer, I want REST endpoints for Redis Sorted Set operations, so that I can test scored member storage and ranked retrieval.

#### Acceptance Criteria

1. WHEN a POST request with a sorted set key, member, and score is received, THE Sorted_Set_Endpoint SHALL add the member with the specified score to the Redis Sorted Set using RedisTemplate
2. WHEN a GET request with a sorted set key is received, THE Sorted_Set_Endpoint SHALL retrieve and return all members ordered by score in ascending order
3. WHEN a GET request with a sorted set key and a rank range is received, THE Sorted_Set_Endpoint SHALL retrieve and return members within the specified rank range
4. WHEN a DELETE request with a sorted set key and member is received, THE Sorted_Set_Endpoint SHALL remove the specified member from the Redis Sorted Set
5. WHEN a GET request for the score of a specific member is received, THE Sorted_Set_Endpoint SHALL return the score of that member
6. IF a GET request references a sorted set key that does not exist, THEN THE Sorted_Set_Endpoint SHALL return an empty set response

### Requirement 6: Caching with Spring Cache Abstraction

**User Story:** As a developer, I want a caching demo using Spring Cache annotations, so that I can observe cache hit/miss behavior backed by Redis.

#### Acceptance Criteria

1. WHEN a GET request for a cacheable resource is received and the result is not in the cache, THE Cache_Endpoint SHALL compute the result, store the result in Redis via @Cacheable, and return the result
2. WHEN a GET request for a cacheable resource is received and the result is already in the cache, THE Cache_Endpoint SHALL return the cached result from Redis without recomputing
3. WHEN a DELETE request to evict a cached resource is received, THE Cache_Endpoint SHALL remove the entry from the Redis cache via @CacheEvict and return a confirmation response
4. THE Application SHALL use Spring_Cache annotations (@Cacheable, @CacheEvict) with Redis as the cache store for the caching demo

### Requirement 7: Session Store

**User Story:** As a developer, I want REST endpoints for session management backed by Redis, so that I can test storing and retrieving session attributes.

#### Acceptance Criteria

1. WHEN a PUT request with a session identifier and attribute key-value pair is received, THE Session_Endpoint SHALL store the attribute in Redis under the session identifier using RedisTemplate
2. WHEN a GET request with a session identifier is received, THE Session_Endpoint SHALL retrieve and return all attributes stored for that session from Redis
3. WHEN a DELETE request with a session identifier is received, THE Session_Endpoint SHALL remove all attributes associated with that session from Redis
4. WHEN a PUT request with a session identifier and a time-to-live value is received, THE Session_Endpoint SHALL set the expiration of the session key in Redis to the specified time-to-live in seconds
5. IF a GET request references a session identifier that does not exist, THEN THE Session_Endpoint SHALL return an HTTP 404 response with a descriptive message

### Requirement 8: Pub/Sub Messaging

**User Story:** As a developer, I want REST endpoints for Redis Pub/Sub, so that I can publish messages to channels and observe received messages.

#### Acceptance Criteria

1. WHEN a POST request with a channel name and message body is received, THE PubSub_Endpoint SHALL publish the message to the specified Redis channel using RedisTemplate
2. THE Application SHALL register a message listener that subscribes to configured Redis channels and stores received messages in memory
3. WHEN a GET request for received messages on a channel is received, THE PubSub_Endpoint SHALL return the list of messages received by the listener for that channel
4. WHEN a DELETE request for received messages on a channel is received, THE PubSub_Endpoint SHALL clear the stored messages for that channel and return a confirmation response

### Requirement 9: Rate Limiting

**User Story:** As a developer, I want a rate-limiting demo backed by Redis, so that I can test request throttling behavior using Redis atomic operations.

#### Acceptance Criteria

1. WHEN a request to a rate-limited endpoint is received and the client has not exceeded the configured request limit within the time window, THE Rate_Limiter SHALL allow the request and return a successful response with remaining request count in a response header
2. WHEN a request to a rate-limited endpoint is received and the client has exceeded the configured request limit within the time window, THE Rate_Limiter SHALL reject the request with an HTTP 429 response and include a Retry-After header indicating seconds until the window resets
3. THE Rate_Limiter SHALL use RedisTemplate with atomic increment operations to track request counts per client identifier
4. THE Rate_Limiter SHALL use Redis key expiration to automatically reset the request count after the configured time window elapses

### Requirement 10: Leaderboard

**User Story:** As a developer, I want REST endpoints for a leaderboard backed by Redis Sorted Sets, so that I can test score-based ranking operations.

#### Acceptance Criteria

1. WHEN a POST request with a player name and score is received, THE Leaderboard_Endpoint SHALL add or update the player score in the Redis Sorted Set using RedisTemplate
2. WHEN a GET request for the top N players is received, THE Leaderboard_Endpoint SHALL return the top N players ordered by score in descending order with their ranks
3. WHEN a GET request for a specific player rank is received, THE Leaderboard_Endpoint SHALL return the player rank and score from the Redis Sorted Set
4. WHEN a POST request to increment a player score is received, THE Leaderboard_Endpoint SHALL atomically increment the player score in the Redis Sorted Set and return the new score
5. IF a GET request for a specific player references a player that does not exist in the leaderboard, THEN THE Leaderboard_Endpoint SHALL return an HTTP 404 response with a descriptive message

### Requirement 11: Project Configuration

**User Story:** As a developer, I want the project properly configured with required dependencies, so that all Redis features work out of the box.

#### Acceptance Criteria

1. THE Application SHALL include spring-boot-starter-web and spring-boot-starter-data-redis as Gradle dependencies
2. THE Application SHALL use Java 21 as the language version in the Gradle toolchain configuration
3. THE Application SHALL use Spring Boot 4.1.1 as the framework version
4. THE Application SHALL use the com.example.demo base package
5. THE Application SHALL include Redis connection configuration in the application.yaml file with sensible defaults for local development

### Requirement 12: Docker Containerization

**User Story:** As a developer, I want Docker Compose and a Dockerfile for the application, so that I can run the entire PoC stack (Redis + app) with a single command without installing Redis locally.

#### Acceptance Criteria

1. THE Application SHALL include a multi-stage Dockerfile that builds the application using Gradle and produces a minimal runtime image based on Eclipse Temurin JRE 21
2. THE Application SHALL include a docker-compose.yml that defines a Redis service using the official redis:7-alpine image exposed on port 6379
3. THE docker-compose.yml SHALL define an application service that builds from the Dockerfile and depends on the Redis service
4. THE application service in docker-compose.yml SHALL configure the Spring Redis host to connect to the Redis container by service name
5. THE docker-compose.yml SHALL expose the application on port 8080 of the host machine
6. WHEN `docker compose up` is executed, THE Redis service SHALL start first and THE application service SHALL wait for Redis to be ready before starting via a health check or depends_on condition
7. THE Dockerfile SHALL use a .dockerignore file to exclude build artifacts, IDE files, and the .git directory from the build context

### Requirement 13: Helper Scripts

**User Story:** As a developer, I want shell scripts for starting infrastructure and running end-to-end tests, so that I can quickly set up the environment and validate all endpoints with a single command.

#### Acceptance Criteria

1. THE Application SHALL include a `scripts/` directory at the project root containing shell scripts (.sh) for development workflows
2. THE Application SHALL include a `scripts/start-infra.sh` script that starts only the Redis container using Docker Compose profiles, allowing the developer to run the Spring Boot app locally against containerized Redis
3. THE Application SHALL include a `scripts/start-all.sh` script that starts both the Redis container and the application container using Docker Compose profiles
4. THE docker-compose.yml SHALL define Docker Compose profiles: an "infra" profile for the Redis service and an "app" profile for the application service
5. THE Application SHALL include a `scripts/e2e-test.sh` script that executes HTTP requests against all REST API endpoints and reports success or failure for each endpoint
6. THE `scripts/e2e-test.sh` script SHALL test at least one operation per feature area: Strings, Hashes, Lists, Sets, Sorted Sets, Cache, Sessions, Pub/Sub, Rate Limiting, and Leaderboard
7. THE `scripts/e2e-test.sh` script SHALL exit with code 0 when all tests pass and exit with a non-zero code when any test fails
8. THE `scripts/start-infra.sh` script SHALL wait for Redis to be healthy before returning, using the Docker health check status
9. All scripts in the `scripts/` directory SHALL be executable (chmod +x) and include a shebang line (#!/bin/bash)
