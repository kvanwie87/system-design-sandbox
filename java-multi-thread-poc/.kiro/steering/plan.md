# Implementation Plan — Spring Boot Multithreading PoC

## Problem Statement

Explore how Spring's `@Async` and `@Scheduled` work under the hood by building two modules: one using Spring's built-in annotations, and a second that replicates the same behavior with custom annotations backed by a hand-rolled BeanPostProcessor + proxy approach.

## Requirements

1. **Three use cases**, identical API across both modules:
   - **Fire-and-forget:** `POST /notifications/send` — triggers async work, returns immediately
   - **Async with polling:** `POST /reports/generate` returns a task ID; `GET /reports/{id}` returns status/result
   - **Scheduled task:** A job running on a fixed rate, observable via logs only

2. **Two Gradle submodules:**
   - `:spring-threading` — uses `@Async`, `@Scheduled`, `@EnableAsync`, `@EnableScheduling`
   - `:custom-threading` — uses `@MyAsync`, `@MyScheduled` with custom infrastructure

3. **Custom module approach:** Annotation + `BeanPostProcessor` that creates JDK dynamic proxies (or CGLIB via Spring's `ProxyFactory`), dispatching to a custom `ExecutorService`. Lightweight/demo-grade.

4. **Threading config (custom module):** Separate pools — a `ThreadPoolExecutor` for async work, a `ScheduledExecutorService` for scheduled work.

5. **Java 21, Spring Boot 4.1, Gradle multi-module.**

## Background

- Spring's `@Async` is processed by `AsyncAnnotationBeanPostProcessor`, which wraps beans in a proxy that submits method calls to a `TaskExecutor`.
- Spring's `@Scheduled` is processed by `ScheduledAnnotationBeanPostProcessor`, which registers methods with a `TaskScheduler`.
- Our custom implementation mirrors this: a BeanPostProcessor scans for `@MyAsync`/`@MyScheduled`, wraps the bean in a proxy, and routes invocations to our executors.
- For the polling pattern, we'll use a `ConcurrentHashMap<String, TaskStatus>` as an in-memory task store.

## Architecture

```mermaid
graph TD
    subgraph "Root Project"
        A[settings.gradle] --> B[:spring-threading]
        A --> C[:custom-threading]
    end

    subgraph ":spring-threading (port 8080)"
        B1[NotificationController] -->|@Async| B2[NotificationService]
        B3[ReportController] -->|@Async + Future| B4[ReportService]
        B5[ScheduledCleanupTask] -->|@Scheduled| B6[logs]
    end

    subgraph ":custom-threading (port 8081)"
        C1[NotificationController] -->|@MyAsync| C2[NotificationService]
        C3[ReportController] -->|@MyAsync + Future| C4[ReportService]
        C5[ScheduledCleanupTask] -->|@MyScheduled| C6[logs]
        C7[MyAsyncBeanPostProcessor] --> C8[AsyncProxy]
        C9[MyScheduledBeanPostProcessor] --> C10[ScheduledExecutorService]
    end
```

## Task Breakdown

### Task 1: Convert to Gradle multi-module structure

- **Objective:** Restructure the project into a multi-module Gradle build with `:spring-threading` and `:custom-threading` submodules.
- **Guidance:**
  - Update `settings.gradle` to include both submodules
  - Convert root `build.gradle` to a parent that applies common config (Java 21, Spring dependency management) via `subprojects {}`
  - Each submodule gets its own `build.gradle` with `spring-boot-starter-web` dependency
  - Each submodule gets its own `Application.java` main class
  - Configure `:spring-threading` on port 8080, `:custom-threading` on port 8081 via `application.properties`
- **Test:** Both apps start independently without errors (`./gradlew :spring-threading:bootRun` and `./gradlew :custom-threading:bootRun`)
- **Demo:** Two empty Spring Boot apps running on different ports, each returning a 404 on any request.

---

### Task 2: Implement fire-and-forget async in the spring-threading module

- **Objective:** Build `POST /notifications/send` using Spring's `@Async`.
- **Guidance:**
  - Add `@EnableAsync` to the application config
  - Create `NotificationService` with an `@Async` method `sendNotification(String message)` that sleeps 2s and logs completion
  - Create `NotificationController` with `POST /notifications/send` accepting a JSON body `{ "message": "..." }`, calls the service, returns `202 Accepted` immediately
  - Configure a `TaskExecutor` bean with a pool of 4 threads
- **Test:** Integration test that calls the endpoint, asserts 202 response returns in <500ms, and verifies (via log or spy) that the async method executes on a different thread.
- **Demo:** `curl -X POST localhost:8080/notifications/send -d '{"message":"hello"}'` returns instantly, log shows async execution 2s later.

---

### Task 3: Implement async-with-polling in the spring-threading module

- **Objective:** Build `POST /reports/generate` and `GET /reports/{id}` using `@Async` returning `CompletableFuture`.
- **Guidance:**
  - Create `ReportService` with `@Async CompletableFuture<String> generateReport(String taskId)` — sleeps 5s, produces a dummy report string, updates an in-memory `ConcurrentHashMap<String, TaskStatus>` (status: PENDING → COMPLETE, plus result)
  - Create `ReportController`:
    - `POST /reports/generate` — generates a UUID task ID, submits the async work, returns `202` with `{ "taskId": "..." }`
    - `GET /reports/{id}` — looks up the task store, returns status and result if complete, or 404 if unknown
  - Define a simple `TaskStatus` record/class with fields: `status` (PENDING/COMPLETE), `result`
- **Test:** Integration test that posts, gets PENDING, waits, then gets COMPLETE with result.
- **Demo:** Demonstrate the polling flow via curl.

---

### Task 4: Implement scheduled task in the spring-threading module

- **Objective:** Add a `@Scheduled` task that logs a message every 10 seconds.
- **Guidance:**
  - Add `@EnableScheduling` to the application config
  - Create `ScheduledCleanupTask` with `@Scheduled(fixedRate = 10000)` method that logs timestamp + "cleanup executed"
- **Test:** Integration test using `Awaitility` — start context, wait up to 15s, verify log output contains at least one execution.
- **Demo:** Start the app, observe log output every 10 seconds.

---

### Task 5: Create custom annotations and async BeanPostProcessor in the custom-threading module

- **Objective:** Implement `@MyAsync` annotation and the `MyAsyncBeanPostProcessor` that wraps annotated beans in a proxy dispatching to a custom `ExecutorService`.
- **Guidance:**
  - Define `@MyAsync` annotation (method-level, runtime retention)
  - Create `MyAsyncBeanPostProcessor` implementing `BeanPostProcessor`:
    - In `postProcessAfterInitialization`, scan the bean's methods for `@MyAsync`
    - If found, wrap the bean in a JDK dynamic proxy (or use Spring's `ProxyFactory`) that intercepts `@MyAsync` methods and submits them to an injected `ExecutorService`
    - Handle `CompletableFuture` return types: if method returns `CompletableFuture`, the proxy returns a future that completes when the async work finishes
    - For void methods, fire-and-forget
  - Register a `ThreadPoolExecutor` bean (4 threads) for async work
- **Test:** Unit test — create a test bean with `@MyAsync void` and `@MyAsync CompletableFuture<String>` methods, process it through the BPP, invoke both, verify execution happens on pool thread.
- **Demo:** Unit test passes, proving proxy interception works.

---

### Task 6: Create custom scheduled BeanPostProcessor in the custom-threading module

- **Objective:** Implement `@MyScheduled` annotation and `MyScheduledBeanPostProcessor` that registers annotated methods with a `ScheduledExecutorService`.
- **Guidance:**
  - Define `@MyScheduled` annotation with `fixedRate` attribute (milliseconds)
  - Create `MyScheduledBeanPostProcessor` implementing `BeanPostProcessor` and `DisposableBean`:
    - In `postProcessAfterInitialization`, scan for `@MyScheduled` methods
    - Schedule them on a `ScheduledExecutorService` at the specified fixed rate
    - On `destroy()`, shut down the executor gracefully
  - Register a `ScheduledExecutorService` bean (2 threads)
- **Test:** Unit test — create a test bean with `@MyScheduled(fixedRate = 100)`, process through BPP, verify using `Awaitility` that the method executes multiple times within 500ms.
- **Demo:** Unit test passes, proving scheduled execution works.

---

### Task 7: Wire up controllers and services in the custom-threading module

- **Objective:** Replicate the same REST API (`/notifications/send`, `/reports/generate`, `/reports/{id}`) and scheduled task in the custom module using `@MyAsync` and `@MyScheduled`.
- **Guidance:**
  - Create `NotificationService` with `@MyAsync void sendNotification(...)` — same behavior as spring module
  - Create `ReportService` with `@MyAsync CompletableFuture<String> generateReport(...)` — same polling pattern with `ConcurrentHashMap`
  - Create `ScheduledCleanupTask` with `@MyScheduled(fixedRate = 10000)` — same log output
  - Create controllers identical to the spring module
  - Ensure the BPPs, executors, and services are all registered in the Spring context
- **Test:** Same integration tests as Tasks 2–4 but running against port 8081.
- **Demo:** `curl` against port 8081 produces identical behavior to port 8080 — fire-and-forget returns 202, polling works, scheduled task logs every 10s.
