# Spring Boot Concurrency Concerns PoC

A proof-of-concept demonstrating common concurrency bugs and their fixes side-by-side using Spring Boot 4.1.0 and Java 21.

## Project Structure

```
root
├── common-api/              Shared interfaces, DTOs, JPA entity
├── concurrency-broken/      Demonstrates concurrency bugs
├── concurrency-fixed/       Demonstrates correct solutions
├── build.gradle             Root build with shared config
└── settings.gradle          Multi-module includes
```

## Quick Start

```bash
# Run all tests (broken module proves bugs, fixed module proves corrections)
./gradlew test

# Run only broken module tests
./gradlew :concurrency-broken:test

# Run only fixed module tests
./gradlew :concurrency-fixed:test

# Run the broken app (port 8081)
./gradlew :concurrency-broken:bootRun

# Run the fixed app (port 8082)
./gradlew :concurrency-fixed:bootRun
```

## Concurrency Scenarios

| Scenario | Broken Behavior | Fix (Java) | Fix (DB) |
|----------|----------------|------------|----------|
| Race condition | Plain `HashMap` + `long` counter | `ConcurrentHashMap` + `AtomicLong` | N/A |
| Shared fields | Singleton stores per-request state in instance fields | Stateless design (local variables) / `@RequestScope` bean | N/A |
| Non-atomic compound ops | `get` + modify + `put` on ConcurrentHashMap | `merge()` / `computeIfPresent()` | N/A |
| Check-then-act (TOCTOU) | `containsKey` + `put` as separate calls | `ConcurrentHashMap.computeIfAbsent` | `INSERT ... ON CONFLICT` |
| Lost update | Read-modify-write without lock | N/A | `@Version` optimistic locking / `SELECT FOR UPDATE` |
| Thread starvation | Single shared pool exhausted by slow tasks | Bulkhead pattern (separate pools per task type) | N/A |
| Visibility | Non-volatile flag | `volatile` keyword | N/A |
| Deadlock | Lock A then B; lock B then A | Lexicographic lock ordering via `ReentrantLock` | Consistent row-lock ordering |

## Test Results Summary

### Broken Module (`concurrency-broken`)

| Test | What It Proves | Expected Output |
|------|---------------|-----------------|
| `RaceConditionTest` | 100 threads × 1000 increments lose updates | Final count ~35,000-41,000 (not 100,000) |
| `LostUpdateTest` | 50 threads × 100 DB increments lose updates | Final count ~600 (not 5,000) |
| `DeadlockTest` | Opposing transfers deadlock | Both threads timeout (5s each) |
| `SharedFieldTest` | Singleton fields bleed between concurrent requests | Corruptions detected (wrong user, wrong totals) |
| `CheckThenActTest` | Multiple threads initialize same counter | 49 duplicate initializations (should be 1) |
| `CompoundOperationTest` | get+modify+put loses updates, containsKey+get NPEs | Lost updates, NPEs, over-decrement |
| `ThreadStarvationTest` | Fast tasks starved by slow tasks in shared pool | Fast task times out (2s) despite being instant |

### Fixed Module (`concurrency-fixed`)

| Test | What It Proves | Expected Output |
|------|---------------|-----------------|
| `RaceConditionFixedTest` | AtomicLong prevents data loss | Final count = exactly 100,000 |
| `LostUpdateFixedTest` | DB locking prevents lost updates | Final count = exactly 5,000 (both strategies) |
| `DeadlockFixedTest` | Ordered locking prevents deadlock | Both threads complete, balances conserved |
| `SharedFieldFixedTest` | Stateless and request-scoped approaches prevent corruption | Zero corruptions across all runs |
| `CheckThenActFixedTest` | computeIfAbsent initializes exactly once | Exactly 1 initialization every run |
| `CompoundOperationFixedTest` | merge/computeIfPresent are atomic | Exact counts, zero NPEs, correct guards |
| `ThreadStarvationFixedTest` | Bulkhead pattern isolates task pools | Fast tasks complete instantly despite slow load |

## API Endpoints

Both modules implement identical APIs:

### In-Memory Counters
- `POST /counters/{name}/increment` - Increment a named counter
- `POST /counters/{name}/decrement` - Decrement a named counter
- `GET /counters/{name}` - Read current value
- `POST /counters/transfer` - Transfer between counters (body: `{"fromCounter": "A", "toCounter": "B", "amount": 10}`)

### DB-Backed Counters
- `POST /db/counters/{name}/increment` - Increment (DB-backed)
- `POST /db/counters/{name}/decrement` - Decrement (DB-backed)
- `GET /db/counters/{name}` - Read current value (DB-backed)
- `POST /db/counters/transfer` - Transfer between counters (DB-backed)

## Key Implementation Details

### Race Condition (In-Memory)
- **Broken:** `HashMap.getOrDefault()` + arithmetic + `put()` — three separate non-atomic operations
- **Fixed:** `ConcurrentHashMap.computeIfAbsent()` + `AtomicLong.incrementAndGet()` — single atomic operation

### Lost Update (Database)
- **Broken:** Entity without `@Version`, read-modify-write in transaction with no locking — concurrent transactions overwrite each other
- **Fixed (Optimistic):** `@Version` field on entity; `ObjectOptimisticLockingFailureException` triggers retry in a new transaction (`REQUIRES_NEW`)
- **Fixed (Pessimistic):** `@Lock(PESSIMISTIC_WRITE)` on repository query issues `SELECT FOR UPDATE`, blocking concurrent readers

### Deadlock (Transfer)
- **Broken:** Locks acquired in argument order (`from` → `to`) — two threads transferring in opposite directions create circular wait
- **Fixed:** Locks acquired in lexicographic order of counter name — both threads lock "A" before "B" regardless of transfer direction, eliminating circular wait

### Visibility
- **Broken:** Plain `boolean` field — JIT compiler may cache value in CPU register, never re-reading from main memory
- **Fixed:** `volatile boolean` — forces read/write through main memory, establishing happens-before relationship

### Shared Fields (Singleton State Bleed)
- **Broken:** Singleton service stores `currentUser`, `runningTotal`, and `operationLog` as instance fields — concurrent requests overwrite each other's state (User A sees User B's data)
- **Fixed (Stateless):** Eliminate instance fields entirely. All per-request state lives in local variables passed through method parameters. The simplest and most performant fix.
- **Fixed (Request-Scoped):** Annotate the stateful class with `@RequestScope`. Spring creates a new instance per HTTP request via a scoping proxy, isolating state automatically. Use when state must be shared across multiple method calls within a single request.

### Check-Then-Act (TOCTOU)
- **Broken:** `if (!map.containsKey(k)) { map.put(k, value) }` — between the check and the put, another thread can also pass the check and create a duplicate. 49 out of 50 threads all "initialize" the same counter.
- **Fixed:** `ConcurrentHashMap.computeIfAbsent(key, mappingFunction)` — the check-and-create is a single atomic operation. The mapping function executes at most once per key regardless of concurrent access.

### Non-Atomic Compound Operations
- **Broken:** Even on a `ConcurrentHashMap`, multi-step operations like `get()` + arithmetic + `put()` are NOT atomic. Individual method calls are thread-safe, but the compound sequence is still racy. Also: `containsKey()` + `get()` can NPE if the entry is removed between the two calls.
- **Fixed:** Use `merge()` for atomic accumulation, `computeIfPresent()` for atomic conditional updates, and single `get()` with null-check instead of `containsKey()` + `get()`.

### Thread Starvation (Resource Exhaustion)
- **Broken:** A single 2-thread pool handles both slow (5-second) and fast (instant) tasks. When slow tasks occupy all threads, fast tasks queue up and effectively hang — even though they need less than 1ms to complete.
- **Fixed:** Bulkhead pattern — separate dedicated thread pools for different task types. Slow tasks get their own pool sized for throughput; fast/critical tasks get a separate pool with more threads. One category can never starve the other.

## Technology Stack

- Java 21
- Spring Boot 4.1.0
- Gradle 9.5.1 (multi-module)
- H2 in-memory database
- Spring Data JPA
- JUnit 5 with JUnit Platform Suite
- AssertJ

## Future Work

See [TODO.md](TODO.md) for additional concurrency scenarios planned for this PoC, including check-then-act (TOCTOU), thread starvation, lazy initialization races, and more.
