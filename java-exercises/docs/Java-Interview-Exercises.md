# Core Java Interview Exercises (50)

Programming exercises aligned with the most common core Java interview topics, ordered by how frequently they appear in interviews (most common first).

---

## TIER 1 — Asked in Almost Every Interview

---

### Exercise 1. HashMap Internals — Custom Key & Immutability

**Concept:** How `hashCode()` and `equals()` contract works. What happens when you use a mutable key. How immutability fixes it.

**Exercise:** Create a `Person` class (with `name`, `age`, and `List<String> nicknames`) to use as a HashMap key. Demonstrate:
1. Correct implementation of `hashCode()` and `equals()`
2. What breaks when you mutate a key after insertion
3. What breaks when `hashCode()` always returns the same value (bucket collision)
4. Fix the mutable key problem by creating an `ImmutablePerson` class:
   - All fields `private final`
   - No setters
   - Class is `final` (prevent subclass breaking invariants)
   - Defensive copy of mutable fields in constructor (`List.copyOf`)
   - Return unmodifiable views from getters
   - Show how `record` achieves the same thing automatically

```java
// Mutable version (broken as a key)
public class Person {
    private String name;
    private int age;
    private List<String> nicknames;
}

// Immutable version (safe as a key)
public final class ImmutablePerson {
    private final String name;
    private final int age;
    private final List<String> nicknames;

    public ImmutablePerson(String name, int age, List<String> nicknames) {
        this.name = name;
        this.age = age;
        this.nicknames = List.copyOf(nicknames); // defensive copy
    }
    // Getters only, no setters
}

// Record version (immutable by default)
public record PersonRecord(String name, int age, List<String> nicknames) {
    public PersonRecord {
        nicknames = List.copyOf(nicknames); // compact constructor for defensive copy
    }
}
```

[View Solution](Answers-01-10.md#exercise-1-hashmap-internals--custom-key)

---

### Exercise 2. String Immutability and the String Pool

**Concept:** Why are Strings immutable? What is the String pool? `==` vs `.equals()`.

**Exercise:** Implement a `StringAnalyzer` class with a method that takes two strings and returns whether they are:
- Same reference (pool hit)
- Same value (content equal)
- Anagram of each other

```java
public class StringAnalyzer {
    public String analyze(String a, String b) { }
}
```

[View Solution](Answers-01-10.md#exercise-2-string-immutability-and-the-string-pool)

---

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

### Exercise 4. Java Streams — Data Processing Pipeline

**Concept:** Intermediate vs terminal operations, lazy evaluation, collectors.

**Exercise:** Given a list of `Transaction` objects, write stream pipelines to:
1. Find the top 3 highest-value COMPLETED transactions
2. Group by category and sum amounts
3. Find the first transaction over $10,000 (short-circuit)
4. Build a comma-separated string of all transaction IDs
5. Partition into COMPLETED vs PENDING
6. Calculate running total using `reduce()`

```java
record Transaction(String id, String category, double amount, LocalDate date, String status) {}
```

[View Solution](Answers-01-10.md#exercise-4-java-streams--data-processing-pipeline)

---

### Exercise 5. Concurrency — Thread-Safe Singleton

**Concept:** Double-checked locking, `volatile`, lazy initialization, enum singleton.

**Exercise:** Implement the same singleton three ways:
1. Double-checked locking with `volatile`
2. Static inner class (Bill Pugh pattern)
3. Enum singleton

Write a test that spawns 100 threads and verifies all get the same instance.

[View Solution](Answers-01-10.md#exercise-5-concurrency--thread-safe-singleton)

---

### Exercise 6. Java Pass-by-Value

**Concept:** Java is always pass-by-value (references are passed by value, not by reference).

**Exercise:** Create demonstrations that prove:
1. Primitives are copied — modifying the parameter doesn't affect the caller
2. Object references are copied — reassigning the parameter doesn't affect the caller
3. Object state can be modified through the copied reference

```java
public class PassByValueDemo {
    static void tryToChangeInt(int x) { x = 99; }
    static void tryToReassign(StringBuilder sb) { sb = new StringBuilder("new"); }
    static void mutateObject(StringBuilder sb) { sb.append(" world"); }
}
```

[View Solution](Answers-01-10.md#exercise-6-java-pass-by-value)

---

### Exercise 7. Comparable vs Comparator

**Concept:** Natural ordering vs custom ordering, consistent with equals.

**Exercise:** Create an `Employee` class with `name`, `salary`, `hireDate`. Implement:
1. `Comparable<Employee>` for natural ordering by name
2. A `Comparator` that sorts by salary descending
3. A multi-field `Comparator` using `thenComparing` (salary desc, then name asc)
4. Use all three to sort a list and demonstrate the differences

```java
public class Employee implements Comparable<Employee> {
    // Natural ordering: by name
    // Custom comparators: by salary, by salary then name
}
```

[View Solution](Answers-01-10.md#exercise-7-comparable-vs-comparator)

---

### Exercise 8. equals() and hashCode() with Inheritance

**Concept:** The broken symmetry problem when a subclass adds fields.

**Exercise:** Demonstrate the problem:
1. `Point` with `x`, `y` and correct `equals()`/`hashCode()`
2. `ColorPoint extends Point` with `color` — show how symmetry breaks (`point.equals(colorPoint)` vs `colorPoint.equals(point)`)
3. Fix it using composition instead of inheritance

```java
class Point { int x, y; }
class ColorPoint extends Point { String color; } // Show the problem
class ColorPointFixed { Point point; String color; } // Fix with composition
```

[View Solution](Answers-01-10.md#exercise-8-equals-and-hashcode-with-inheritance)

---

### Exercise 9. Autoboxing and Unboxing Pitfalls

**Concept:** Integer cache (-128 to 127), `==` vs `.equals()` on wrappers, NPE from unboxing null.

**Exercise:** Predict and verify the output of:
```java
Integer a = 127; Integer b = 127;
Integer c = 128; Integer d = 128;
System.out.println(a == b);  // ?
System.out.println(c == d);  // ?

Integer e = null;
int f = e;  // What happens?

List<Integer> list = new ArrayList<>();
list.add(1); list.add(2); list.add(3);
list.remove(1);  // Which overload? remove(int index) or remove(Object)?
```

[View Solution](Answers-01-10.md#exercise-9-autoboxing-and-unboxing-pitfalls)

---

### Exercise 10. Functional Interfaces and Lambdas

**Concept:** `Function`, `Predicate`, `Consumer`, `Supplier`, method references, composition.

**Exercise:** Implement a `ValidationEngine<T>` that:
- Accepts multiple `Predicate<T>` rules with error messages
- Validates an object against all rules
- Returns ALL failures (not just the first)
- Demonstrates predicate composition with `.and()`, `.or()`, `.negate()`

```java
public class ValidationEngine<T> {
    public ValidationEngine<T> addRule(Predicate<T> rule, String errorMessage) { }
    public ValidationResult validate(T object) { }
}
```

[View Solution](Answers-01-10.md#exercise-10-functional-interfaces-and-lambdas)

---

## TIER 2 — Very Common

---

### Exercise 11. Collections — Implement Your Own ArrayList

**Concept:** Dynamic arrays, amortized resizing, generics, bounds checking.

**Exercise:** Implement `MyArrayList<T>` supporting:
- `add(T element)` — amortized O(1) with 1.5x growth
- `get(int index)` — O(1) with bounds check
- `remove(int index)` — O(n) with shift
- `size()`
- Implement `Iterable<T>` with a fail-fast iterator

[View Solution](Answers-11-20.md#exercise-11-collections--implement-your-own-arraylist)

---

### Exercise 12. Implement a LinkedList

**Concept:** Node-based data structures, pointer manipulation, O(1) insert/delete at head.

**Exercise:** Implement `MyLinkedList<T>` (singly linked) with:
- `addFirst(T)`, `addLast(T)`
- `removeFirst()`, `removeLast()`
- `get(int index)` — O(n) traversal
- `reverse()` — in-place reversal
- `size()`

```java
public class MyLinkedList<T> {
    private Node<T> head;
    private int size;

    private static class Node<T> {
        T data;
        Node<T> next;
    }
}
```

[View Solution](Answers-11-20.md#exercise-12-implement-a-linkedlist)

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

### Exercise 14. Exceptions — Custom Exception Hierarchy

**Concept:** Checked vs unchecked, exception chaining, try-with-resources.

**Exercise:** Build a mini banking system with:
- `InsufficientFundsException` (checked — recoverable)
- `AccountLockedException` (unchecked — programming error)
- A `BankAccount` with `withdraw()` that throws appropriately
- A `TransactionProcessor` using try-with-resources and exception chaining

[View Solution](Answers-11-20.md#exercise-14-exceptions--custom-exception-hierarchy)

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

## TIER 3 — Frequently Asked

---

### Exercise 21. Implement an LRU Cache

**Concept:** LinkedHashMap access-order, O(1) get/put, capacity eviction.

**Exercise:** Implement `LRUCache<K, V>` two ways:
1. Using `LinkedHashMap` with `removeEldestEntry()`
2. Using a `HashMap` + custom doubly-linked list (the classic interview version)

Both must be O(1) for `get()` and `put()`.

```java
public class LRUCache<K, V> {
    public LRUCache(int capacity) { }
    public V get(K key) { }
    public void put(K key, V value) { }
}
```

[View Solution](Answers-21-30.md#exercise-21-implement-an-lru-cache)

---

### Exercise 22. Implement a Stack and Queue

**Concept:** LIFO vs FIFO, implementing one from the other.

**Exercise:**
1. Implement `MyStack<T>` using an array (push, pop, peek)
2. Implement `MyQueue<T>` using two stacks (enqueue, dequeue — amortized O(1))
3. Demonstrate: validate balanced parentheses using your stack

```java
public class MyQueue<T> {
    private final Deque<T> inbox = new ArrayDeque<>();
    private final Deque<T> outbox = new ArrayDeque<>();
    public void enqueue(T item) { }
    public T dequeue() { }
}
```

[View Solution](Answers-21-30.md#exercise-22-implement-a-stack-and-queue)

---

### Exercise 23. PriorityQueue and Heap Concepts

**Concept:** Min/max heap, natural ordering vs comparator, use cases.

**Exercise:** Using `PriorityQueue`:
1. Find the K largest elements in a stream of integers (use a min-heap of size K)
2. Merge K sorted lists into one sorted list
3. Implement a simple task scheduler with priority levels

```java
public class TopKFinder {
    private final PriorityQueue<Integer> minHeap;
    private final int k;
    public void add(int value) { }
    public List<Integer> getTopK() { }
}
```

[View Solution](Answers-21-30.md#exercise-23-priorityqueue-and-heap-concepts)

---

### Exercise 24. Iterator and Iterable — Custom Implementation

**Concept:** Iterator protocol, lazy evaluation, fail-fast vs fail-safe.

**Exercise:** Implement a `FilteredIterator<T>` that wraps any iterator and only yields elements matching a predicate. Then implement a `FlatMapIterator<T>` that flattens an iterator of iterators.

```java
public class FilteredIterator<T> implements Iterator<T> {
    public FilteredIterator(Iterator<T> source, Predicate<T> predicate) { }
    public boolean hasNext() { }
    public T next() { }
}
```

[View Solution](Answers-21-30.md#exercise-24-iterator-and-iterable--custom-implementation)

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

### Exercise 27. String Manipulation — Reverse and Palindrome

**Concept:** char array manipulation, two-pointer technique, StringBuilder.

**Exercise:** Implement without using library reverse methods:
1. Reverse a string in-place (using char array)
2. Reverse words in a sentence ("hello world" → "world hello")
3. Check if a string is a palindrome (ignoring case and non-alphanumeric chars)
4. Find the longest palindromic substring

```java
public class StringProblems {
    public String reverse(String s) { }
    public String reverseWords(String s) { }
    public boolean isPalindrome(String s) { }
    public String longestPalindrome(String s) { }
}
```

[View Solution](Answers-21-30.md#exercise-27-string-manipulation--reverse-and-palindrome)

---

### Exercise 28. String Compression and Duplicate Detection

**Concept:** Character counting, StringBuilder efficiency, Set usage.

**Exercise:**
1. Compress: "aabcccccaaa" → "a2b1c5a3"
2. Find first non-repeating character
3. Remove duplicate characters preserving order
4. Check if two strings are rotations of each other ("abcde" / "cdeab")

```java
public class StringAlgorithms {
    public String compress(String s) { }
    public char firstNonRepeating(String s) { }
    public String removeDuplicates(String s) { }
    public boolean isRotation(String s1, String s2) { }
}
```

[View Solution](Answers-21-30.md#exercise-28-string-compression-and-duplicate-detection)

---

### Exercise 29. Abstract Class vs Interface

**Concept:** When to use which, default methods, diamond problem, state vs behavior.

**Exercise:** Design a payment processing system:
1. `PaymentProcessor` (abstract class) — holds common state (transaction ID generator, retry count) and template method
2. `Auditable` (interface with default method) — provides default audit logging
3. `Refundable` (interface) — not all processors support refunds
4. Concrete: `CreditCardProcessor`, `PayPalProcessor`, `CryptoProcessor`

Demonstrate: why the abstract class is needed (shared state), why interfaces are used (capabilities), and how default methods avoid breaking existing implementations.

[View Solution](Answers-21-30.md#exercise-29-abstract-class-vs-interface)

---

### Exercise 30. SOLID — Single Responsibility and Open/Closed

**Concept:** SRP: one reason to change. OCP: open for extension, closed for modification.

**Exercise:** Refactor a monolithic `OrderProcessor` class that:
- Validates orders
- Calculates discounts
- Persists to database
- Sends notification emails

Split into single-responsibility classes. Then make the discount calculation open/closed using a strategy pattern (add new discount types without modifying existing code).

```java
// BEFORE: One class does everything
class OrderProcessor {
    void process(Order order) { /* validate, discount, persist, notify */ }
}

// AFTER: Each class has one responsibility, discount is extensible
```

[View Solution](Answers-21-30.md#exercise-30-solid--single-responsibility-and-openclosed)

---

### Exercise 31. SOLID — Liskov Substitution and Interface Segregation

**Concept:** LSP: subtypes must be substitutable. ISP: no client should depend on methods it doesn't use.

**Exercise:**
1. **LSP violation:** `Rectangle`/`Square` problem — show how `Square extends Rectangle` breaks when calling `setWidth()`/`setHeight()`
2. **ISP violation:** A `Worker` interface with `work()`, `eat()`, `sleep()` — robots can't eat or sleep
3. Fix both with proper abstractions

```java
// LSP fix: separate Shape interface with area(), no setters that imply independent dimensions
// ISP fix: split into Workable, Feedable, Restable
```

[View Solution](Answers-31-40.md#exercise-31-solid--liskov-substitution-and-interface-segregation)

---

### Exercise 32. SOLID — Dependency Inversion

**Concept:** Depend on abstractions, not concretions. High-level modules shouldn't depend on low-level modules.

**Exercise:** Refactor a notification service:
```java
// BEFORE: High-level directly depends on low-level
class NotificationService {
    private final SmtpEmailSender sender = new SmtpEmailSender(); // Tight coupling
    void notify(User user) { sender.send(user.getEmail(), "Hello"); }
}

// AFTER: Depend on abstraction, inject implementation
```

Add: constructor injection, a `MessageSender` interface, and swap between Email/SMS/Push at runtime.

[View Solution](Answers-31-40.md#exercise-32-solid--dependency-inversion)

---

### Exercise 33. Factory and Abstract Factory

**Concept:** Encapsulate object creation, decouple client from concrete classes.

**Exercise:** Implement:
1. **Simple Factory:** `NotificationFactory.create(type)` returns Email/SMS/Push notification
2. **Abstract Factory:** `UIFactory` that creates Button + TextField for different platforms (Web, Mobile, Desktop) — each factory produces a consistent family

```java
interface UIFactory {
    Button createButton();
    TextField createTextField();
}
class WebUIFactory implements UIFactory { }
class MobileUIFactory implements UIFactory { }
```

[View Solution](Answers-31-40.md#exercise-33-factory-and-abstract-factory)

---

### Exercise 34. Proxy Pattern — Lazy Loading and Access Control

**Concept:** Same interface, intercept calls, add cross-cutting concerns.

**Exercise:** Implement:
1. **Virtual Proxy:** A `LazyImage` that only loads the heavy image data from disk when `display()` is first called
2. **Protection Proxy:** A `SecureDocumentService` that checks user roles before delegating to the real service
3. **Dynamic Proxy:** Use `java.lang.reflect.Proxy` to create a logging proxy for any interface

```java
public class LoggingProxyFactory {
    public static <T> T create(T target, Class<T> iface) {
        // Return a dynamic proxy that logs all method calls
    }
}
```

[View Solution](Answers-31-40.md#exercise-34-proxy-pattern--lazy-loading-and-access-control)

---

### Exercise 35. Template Method Pattern

**Concept:** Define algorithm skeleton in base class, let subclasses fill in steps.

**Exercise:** Implement a data processing framework:
1. Abstract `DataProcessor` with template method `process()`: read → transform → validate → write
2. `CsvProcessor` fills in CSV-specific read/write
3. `JsonProcessor` fills in JSON-specific read/write
4. Hook methods: `beforeProcess()`, `afterProcess()` (optional overrides)

```java
abstract class DataProcessor {
    public final void process() { // Template method — final!
        var data = read();
        var transformed = transform(data);
        validate(transformed);
        write(transformed);
    }
    protected abstract List<String> read();
    protected abstract List<String> transform(List<String> data);
    // ...
}
```

[View Solution](Answers-31-40.md#exercise-35-template-method-pattern)

---

### Exercise 36. Chain of Responsibility

**Concept:** Decouple sender from receiver, pass request along a chain until handled.

**Exercise:** Implement a request authentication/authorization pipeline:
1. `AuthenticationHandler` — verify token is valid
2. `RateLimitHandler` — check request rate
3. `AuthorizationHandler` — check user has required role
4. `LoggingHandler` — log the request

Each handler either processes and passes on, or rejects.

```java
abstract class Handler {
    private Handler next;
    public Handler setNext(Handler next) { this.next = next; return next; }
    public abstract boolean handle(Request request);
}
```

[View Solution](Answers-31-40.md#exercise-36-chain-of-responsibility)

---

## TIER 4 — Senior / Architect Level

---

### Exercise 37. Inheritance vs Composition — Design Exercise

**Concept:** Fragile base class problem, favor composition, interface segregation.

**Exercise:** Refactor the broken Bird/Penguin inheritance using:
1. Interface segregation (`Flyable`, `Swimmable`, `Eatable`)
2. Composition (inject behaviors as strategy objects)
3. Demonstrate adding new behavior without modifying existing classes

[View Solution](Answers-31-40.md#exercise-37-inheritance-vs-composition--design-exercise)

---

### Exercise 38. Design Patterns — Strategy, Observer, Builder, Decorator Combined

**Concept:** Apply multiple patterns together in one cohesive system.

**Exercise:** Implement a notification system with:
- **Strategy** — different channels (email, SMS, push)
- **Observer** — subscribers notified on events
- **Builder** — construct complex `Notification` objects
- **Decorator** — add logging, retry, rate-limiting to any channel

```java
NotificationChannel channel = new RetryDecorator(
    new LoggingDecorator(new EmailChannel()), 3
);
channel.send(notification);
```

[View Solution](Answers-31-40.md#exercise-38-design-patterns--strategy-observer-builder-decorator-combined)

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

### Exercise 40. Serialization and Object Cloning

**Concept:** `Serializable`, `transient`, deep copy vs shallow copy, `Cloneable` pitfalls.

**Exercise:** Deep copy a `Department` containing `List<Employee>` three ways:
1. Serialization-based
2. Copy constructor
3. Clone (demonstrate the shallow copy pitfall)

[View Solution](Answers-31-40.md#exercise-40-serialization-and-object-cloning)

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

### Exercise 43. Implement a Binary Search Tree

**Concept:** Recursive data structures, tree traversals, searching.

**Exercise:** Implement `BST<T extends Comparable<T>>` with:
- `insert(T value)`
- `contains(T value)`
- `delete(T value)` (handle all three cases: leaf, one child, two children)
- `inOrder()` — returns sorted list
- `height()`

```java
public class BST<T extends Comparable<T>> {
    private Node<T> root;
    private static class Node<T> { T value; Node<T> left, right; }
}
```

[View Solution](Answers-41-50.md#exercise-43-implement-a-binary-search-tree)

---

### Exercise 44. Records and Sealed Classes (Java 17+)

**Concept:** Immutable data carriers, restricted type hierarchies, exhaustive pattern matching.

**Exercise:** Model an expression evaluator using sealed classes:
```java
sealed interface Expr permits Num, Add, Mul, Neg {}
record Num(double value) implements Expr {}
record Add(Expr left, Expr right) implements Expr {}
record Mul(Expr left, Expr right) implements Expr {}
record Neg(Expr expr) implements Expr {}
```

Implement `evaluate(Expr expr)` using pattern matching (`switch` with sealed type exhaustiveness).

[View Solution](Answers-41-50.md#exercise-44-records-and-sealed-classes-java-17)

---

### Exercise 45. Pattern Matching and Switch Expressions (Java 21+)

**Concept:** `instanceof` pattern matching, switch expressions, guarded patterns.

**Exercise:** Implement a `format(Object obj)` method that uses pattern matching switch:
```java
public String format(Object obj) {
    return switch (obj) {
        case Integer i when i < 0 -> "negative: " + i;
        case Integer i -> "int: " + i;
        case String s when s.isBlank() -> "blank string";
        case String s -> "string: " + s;
        case List<?> list when list.isEmpty() -> "empty list";
        case List<?> list -> "list of " + list.size();
        case null -> "null";
        default -> "unknown: " + obj.getClass().getSimpleName();
    };
}
```

Demonstrate record patterns: `case Point(int x, int y) when x == y -> "diagonal"`

[View Solution](Answers-41-50.md#exercise-45-pattern-matching-and-switch-expressions-java-21)

---

### Exercise 46. Optional Best Practices

**Concept:** Avoiding null, monadic operations, when NOT to use Optional.

**Exercise:** Refactor a null-heavy service:
```java
// BEFORE: Null checks everywhere
User user = userRepo.findById(id);
if (user != null) {
    Address address = user.getAddress();
    if (address != null) {
        String city = address.getCity();
        if (city != null) { return city.toUpperCase(); }
    }
}
return "UNKNOWN";
```

Rewrite using `Optional` chaining: `map`, `flatMap`, `orElse`, `orElseGet`, `orElseThrow`. Then list the anti-patterns: `Optional.get()` without check, Optional as method parameter, Optional for collection fields.

[View Solution](Answers-41-50.md#exercise-46-optional-best-practices)

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

### Exercise 48. TreeMap and NavigableMap Operations

**Concept:** Red-black tree, sorted keys, range queries, floor/ceiling.

**Exercise:** Implement a simple time-based key-value store using `TreeMap`:
- `put(timestamp, value)` — store value at timestamp
- `get(timestamp)` — return exact match or most recent value before timestamp (`floorEntry`)
- `getRange(from, to)` — return all entries in time range (`subMap`)
- `getLatest()` — return most recent entry (`lastEntry`)

```java
public class TimeSeriesStore<V> {
    private final TreeMap<Long, V> store = new TreeMap<>();
    public void put(long timestamp, V value) { }
    public V get(long timestamp) { }
    public List<V> getRange(long from, long to) { }
}
```

[View Solution](Answers-41-50.md#exercise-48-treemap-and-navigablemap-operations)

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
