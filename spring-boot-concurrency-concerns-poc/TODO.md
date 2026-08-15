# TODO — Additional Concurrency Scenarios

Future additions to the PoC, ordered by real-world impact for Spring Boot developers.

## Implemented

- ~~1. Check-Then-Act (TOCTOU)~~ ✅
- ~~2. Non-Atomic Compound Operations on Concurrent Collections~~ ✅
- ~~3. Thread Starvation / Resource Exhaustion~~ ✅

## High Priority

### 4. Lazy Initialization Race (Double-Checked Locking)
- **Broken:** Singleton lazily initializes an expensive resource without synchronization — two threads both see `null`, create duplicates, or see a partially constructed object
- **Fix:** Proper DCL with `volatile`, Holder class idiom, or `AtomicReference.compareAndSet`
- **Test:** Multiple threads access the lazy field simultaneously; assert only one instance is ever created

### 5. Broken Iterator / Concurrent Modification
- **Broken:** Iterating a shared `ArrayList` while another thread adds/removes elements → `ConcurrentModificationException` or skipped elements
- **Fix:** `CopyOnWriteArrayList`, snapshot iteration, or synchronizing the iteration block
- **Test:** One thread iterates while another mutates; assert no exceptions and consistent view

## Medium Priority

### 6. @Async + @Transactional Interaction
- **Broken:** Method annotated with both `@Async` and `@Transactional` runs in a new thread where the transaction context is lost → `LazyInitializationException` or silent non-transactional writes
- **Fix:** Separate the async dispatch from the transactional work into distinct beans (async bean calls transactional bean)
- **Test:** Trigger async+transactional operation; assert data is persisted correctly and lazy-loaded relationships resolve

### 7. Publish-Before-Construction (This-Escape)
- **Broken:** Passing `this` to another thread or registering a listener in the constructor before fields are fully initialized → observer sees null/default field values
- **Fix:** Factory method pattern — complete construction before publishing the reference
- **Test:** Observer thread reads fields immediately after registration; assert fields are non-null

### 8. Stale Cache Reads
- **Broken:** In-memory cache (`HashMap`) serves reads while another thread writes — readers see partially updated or stale entries indefinitely
- **Fix:** `ConcurrentHashMap`, read/write locks, or Spring `@Cacheable` with a proper cache provider
- **Test:** Writer updates cache while readers read; assert readers eventually see the update and never see partial state

## Low Priority (Niche)

### 9. ABA Problem
- **Broken:** A value changes A→B→A; a compare-and-swap thinks nothing changed and proceeds incorrectly
- **Fix:** `AtomicStampedReference` — tracks a version stamp alongside the value
- **Test:** Simulate A→B→A transition and show CAS succeeds incorrectly vs stamped reference detecting the change

### 10. Livelock
- **Broken:** Two threads keep yielding to each other in a retry loop but never make progress (like two people sidestepping in a hallway)
- **Fix:** Randomized exponential backoff to break symmetry
- **Test:** Two threads that deterministically yield to each other; assert they eventually make progress with backoff but don't without it
