# Design Document

## Overview

This document describes the architecture and design for the Redis Proof-of-Concept (PoC) application. The application is a Spring Boot 4.1.1 REST API built with Java 21 that demonstrates Redis data types (Strings, Hashes, Lists, Sets, Sorted Sets) and common application patterns (Caching, Session Store, Pub/Sub Messaging, Rate Limiting, Leaderboard).

## Architecture

The application follows a layered architecture with clear separation between REST controllers, service layer, and Redis data access:

```
┌─────────────────────────────────────────────────────────┐
│                   REST Controllers                        │
│  (StringController, HashController, ListController, ...) │
├─────────────────────────────────────────────────────────┤
│                   Service Layer                           │
│  (StringService, HashService, ListService, ...)          │
├─────────────────────────────────────────────────────────┤
│             Redis Data Access Layer                       │
│  (RedisTemplate / StringRedisTemplate)                   │
├─────────────────────────────────────────────────────────┤
│            Spring Cache Abstraction                       │
│  (@Cacheable, @CacheEvict with RedisCacheManager)        │
├─────────────────────────────────────────────────────────┤
│                   Redis Server                            │
└─────────────────────────────────────────────────────────┘
```

## Package Structure

```
com.example.demo
├── DemoApplication.java
├── config
│   ├── RedisConfig.java
│   └── CacheConfig.java
├── controller
│   ├── StringController.java
│   ├── HashController.java
│   ├── ListController.java
│   ├── SetController.java
│   ├── SortedSetController.java
│   ├── CacheController.java
│   ├── SessionController.java
│   ├── PubSubController.java
│   ├── RateLimitController.java
│   └── LeaderboardController.java
├── service
│   ├── StringService.java
│   ├── HashService.java
│   ├── ListService.java
│   ├── SetService.java
│   ├── SortedSetService.java
│   ├── CacheService.java
│   ├── SessionService.java
│   ├── PubSubService.java
│   ├── RateLimitService.java
│   └── LeaderboardService.java
├── listener
│   └── RedisMessageListener.java
├── exception
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
└── dto
    ├── LeaderboardEntry.java
    └── RateLimitInfo.java
```

## Components and Interfaces

### Configuration

#### RedisConfig

Configures `RedisConnectionFactory`, `RedisTemplate<String, Object>`, and `StringRedisTemplate`. Also configures the Redis message listener container for Pub/Sub.

```java
package com.example.demo.config;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisMessageListenerContainer messageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisMessageListener messageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // Channels registered dynamically via PubSubService
        return container;
    }
}
```

#### CacheConfig

Configures `RedisCacheManager` for the Spring Cache abstraction.

```java
package com.example.demo.config;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
```

### Controllers

Each controller is a `@RestController` with a base path mapping. Controllers delegate to their corresponding service.

#### StringController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/strings")
public class StringController {

    private final StringService stringService;

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key, @RequestBody String value) { ... }

    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable String key) { ... }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) { ... }
}
```

#### HashController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/hashes")
public class HashController {

    private final HashService hashService;

    @PutMapping("/{key}/{field}")
    public ResponseEntity<Void> putField(@PathVariable String key, @PathVariable String field, @RequestBody String value) { ... }

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getAll(@PathVariable String key) { ... }

    @GetMapping("/{key}/{field}")
    public ResponseEntity<Object> getField(@PathVariable String key, @PathVariable String field) { ... }

    @DeleteMapping("/{key}/{field}")
    public ResponseEntity<Void> deleteField(@PathVariable String key, @PathVariable String field) { ... }
}
```

#### ListController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/lists")
public class ListController {

    private final ListService listService;

    @PostMapping("/{key}")
    public ResponseEntity<Void> push(@PathVariable String key, @RequestBody String value,
                                     @RequestParam(defaultValue = "left") String direction) { ... }

    @GetMapping("/{key}")
    public ResponseEntity<List<Object>> getAll(@PathVariable String key) { ... }

    @DeleteMapping("/{key}")
    public ResponseEntity<Object> pop(@PathVariable String key,
                                      @RequestParam(defaultValue = "left") String direction) { ... }
}
```

#### SetController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/sets")
public class SetController {

    private final SetService setService;

    @PostMapping("/{key}")
    public ResponseEntity<Void> add(@PathVariable String key, @RequestBody String member) { ... }

    @GetMapping("/{key}")
    public ResponseEntity<Set<Object>> getAll(@PathVariable String key) { ... }

    @GetMapping("/{key}/member/{member}")
    public ResponseEntity<Boolean> isMember(@PathVariable String key, @PathVariable String member) { ... }

    @DeleteMapping("/{key}/{member}")
    public ResponseEntity<Void> remove(@PathVariable String key, @PathVariable String member) { ... }
}
```

#### SortedSetController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/sorted-sets")
public class SortedSetController {

    private final SortedSetService sortedSetService;

    @PostMapping("/{key}")
    public ResponseEntity<Void> add(@PathVariable String key, @RequestParam String member, @RequestParam double score) { ... }

    @GetMapping("/{key}")
    public ResponseEntity<Set<Object>> getAll(@PathVariable String key) { ... }

    @GetMapping("/{key}/range")
    public ResponseEntity<Set<Object>> getRange(@PathVariable String key, @RequestParam long start, @RequestParam long end) { ... }

    @GetMapping("/{key}/score/{member}")
    public ResponseEntity<Double> getScore(@PathVariable String key, @PathVariable String member) { ... }

    @DeleteMapping("/{key}/{member}")
    public ResponseEntity<Void> remove(@PathVariable String key, @PathVariable String member) { ... }
}
```

#### CacheController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheService cacheService;

    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable String key) { ... }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> evict(@PathVariable String key) { ... }
}
```

#### SessionController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    @PutMapping("/{sessionId}")
    public ResponseEntity<Void> putAttribute(@PathVariable String sessionId,
                                             @RequestParam String attributeKey,
                                             @RequestBody String attributeValue,
                                             @RequestParam(required = false) Long ttlSeconds) { ... }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getAttributes(@PathVariable String sessionId) { ... }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) { ... }
}
```

#### PubSubController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/pubsub")
public class PubSubController {

    private final PubSubService pubSubService;

    @PostMapping("/{channel}")
    public ResponseEntity<Void> publish(@PathVariable String channel, @RequestBody String message) { ... }

    @GetMapping("/{channel}")
    public ResponseEntity<List<String>> getMessages(@PathVariable String channel) { ... }

    @DeleteMapping("/{channel}")
    public ResponseEntity<Void> clearMessages(@PathVariable String channel) { ... }
}
```

#### RateLimitController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @GetMapping("/check/{clientId}")
    public ResponseEntity<String> checkRateLimit(@PathVariable String clientId) { ... }
}
```

The response includes headers:
- `X-RateLimit-Remaining`: number of requests remaining in the window
- `Retry-After`: seconds until window resets (only on 429 responses)

#### LeaderboardController

```java
package com.example.demo.controller;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @PostMapping
    public ResponseEntity<Void> addScore(@RequestParam String player, @RequestParam double score) { ... }

    @PostMapping("/increment")
    public ResponseEntity<Double> incrementScore(@RequestParam String player, @RequestParam double increment) { ... }

    @GetMapping("/top/{n}")
    public ResponseEntity<List<LeaderboardEntry>> getTopN(@PathVariable int n) { ... }

    @GetMapping("/rank/{player}")
    public ResponseEntity<LeaderboardEntry> getPlayerRank(@PathVariable String player) { ... }
}
```

### Services

Each service encapsulates the Redis operations for its domain. Services use `RedisTemplate<String, Object>` or `StringRedisTemplate` depending on the use case.

#### StringService

Uses `ValueOperations` from RedisTemplate to perform `set`, `get`, and `delete` operations.

#### HashService

Uses `HashOperations` from RedisTemplate to perform `put`, `get`, `entries`, and `delete` operations on Redis Hashes.

#### ListService

Uses `ListOperations` from RedisTemplate to perform `leftPush`, `rightPush`, `leftPop`, `rightPop`, and `range` operations.

#### SetService

Uses `SetOperations` from RedisTemplate to perform `add`, `members`, `isMember`, and `remove` operations.

#### SortedSetService

Uses `ZSetOperations` from RedisTemplate to perform `add`, `rangeWithScores`, `rangeByScore`, `score`, `rank`, and `remove` operations.

#### CacheService

Uses `@Cacheable` and `@CacheEvict` annotations. Contains a simulated expensive computation method.

```java
package com.example.demo.service;

@Service
public class CacheService {

    @Cacheable(value = "demoCache", key = "#key")
    public String computeValue(String key) {
        // Simulate expensive computation
        return "computed-" + key + "-" + System.currentTimeMillis();
    }

    @CacheEvict(value = "demoCache", key = "#key")
    public void evict(String key) {
        // Eviction handled by annotation
    }
}
```

#### SessionService

Uses `HashOperations` to store session attributes under a prefixed key (e.g., `session:{sessionId}`). Uses `RedisTemplate.expire()` to set TTL.

#### PubSubService

Uses `RedisTemplate.convertAndSend()` to publish messages. Maintains a `ConcurrentHashMap<String, List<String>>` for received messages.

#### RateLimitService

Uses `ValueOperations.increment()` for atomic counter and `RedisTemplate.expire()` for window expiration.

```java
package com.example.demo.service;

@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public RateLimitInfo checkRateLimit(String clientId) {
        String key = "rate_limit:" + clientId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }
        boolean allowed = count <= MAX_REQUESTS;
        int remaining = Math.max(0, (int)(MAX_REQUESTS - count));
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return new RateLimitInfo(allowed, remaining, ttl != null ? ttl : WINDOW_SECONDS);
    }
}
```

#### LeaderboardService

Uses `ZSetOperations` with a fixed sorted set key (e.g., `leaderboard`). Uses `incrementScore` for atomic score increments and `reverseRangeWithScores` for descending-order retrieval.

### Listener

#### RedisMessageListener

Implements `MessageListener` interface. On message receipt, stores the message in the `PubSubService`'s in-memory store.

```java
package com.example.demo.listener;

@Component
public class RedisMessageListener implements MessageListener {

    private final PubSubService pubSubService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        pubSubService.storeMessage(channel, body);
    }
}
```

### Exception Handling

#### ResourceNotFoundException

A custom runtime exception thrown when a requested resource (key, session, player) does not exist.

#### GlobalExceptionHandler

A `@RestControllerAdvice` that catches `ResourceNotFoundException` and returns HTTP 404 with a JSON error body containing a descriptive message.

```java
package com.example.demo.exception;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
```

### DTOs

#### LeaderboardEntry

```java
package com.example.demo.dto;

public record LeaderboardEntry(String player, double score, long rank) {}
```

#### RateLimitInfo

```java
package com.example.demo.dto;

public record RateLimitInfo(boolean allowed, int remaining, long retryAfterSeconds) {}
```

## Data Models

### Redis Key Patterns

| Feature       | Key Pattern                     | Redis Data Type |
|---------------|----------------------------------|-----------------|
| Strings       | `{user-provided-key}`           | String          |
| Hashes        | `{user-provided-key}`           | Hash            |
| Lists         | `{user-provided-key}`           | List            |
| Sets          | `{user-provided-key}`           | Set             |
| Sorted Sets   | `{user-provided-key}`           | Sorted Set      |
| Cache         | `demoCache::{key}`              | String (JSON)   |
| Sessions      | `session:{sessionId}`           | Hash            |
| Rate Limiting | `rate_limit:{clientId}`         | String (counter)|
| Leaderboard   | `leaderboard`                   | Sorted Set      |

### Serialization Strategy

- **Keys**: Always serialized as plain strings via `StringRedisSerializer`
- **Values**: Serialized as JSON via `GenericJackson2JsonRedisSerializer` for the general `RedisTemplate<String, Object>`
- **Cache**: Serialized via `GenericJackson2JsonRedisSerializer` in the `RedisCacheConfiguration`
- **Rate Limit counters**: Use `StringRedisTemplate` for simple numeric string values

### Interfaces

#### REST API Endpoints

| Method   | Path                                    | Description                              |
|----------|------------------------------------------|------------------------------------------|
| PUT      | `/api/strings/{key}`                    | Store a string value                      |
| GET      | `/api/strings/{key}`                    | Retrieve a string value                   |
| DELETE   | `/api/strings/{key}`                    | Delete a string key                       |
| PUT      | `/api/hashes/{key}/{field}`             | Store a hash field-value                  |
| GET      | `/api/hashes/{key}`                     | Get all hash fields                       |
| GET      | `/api/hashes/{key}/{field}`             | Get a specific hash field value           |
| DELETE   | `/api/hashes/{key}/{field}`             | Delete a hash field                       |
| POST     | `/api/lists/{key}?direction=left|right` | Push value to list                        |
| GET      | `/api/lists/{key}`                      | Get all list elements                     |
| DELETE   | `/api/lists/{key}?direction=left|right` | Pop element from list                     |
| POST     | `/api/sets/{key}`                       | Add member to set                         |
| GET      | `/api/sets/{key}`                       | Get all set members                       |
| GET      | `/api/sets/{key}/member/{member}`       | Check set membership                      |
| DELETE   | `/api/sets/{key}/{member}`              | Remove set member                         |
| POST     | `/api/sorted-sets/{key}?member=&score=` | Add scored member                         |
| GET      | `/api/sorted-sets/{key}`               | Get all members (ascending)               |
| GET      | `/api/sorted-sets/{key}/range?start=&end=` | Get members by rank range            |
| GET      | `/api/sorted-sets/{key}/score/{member}` | Get member score                         |
| DELETE   | `/api/sorted-sets/{key}/{member}`       | Remove sorted set member                  |
| GET      | `/api/cache/{key}`                      | Get cached value (computes on miss)       |
| DELETE   | `/api/cache/{key}`                      | Evict cached entry                        |
| PUT      | `/api/sessions/{sessionId}?attributeKey=&ttlSeconds=` | Store session attribute    |
| GET      | `/api/sessions/{sessionId}`             | Get all session attributes                |
| DELETE   | `/api/sessions/{sessionId}`             | Delete session                            |
| POST     | `/api/pubsub/{channel}`                 | Publish message to channel                |
| GET      | `/api/pubsub/{channel}`                 | Get received messages                     |
| DELETE   | `/api/pubsub/{channel}`                 | Clear received messages                   |
| GET      | `/api/rate-limit/check/{clientId}`      | Check rate limit                          |
| POST     | `/api/leaderboard?player=&score=`       | Add/update player score                   |
| POST     | `/api/leaderboard/increment?player=&increment=` | Increment player score          |
| GET      | `/api/leaderboard/top/{n}`              | Get top N players                         |
| GET      | `/api/leaderboard/rank/{player}`        | Get player rank and score                 |

## Error Handling

- **404 Not Found**: Thrown via `ResourceNotFoundException` when a key, session, or player does not exist. Handled by `GlobalExceptionHandler` returning a JSON body `{"error": "descriptive message"}`.
- **429 Too Many Requests**: Returned by `RateLimitController` when the client exceeds the rate limit. Includes `Retry-After` header.
- **400 Bad Request**: Returned for malformed request parameters (handled by Spring's built-in validation).
- **500 Internal Server Error**: Unhandled exceptions caught by a generic handler returning a safe error message.

## Configuration

### application.yaml

```yaml
spring:
  application:
    name: demo
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

rate-limit:
  max-requests: 10
  window-seconds: 60
```

### Gradle Dependencies (additions to build.gradle)

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

## Docker Containerization

### Dockerfile (Multi-Stage)

The application uses a multi-stage Dockerfile for optimized image size:

**Stage 1 (Build):** Uses Eclipse Temurin JDK 21 with Gradle to compile and package the application as a fat JAR.

**Stage 2 (Runtime):** Uses Eclipse Temurin JRE 21 Alpine for a minimal runtime image (~200MB vs ~500MB with full JDK).

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

Orchestrates the Redis server and the Spring Boot application:

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    profiles: ["infra", "app"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  app:
    build: .
    ports:
      - "8080:8080"
    profiles: ["app"]
    environment:
      SPRING_DATA_REDIS_HOST: redis
    depends_on:
      redis:
        condition: service_healthy
```

### .dockerignore

Excludes unnecessary files from the Docker build context:

```
.git
.gitignore
.kiro
.gradle
build
*.md
.idea
.vscode
*.iml
```

### Container Networking

- The `app` service connects to Redis using the service name `redis` as the hostname (Docker Compose default network)
- The `SPRING_DATA_REDIS_HOST` environment variable overrides the `spring.data.redis.host` property in application.yaml
- Redis is accessible from the host at `localhost:6379` for debugging with redis-cli
- The application is accessible from the host at `localhost:8080`

### Deployment Commands

| Command | Description |
|---------|-------------|
| `docker compose up` | Start both Redis and app (builds if needed) |
| `docker compose up -d` | Start in detached mode |
| `docker compose up --build` | Force rebuild the app image |
| `docker compose down` | Stop and remove containers |
| `docker compose down -v` | Stop, remove containers, and delete volumes |

## Helper Scripts

The `scripts/` directory contains shell scripts (.sh) for common development workflows.

### Directory Structure

```
scripts/
├── start-infra.sh    # Start Redis only (for local app development)
├── start-all.sh      # Start Redis + application containers
└── e2e-test.sh       # Run end-to-end tests against all endpoints
```

### start-infra.sh

Starts only the Redis container using the "infra" Docker Compose profile. Waits for the Redis health check to pass before returning.

```bash
#!/bin/bash
set -e

echo "Starting Redis infrastructure..."
docker compose --profile infra up -d

echo "Waiting for Redis to be healthy..."
until docker compose ps redis --format json | grep -q '"Health":"healthy"'; do
  sleep 1
done

echo "Redis is ready on localhost:6379"
```

### start-all.sh

Starts both Redis and the application using the "app" Docker Compose profile (which includes Redis via the profile dependency).

```bash
#!/bin/bash
set -e

echo "Starting full stack (Redis + App)..."
docker compose --profile app up -d --build

echo "Waiting for Redis to be healthy..."
until docker compose ps redis --format json | grep -q '"Health":"healthy"'; do
  sleep 1
done

echo "Waiting for app to be ready..."
until curl -sf http://localhost:8080/api/strings/healthcheck > /dev/null 2>&1; do
  sleep 2
done

echo "Stack is ready! App: http://localhost:8080"
```

### e2e-test.sh

Runs HTTP requests against all feature area endpoints and reports pass/fail results. Uses curl for HTTP calls. Assumes the application is running on localhost:8080.

```bash
#!/bin/bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0
FAIL=0

test_endpoint() {
  local description="$1"
  local method="$2"
  local url="$3"
  local expected_code="$4"
  local data="$5"
  
  if [ -n "$data" ]; then
    actual_code=$(curl -sf -o /dev/null -w "%{http_code}" -X "$method" "$url" -H "Content-Type: text/plain" -d "$data")
  else
    actual_code=$(curl -sf -o /dev/null -w "%{http_code}" -X "$method" "$url")
  fi
  
  if [ "$actual_code" = "$expected_code" ]; then
    echo "✓ PASS: $description"
    PASS=$((PASS + 1))
  else
    echo "✗ FAIL: $description (expected $expected_code, got $actual_code)"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Redis PoC E2E Tests ==="
echo ""

# Strings
test_endpoint "String PUT" PUT "$BASE_URL/api/strings/test-key" 200 "test-value"
test_endpoint "String GET" GET "$BASE_URL/api/strings/test-key" 200
test_endpoint "String DELETE" DELETE "$BASE_URL/api/strings/test-key" 200
test_endpoint "String GET 404" GET "$BASE_URL/api/strings/nonexistent" 404

# Hashes
test_endpoint "Hash PUT field" PUT "$BASE_URL/api/hashes/h1/name" 200 "Alice"
test_endpoint "Hash GET all" GET "$BASE_URL/api/hashes/h1" 200
test_endpoint "Hash DELETE field" DELETE "$BASE_URL/api/hashes/h1/name" 200

# Lists
test_endpoint "List PUSH" POST "$BASE_URL/api/lists/mylist?direction=left" 200 "item1"
test_endpoint "List GET all" GET "$BASE_URL/api/lists/mylist" 200
test_endpoint "List POP" DELETE "$BASE_URL/api/lists/mylist?direction=left" 200

# Sets
test_endpoint "Set ADD" POST "$BASE_URL/api/sets/myset" 200 "member1"
test_endpoint "Set GET all" GET "$BASE_URL/api/sets/myset" 200
test_endpoint "Set ISMEMBER" GET "$BASE_URL/api/sets/myset/member/member1" 200
test_endpoint "Set REMOVE" DELETE "$BASE_URL/api/sets/myset/member1" 200

# Sorted Sets
test_endpoint "ZSet ADD" POST "$BASE_URL/api/sorted-sets/zset1?member=player1&score=100" 200
test_endpoint "ZSet GET all" GET "$BASE_URL/api/sorted-sets/zset1" 200
test_endpoint "ZSet GET score" GET "$BASE_URL/api/sorted-sets/zset1/score/player1" 200
test_endpoint "ZSet DELETE" DELETE "$BASE_URL/api/sorted-sets/zset1/player1" 200

# Cache
test_endpoint "Cache GET (miss)" GET "$BASE_URL/api/cache/demo-key" 200
test_endpoint "Cache GET (hit)" GET "$BASE_URL/api/cache/demo-key" 200
test_endpoint "Cache EVICT" DELETE "$BASE_URL/api/cache/demo-key" 200

# Sessions
test_endpoint "Session PUT" PUT "$BASE_URL/api/sessions/sess1?attributeKey=user" 200 "john"
test_endpoint "Session GET" GET "$BASE_URL/api/sessions/sess1" 200
test_endpoint "Session DELETE" DELETE "$BASE_URL/api/sessions/sess1" 200

# Pub/Sub
test_endpoint "PubSub PUBLISH" POST "$BASE_URL/api/pubsub/news" 200 "hello world"
test_endpoint "PubSub GET messages" GET "$BASE_URL/api/pubsub/news" 200
test_endpoint "PubSub CLEAR" DELETE "$BASE_URL/api/pubsub/news" 200

# Rate Limiting
test_endpoint "Rate Limit CHECK" GET "$BASE_URL/api/rate-limit/check/client1" 200

# Leaderboard
test_endpoint "Leaderboard ADD" POST "$BASE_URL/api/leaderboard?player=alice&score=500" 200
test_endpoint "Leaderboard TOP" GET "$BASE_URL/api/leaderboard/top/10" 200
test_endpoint "Leaderboard RANK" GET "$BASE_URL/api/leaderboard/rank/alice" 200
test_endpoint "Leaderboard INCREMENT" POST "$BASE_URL/api/leaderboard/increment?player=alice&increment=50" 200

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="

if [ $FAIL -gt 0 ]; then
  exit 1
fi
```

### Script Usage

| Command | Description |
|---------|-------------|
| `./scripts/start-infra.sh` | Start Redis only, run app locally with `./gradlew bootRun` |
| `./scripts/start-all.sh` | Start full Docker stack (Redis + app) |
| `./scripts/e2e-test.sh` | Run E2E tests against running app (default: localhost:8080) |
| `BASE_URL=http://host:port ./scripts/e2e-test.sh` | Run E2E tests against a custom host |

## Testing Strategy

The application uses a dual testing approach:

- **Property-based tests**: Validate universal correctness properties (round-trips, ordering invariants, boundary enforcement) across many randomized inputs using JUnit 5 with jqwik as the property-based testing library.
- **Integration tests**: Use Testcontainers with a real Redis instance to verify service-layer behavior against an actual Redis server, ensuring no mocking artifacts.
- **Unit tests**: Cover specific examples, edge cases, and error conditions for controller and service layers.

Each property-based test runs a minimum of 100 iterations and references its corresponding design property. Testcontainers ensures tests run against a real Redis 7 container, providing confidence that Redis behavior matches production.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: String value round-trip

*For any* non-null key and non-null value, storing the value via PUT and then retrieving it via GET with the same key SHALL return the original value.

**Validates: Requirements 1.1, 1.2**

### Property 2: String deletion removes key

*For any* key that has been stored, deleting it via DELETE and then retrieving it via GET SHALL result in a 404 response.

**Validates: Requirements 1.3, 1.4**

### Property 3: Hash field round-trip

*For any* hash key, field name, and field value, storing the field-value pair via PUT and then retrieving it via GET (either the single field or all fields) SHALL return the original value.

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 4: Hash field deletion removes field

*For any* hash key and field that has been stored, deleting the field and then retrieving all fields SHALL return a map that does not contain the deleted field.

**Validates: Requirements 2.4**

### Property 5: List push-then-retrieve preserves order

*For any* sequence of values pushed to a list (with known directions), retrieving all elements SHALL return the values in the correct positional order (left-pushes prepend, right-pushes append).

**Validates: Requirements 3.1, 3.2, 3.3**

### Property 6: List pop returns correct end element

*For any* non-empty list, a left-pop SHALL return the head element and a right-pop SHALL return the tail element, reducing the list size by one.

**Validates: Requirements 3.4, 3.5**

### Property 7: Set membership round-trip

*For any* set key and member value, adding the member and then checking membership SHALL return true, and retrieving all members SHALL include that member.

**Validates: Requirements 4.1, 4.2, 4.4**

### Property 8: Set removal revokes membership

*For any* set key and member that has been added, removing the member and then checking membership SHALL return false.

**Validates: Requirements 4.3**

### Property 9: Sorted set score round-trip

*For any* sorted set key, member, and score, adding the member with a score and then querying the score for that member SHALL return the original score.

**Validates: Requirements 5.1, 5.5**

### Property 10: Sorted set ordering invariant

*For any* sorted set with multiple members, retrieving all members SHALL return them in ascending order by score (i.e., for consecutive elements a and b in the result, score(a) <= score(b)).

**Validates: Requirements 5.2**

### Property 11: Cache miss-hit-evict lifecycle

*For any* cache key, the first GET SHALL compute and return a value; a second GET with the same key SHALL return the identical value without recomputation; after a DELETE (evict), a subsequent GET SHALL compute a fresh value.

**Validates: Requirements 6.1, 6.2, 6.3**

### Property 12: Session attribute round-trip

*For any* session identifier and set of attribute key-value pairs, storing the attributes and then retrieving the session SHALL return all stored attributes with their original values.

**Validates: Requirements 7.1, 7.2**

### Property 13: Session deletion removes all data

*For any* session identifier that has been created with attributes, deleting the session and then retrieving it SHALL result in a 404 response.

**Validates: Requirements 7.3, 7.5**

### Property 14: Pub/Sub message storage round-trip

*For any* channel and list of messages stored in the in-memory listener store, retrieving messages for that channel SHALL return all stored messages in order.

**Validates: Requirements 8.3**

### Property 15: Pub/Sub message clearing

*For any* channel with stored messages, clearing the messages and then retrieving SHALL return an empty list.

**Validates: Requirements 8.4**

### Property 16: Rate limiter boundary enforcement

*For any* client identifier, the first N requests (where N = max-requests) within a time window SHALL succeed with a decreasing remaining count, and the (N+1)th request SHALL be rejected with HTTP 429.

**Validates: Requirements 9.1, 9.2**

### Property 17: Leaderboard score round-trip

*For any* player name and score, adding the player to the leaderboard and then querying their rank SHALL return the correct score.

**Validates: Requirements 10.1, 10.3**

### Property 18: Leaderboard descending order invariant

*For any* leaderboard with multiple players, retrieving the top N SHALL return players in descending order by score (i.e., for consecutive entries a and b, score(a) >= score(b)) and the result size SHALL be at most N.

**Validates: Requirements 10.2**

### Property 19: Leaderboard atomic score increment

*For any* player with an existing score S and any increment value D, atomically incrementing the score SHALL result in the new score being exactly S + D.

**Validates: Requirements 10.4**

### Property 20: Non-existent resource returns 404

*For any* key/session/player that has never been created, a GET request SHALL return an HTTP 404 response with a descriptive error message.

**Validates: Requirements 1.4, 2.5, 7.5, 10.5**


### Property 21: Docker Compose full-stack startup

*For any* fresh environment with Docker installed, running `docker compose up` SHALL result in both the Redis container and application container running, with the application able to successfully connect to Redis and respond to API requests on port 8080.

**Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6**

### Property 22: E2E test script validates all feature areas

*For any* running instance of the application connected to Redis, executing `scripts/e2e-test.sh` SHALL test at least one operation per feature area (Strings, Hashes, Lists, Sets, Sorted Sets, Cache, Sessions, Pub/Sub, Rate Limiting, Leaderboard) and exit with code 0 when all tests pass.

**Validates: Requirements 13.5, 13.6, 13.7**
