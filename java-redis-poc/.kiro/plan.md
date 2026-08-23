# Implementation Plan — Redis PoC Spring Boot Application

## Problem Statement

Build a Spring Boot 4.1.1 PoC that demonstrates the breadth of Redis functionality — five core data types (Strings, Hashes, Lists, Sets, Sorted Sets) and five application patterns (Caching, Session Store, Pub/Sub, Rate Limiting, Leaderboard) — all accessible through REST endpoints. Include Docker containerization and shell helper scripts for zero-friction local development and end-to-end validation.

## Requirements Summary

| # | Area | Scope |
|---|------|-------|
| 1–5 | Data Types | Strings, Hashes, Lists, Sets, Sorted Sets — CRUD via REST |
| 6 | Caching | Spring Cache abstraction (@Cacheable/@CacheEvict) backed by Redis |
| 7 | Sessions | Hash-backed session store with TTL |
| 8 | Pub/Sub | Publish messages, in-memory listener, retrieve received messages |
| 9 | Rate Limiting | Atomic increment counter with time-window expiration |
| 10 | Leaderboard | Sorted Set rankings with atomic score increments |
| 11 | Configuration | Gradle deps, application.yaml, Java 21, Spring Boot 4.1.1 |
| 12 | Docker | Multi-stage Dockerfile, docker-compose.yml with health checks |
| 13 | Scripts | start-infra.sh, start-all.sh, e2e-test.sh with Docker profiles |

## Technical Stack

- **Language:** Java 21
- **Framework:** Spring Boot 4.1.1
- **Build:** Gradle
- **Redis Client:** Spring Data Redis (Lettuce)
- **Caching:** Spring Cache with RedisCacheManager
- **Containerization:** Docker, Docker Compose
- **Testing:** JUnit 5, curl-based E2E scripts

## Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                   REST Controllers (10)                   │
├─────────────────────────────────────────────────────────┤
│                   Service Layer (10)                      │
├─────────────────────────────────────────────────────────┤
│   RedisTemplate / StringRedisTemplate / Spring Cache     │
├─────────────────────────────────────────────────────────┤
│                   Redis Server (Docker)                   │
└─────────────────────────────────────────────────────────┘
```

## REST API Endpoints (28 total)

| Base Path | Feature | Operations |
|-----------|---------|------------|
| `/api/strings` | Strings | PUT, GET, DELETE |
| `/api/hashes` | Hashes | PUT field, GET all, GET field, DELETE field |
| `/api/lists` | Lists | POST push, GET all, DELETE pop |
| `/api/sets` | Sets | POST add, GET all, GET member check, DELETE |
| `/api/sorted-sets` | Sorted Sets | POST add, GET all, GET range, GET score, DELETE |
| `/api/cache` | Caching | GET (compute/hit), DELETE (evict) |
| `/api/sessions` | Sessions | PUT attr, GET all, DELETE |
| `/api/pubsub` | Pub/Sub | POST publish, GET messages, DELETE clear |
| `/api/rate-limit` | Rate Limiting | GET check |
| `/api/leaderboard` | Leaderboard | POST add, POST increment, GET top, GET rank |

## Task Breakdown

### Task 1: Project Setup — Update build.gradle

- **Objective:** Add required Spring Boot starters for web, redis, and cache
- **Implementation:** Add `spring-boot-starter-web`, `spring-boot-starter-data-redis`, `spring-boot-starter-cache` to Gradle dependencies
- **Demo:** Project compiles with `./gradlew build`

### Task 2: Application Configuration

- **Objective:** Create application.yaml with Redis connection and custom properties
- **Implementation:** Configure `spring.data.redis.host=localhost`, `port=6379`, `timeout=2000ms`, plus `rate-limit.max-requests=10` and `rate-limit.window-seconds=60`
- **Demo:** Application loads configuration on startup

### Task 3: Redis Configuration Classes

- **Objective:** Create RedisConfig and CacheConfig Spring beans
- **Implementation:**
  - `RedisConfig`: RedisTemplate with StringRedisSerializer keys, GenericJackson2JsonRedisSerializer values, RedisMessageListenerContainer
  - `CacheConfig`: @EnableCaching, RedisCacheManager with JSON serialization
- **Demo:** Application context loads all Redis beans

### Task 4: Exception Handling and DTOs

- **Objective:** Create shared infrastructure classes
- **Implementation:**
  - `ResourceNotFoundException` extending RuntimeException
  - `GlobalExceptionHandler` with @RestControllerAdvice returning 404 JSON
  - `LeaderboardEntry` record (player, score, rank)
  - `RateLimitInfo` record (allowed, remaining, retryAfterSeconds)
- **Demo:** Project compiles, 404 handling wired

### Task 5: Redis String Operations

- **Objective:** Implement String data type demo endpoints
- **Implementation:**
  - `StringService` using ValueOperations (set, get, delete)
  - `StringController` at `/api/strings` (PUT/GET/DELETE `/{key}`)
  - 404 on missing key
- **Test:** Round-trip store/retrieve, 404 on missing key
- **Demo:** `curl -X PUT localhost:8080/api/strings/hello -d "world"` then `curl localhost:8080/api/strings/hello` returns "world"

### Task 6: Redis Hash Operations

- **Objective:** Implement Hash data type demo endpoints
- **Implementation:**
  - `HashService` using HashOperations (putField, getAll, getField, deleteField)
  - `HashController` at `/api/hashes` (PUT/GET/DELETE `/{key}/{field}`, GET `/{key}`)
  - 404 on missing hash
- **Test:** Store fields, retrieve all/one, delete field
- **Demo:** Store user fields, retrieve complete hash

### Task 7: Redis List Operations

- **Objective:** Implement List data type demo endpoints
- **Implementation:**
  - `ListService` using ListOperations (leftPush, rightPush, leftPop, rightPop, range)
  - `ListController` at `/api/lists` (POST/GET/DELETE with `?direction=` param)
  - Empty list on missing key
- **Test:** Push multiple values, verify order, pop from each end
- **Demo:** Push items, GET shows ordered list

### Task 8: Redis Set Operations

- **Objective:** Implement Set data type demo endpoints
- **Implementation:**
  - `SetService` using SetOperations (add, members, isMember, remove)
  - `SetController` at `/api/sets` (POST/GET/DELETE, GET member check)
  - Empty set on missing key
- **Test:** Add members, check membership, remove
- **Demo:** Add items, check membership returns true/false

### Task 9: Redis Sorted Set Operations

- **Objective:** Implement Sorted Set data type demo endpoints
- **Implementation:**
  - `SortedSetService` using ZSetOperations (add, rangeWithScores, score, rank, remove)
  - `SortedSetController` at `/api/sorted-sets` (POST/GET/DELETE, GET range, GET score)
  - Empty set on missing key
- **Test:** Add scored members, verify ascending order, query scores
- **Demo:** Add members with scores, GET returns ordered by score

### Task 10: Caching with Spring Cache

- **Objective:** Demonstrate @Cacheable/@CacheEvict with Redis backing
- **Implementation:**
  - `CacheService` with @Cacheable("demoCache") on compute, @CacheEvict on evict
  - `CacheController` at `/api/cache` (GET `/{key}`, DELETE `/{key}`)
  - Simulated expensive computation with timestamp
- **Test:** First GET computes, second GET returns same value, DELETE evicts, next GET recomputes
- **Demo:** Two GETs return identical timestamp (cache hit), evict + GET returns new timestamp

### Task 11: Session Store

- **Objective:** Demonstrate Redis-backed session management with TTL
- **Implementation:**
  - `SessionService` using HashOperations with key `session:{id}`, expire() for TTL
  - `SessionController` at `/api/sessions` (PUT/GET/DELETE)
  - 404 on missing session
- **Test:** Store attributes, retrieve all, set TTL, delete session
- **Demo:** Create session, retrieve attributes, delete and confirm 404

### Task 12: Pub/Sub Messaging

- **Objective:** Demonstrate Redis publish/subscribe pattern
- **Implementation:**
  - `PubSubService` with convertAndSend(), ConcurrentHashMap for received messages
  - `RedisMessageListener` implementing MessageListener, stores in PubSubService
  - `PubSubController` at `/api/pubsub` (POST/GET/DELETE)
  - Wire listener to RedisMessageListenerContainer
- **Test:** Publish message, retrieve from store, clear
- **Demo:** POST message, GET shows received message

### Task 13: Rate Limiting

- **Objective:** Demonstrate Redis-backed request throttling
- **Implementation:**
  - `RateLimitService` using StringRedisTemplate atomic increment + expire
  - `RateLimitController` at `/api/rate-limit/check/{clientId}`
  - 200 + X-RateLimit-Remaining header when under limit
  - 429 + Retry-After header when over limit
- **Test:** Make N+1 requests, verify 429 on overflow
- **Demo:** Rapid requests show decreasing remaining count, then 429

### Task 14: Leaderboard

- **Objective:** Demonstrate sorted set-backed rankings
- **Implementation:**
  - `LeaderboardService` using ZSetOperations with fixed key "leaderboard"
  - reverseRangeWithScores for descending, incrementScore for atomic updates
  - `LeaderboardController` at `/api/leaderboard` (POST add, POST increment, GET top, GET rank)
  - 404 on missing player
- **Test:** Add players, get top N descending, increment score, verify rank
- **Demo:** Add 5 players, GET top 3 shows highest first

### Task 15: Integration Wiring

- **Objective:** Wire Pub/Sub listener to container and validate 404 handling
- **Implementation:**
  - Update RedisConfig to register listener on channels
  - Verify all 404 responses work consistently across controllers
- **Demo:** Published messages appear in GET, missing resources return 404 JSON

### Task 16: Docker — .dockerignore and Dockerfile

- **Objective:** Containerize the Spring Boot application
- **Implementation:**
  - `.dockerignore`: exclude .git, .kiro, .gradle, build, *.md, IDE files
  - Multi-stage `Dockerfile`:
    - Stage 1: Eclipse Temurin JDK 21, Gradle build, bootJar
    - Stage 2: Eclipse Temurin JRE 21 Alpine, copy JAR, expose 8080
- **Demo:** `docker build .` produces ~200MB runtime image

### Task 17: Docker Compose

- **Objective:** Orchestrate Redis + app with health checks
- **Implementation:**
  - `docker-compose.yml` with redis (redis:7-alpine, healthcheck) and app services
  - App depends_on redis with condition: service_healthy
  - Environment: SPRING_DATA_REDIS_HOST=redis
  - Ports: 6379 (redis), 8080 (app)
- **Demo:** `docker compose up` starts both, app connects to Redis

### Task 18: Docker Compose Profiles

- **Objective:** Enable selective service startup
- **Implementation:**
  - Add `profiles: ["infra", "app"]` to redis service
  - Add `profiles: ["app"]` to app service
  - `--profile infra` starts Redis only, `--profile app` starts both
- **Demo:** `docker compose --profile infra up` starts only Redis

### Task 19: Helper Script — start-infra.sh

- **Objective:** One-command Redis startup for local development
- **Implementation:**
  - `scripts/start-infra.sh`: docker compose --profile infra up -d
  - Wait for Redis health check to pass
  - #!/bin/bash, set -e, chmod +x
- **Demo:** Run script, Redis available at localhost:6379, run app with `./gradlew bootRun`

### Task 20: Helper Script — start-all.sh

- **Objective:** One-command full stack startup
- **Implementation:**
  - `scripts/start-all.sh`: docker compose --profile app up -d --build
  - Wait for Redis healthy, then wait for app responding on :8080
  - #!/bin/bash, set -e, chmod +x
- **Demo:** Run script, app available at localhost:8080

### Task 21: Helper Script — e2e-test.sh

- **Objective:** Automated validation of all endpoints
- **Implementation:**
  - `scripts/e2e-test.sh`: curl-based tests for all 10 feature areas
  - Supports `BASE_URL` env var (default: http://localhost:8080)
  - Reports pass/fail per test, summary count, exit 0/1
  - Tests: String CRUD + 404, Hash field ops, List push/pop, Set add/member/remove, ZSet add/score, Cache hit/miss/evict, Session CRUD, PubSub publish/get/clear, Rate limit check, Leaderboard add/top/rank/increment
  - #!/bin/bash, set -e, chmod +x
- **Demo:** `./scripts/e2e-test.sh` outputs all passing tests with exit 0

## Developer Workflow

```text
# Option A: Local app development
./scripts/start-infra.sh          # Redis in Docker
./gradlew bootRun                 # App locally (hot reload)
./scripts/e2e-test.sh             # Validate endpoints

# Option B: Full Docker stack
./scripts/start-all.sh            # Redis + App in Docker
./scripts/e2e-test.sh             # Validate endpoints

# Cleanup
docker compose down -v
```

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| GenericJackson2JsonRedisSerializer | Human-readable values in Redis, easy debugging |
| Atomic increment (not Lua scripts) for rate limiting | Simpler for PoC, sufficient for demo |
| ConcurrentHashMap for Pub/Sub message store | Demo-appropriate, not production-grade |
| Docker Compose profiles | Flexible: run Redis-only for local dev, or full stack |
| curl-based E2E tests | Zero dependencies beyond bash and curl |
| Multi-stage Dockerfile | ~200MB image vs ~500MB with full JDK |

## File Structure (Final)

```text
java-redis-poc/
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── scripts/
│   ├── start-infra.sh
│   ├── start-all.sh
│   └── e2e-test.sh
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java
    │   │   ├── config/ (RedisConfig, CacheConfig)
    │   │   ├── controller/ (10 controllers)
    │   │   ├── service/ (10 services)
    │   │   ├── listener/ (RedisMessageListener)
    │   │   ├── exception/ (ResourceNotFoundException, GlobalExceptionHandler)
    │   │   └── dto/ (LeaderboardEntry, RateLimitInfo)
    │   └── resources/
    │       └── application.yaml
    └── test/
        └── java/com/example/demo/ (property-based + integration tests)
```
