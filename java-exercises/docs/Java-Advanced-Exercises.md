# Core Java Interview Exercises (50)

Programming exercises aligned with the most common core Java interview topics, ordered by how frequently they appear in interviews (most common first).

### Exercise 3. Concurrency — Producer/Consumer

**Concept:** Thread coordination, race conditions, proper shutdown signaling.

**Exercise:** Implement a bounded producer/consumer using `BlockingQueue`:
- Producer generates integers 1–100
- Consumer processes them (prints or sums)
- Use a poison pill to signal termination
- Handle `InterruptedException` properly

```java
public class ProducerConsumer {
    private final BlockingQueue<Integer> queue;
    private static final int POISON_PILL = -1;
    // Implement Producer and Consumer as Runnables
}
```

[View Solution](Answers-01-10.md#exercise-3-concurrency--producerconsumer)

---

### Exercise 13. Generics — Bounded Type Parameters (PECS)

**Concept:** Upper/lower bounds, type erasure, PECS (Producer Extends, Consumer Super).

**Exercise:** Implement a `CollectionUtils` class:
```java
public class CollectionUtils {
    public static <T> void copy(List<? super T> dest, List<? extends T> src) { }
    public static <T extends Comparable<T>> T max(List<T> list) { }
    public static <T extends Comparable<T>> List<T> mergeSorted(List<T> a, List<T> b) { }
}
```

[View Solution](Answers-11-20.md#exercise-13-generics--bounded-type-parameters-pecs)

---

### Exercise 15. ConcurrentHashMap Internals

**Concept:** Segment-based locking (pre-Java 8), CAS + synchronized buckets (Java 8+), when to use over synchronized HashMap.

**Exercise:** Demonstrate:
1. Why `HashMap` fails under concurrent access (lost updates)
2. `ConcurrentHashMap` atomics: `putIfAbsent`, `compute`, `merge`
3. Implement a thread-safe word frequency counter using `ConcurrentHashMap`

```java
public class WordCounter {
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    public void countWords(String text) { }
    public Map<String, Long> getTopN(int n) { }
}
```

[View Solution](Answers-11-20.md#exercise-15-concurrenthashmap-internals)

---

### Exercise 16. Multithreading — CompletableFuture Composition

**Concept:** Async programming, future chaining, exception handling in async code.

**Exercise:** Simulate an API aggregation service:
1. Fetch user profile (200ms), orders (300ms), recommendations (150ms) in parallel
2. Combine into a single response
3. Timeout at 500ms with fallback values
4. Handle partial failures gracefully

```java
public class ApiAggregator {
    public CompletableFuture<AggregatedResponse> fetchUserDashboard(String userId) { }
}
```

[View Solution](Answers-11-20.md#exercise-16-multithreading--completablefuture-composition)

---

### Exercise 17. volatile and Happens-Before

**Concept:** Memory visibility, instruction reordering, when volatile is sufficient vs when you need locks.

**Exercise:** Demonstrate:
1. A broken flag-based thread stop (without volatile — may never see update)
2. Fixed with `volatile`
3. A scenario where volatile is NOT sufficient (check-then-act race condition)

```java
public class VolatileDemo {
    private /* volatile? */ boolean running = true;

    public void stop() { running = false; }
    public void run() { while (running) { /* work */ } }
}
```

[View Solution](Answers-11-20.md#exercise-17-volatile-and-happens-before)

---

### Exercise 18. ReentrantLock vs synchronized

**Concept:** Interruptible locking, tryLock, fairness, multiple conditions.

**Exercise:** Implement a bounded buffer using `ReentrantLock` with two `Condition` objects (notFull, notEmpty). Show why this is better than `synchronized` + `wait()/notify()`:
- `tryLock` with timeout (don't wait forever)
- Interruptible lock acquisition
- Separate conditions for producers and consumers

```java
public class BoundedBuffer<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    public void put(T item) throws InterruptedException { }
    public T take() throws InterruptedException { }
}
```

[View Solution](Answers-11-20.md#exercise-18-reentrantlock-vs-synchronized)

---

### Exercise 19. CountDownLatch and CyclicBarrier

**Concept:** Thread coordination patterns — wait-for-all vs meet-at-point.

**Exercise:** Implement two scenarios:
1. **CountDownLatch:** A service startup that waits for 3 subsystems to initialize before accepting requests
2. **CyclicBarrier:** A parallel computation where 4 threads each process a quarter of an array, then merge results

```java
public class StartupCoordinator {
    private final CountDownLatch latch = new CountDownLatch(3);
    public void subsystemReady(String name) { }
    public void awaitStartup() throws InterruptedException { }
}
```

[View Solution](Answers-11-20.md#exercise-19-countdownlatch-and-cyclicbarrier)

---

### Exercise 20. ThreadLocal — Per-Thread Context

**Concept:** Thread-local storage, memory leaks in thread pools, when to use.

**Exercise:** Implement a `RequestContext` that stores user info per thread:
1. Set context at request entry, read it deep in the call stack without passing parameters
2. Demonstrate the memory leak when using ThreadLocal with a thread pool (forgetting to `remove()`)
3. Fix with try-finally cleanup

```java
public class RequestContext {
    private static final ThreadLocal<UserInfo> context = new ThreadLocal<>();
    public static void set(UserInfo info) { }
    public static UserInfo get() { }
    public static void clear() { }
}
```

[View Solution](Answers-11-20.md#exercise-20-threadlocal--per-thread-context)

---

### Exercise 25. Semaphore — Rate Limiter

**Concept:** Counting semaphore, resource pool limiting, fairness.

**Exercise:** Implement a connection pool using `Semaphore`:
- Fixed number of connections (e.g., 5)
- `acquire()` blocks when all connections are in use
- `release()` returns a connection
- Add a timeout variant (`tryAcquire`)

```java
public class ConnectionPool {
    private final Semaphore semaphore;
    private final BlockingQueue<Connection> pool;
    public Connection borrowConnection(long timeout, TimeUnit unit) throws Exception { }
    public void returnConnection(Connection conn) { }
}
```

[View Solution](Answers-21-30.md#exercise-25-semaphore--rate-limiter)

---

### Exercise 26. ForkJoinPool — Parallel Divide and Conquer

**Concept:** Work-stealing, RecursiveTask vs RecursiveAction, when to fork.

**Exercise:** Implement parallel merge sort using `ForkJoinPool`:
- Extend `RecursiveTask<int[]>`
- Fork when array size > threshold, compute directly when small
- Compare performance against sequential sort

```java
public class ParallelMergeSort extends RecursiveTask<int[]> {
    private final int[] array;
    private static final int THRESHOLD = 1000;
    @Override
    protected int[] compute() { }
}
```

[View Solution](Answers-21-30.md#exercise-26-forkjoinpool--parallel-divide-and-conquer)

---

### Exercise 39. Memory and GC — Identify the Leak

**Concept:** Strong/soft/weak references, common leak patterns.

**Exercise:** Implement a cache three ways:
1. `HashMap` (leaks)
2. `WeakHashMap` (GC-friendly)
3. Bounded LRU `LinkedHashMap`

Test with 100,000 entries, trigger GC, show memory difference.

[View Solution](Answers-31-40.md#exercise-39-memory-and-gc--identify-the-leak)

---



### Exercise 41. Reflection and Custom DI Container

**Concept:** Runtime class inspection, custom annotations, field injection.

**Exercise:** Build a mini DI framework:
1. `@MyComponent` and `@MyInject` annotations
2. `DIContainer` that scans classes, instantiates, and injects dependencies

```java
@MyComponent
public class UserService {
    @MyInject
    private UserRepository repository;
}
```

[View Solution](Answers-41-50.md#exercise-41-reflection-and-custom-di-container)

---

### Exercise 42. Class Loading and Initialization Order

**Concept:** Static blocks, instance blocks, parent-before-child, when classes are loaded.

**Exercise:** Predict the exact output of this inheritance chain:

```java
class Parent {
    static { System.out.println("Parent static"); }
    { System.out.println("Parent instance"); }
    Parent() { System.out.println("Parent constructor"); }
}
class Child extends Parent {
    static { System.out.println("Child static"); }
    { System.out.println("Child instance"); }
    Child() { System.out.println("Child constructor"); }
}
// What prints when: new Child(); new Child();
```

Then demonstrate: `Class.forName()` triggers static init, `ClassLoader.loadClass()` does not.

[View Solution](Answers-41-50.md#exercise-42-class-loading-and-initialization-order)

---

### Exercise 47. Virtual Threads (Java 21+)

**Concept:** Project Loom, platform vs virtual threads, structured concurrency.

**Exercise:** Compare:
1. 10,000 blocking I/O tasks on platform threads (thread pool exhaustion)
2. Same 10,000 tasks on virtual threads (scales easily)
3. Demonstrate `Thread.ofVirtual().start()` and `Executors.newVirtualThreadPerTaskExecutor()`

```java
public class VirtualThreadDemo {
    // Simulate 10k HTTP calls that each block for 100ms
    public void platformThreads() { }  // Struggles with default pool size
    public void virtualThreads() { }    // Handles it effortlessly
}
```

[View Solution](Answers-41-50.md#exercise-47-virtual-threads-java-21)

---



### Exercise 49. Memory Visibility — Double-Checked Locking Deep Dive

**Concept:** Why DCL was broken before Java 5, what volatile actually guarantees, happens-before edges.

**Exercise:** Demonstrate the three versions of lazy initialization:
1. **Broken DCL** (no volatile) — explain what can go wrong (partially constructed object)
2. **Fixed DCL** (with volatile) — explain the happens-before guarantee
3. **Holder idiom** — explain why class loading provides the same guarantee without volatile

Write comments explaining the JMM guarantees for each line.

[View Solution](Answers-41-50.md#exercise-49-memory-visibility--double-checked-locking-deep-dive)

---

### Exercise 50. Putting It All Together — Mini In-Memory Database

**Concept:** Combines collections, concurrency, generics, streams, design patterns.

**Exercise:** Build a thread-safe in-memory data store:
- Generic `Table<T>` with CRUD operations
- Index support (secondary indexes using `ConcurrentHashMap`)
- Query API using predicates: `table.where(user -> user.age() > 25).orderBy(User::name).limit(10)`
- Read-write lock for concurrent access (multiple readers, exclusive writer)
- Builder pattern for configuration

```java
public class MiniDB {
    public <T> Table<T> createTable(String name, Class<T> type) { }
}

public class Table<T> {
    public void insert(T record) { }
    public Query<T> where(Predicate<T> filter) { }
    public void createIndex(String name, Function<T, ?> keyExtractor) { }
}
```

[View Solution](Answers-41-50.md#exercise-50-putting-it-all-together--mini-in-memory-database)
