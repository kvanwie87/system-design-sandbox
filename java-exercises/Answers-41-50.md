# Answers — Exercises 41–50

Solutions for exercises 41–50 in [Java-Interview-Exercises.md](Java-Interview-Exercises.md).

---

## Exercise 41. Reflection and Custom DI Container

```java
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface MyComponent {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface MyInject {}

// Sample components
@MyComponent
class UserRepository {
    public String findUser(String id) { return "User-" + id; }
}

@MyComponent
class NotificationClient {
    public void notify(String userId, String msg) {
        System.out.println("Notifying " + userId + ": " + msg);
    }
}

@MyComponent
class UserService {
    @MyInject
    private UserRepository repository;

    @MyInject
    private NotificationClient notifications;

    public String getUser(String id) {
        String user = repository.findUser(id);
        notifications.notify(id, "Profile accessed");
        return user;
    }
}

class DIContainer {
    private final Map<Class<?>, Object> instances = new HashMap<>();

    public void register(Class<?>... classes) throws Exception {
        // First pass: instantiate all @MyComponent classes
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(MyComponent.class)) {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                instances.put(clazz, instance);
            }
        }
        // Second pass: inject @MyInject fields
        for (Object instance : instances.values()) {
            for (Field field : instance.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(MyInject.class)) {
                    Object dependency = instances.get(field.getType());
                    if (dependency == null) {
                        throw new RuntimeException("No component registered for: " + field.getType().getName());
                    }
                    field.setAccessible(true); // Bypass private
                    field.set(instance, dependency);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        return (T) instances.get(clazz);
    }
}

public class DIDemo {
    public static void main(String[] args) throws Exception {
        DIContainer container = new DIContainer();
        container.register(UserRepository.class, NotificationClient.class, UserService.class);

        UserService service = container.get(UserService.class);
        System.out.println(service.getUser("42"));
        // Output:
        // Notifying 42: Profile accessed
        // User-42
    }
}
```

**Key points:**
- `@Retention(RUNTIME)` is required for reflection-based discovery (vs SOURCE/CLASS)
- `field.setAccessible(true)` bypasses private — this is how Spring injects private fields
- Two-pass registration: instantiate first, then wire — handles any dependency order (non-circular)
- Real DI containers add: scopes (singleton/prototype), proxies, lazy init, circular detection, interface→impl mapping
- Reflection is slow at first access — frameworks cache metadata at startup

---

## Exercise 42. Class Loading and Initialization Order

```java
class Parent {
    static { System.out.println("1. Parent static block"); }
    { System.out.println("4. Parent instance block"); }

    Parent() {
        System.out.println("5. Parent constructor");
    }
}

class Child extends Parent {
    static { System.out.println("2. Child static block"); }
    { System.out.println("6. Child instance block"); }

    Child() {
        System.out.println("7. Child constructor");
    }
}

public class ClassLoadingDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== First new Child() ===");
        new Child();
        // Output:
        // 1. Parent static block
        // 2. Child static block
        // 4. Parent instance block
        // 5. Parent constructor
        // 6. Child instance block
        // 7. Child constructor

        System.out.println("\n=== Second new Child() ===");
        new Child();
        // Output (static blocks DON'T repeat):
        // 4. Parent instance block
        // 5. Parent constructor
        // 6. Child instance block
        // 7. Child constructor

        System.out.println("\n=== Class.forName vs ClassLoader ===");

        // Class.forName triggers static initialization
        System.out.println("Before Class.forName:");
        Class.forName("LazyClass");
        // Prints "LazyClass static init"

        // ClassLoader.loadClass does NOT trigger static init
        System.out.println("\nBefore ClassLoader.loadClass:");
        ClassLoader.getSystemClassLoader().loadClass("LazyClass2");
        // Does NOT print anything — class loaded but not initialized
    }
}

class LazyClass {
    static { System.out.println("LazyClass static init"); }
}

class LazyClass2 {
    static { System.out.println("LazyClass2 static init"); }
}
```

**Key points — initialization order:**
1. Parent static blocks/fields (once, in declaration order)
2. Child static blocks/fields (once, in declaration order)
3. Parent instance blocks (each time)
4. Parent constructor
5. Child instance blocks (each time)
6. Child constructor

**Key points — class loading:**
- Static blocks run exactly once when the class is first **initialized** (not just loaded)
- `Class.forName("X")` → loads AND initializes (triggers static blocks)
- `ClassLoader.loadClass("X")` → loads only, defers initialization until first use
- This is why the Bill Pugh singleton works: the inner Holder class isn't initialized until `getInstance()` is called

---

## Exercise 43. Implement a Binary Search Tree

```java
import java.util.*;

public class BST<T extends Comparable<T>> {
    private Node<T> root;

    private static class Node<T> {
        T value;
        Node<T> left, right;
        Node(T value) { this.value = value; }
    }

    public void insert(T value) {
        root = insertRec(root, value);
    }

    private Node<T> insertRec(Node<T> node, T value) {
        if (node == null) return new Node<>(value);
        int cmp = value.compareTo(node.value);
        if (cmp < 0) node.left = insertRec(node.left, value);
        else if (cmp > 0) node.right = insertRec(node.right, value);
        // cmp == 0: duplicate, ignore
        return node;
    }

    public boolean contains(T value) {
        return containsRec(root, value);
    }

    private boolean containsRec(Node<T> node, T value) {
        if (node == null) return false;
        int cmp = value.compareTo(node.value);
        if (cmp < 0) return containsRec(node.left, value);
        if (cmp > 0) return containsRec(node.right, value);
        return true;
    }

    public void delete(T value) {
        root = deleteRec(root, value);
    }

    private Node<T> deleteRec(Node<T> node, T value) {
        if (node == null) return null;
        int cmp = value.compareTo(node.value);
        if (cmp < 0) { node.left = deleteRec(node.left, value); }
        else if (cmp > 0) { node.right = deleteRec(node.right, value); }
        else {
            // Case 1: Leaf node
            if (node.left == null && node.right == null) return null;
            // Case 2: One child
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            // Case 3: Two children — replace with in-order successor
            Node<T> successor = findMin(node.right);
            node.value = successor.value;
            node.right = deleteRec(node.right, successor.value);
        }
        return node;
    }

    private Node<T> findMin(Node<T> node) {
        while (node.left != null) node = node.left;
        return node;
    }

    public List<T> inOrder() {
        List<T> result = new ArrayList<>();
        inOrderRec(root, result);
        return result;
    }

    private void inOrderRec(Node<T> node, List<T> result) {
        if (node == null) return;
        inOrderRec(node.left, result);
        result.add(node.value);
        inOrderRec(node.right, result);
    }

    public int height() {
        return heightRec(root);
    }

    private int heightRec(Node<T> node) {
        if (node == null) return 0;
        return 1 + Math.max(heightRec(node.left), heightRec(node.right));
    }

    public static void main(String[] args) {
        BST<Integer> bst = new BST<>();
        for (int v : new int[]{8, 3, 10, 1, 6, 14, 4, 7, 13}) {
            bst.insert(v);
        }

        System.out.println("In-order: " + bst.inOrder()); // [1, 3, 4, 6, 7, 8, 10, 13, 14]
        System.out.println("Contains 6: " + bst.contains(6)); // true
        System.out.println("Contains 5: " + bst.contains(5)); // false
        System.out.println("Height: " + bst.height()); // 4

        bst.delete(3); // Delete node with two children
        System.out.println("After delete 3: " + bst.inOrder()); // [1, 4, 6, 7, 8, 10, 13, 14]

        bst.delete(14); // Delete node with one child
        System.out.println("After delete 14: " + bst.inOrder()); // [1, 4, 6, 7, 8, 10, 13]
    }
}
```

**Key points:**
- In-order traversal of a BST always produces sorted output
- Delete has 3 cases: leaf (remove), one child (replace with child), two children (replace with in-order successor then delete successor)
- Unbalanced BST degrades to O(n) — that's why TreeMap uses Red-Black trees (self-balancing, O(log n) guaranteed)
- Height of balanced BST: O(log n). Worst case (sorted input): O(n).

---

## Exercise 44. Records and Sealed Classes (Java 17+)

```java
// Sealed interface: restricts which classes can implement it
sealed interface Expr permits Num, Add, Mul, Neg {}

record Num(double value) implements Expr {}
record Add(Expr left, Expr right) implements Expr {}
record Mul(Expr left, Expr right) implements Expr {}
record Neg(Expr expr) implements Expr {}

public class ExpressionEvaluator {

    // Pattern matching switch — exhaustive over sealed type (no default needed)
    public static double evaluate(Expr expr) {
        return switch (expr) {
            case Num n -> n.value();
            case Add a -> evaluate(a.left()) + evaluate(a.right());
            case Mul m -> evaluate(m.left()) * evaluate(m.right());
            case Neg n -> -evaluate(n.expr());
        };
    }

    // Pretty printer using same pattern matching
    public static String prettyPrint(Expr expr) {
        return switch (expr) {
            case Num n -> String.valueOf(n.value());
            case Add a -> "(" + prettyPrint(a.left()) + " + " + prettyPrint(a.right()) + ")";
            case Mul m -> "(" + prettyPrint(m.left()) + " * " + prettyPrint(m.right()) + ")";
            case Neg n -> "-(" + prettyPrint(n.expr()) + ")";
        };
    }

    public static void main(String[] args) {
        // Expression: -(3 + 4) * 2
        Expr expr = new Mul(
            new Neg(new Add(new Num(3), new Num(4))),
            new Num(2)
        );

        System.out.println("Expression: " + prettyPrint(expr)); // -((3.0 + 4.0)) * 2.0)
        System.out.println("Result: " + evaluate(expr));         // -14.0

        // Simple: 5 + 3
        Expr simple = new Add(new Num(5), new Num(3));
        System.out.println(prettyPrint(simple) + " = " + evaluate(simple)); // (5.0 + 3.0) = 8.0
    }
}
```

**Key points:**
- **Records:** Immutable data carriers. Auto-generate: constructor, getters, equals(), hashCode(), toString().
- **Sealed classes:** Restrict type hierarchy. Compiler knows all subtypes → enables exhaustive switch without `default`.
- If you add a new `permits` class (e.g., `Div`), all switch statements break at compile time — forces you to handle it.
- This pattern (sealed + records + pattern matching) is Java's answer to algebraic data types (ADTs) from functional languages.
- Records cannot extend other classes (implicitly extend `Record`), but can implement interfaces.

---

## Exercise 45. Pattern Matching and Switch Expressions (Java 21+)

```java
import java.util.List;

record Point(int x, int y) {}

public class PatternMatchingDemo {

    // Pattern matching switch with guards
    public static String format(Object obj) {
        return switch (obj) {
            case null -> "null";
            case Integer i when i < 0 -> "negative int: " + i;
            case Integer i -> "int: " + i;
            case String s when s.isBlank() -> "blank string";
            case String s -> "string: \"" + s + "\"";
            case List<?> list when list.isEmpty() -> "empty list";
            case List<?> list -> "list of " + list.size() + " elements";
            case Point(int x, int y) when x == y -> "diagonal point (" + x + "," + y + ")";
            case Point(int x, int y) -> "point (" + x + "," + y + ")";
            case double[] arr -> "double array of length " + arr.length;
            default -> "unknown: " + obj.getClass().getSimpleName();
        };
    }

    // Record patterns for destructuring
    public static double distanceFromOrigin(Object obj) {
        return switch (obj) {
            case Point(int x, int y) -> Math.sqrt(x * x + y * y);
            default -> throw new IllegalArgumentException("Not a point");
        };
    }

    // instanceof pattern matching (Java 16+)
    public static void processOld(Object obj) {
        // Old way:
        // if (obj instanceof String) { String s = (String) obj; ... }

        // New way: binding variable in instanceof
        if (obj instanceof String s && s.length() > 5) {
            System.out.println("Long string: " + s.toUpperCase());
        } else if (obj instanceof Integer i && i > 0) {
            System.out.println("Positive int: " + i);
        }
    }

    public static void main(String[] args) {
        System.out.println(format(null));                  // null
        System.out.println(format(-5));                    // negative int: -5
        System.out.println(format(42));                    // int: 42
        System.out.println(format(""));                    // blank string
        System.out.println(format("hello"));              // string: "hello"
        System.out.println(format(List.of()));            // empty list
        System.out.println(format(List.of(1, 2, 3)));    // list of 3 elements
        System.out.println(format(new Point(3, 3)));     // diagonal point (3,3)
        System.out.println(format(new Point(1, 5)));     // point (1,5)
        System.out.println(format(new double[]{1.0}));   // double array of length 1

        System.out.println("\nDistance: " + distanceFromOrigin(new Point(3, 4))); // 5.0

        processOld("Hello World"); // Long string: HELLO WORLD
        processOld(7);             // Positive int: 7
    }
}
```

**Key points:**
- **Guarded patterns (`when`):** Adds conditions after the type check. Order matters — more specific patterns first.
- **Record patterns:** Destructure records directly in `case`. `case Point(int x, int y)` extracts fields.
- **instanceof pattern:** Combines type check + cast + variable binding in one expression.
- **null case:** Explicitly handled in switch (Java 21+) — previously NPE'd.
- Exhaustive switch: with sealed types, compiler verifies all cases covered. With `Object`, you need `default`.

---

## Exercise 46. Optional Best Practices

```java
import java.util.*;

record Address(String city) {}
record User(String name, Address address) {}

interface UserRepository {
    Optional<User> findById(String id);
}

public class OptionalDemo {

    // --- BEFORE: null checks everywhere ---
    static String getCityOldWay(UserRepository repo, String id) {
        User user = null; // repo.findByIdNullable(id);
        if (user != null) {
            Address address = user.address();
            if (address != null) {
                String city = address.city();
                if (city != null) {
                    return city.toUpperCase();
                }
            }
        }
        return "UNKNOWN";
    }

    // --- AFTER: Optional chaining ---
    static String getCityOptional(UserRepository repo, String id) {
        return repo.findById(id)
            .map(User::address)           // Optional<Address>
            .map(Address::city)           // Optional<String>
            .map(String::toUpperCase)     // Optional<String>
            .orElse("UNKNOWN");
    }

    // flatMap: when the mapper itself returns Optional
    static Optional<String> findUserCity(UserRepository repo, String id) {
        return repo.findById(id)
            .flatMap(user -> Optional.ofNullable(user.address()))
            .flatMap(addr -> Optional.ofNullable(addr.city()));
    }

    // orElseGet vs orElse
    static String demonstrateOrElse(UserRepository repo, String id) {
        // orElse: default is always evaluated (even if Optional has value)
        // orElseGet: default is only computed when Optional is empty (lazy)
        return repo.findById(id)
            .map(User::name)
            .orElseGet(() -> expensiveComputation()); // Lazy — only called if empty
    }

    // orElseThrow: when absence is an error
    static User getOrThrow(UserRepository repo, String id) {
        return repo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    private static String expensiveComputation() {
        System.out.println("Computing fallback...");
        return "FALLBACK";
    }

    public static void main(String[] args) {
        // Mock repository
        UserRepository repo = id -> switch (id) {
            case "1" -> Optional.of(new User("Alice", new Address("Seattle")));
            case "2" -> Optional.of(new User("Bob", null)); // No address
            default -> Optional.empty();
        };

        System.out.println(getCityOptional(repo, "1")); // SEATTLE
        System.out.println(getCityOptional(repo, "2")); // UNKNOWN (no address)
        System.out.println(getCityOptional(repo, "9")); // UNKNOWN (no user)

        System.out.println(findUserCity(repo, "1")); // Optional[Seattle]
        System.out.println(findUserCity(repo, "2")); // Optional.empty

        System.out.println(getOrThrow(repo, "1").name()); // Alice
        try {
            getOrThrow(repo, "9");
        } catch (NoSuchElementException e) {
            System.out.println(e.getMessage()); // User not found: 9
        }
    }
}

// --- ANTI-PATTERNS (don't do these) ---
/*
 * 1. Optional.get() without isPresent() check — throws NoSuchElementException
 * 2. Optional as method parameter — use overloading or @Nullable instead
 * 3. Optional for collection fields — use empty collection instead
 * 4. Optional.of(null) — throws NPE. Use Optional.ofNullable() for possibly-null values
 * 5. if (opt.isPresent()) { opt.get() } — defeats the purpose. Use map/flatMap/orElse
 */
```

**Key points:**
- `map`: transform value if present, unwrap if null → stays Optional
- `flatMap`: use when mapper returns Optional (avoids `Optional<Optional<T>>`)
- `orElse` vs `orElseGet`: orElse always evaluates the default; orElseGet is lazy
- `orElseThrow`: preferred over `.get()` — intention is clear, exception is explicit
- Optional is for **return types** — not for fields, parameters, or collections

---

## Exercise 47. Virtual Threads (Java 21+)

```java
import java.time.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class VirtualThreadDemo {

    // Simulates a blocking I/O call (HTTP request, DB query, etc.)
    static String blockingCall(int taskId) {
        try { Thread.sleep(100); } // 100ms blocking I/O
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return "Result-" + taskId;
    }

    // Platform threads: limited by pool size
    static void platformThreads(int taskCount) throws Exception {
        long start = System.currentTimeMillis();
        // Default pool: limited threads (e.g., 200)
        try (ExecutorService exec = Executors.newFixedThreadPool(200)) {
            List<Future<String>> futures = IntStream.range(0, taskCount)
                .mapToObj(i -> exec.submit(() -> blockingCall(i)))
                .toList();

            for (Future<String> f : futures) f.get();
        }
        System.out.println("Platform threads (" + taskCount + " tasks): " +
            (System.currentTimeMillis() - start) + "ms");
    }

    // Virtual threads: scales to millions
    static void virtualThreads(int taskCount) throws Exception {
        long start = System.currentTimeMillis();
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = IntStream.range(0, taskCount)
                .mapToObj(i -> exec.submit(() -> blockingCall(i)))
                .toList();

            for (Future<String> f : futures) f.get();
        }
        System.out.println("Virtual threads (" + taskCount + " tasks): " +
            (System.currentTimeMillis() - start) + "ms");
    }

    // Direct creation
    static void directVirtualThread() throws Exception {
        Thread vThread = Thread.ofVirtual()
            .name("my-virtual-thread")
            .start(() -> {
                System.out.println("Running on: " + Thread.currentThread());
                System.out.println("Is virtual: " + Thread.currentThread().isVirtual());
            });
        vThread.join();
    }

    public static void main(String[] args) throws Exception {
        int tasks = 10_000;

        directVirtualThread();
        System.out.println();

        // Platform: 10k tasks with 200 threads → 10000/200 * 100ms = ~5000ms
        platformThreads(tasks);

        // Virtual: 10k tasks all concurrent → ~100ms (all run simultaneously)
        virtualThreads(tasks);
    }
}
```

**Key points:**
- Virtual threads are lightweight (few KB stack vs ~1MB for platform threads). You can create millions.
- They're ideal for I/O-bound workloads (HTTP calls, DB queries) — NOT for CPU-bound work.
- When a virtual thread blocks (sleep, I/O, lock), it unmounts from the carrier thread, freeing it for other virtual threads.
- `Executors.newVirtualThreadPerTaskExecutor()` — one virtual thread per task, no pool sizing needed.
- **Don't pool virtual threads** — they're cheap to create. Pooling adds overhead for no benefit.
- ThreadLocal works but is discouraged (millions of threads × per-thread storage = memory issues). Use ScopedValues instead (Java 21 preview).

---

## Exercise 48. TreeMap and NavigableMap Operations

```java
import java.util.*;

public class TimeSeriesStore<V> {
    private final TreeMap<Long, V> store = new TreeMap<>();

    public void put(long timestamp, V value) {
        store.put(timestamp, value);
    }

    // Get exact match or most recent value before timestamp
    public V get(long timestamp) {
        Map.Entry<Long, V> entry = store.floorEntry(timestamp);
        return entry != null ? entry.getValue() : null;
    }

    // Get all entries in time range [from, to]
    public List<V> getRange(long from, long to) {
        return new ArrayList<>(store.subMap(from, true, to, true).values());
    }

    // Most recent entry
    public V getLatest() {
        Map.Entry<Long, V> entry = store.lastEntry();
        return entry != null ? entry.getValue() : null;
    }

    // Oldest entry
    public V getOldest() {
        Map.Entry<Long, V> entry = store.firstEntry();
        return entry != null ? entry.getValue() : null;
    }

    // Get the entry after the given timestamp
    public V getAfter(long timestamp) {
        Map.Entry<Long, V> entry = store.higherEntry(timestamp);
        return entry != null ? entry.getValue() : null;
    }

    public int size() { return store.size(); }

    public static void main(String[] args) {
        TimeSeriesStore<String> ts = new TimeSeriesStore<>();
        ts.put(1000L, "Start");
        ts.put(2000L, "Processing");
        ts.put(3000L, "Halfway");
        ts.put(4000L, "Almost done");
        ts.put(5000L, "Complete");

        // Exact match
        System.out.println(ts.get(3000L)); // "Halfway"

        // Floor: no exact match at 3500, returns most recent before
        System.out.println(ts.get(3500L)); // "Halfway"

        // Range query
        System.out.println(ts.getRange(2000L, 4000L)); // [Processing, Halfway, Almost done]

        // Navigation
        System.out.println(ts.getLatest());     // "Complete"
        System.out.println(ts.getOldest());     // "Start"
        System.out.println(ts.getAfter(3000L)); // "Almost done"

        // TreeMap NavigableMap methods summary:
        // floorEntry(key)   — greatest entry ≤ key
        // ceilingEntry(key) — smallest entry ≥ key
        // lowerEntry(key)   — greatest entry < key
        // higherEntry(key)  — smallest entry > key
        // subMap(from, to)  — view of entries in range
        // headMap(key)      — entries < key
        // tailMap(key)      — entries ≥ key
    }
}
```

**Key points:**
- TreeMap maintains keys in sorted order (Red-Black tree) — O(log n) for all operations
- NavigableMap methods (`floor`, `ceiling`, `lower`, `higher`) enable range-based lookups
- `subMap` returns a **view** — changes to the view affect the original map (and vice versa)
- Use case: time-series data, interval lookups, "find nearest value", ordered indexes
- TreeMap vs HashMap: TreeMap is slower for point lookups (O(log n) vs O(1)) but supports ordered operations

---

## Exercise 49. Memory Visibility — Double-Checked Locking Deep Dive

```java
public class DCLDeepDive {

    // --- VERSION 1: BROKEN (no volatile) ---
    // Before Java 5, this could expose a partially constructed object
    static class BrokenSingleton {
        private static BrokenSingleton instance; // NO volatile!

        public static BrokenSingleton getInstance() {
            if (instance == null) {                    // Thread A reads null
                synchronized (BrokenSingleton.class) {
                    if (instance == null) {
                        instance = new BrokenSingleton();
                        // JVM can reorder:
                        //   1. Allocate memory
                        //   2. Assign reference to 'instance' ← Thread B sees non-null here!
                        //   3. Call constructor           ← But object isn't fully constructed
                        // Thread B sees non-null instance, skips sync block, uses half-initialized object
                    }
                }
            }
            return instance;
        }
    }

    // --- VERSION 2: FIXED (with volatile) ---
    // volatile establishes happens-before: all writes before the volatile store
    // are visible to any thread that reads the volatile field
    static class FixedSingleton {
        private static volatile FixedSingleton instance; // volatile!

        public static FixedSingleton getInstance() {
            FixedSingleton local = instance;  // Single volatile read (optimization)
            if (local == null) {              // First check: no lock
                synchronized (FixedSingleton.class) {
                    local = instance;         // Second volatile read inside lock
                    if (local == null) {      // Second check: with lock
                        instance = local = new FixedSingleton();
                        // volatile write guarantees:
                        // 1. Constructor completes (all fields initialized)
                        // 2. THEN reference is published (made visible to other threads)
                        // No reordering past the volatile store
                    }
                }
            }
            return local;
        }
    }

    // --- VERSION 3: HOLDER IDIOM (best — no volatile, no synchronized on fast path) ---
    // JLS guarantees: class initialization is thread-safe and happens-before first use
    static class HolderSingleton {
        private HolderSingleton() {}

        private static class Holder {
            // Initialized exactly once, when Holder class is first accessed
            // Class initialization has a happens-before guarantee built into the JLS
            static final HolderSingleton INSTANCE = new HolderSingleton();
        }

        public static HolderSingleton getInstance() {
            return Holder.INSTANCE;
            // First call: JVM initializes Holder class (thread-safe by JLS §12.4.2)
            // Subsequent calls: just a field read — no synchronization, no volatile
        }
    }

    /*
     * JMM Happens-Before Edges:
     *
     * 1. Program order: each action in a thread happens-before the next action in that thread
     * 2. Monitor lock: unlock happens-before subsequent lock of same monitor
     * 3. Volatile: write to volatile field happens-before subsequent read of same field
     * 4. Thread start: Thread.start() happens-before any action in the started thread
     * 5. Thread join: all actions in a thread happen-before join() returns
     * 6. Class initialization: initialization of a class happens-before first use
     *
     * DCL exploits #3 (volatile version) or #6 (holder version).
     */

    public static void main(String[] args) {
        // All three approaches produce a singleton — but with different safety guarantees
        System.out.println(FixedSingleton.getInstance() == FixedSingleton.getInstance()); // true
        System.out.println(HolderSingleton.getInstance() == HolderSingleton.getInstance()); // true
    }
}
```

**Key points:**
- **Without volatile:** JIT can reorder object allocation and reference assignment. Another thread sees non-null reference to uninitialized object.
- **With volatile:** The volatile write creates a happens-before edge. All constructor writes are visible before the reference is published.
- **Holder idiom:** No volatile, no synchronized on the fast path. Relies on JLS class initialization guarantee — the JVM handles synchronization internally.
- The local variable optimization (`FixedSingleton local = instance`) reduces volatile reads from 2 to 1 in the fast path (minor perf gain).
- **Best practice:** Use the Holder idiom for lazy singletons. Use enum for eager singletons.

---

## Exercise 50. Putting It All Together — Mini In-Memory Database

```java
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

// Generic table with CRUD, indexes, queries, and read-write locking
public class Table<T> {
    private final List<T> records = new ArrayList<>();
    private final Map<String, Map<Object, List<T>>> indexes = new HashMap<>();
    private final Map<String, Function<T, ?>> indexExtractors = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Create index for faster lookups
    public void createIndex(String name, Function<T, ?> keyExtractor) {
        lock.writeLock().lock();
        try {
            indexExtractors.put(name, keyExtractor);
            Map<Object, List<T>> idx = new HashMap<>();
            for (T record : records) {
                Object key = keyExtractor.apply(record);
                idx.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
            indexes.put(name, idx);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Insert
    public void insert(T record) {
        lock.writeLock().lock();
        try {
            records.add(record);
            // Update all indexes
            for (var entry : indexExtractors.entrySet()) {
                Object key = entry.getValue().apply(record);
                indexes.get(entry.getKey()).computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Query by index (O(1) lookup)
    public List<T> findByIndex(String indexName, Object value) {
        lock.readLock().lock();
        try {
            Map<Object, List<T>> idx = indexes.get(indexName);
            if (idx == null) throw new IllegalArgumentException("No index: " + indexName);
            return new ArrayList<>(idx.getOrDefault(value, List.of()));
        } finally {
            lock.readLock().unlock();
        }
    }

    // Fluent query builder
    public Query<T> where(Predicate<T> filter) {
        return new Query<>(this, filter);
    }

    // Get all records (snapshot)
    List<T> getAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(records);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try { return records.size(); }
        finally { lock.readLock().unlock(); }
    }
}

// Fluent query API
class Query<T> {
    private final Table<T> table;
    private Predicate<T> filter;
    private Comparator<T> sorter;
    private int limitCount = Integer.MAX_VALUE;

    Query(Table<T> table, Predicate<T> filter) {
        this.table = table;
        this.filter = filter;
    }

    public Query<T> and(Predicate<T> additional) {
        this.filter = this.filter.and(additional);
        return this;
    }

    public Query<T> orderBy(Comparator<T> comparator) {
        this.sorter = comparator;
        return this;
    }

    public <U extends Comparable<U>> Query<T> orderBy(Function<T, U> keyExtractor) {
        this.sorter = Comparator.comparing(keyExtractor);
        return this;
    }

    public Query<T> limit(int count) {
        this.limitCount = count;
        return this;
    }

    public List<T> execute() {
        Stream<T> stream = table.getAll().stream().filter(filter);
        if (sorter != null) stream = stream.sorted(sorter);
        return stream.limit(limitCount).toList();
    }

    public Optional<T> findFirst() {
        return table.getAll().stream().filter(filter).findFirst();
    }

    public long count() {
        return table.getAll().stream().filter(filter).count();
    }
}

// Mini database
class MiniDB {
    private final Map<String, Table<?>> tables = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Table<T> createTable(String name) {
        Table<T> table = new Table<>();
        tables.put(name, table);
        return table;
    }

    @SuppressWarnings("unchecked")
    public <T> Table<T> getTable(String name) {
        return (Table<T>) tables.get(name);
    }
}

// Demo
record UserRecord(String id, String name, int age, String city) {}

class MiniDBDemo {
    public static void main(String[] args) {
        MiniDB db = new MiniDB();
        Table<UserRecord> users = db.createTable("users");

        // Create indexes
        users.createIndex("city", UserRecord::city);
        users.createIndex("age", UserRecord::age);

        // Insert data
        users.insert(new UserRecord("1", "Alice", 30, "Seattle"));
        users.insert(new UserRecord("2", "Bob", 25, "Portland"));
        users.insert(new UserRecord("3", "Charlie", 35, "Seattle"));
        users.insert(new UserRecord("4", "Diana", 28, "Portland"));
        users.insert(new UserRecord("5", "Eve", 32, "Seattle"));

        // Index lookup — O(1)
        System.out.println("=== Seattle residents (index) ===");
        users.findByIndex("city", "Seattle").forEach(System.out::println);

        // Fluent query — filter + sort + limit
        System.out.println("\n=== Age > 27, ordered by name, limit 3 ===");
        List<UserRecord> results = users
            .where(u -> u.age() > 27)
            .orderBy(UserRecord::name)
            .limit(3)
            .execute();
        results.forEach(System.out::println);

        // Compound query
        System.out.println("\n=== Seattle AND age > 30 ===");
        users.where(u -> u.city().equals("Seattle"))
             .and(u -> u.age() > 30)
             .execute()
             .forEach(System.out::println);

        // Count
        long count = users.where(u -> u.age() >= 30).count();
        System.out.println("\nUsers aged 30+: " + count);

        // Concurrent safety: multiple readers, exclusive writer
        System.out.println("\nTable size: " + users.size());
    }
}
```

**Key points:**
- **ReadWriteLock:** Multiple concurrent readers, exclusive writer. Better throughput than synchronized for read-heavy workloads.
- **Secondary indexes:** HashMap-based, O(1) lookup. Maintained on insert (trade write speed for read speed).
- **Fluent query API:** Method chaining with `Predicate` composition — same pattern as JPA Criteria API or stream pipelines.
- **Generics:** `Table<T>` works with any record type without code changes.
- **Streams:** Used internally for filtering/sorting — but exposed as a domain-specific query interface.
- This exercise ties together: collections, concurrency, generics, streams, builder pattern, and functional interfaces.
