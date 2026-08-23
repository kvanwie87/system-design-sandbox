# Java Redis PoC

A Spring Boot 4.1.1 proof-of-concept application demonstrating Redis data types and common application patterns through a REST API.

## Overview

This project showcases 10 Redis use cases across 28 REST endpoints, all runnable with a single `docker compose` command. It serves as a reference for how to integrate Redis with Spring Boot using both `RedisTemplate` (for direct Redis operations) and Spring Cache abstraction (for annotation-driven caching).

## Features

### Redis Data Types
| Feature | Endpoint | Redis Operations |
|---------|----------|-----------------|
| Strings | `/api/strings` | GET, SET, DEL |
| Hashes | `/api/hashes` | HSET, HGET, HGETALL, HDEL |
| Lists | `/api/lists` | LPUSH, RPUSH, LRANGE, LPOP, RPOP |
| Sets | `/api/sets` | SADD, SMEMBERS, SISMEMBER, SREM |
| Sorted Sets | `/api/sorted-sets` | ZADD, ZRANGE, ZSCORE, ZREM |

### Application Patterns
| Feature | Endpoint | Pattern |
|---------|----------|---------|
| Caching | `/api/cache` | Spring @Cacheable / @CacheEvict with Redis backing |
| Session Store | `/api/sessions` | Hash-backed sessions with TTL expiration |
| Pub/Sub | `/api/pubsub` | Publish messages, subscribe via listener, retrieve |
| Rate Limiting | `/api/rate-limit` | Atomic increment with key expiration |
| Leaderboard | `/api/leaderboard` | Sorted Set rankings with atomic score updates |

## Tech Stack

- **Java 21** with Spring Boot 4.1.1
- **Spring Data Redis** (Lettuce client)
- **Spring Cache** with RedisCacheManager
- **Docker** and Docker Compose
- **Redis 7** (Alpine)
- **Redis Commander** (web GUI)

## Prerequisites

- Docker and Docker Compose
- Java 21 (for local development only)
- curl (for running E2E tests)

## Quick Start

### Full Docker Stack (recommended)

```bash
./scripts/start-all.sh
```

This starts Redis, Redis Commander, and the Spring Boot app. Once ready:
- App: http://localhost:8080
- Redis Commander GUI: http://localhost:8081

### Local Development

```bash
./scripts/start-infra.sh    # Start Redis + Redis Commander
./gradlew bootRun           # Run app locally with hot reload
```

### Run E2E Tests

```bash
./scripts/e2e-test.sh
```

Tests all 10 feature areas and reports pass/fail for each endpoint.

### Cleanup

```bash
./scripts/cleanup.sh
```

## API Examples

### Strings
```bash
# Store a value
curl -X PUT http://localhost:8080/api/strings/greeting -d "Hello, Redis!"

# Retrieve it
curl http://localhost:8080/api/strings/greeting

# Delete it
curl -X DELETE http://localhost:8080/api/strings/greeting
```

### Hashes
```bash
# Store user fields
curl -X PUT http://localhost:8080/api/hashes/user:1/name -d "Alice"
curl -X PUT http://localhost:8080/api/hashes/user:1/email -d "alice@example.com"

# Get all fields
curl http://localhost:8080/api/hashes/user:1
```

### Lists
```bash
# Push items
curl -X POST "http://localhost:8080/api/lists/queue?direction=right" -d "task1"
curl -X POST "http://localhost:8080/api/lists/queue?direction=right" -d "task2"

# Get all items
curl http://localhost:8080/api/lists/queue

# Pop from left (FIFO)
curl -X DELETE "http://localhost:8080/api/lists/queue?direction=left"
```

### Caching
```bash
# First call computes and caches
curl http://localhost:8080/api/cache/expensive-query

# Second call returns cached value (same response, instant)
curl http://localhost:8080/api/cache/expensive-query

# Evict the cache
curl -X DELETE http://localhost:8080/api/cache/expensive-query
```

### Rate Limiting
```bash
# Check rate limit (10 requests per 60-second window)
curl -i http://localhost:8080/api/rate-limit/check/my-client
# Look for X-RateLimit-Remaining header

# Exceed the limit and get 429
for i in $(seq 1 11); do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/rate-limit/check/my-client; done
```

### Leaderboard
```bash
# Add players
curl -X POST "http://localhost:8080/api/leaderboard?player=alice&score=1500"
curl -X POST "http://localhost:8080/api/leaderboard?player=bob&score=1200"
curl -X POST "http://localhost:8080/api/leaderboard?player=charlie&score=1800"

# Get top 3
curl http://localhost:8080/api/leaderboard/top/3

# Increment a score
curl -X POST "http://localhost:8080/api/leaderboard/increment?player=bob&increment=400"

# Check rank
curl http://localhost:8080/api/leaderboard/rank/bob
```

## Project Structure

```
java-redis-poc/
├── build.gradle
├── Dockerfile                  # Multi-stage (JDK build → JRE runtime)
├── docker-compose.yml          # Redis + Redis Commander + App (with profiles)
├── .dockerignore
├── scripts/
│   ├── start-infra.sh         # Redis only
│   ├── start-all.sh           # Full stack
│   ├── e2e-test.sh            # Endpoint validation
│   └── cleanup.sh             # Tear down containers
├── about-redis.md             # Redis reference guide
└── src/main/java/com/example/demo/
    ├── DemoApplication.java
    ├── config/                 # RedisConfig, CacheConfig
    ├── controller/             # 10 REST controllers
    ├── service/                # 10 service classes
    ├── listener/               # Redis Pub/Sub message listener
    ├── exception/              # Global error handling
    └── dto/                    # LeaderboardEntry, RateLimitInfo
```

## Docker Compose Profiles

| Profile | Services Started | Use Case |
|---------|-----------------|----------|
| `infra` | Redis, Redis Commander | Local app development |
| `app` | Redis, Redis Commander, Spring Boot App | Full containerized stack |

## Configuration

Key settings in `src/main/resources/application.yaml`:

```yaml
spring:
  data:
    redis:
      host: localhost      # overridden to "redis" in Docker via env var
      port: 6379
      timeout: 2000ms

rate-limit:
  max-requests: 10         # requests per window
  window-seconds: 60       # window duration
```

## Further Reading

- [about-redis.md](about-redis.md) — Detailed Redis reference (data types, characteristics, use cases)
- [Spring Data Redis docs](https://docs.spring.io/spring-data/redis/reference/)
- [Redis commands reference](https://redis.io/commands/)
