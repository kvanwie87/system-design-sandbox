# Implementation Plan: Redis PoC Spring Boot Application

## Overview

This plan implements a Spring Boot 4.1.1 REST API demonstrating Redis data types (Strings, Hashes, Lists, Sets, Sorted Sets) and common application patterns (Caching, Session Store, Pub/Sub Messaging, Rate Limiting, Leaderboard). Tasks are organized from foundational configuration through data type endpoints, pattern endpoints, and property-based tests. Each task builds incrementally on previous work.

## Tasks

- [ ] 1. Project setup and configuration
  - [ ] 1.1 Update build.gradle with required dependencies
    - Add `spring-boot-starter-web`, `spring-boot-starter-data-redis`, and `spring-boot-starter-cache` to dependencies
    - _Requirements: 11.1, 11.2, 11.3_

  - [ ] 1.2 Create application.yaml configuration
    - Configure Spring application name, Redis connection (host, port, timeout), and rate-limit properties
    - Place at `src/main/resources/application.yaml`
    - _Requirements: 11.5_

  - [ ] 1.3 Create RedisConfig configuration class
    - Create `com.example.demo.config.RedisConfig` with `RedisTemplate<String, Object>` bean using StringRedisSerializer for keys and GenericJackson2JsonRedisSerializer for values
    - Configure hash key/value serializers
    - Configure `RedisMessageListenerContainer` bean
    - _Requirements: 11.4, 8.2_

  - [ ] 1.4 Create CacheConfig configuration class
    - Create `com.example.demo.config.CacheConfig` with `@EnableCaching` and `RedisCacheManager` bean
    - Configure default cache serialization with GenericJackson2JsonRedisSerializer
    - _Requirements: 6.4_

- [ ] 2. Exception handling and DTOs
  - [ ] 2.1 Create ResourceNotFoundException class
    - Create `com.example.demo.exception.ResourceNotFoundException` extending `RuntimeException`
    - _Requirements: 1.4, 2.5, 7.5, 10.5_

  - [ ] 2.2 Create GlobalExceptionHandler
    - Create `com.example.demo.exception.GlobalExceptionHandler` with `@RestControllerAdvice`
    - Handle `ResourceNotFoundException` returning HTTP 404 with JSON body `{"error": "message"}`
    - _Requirements: 1.4, 2.5, 7.5, 10.5_

  - [ ] 2.3 Create DTO records
    - Create `com.example.demo.dto.LeaderboardEntry` record with fields: `player`, `score`, `rank`
    - Create `com.example.demo.dto.RateLimitInfo` record with fields: `allowed`, `remaining`, `retryAfterSeconds`
    - _Requirements: 9.1, 9.2, 10.2, 10.3_

- [ ] 3. Checkpoint - Ensure project compiles
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Redis String operations
  - [ ] 4.1 Implement StringService
    - Create `com.example.demo.service.StringService` using `ValueOperations` from RedisTemplate
    - Implement `set(key, value)`, `get(key)`, and `delete(key)` methods
    - Throw `ResourceNotFoundException` when key does not exist on get
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [ ] 4.2 Implement StringController
    - Create `com.example.demo.controller.StringController` at `/api/strings`
    - Implement PUT `/{key}`, GET `/{key}`, DELETE `/{key}` endpoints
    - Delegate to StringService
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [ ]* 4.3 Write property tests for String operations
    - **Property 1: String value round-trip**
    - **Property 2: String deletion removes key**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4**

- [ ] 5. Redis Hash operations
  - [ ] 5.1 Implement HashService
    - Create `com.example.demo.service.HashService` using `HashOperations` from RedisTemplate
    - Implement `putField(key, field, value)`, `getAll(key)`, `getField(key, field)`, and `deleteField(key, field)` methods
    - Throw `ResourceNotFoundException` when hash key does not exist on get
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 5.2 Implement HashController
    - Create `com.example.demo.controller.HashController` at `/api/hashes`
    - Implement PUT `/{key}/{field}`, GET `/{key}`, GET `/{key}/{field}`, DELETE `/{key}/{field}` endpoints
    - Delegate to HashService
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ]* 5.3 Write property tests for Hash operations
    - **Property 3: Hash field round-trip**
    - **Property 4: Hash field deletion removes field**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**

- [ ] 6. Redis List operations
  - [ ] 6.1 Implement ListService
    - Create `com.example.demo.service.ListService` using `ListOperations` from RedisTemplate
    - Implement `push(key, value, direction)`, `getAll(key)`, and `pop(key, direction)` methods
    - Support "left" and "right" direction parameters
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [ ] 6.2 Implement ListController
    - Create `com.example.demo.controller.ListController` at `/api/lists`
    - Implement POST `/{key}?direction=`, GET `/{key}`, DELETE `/{key}?direction=` endpoints
    - Delegate to ListService
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [ ]* 6.3 Write property tests for List operations
    - **Property 5: List push-then-retrieve preserves order**
    - **Property 6: List pop returns correct end element**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

- [ ] 7. Redis Set operations
  - [ ] 7.1 Implement SetService
    - Create `com.example.demo.service.SetService` using `SetOperations` from RedisTemplate
    - Implement `add(key, member)`, `getAll(key)`, `isMember(key, member)`, and `remove(key, member)` methods
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ] 7.2 Implement SetController
    - Create `com.example.demo.controller.SetController` at `/api/sets`
    - Implement POST `/{key}`, GET `/{key}`, GET `/{key}/member/{member}`, DELETE `/{key}/{member}` endpoints
    - Delegate to SetService
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 7.3 Write property tests for Set operations
    - **Property 7: Set membership round-trip**
    - **Property 8: Set removal revokes membership**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4**

- [ ] 8. Redis Sorted Set operations
  - [ ] 8.1 Implement SortedSetService
    - Create `com.example.demo.service.SortedSetService` using `ZSetOperations` from RedisTemplate
    - Implement `add(key, member, score)`, `getAll(key)`, `getRange(key, start, end)`, `getScore(key, member)`, and `remove(key, member)` methods
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ] 8.2 Implement SortedSetController
    - Create `com.example.demo.controller.SortedSetController` at `/api/sorted-sets`
    - Implement POST `/{key}?member=&score=`, GET `/{key}`, GET `/{key}/range?start=&end=`, GET `/{key}/score/{member}`, DELETE `/{key}/{member}` endpoints
    - Delegate to SortedSetService
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 8.3 Write property tests for Sorted Set operations
    - **Property 9: Sorted set score round-trip**
    - **Property 10: Sorted set ordering invariant**
    - **Validates: Requirements 5.1, 5.2, 5.5**

- [ ] 9. Checkpoint - Ensure all data type endpoints compile and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Caching with Spring Cache Abstraction
  - [ ] 10.1 Implement CacheService
    - Create `com.example.demo.service.CacheService` with `@Cacheable` on compute method and `@CacheEvict` on evict method
    - Use cache name "demoCache" with key parameter
    - Simulate expensive computation
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [ ] 10.2 Implement CacheController
    - Create `com.example.demo.controller.CacheController` at `/api/cache`
    - Implement GET `/{key}` (triggers cacheable compute) and DELETE `/{key}` (evicts cache entry) endpoints
    - Delegate to CacheService
    - _Requirements: 6.1, 6.2, 6.3_

  - [ ]* 10.3 Write property test for Cache lifecycle
    - **Property 11: Cache miss-hit-evict lifecycle**
    - **Validates: Requirements 6.1, 6.2, 6.3**

- [ ] 11. Session Store
  - [ ] 11.1 Implement SessionService
    - Create `com.example.demo.service.SessionService` using `HashOperations` from RedisTemplate
    - Store attributes under key pattern `session:{sessionId}`
    - Implement `putAttribute(sessionId, key, value, ttlSeconds)`, `getAttributes(sessionId)`, and `deleteSession(sessionId)` methods
    - Set TTL using `RedisTemplate.expire()` when ttlSeconds is provided
    - Throw `ResourceNotFoundException` when session does not exist on get
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ] 11.2 Implement SessionController
    - Create `com.example.demo.controller.SessionController` at `/api/sessions`
    - Implement PUT `/{sessionId}?attributeKey=&ttlSeconds=`, GET `/{sessionId}`, DELETE `/{sessionId}` endpoints
    - Delegate to SessionService
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ]* 11.3 Write property tests for Session Store
    - **Property 12: Session attribute round-trip**
    - **Property 13: Session deletion removes all data**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.5**

- [ ] 12. Pub/Sub Messaging
  - [ ] 12.1 Implement PubSubService
    - Create `com.example.demo.service.PubSubService` using `RedisTemplate.convertAndSend()` for publishing
    - Maintain a `ConcurrentHashMap<String, List<String>>` for received messages
    - Implement `publish(channel, message)`, `getMessages(channel)`, `storeMessage(channel, message)`, and `clearMessages(channel)` methods
    - _Requirements: 8.1, 8.3, 8.4_

  - [ ] 12.2 Implement RedisMessageListener
    - Create `com.example.demo.listener.RedisMessageListener` implementing `MessageListener`
    - On message receipt, extract channel and body, delegate to `PubSubService.storeMessage()`
    - _Requirements: 8.2_

  - [ ] 12.3 Implement PubSubController
    - Create `com.example.demo.controller.PubSubController` at `/api/pubsub`
    - Implement POST `/{channel}`, GET `/{channel}`, DELETE `/{channel}` endpoints
    - Delegate to PubSubService
    - _Requirements: 8.1, 8.3, 8.4_

  - [ ]* 12.4 Write property tests for Pub/Sub
    - **Property 14: Pub/Sub message storage round-trip**
    - **Property 15: Pub/Sub message clearing**
    - **Validates: Requirements 8.3, 8.4**

- [ ] 13. Rate Limiting
  - [ ] 13.1 Implement RateLimitService
    - Create `com.example.demo.service.RateLimitService` using `StringRedisTemplate`
    - Use atomic `increment()` for request counting with key pattern `rate_limit:{clientId}`
    - Set key expiration on first request in window using `RedisTemplate.expire()`
    - Return `RateLimitInfo` with allowed status, remaining count, and retry-after seconds
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ] 13.2 Implement RateLimitController
    - Create `com.example.demo.controller.RateLimitController` at `/api/rate-limit`
    - Implement GET `/check/{clientId}` endpoint
    - Return 200 with `X-RateLimit-Remaining` header when allowed
    - Return 429 with `Retry-After` header when rate limited
    - Delegate to RateLimitService
    - _Requirements: 9.1, 9.2_

  - [ ]* 13.3 Write property test for Rate Limiting
    - **Property 16: Rate limiter boundary enforcement**
    - **Validates: Requirements 9.1, 9.2**

- [ ] 14. Leaderboard
  - [ ] 14.1 Implement LeaderboardService
    - Create `com.example.demo.service.LeaderboardService` using `ZSetOperations` from RedisTemplate
    - Use fixed sorted set key `leaderboard`
    - Implement `addScore(player, score)`, `incrementScore(player, increment)`, `getTopN(n)`, and `getPlayerRank(player)` methods
    - Use `reverseRangeWithScores` for descending order retrieval
    - Throw `ResourceNotFoundException` when player does not exist
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 14.2 Implement LeaderboardController
    - Create `com.example.demo.controller.LeaderboardController` at `/api/leaderboard`
    - Implement POST `?player=&score=`, POST `/increment?player=&increment=`, GET `/top/{n}`, GET `/rank/{player}` endpoints
    - Delegate to LeaderboardService
    - Return `LeaderboardEntry` DTOs
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ]* 14.3 Write property tests for Leaderboard
    - **Property 17: Leaderboard score round-trip**
    - **Property 18: Leaderboard descending order invariant**
    - **Property 19: Leaderboard atomic score increment**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4**

- [ ] 15. Checkpoint - Ensure all pattern endpoints compile and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 16. Integration wiring and 404 validation
  - [ ] 16.1 Wire Pub/Sub listener to RedisMessageListenerContainer
    - Update `RedisConfig` to register `RedisMessageListener` on channels dynamically via PubSubService or configure default channels
    - Ensure listener container starts and receives published messages
    - _Requirements: 8.2_

  - [ ]* 16.2 Write property test for non-existent resource handling
    - **Property 20: Non-existent resource returns 404**
    - **Validates: Requirements 1.4, 2.5, 7.5, 10.5**

- [ ] 17. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 18. Docker Containerization
  - [ ] 18.1 Create .dockerignore file
    - Create `.dockerignore` at project root excluding: `.git`, `.gitignore`, `.kiro`, `.gradle`, `build`, `*.md`, `.idea`, `.vscode`, `*.iml`
    - _Requirements: 12.7_

  - [ ] 18.2 Create multi-stage Dockerfile
    - Create `Dockerfile` at project root with two stages:
    - Stage 1 (builder): Eclipse Temurin JDK 21, copy Gradle wrapper and build files, download dependencies, copy source, run `./gradlew bootJar --no-daemon`
    - Stage 2 (runtime): Eclipse Temurin JRE 21 Alpine, copy JAR from builder, expose port 8080, entrypoint `java -jar app.jar`
    - _Requirements: 12.1_

  - [ ] 18.3 Create docker-compose.yml
    - Create `docker-compose.yml` at project root with two services:
    - `redis` service: `redis:7-alpine` image, port 6379 exposed, health check using `redis-cli ping`
    - `app` service: builds from Dockerfile, port 8080 exposed, environment `SPRING_DATA_REDIS_HOST=redis`, depends_on redis with `condition: service_healthy`
    - _Requirements: 12.2, 12.3, 12.4, 12.5, 12.6_

  - [ ]* 18.4 Verify Docker Compose full-stack startup
    - **Property 21: Docker Compose full-stack startup**
    - Run `docker compose up --build` and verify both containers start, app connects to Redis, and API responds on port 8080
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6**

- [ ] 19. Helper Scripts
  - [ ] 19.1 Update docker-compose.yml with profiles
    - Add `profiles: ["infra", "app"]` to the redis service
    - Add `profiles: ["app"]` to the app service
    - This enables selective startup via `docker compose --profile infra` or `--profile app`
    - _Requirements: 13.4_

  - [ ] 19.2 Create scripts/start-infra.sh
    - Create `scripts/start-infra.sh` that starts Redis only using `docker compose --profile infra up -d`
    - Wait for Redis health check to pass before returning
    - Include shebang line (#!/bin/bash) and set -e
    - Make executable (chmod +x)
    - _Requirements: 13.1, 13.2, 13.8, 13.9_

  - [ ] 19.3 Create scripts/start-all.sh
    - Create `scripts/start-all.sh` that starts both Redis and app using `docker compose --profile app up -d --build`
    - Wait for Redis health check, then wait for app to respond
    - Include shebang line (#!/bin/bash) and set -e
    - Make executable (chmod +x)
    - _Requirements: 13.1, 13.3, 13.9_

  - [ ] 19.4 Create scripts/e2e-test.sh
    - Create `scripts/e2e-test.sh` that runs curl-based HTTP tests against all 10 feature areas
    - Test at least one operation per area: Strings, Hashes, Lists, Sets, Sorted Sets, Cache, Sessions, Pub/Sub, Rate Limiting, Leaderboard
    - Report pass/fail per test, exit 0 on all pass, exit 1 on any failure
    - Support BASE_URL environment variable (default: http://localhost:8080)
    - Include shebang line (#!/bin/bash) and set -e
    - Make executable (chmod +x)
    - _Requirements: 13.1, 13.5, 13.6, 13.7, 13.9_

  - [ ]* 19.5 Verify E2E test script against running application
    - **Property 22: E2E test script validates all feature areas**
    - Start the stack with `./scripts/start-all.sh`, then run `./scripts/e2e-test.sh` and verify exit code 0
    - **Validates: Requirements 13.5, 13.6, 13.7**

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The application uses Java 21 with Spring Boot 4.1.1 and Gradle build system
- Redis must be running locally on port 6379 for integration tests
- Consider using Testcontainers for Redis in tests to avoid external dependency

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1", "2.3"] },
    { "id": 1, "tasks": ["1.3", "1.4", "2.2"] },
    { "id": 2, "tasks": ["4.1", "5.1", "6.1", "7.1", "8.1"] },
    { "id": 3, "tasks": ["4.2", "5.2", "6.2", "7.2", "8.2"] },
    { "id": 4, "tasks": ["4.3", "5.3", "6.3", "7.3", "8.3"] },
    { "id": 5, "tasks": ["10.1", "11.1", "12.1", "13.1", "14.1"] },
    { "id": 6, "tasks": ["10.2", "11.2", "12.2", "12.3", "13.2", "14.2"] },
    { "id": 7, "tasks": ["10.3", "11.3", "12.4", "13.3", "14.3", "16.1"] },
    { "id": 8, "tasks": ["16.2"] },
    { "id": 9, "tasks": ["18.1", "18.2"] },
    { "id": 10, "tasks": ["18.3"] },
    { "id": 11, "tasks": ["18.4"] },
    { "id": 12, "tasks": ["19.1"] },
    { "id": 13, "tasks": ["19.2", "19.3", "19.4"] },
    { "id": 14, "tasks": ["19.5"] }
  ]
}
```
