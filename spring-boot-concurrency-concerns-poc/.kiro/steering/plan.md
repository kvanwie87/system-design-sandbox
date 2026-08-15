# Implementation Plan - Spring Boot Concurrency Concerns PoC

## Problem Statement

Build a Spring Boot PoC that demonstrates common concurrency bugs and their fixes side-by-side. Two Gradle modules expose identical counter/stats APIs — one broken, one fixed — with integration tests that prove the difference under concurrent load.

## Requirements

- Multi-module Gradle project with shared API definition
- Domain: named counters (increment, decrement, read, transfer)
- Concurrency problems demonstrated: race conditions, lost updates, deadlocks, visibility issues
- Both in-memory (Java-level) and DB (H2) examples
- Fixes use Java concurrency primitives (synchronized, ReentrantLock, AtomicLong, volatile) and DB mechanisms (optimistic locking, pessimistic locking, isolation levels)
- Integration tests using JUnit with concurrent threads to assert broken vs correct behavior
- Java 21, Spring Boot 4.1.0, Gradle

## Background

- Existing project: bare Spring Boot 4.1.0 scaffold with Java 21 toolchain, single-module Gradle
- Needs conversion to multi-module layout with three subprojects: `common-api`, `concurrency-broken`, `concurrency-fixed`
- H2 in-memory DB for the database-layer examples (embedded, zero config)
- Spring Data JPA for repository layer

## Proposed Solution
root ├── common-api/ (shared interfaces, DTOs, entity definitions) ├── concurrency-broken/ (demonstrates bugs) ├── concurrency-fixed/ (demonstrates corrections) ├── build.gradle (root build with shared config) └── settings.gradle (includes all subprojects)

Each runnable module (`broken` and `fixed`) implements the same `CounterController` interface from `common-api`. Tests live in each module and use `ExecutorService` + `CountDownLatch` to hammer endpoints concurrently, asserting that the broken module produces incorrect results and the fixed module produces correct results.

### Concurrency Scenarios Mapped to Layers

| Scenario | Broken Behavior | Fix (Java) | Fix (DB) |
|----------|----------------|------------|----------|
| Race condition | Plain `HashMap` + `int` counter | `ConcurrentHashMap` + `AtomicLong` | N/A |
| Lost update | Read-modify-write without lock | `synchronized` block | `@Version` optimistic locking |
| Deadlock | Transfer locks A→B and B→A | Ordered lock acquisition via `ReentrantLock` | `SELECT FOR UPDATE` with consistent ordering |
| Visibility | Non-volatile flag, stale reads | `volatile` keyword | Proper `@Transactional` isolation |

## Task Breakdown

### Task 1: Convert to multi-module Gradle layout

**Objective:** Restructure the project into `common-api`, `concurrency-broken`, and `concurrency-fixed` submodules.

**Implementation guidance:**
- Update `settings.gradle` to include three subprojects
- Root `build.gradle` defines shared plugins/dependencies (Spring Boot, JPA, H2, test)
- `common-api/build.gradle` is a plain Java library (no Spring Boot plugin application)
- Both runnable modules depend on `common-api` and apply the Spring Boot plugin
- Remove existing `src/` directory (it's empty scaffolding)
- Each runnable module gets `src/main/java`, `src/main/resources/application.yml`, `src/test/java`

**Test:** `./gradlew build` compiles all three modules cleanly.

**Demo:** Project compiles with three modules, IDE recognizes the structure.

---

### Task 2: Define shared API in common-api module

**Objective:** Create the shared controller interface, DTOs, and JPA entity.

**Implementation guidance:**
- `CounterController` interface with endpoints: `increment`, `decrement`, `getValue`, `transfer`
- `CounterResponse` DTO (name, value)
- `TransferRequest` DTO (fromCounter, toCounter, amount)
- `CounterEntity` JPA entity with `id`, `name`, `value`, `version` (for optimistic locking later)
- `CounterRepository` extending `JpaRepository`

**Test:** Module compiles, no runtime tests yet (it's a library).

**Demo:** Shared types available for both modules to implement against.

---

### Task 3: Implement broken in-memory counter (race condition + visibility)

**Objective:** Build the broken module's in-memory counter service using a plain `HashMap<String, Long>` with no synchronization.

**Implementation guidance:**
- `BrokenInMemoryCounterService` — uses `HashMap` and plain `long` values
- Read-modify-write in `increment`/`decrement` without any locking
- A `volatile`-missing visibility scenario: a background thread updates a flag; the reader thread sees stale value
- Wire up `CounterController` implementation delegating to this service
- `application.yml` with `server.port=8081`

**Test:** Integration test spawns 100 threads each incrementing same counter 1000 times. Assert final value is NOT 100,000 (proving the race).

**Demo:** Running the test shows a final count less than expected, demonstrating data loss from races.

---

### Task 4: Implement fixed in-memory counter (race condition + visibility)

**Objective:** Build the fixed module's in-memory counter using `ConcurrentHashMap<String, AtomicLong>` and `volatile` flag.

**Implementation guidance:**
- `FixedInMemoryCounterService` — `ConcurrentHashMap` with `AtomicLong`
- `volatile boolean` for the visibility scenario
- Wire up `CounterController` implementation
- `application.yml` with `server.port=8082`

**Test:** Same 100-thread × 1000-increment test. Assert final value IS exactly 100,000.

**Demo:** Test passes proving atomicity; contrast with Task 3's failing assertion.

---

### Task 5: Implement broken DB counter (lost update)

**Objective:** Demonstrate lost updates via read-modify-write in the broken module's DB-backed service.

**Implementation guidance:**
- `BrokenDbCounterService` — reads entity, increments in Java, saves back (no `@Version` check)
- Uses default transaction isolation (READ_COMMITTED)
- No optimistic/pessimistic locking
- New endpoint profile or path prefix `/db/counters/...` to distinguish from in-memory

**Test:** 50 concurrent threads each increment same DB counter 100 times. Assert final value < 5000 (lost updates).

**Demo:** Test proves that DB writes are silently lost without locking.

---

### Task 6: Implement fixed DB counter (optimistic + pessimistic locking)

**Objective:** Fix lost updates using `@Version` optimistic locking with retry, and demonstrate pessimistic locking alternative.

**Implementation guidance:**
- `FixedDbCounterService` — uses `@Version` on entity; catches `OptimisticLockException` and retries (bounded retry loop)
- Alternative method using `@Lock(PESSIMISTIC_WRITE)` repository query for comparison
- Same `/db/counters/...` path prefix

**Test:** Same 50×100 concurrent test. Assert final value IS exactly 5000.

**Demo:** Test passes; all updates accounted for with either locking strategy.

---

### Task 7: Implement broken transfer (deadlock)

**Objective:** Demonstrate deadlock via inconsistent lock ordering in the broken module.

**Implementation guidance:**
- `BrokenTransferService` — acquires lock on `fromCounter` first, then `toCounter` (using `synchronized` on counter objects or `ReentrantLock`)
- Two concurrent transfers A→B and B→A will deadlock
- For DB variant: two transactions lock rows in opposite order

**Test:** Spawn two threads doing opposite transfers simultaneously. Assert that at least one thread times out or the test hangs (with a timeout annotation to catch it).

**Demo:** Test demonstrates deadlock detection (timeout triggers, proving the issue).

---

### Task 8: Implement fixed transfer (ordered locking)

**Objective:** Fix the deadlock by enforcing consistent lock acquisition order.

**Implementation guidance:**
- `FixedTransferService` — always locks the counter with the lower ID/name first (lexicographic ordering)
- For DB variant: `SELECT FOR UPDATE` ordered by counter name/ID
- Both transfers acquire locks in the same global order, preventing circular wait

**Test:** Same opposing-transfer test. Assert both complete within timeout and balances are consistent (zero-sum).

**Demo:** Test passes cleanly, no timeout, balances correct.

---

### Task 9: Add a summary test suite and documentation

**Objective:** Create a top-level test runner and README documenting each scenario.

**Implementation guidance:**
- A test class in each module that runs all scenarios together as a suite
- `README.md` at root explaining: project structure, how to run, what each test proves, expected output for broken vs fixed
- Table mapping each concurrency problem → broken behavior → fix applied

**Test:** `./gradlew test` runs all tests across both modules; broken tests assert incorrect results, fixed tests assert correct results.

**Demo:** Full test suite runs green; README serves as walkthrough guide for the PoC.

