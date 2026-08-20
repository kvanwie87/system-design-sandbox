# Answers — Exercises 11–20

Solutions for exercises 11–20 in [Java-Interview-Exercises.md](Java-Interview-Exercises.md).

---

## Exercise 11. Collections — Implement Your Own ArrayList

```java
import java.util.*;

public class MyArrayList<T> implements Iterable<T> {
    private Object[] data;
    private int size;
    private int modCount;

    public MyArrayList() {
        this.data = new Object[10];
        this.size = 0;
        this.modCount = 0;
    }

    public void add(T element) {
        if (size == data.length) grow();
        data[size++] = element;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkBounds(index);
        return (T) data[index];
    }

    @SuppressWarnings("unchecked")
    public T remove(int index) {
        checkBounds(index);
        T removed = (T) data[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(data, index + 1, data, index, numMoved);
        }
        data[--size] = null; // Prevent memory leak
        modCount++;
        return removed;
    }

    public int size() { return size; }

    private void grow() {
        int newCapacity = data.length + (data.length >> 1); // 1.5x
        Object[] newData = new Object[newCapacity];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
    }

    private void checkBounds(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int cursor = 0;
            private final int expectedModCount = modCount;

            @Override
            public boolean hasNext() { return cursor < size; }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (cursor >= size) throw new NoSuchElementException();
                return (T) data[cursor++];
            }
        };
    }

    public static void main(String[] args) {
        MyArrayList<String> list = new MyArrayList<>();
        list.add("A"); list.add("B"); list.add("C");
        System.out.println(list.get(1)); // B
        list.remove(1);
        System.out.println(list.get(1)); // C
        System.out.println(list.size()); // 2

        // Fail-fast demo
        try {
            for (String s : list) { list.add("D"); }
        } catch (ConcurrentModificationException e) {
            System.out.println("ConcurrentModificationException caught!");
        }
    }
}
```

**Key points:**
- 1.5x growth (same as real ArrayList) balances memory vs resize frequency
- Null out removed slots to allow GC
- Fail-fast iterator uses modCount snapshot
- Amortized O(1) for add: most adds are O(1), occasional resize is O(n)

---

## Exercise 12. Implement a LinkedList

```java
public class MyLinkedList<T> {
    private Node<T> head;
    private int size;

    private static class Node<T> {
        T data;
        Node<T> next;
        Node(T data) { this.data = data; }
    }

    public void addFirst(T element) {
        Node<T> newNode = new Node<>(element);
        newNode.next = head;
        head = newNode;
        size++;
    }

    public void addLast(T element) {
        Node<T> newNode = new Node<>(element);
        if (head == null) { head = newNode; }
        else {
            Node<T> current = head;
            while (current.next != null) current = current.next;
            current.next = newNode;
        }
        size++;
    }

    public T removeFirst() {
        if (head == null) throw new java.util.NoSuchElementException();
        T data = head.data;
        head = head.next;
        size--;
        return data;
    }

    public T removeLast() {
        if (head == null) throw new java.util.NoSuchElementException();
        if (head.next == null) {
            T data = head.data;
            head = null;
            size--;
            return data;
        }
        Node<T> current = head;
        while (current.next.next != null) current = current.next;
        T data = current.next.data;
        current.next = null;
        size--;
        return data;
    }

    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        Node<T> current = head;
        for (int i = 0; i < index; i++) current = current.next;
        return current.data;
    }

    public void reverse() {
        Node<T> prev = null;
        Node<T> current = head;
        while (current != null) {
            Node<T> next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }
        head = prev;
    }

    public int size() { return size; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) sb.append(", ");
            current = current.next;
        }
        return sb.append("]").toString();
    }

    public static void main(String[] args) {
        MyLinkedList<Integer> list = new MyLinkedList<>();
        list.addLast(1); list.addLast(2); list.addLast(3);
        System.out.println(list);          // [1, 2, 3]
        list.addFirst(0);
        System.out.println(list);          // [0, 1, 2, 3]
        list.reverse();
        System.out.println(list);          // [3, 2, 1, 0]
        System.out.println(list.get(2));   // 1
        list.removeFirst();
        list.removeLast();
        System.out.println(list);          // [2, 1]
    }
}
```

**Key points:**
- Reverse: three-pointer technique (prev, current, next) — O(n) time, O(1) space
- addFirst is O(1), addLast is O(n) for singly-linked (O(1) with tail pointer)
- No random access — get() is O(n)
- In interviews, they often ask you to reverse a linked list in-place

---

## Exercise 13. Generics — Bounded Type Parameters (PECS)

```java
import java.util.ArrayList;
import java.util.List;

public class CollectionUtils {

    // PECS: src produces (extends), dest consumes (super)
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        for (T element : src) {
            dest.add(element);
        }
    }

    public static <T extends Comparable<T>> T max(List<T> list) {
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("List must not be empty");
        T max = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).compareTo(max) > 0) max = list.get(i);
        }
        return max;
    }

    public static <T extends Comparable<T>> List<T> mergeSorted(List<T> a, List<T> b) {
        List<T> result = new ArrayList<>(a.size() + b.size());
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            if (a.get(i).compareTo(b.get(j)) <= 0) result.add(a.get(i++));
            else result.add(b.get(j++));
        }
        while (i < a.size()) result.add(a.get(i++));
        while (j < b.size()) result.add(b.get(j++));
        return result;
    }

    public static void main(String[] args) {
        List<Number> numbers = new ArrayList<>();
        List<Integer> ints = List.of(1, 2, 3);
        copy(numbers, ints); // Integer extends Number
        System.out.println(numbers); // [1, 2, 3]

        System.out.println(max(List.of(3, 1, 4, 1, 5, 9))); // 9

        System.out.println(mergeSorted(List.of(1, 3, 5), List.of(2, 4, 6))); // [1,2,3,4,5,6]
    }
}
```

**Key points:**
- **PECS:** Producer Extends, Consumer Super
- Type erasure: generics removed at runtime. Can't do `new T()` or `instanceof T`
- `<T extends Comparable<T>>` is the standard bounded type for natural ordering
- Wildcards (`?`) are for parameter flexibility; named params (`T`) are for correlation

---

## Exercise 14. Exceptions — Custom Exception Hierarchy

```java
class InsufficientFundsException extends Exception {
    private final double deficit;
    public InsufficientFundsException(double deficit) {
        super("Insufficient funds. Deficit: " + deficit);
        this.deficit = deficit;
    }
    public double getDeficit() { return deficit; }
}

class AccountLockedException extends RuntimeException {
    public AccountLockedException(String accountId) {
        super("Account " + accountId + " is locked");
    }
}

class BankAccount {
    private final String id;
    private double balance;
    private boolean locked;

    public BankAccount(String id, double balance) { this.id = id; this.balance = balance; }
    public void lock() { this.locked = true; }

    public void withdraw(double amount) throws InsufficientFundsException {
        if (locked) throw new AccountLockedException(id);
        if (amount > balance) throw new InsufficientFundsException(amount - balance);
        balance -= amount;
    }

    public double getBalance() { return balance; }
}

class MockConnection implements AutoCloseable {
    public MockConnection() { System.out.println("Connection opened"); }
    public void log(String msg) { System.out.println("Logged: " + msg); }
    @Override
    public void close() { System.out.println("Connection closed"); }
}

public class TransactionProcessor {
    public void processWithdrawal(BankAccount account, double amount) {
        try (MockConnection conn = new MockConnection()) {
            account.withdraw(amount);
            conn.log("Withdrew " + amount);
        } catch (InsufficientFundsException e) {
            throw new RuntimeException("Transaction failed", e); // Exception chaining
        }
    }

    public static void main(String[] args) {
        BankAccount account = new BankAccount("ACC-001", 1000);
        TransactionProcessor processor = new TransactionProcessor();

        processor.processWithdrawal(account, 500); // Success
        System.out.println("Balance: " + account.getBalance()); // 500

        try {
            processor.processWithdrawal(account, 900); // InsufficientFunds
        } catch (RuntimeException e) {
            System.out.println(e.getMessage()); // Transaction failed
            System.out.println("Cause: " + e.getCause().getMessage());
        }

        account.lock();
        try {
            processor.processWithdrawal(account, 100); // AccountLocked
        } catch (AccountLockedException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

**Key points:**
- **Checked:** recoverable (IOException, custom business exceptions)
- **Unchecked (RuntimeException):** programming errors, precondition violations
- Try-with-resources guarantees `close()` even on exception
- Exception chaining preserves root cause — never swallow exceptions silently

---

## Exercise 15. ConcurrentHashMap Internals

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class WordCounter {
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();

    public void countWords(String text) {
        for (String word : text.toLowerCase().split("\\W+")) {
            if (!word.isEmpty()) {
                counts.computeIfAbsent(word, k -> new LongAdder()).increment();
            }
        }
    }

    public Map<String, Long> getTopN(int n) {
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, LongAdder>comparingByValue(
                Comparator.comparingLong(LongAdder::sum).reversed()))
            .limit(n)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum(),
                (a, b) -> a, LinkedHashMap::new));
    }

    public static void main(String[] args) throws Exception {
        WordCounter counter = new WordCounter();
        String text = "the quick brown fox jumps over the lazy dog the fox";

        // Concurrent counting from multiple threads
        ExecutorService exec = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            exec.submit(() -> counter.countWords(text));
        }
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println(counter.getTopN(3));
        // {the=12, fox=8, ...} (4 threads × 3 occurrences each)

        // Demonstrating atomic operations
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("count", 0);

        // putIfAbsent — only puts if key is missing
        map.putIfAbsent("count", 99); // No-op, key exists
        System.out.println(map.get("count")); // 0

        // compute — atomic read-modify-write
        map.compute("count", (key, val) -> val + 1);
        System.out.println(map.get("count")); // 1

        // merge — atomic update with remapping function
        map.merge("count", 10, Integer::sum);
        System.out.println(map.get("count")); // 11
    }
}
```

**Key points:**
- Java 8+: CAS operations on individual buckets (no segment locking)
- `computeIfAbsent` + `LongAdder` is the idiomatic thread-safe counter pattern
- Never do `map.get(k); if null; map.put(k, v)` — that's a race condition. Use `computeIfAbsent`.
- `LongAdder` outperforms `AtomicLong` under high contention (striped cells)
- ConcurrentHashMap iterators are weakly consistent (no ConcurrentModificationException)

---

## Exercise 16. Multithreading — CompletableFuture Composition

```java
import java.util.concurrent.*;

record UserProfile(String userId, String name) {}
record Order(String orderId, double amount) {}
record Recommendation(String itemId) {}
record AggregatedResponse(UserProfile profile, Order[] orders, Recommendation[] recommendations) {}

public class ApiAggregator {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public CompletableFuture<AggregatedResponse> fetchUserDashboard(String userId) {
        CompletableFuture<UserProfile> profileFuture = CompletableFuture
            .supplyAsync(() -> fetchProfile(userId), executor)
            .orTimeout(500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> new UserProfile(userId, "Unknown"));

        CompletableFuture<Order[]> ordersFuture = CompletableFuture
            .supplyAsync(() -> fetchOrders(userId), executor)
            .orTimeout(500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> new Order[0]);

        CompletableFuture<Recommendation[]> recoFuture = CompletableFuture
            .supplyAsync(() -> fetchRecommendations(userId), executor)
            .orTimeout(500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> new Recommendation[0]);

        return profileFuture.thenCombine(ordersFuture, (profile, orders) -> new Object[]{profile, orders})
            .thenCombine(recoFuture, (arr, recos) ->
                new AggregatedResponse((UserProfile) arr[0], (Order[]) arr[1], recos));
    }

    private UserProfile fetchProfile(String userId) { sleep(200); return new UserProfile(userId, "Alice"); }
    private Order[] fetchOrders(String userId) { sleep(300); return new Order[]{new Order("ORD-1", 49.99)}; }
    private Recommendation[] fetchRecommendations(String userId) { sleep(150); return new Recommendation[]{new Recommendation("ITEM-42")}; }
    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    public static void main(String[] args) throws Exception {
        ApiAggregator aggregator = new ApiAggregator();
        long start = System.currentTimeMillis();
        AggregatedResponse response = aggregator.fetchUserDashboard("user-1").get();
        System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms"); // ~300ms, not 650ms
        System.out.println("Profile: " + response.profile().name());
        System.out.println("Orders: " + response.orders().length);
    }
}
```

**Key points:**
- `supplyAsync` starts tasks in parallel
- `thenCombine` merges two futures when both complete
- `exceptionally` provides per-future fallbacks (partial failure handling)
- `orTimeout` (Java 9+) completes exceptionally if not done in time
- Total latency = max of parallel calls, not sum

---

## Exercise 17. volatile and Happens-Before

```java
public class VolatileDemo {

    // --- BROKEN: without volatile, the JIT may hoist the read out of the loop ---
    static boolean runningBroken = true;

    // --- FIXED: volatile ensures visibility across threads ---
    static volatile boolean runningFixed = true;

    public static void main(String[] args) throws InterruptedException {

        // Broken version — may run forever on some JVMs
        // The JIT compiler can cache 'runningBroken' in a register
        Thread brokenThread = new Thread(() -> {
            int count = 0;
            while (runningBroken) { count++; }
            System.out.println("Broken stopped at " + count);
        });

        // Fixed version — volatile guarantees visibility
        Thread fixedThread = new Thread(() -> {
            int count = 0;
            while (runningFixed) { count++; }
            System.out.println("Fixed stopped at " + count);
        });

        fixedThread.start();
        Thread.sleep(100);
        runningFixed = false; // Write is visible to fixedThread immediately
        fixedThread.join();
        System.out.println("Fixed thread joined successfully");

        // --- Where volatile is NOT sufficient: check-then-act ---
        // volatile int counter = 0;
        // Thread A: if (counter == 0) counter = 1; // NOT atomic!
        // Thread B: if (counter == 0) counter = 1; // Both can see 0
        // For check-then-act, you need synchronized or AtomicInteger.compareAndSet()
    }
}
```

**Key points:**
- `volatile` guarantees: (1) visibility — writes are immediately visible to all threads, (2) ordering — prevents reordering around volatile access
- `volatile` does NOT guarantee atomicity of compound operations (i++, check-then-act)
- Happens-before: a write to volatile field happens-before every subsequent read of that field
- Use volatile for: flags, one-writer/many-readers, publication of immutable objects
- Use locks/atomics for: compound operations (increment, compare-and-swap)

---

## Exercise 18. ReentrantLock vs synchronized

```java
import java.util.concurrent.locks.*;

public class BoundedBuffer<T> {
    private final Object[] items;
    private int head, tail, count;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public BoundedBuffer(int capacity) {
        items = new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await(); // Only producers wait here
            }
            items[tail] = item;
            tail = (tail + 1) % items.length;
            count++;
            notEmpty.signal(); // Wake one consumer
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await(); // Only consumers wait here
            }
            T item = (T) items[head];
            items[head] = null;
            head = (head + 1) % items.length;
            count--;
            notFull.signal(); // Wake one producer
            return item;
        } finally {
            lock.unlock();
        }
    }

    // tryLock with timeout — don't wait forever
    public boolean offer(T item, long timeoutMs) throws InterruptedException {
        if (!lock.tryLock(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            return false; // Couldn't acquire lock in time
        }
        try {
            if (count == items.length) return false;
            items[tail] = item;
            tail = (tail + 1) % items.length;
            count++;
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws Exception {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(5);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) { buffer.put(i); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) { System.out.println("Got: " + buffer.take()); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }
}
```

**Key points:**
- **Separate Conditions:** `notFull` wakes only producers, `notEmpty` wakes only consumers. With `synchronized`, `notifyAll()` wakes ALL waiters indiscriminately.
- **tryLock:** Non-blocking attempt, avoids deadlock by timing out
- **Interruptible:** `lockInterruptibly()` responds to interrupts while waiting for the lock
- **Fairness:** `new ReentrantLock(true)` grants lock in FIFO order (at performance cost)
- Always use `lock()` in try-finally to guarantee unlock

---

## Exercise 19. CountDownLatch and CyclicBarrier

```java
import java.util.concurrent.*;

public class CoordinationDemo {

    // --- CountDownLatch: wait for N events to complete ---
    static void countDownLatchDemo() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        Runnable subsystem = (String name) -> {
            // Simulated initialization
        };

        for (String name : new String[]{"Database", "Cache", "MessageQueue"}) {
            new Thread(() -> {
                try {
                    Thread.sleep((long)(Math.random() * 1000));
                    System.out.println(name + " ready");
                    latch.countDown();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
        }

        System.out.println("Waiting for subsystems...");
        latch.await(); // Blocks until count reaches 0
        System.out.println("All subsystems ready. Server accepting requests.");
        // CountDownLatch is one-shot — cannot be reset
    }

    // --- CyclicBarrier: all threads meet at a point, then proceed ---
    static void cyclicBarrierDemo() throws Exception {
        int[] data = new int[400];
        for (int i = 0; i < 400; i++) data[i] = i + 1;
        int[] partialSums = new int[4];

        CyclicBarrier barrier = new CyclicBarrier(4, () -> {
            // Barrier action: runs after all threads arrive
            int total = 0;
            for (int s : partialSums) total += s;
            System.out.println("Total sum: " + total); // 80200
        });

        for (int t = 0; t < 4; t++) {
            final int threadId = t;
            new Thread(() -> {
                int start = threadId * 100;
                int sum = 0;
                for (int i = start; i < start + 100; i++) sum += data[i];
                partialSums[threadId] = sum;
                try {
                    barrier.await(); // Wait for all 4 threads
                } catch (Exception e) { Thread.currentThread().interrupt(); }
            }).start();
        }
    }

    public static void main(String[] args) throws Exception {
        countDownLatchDemo();
        Thread.sleep(1500);
        System.out.println();
        cyclicBarrierDemo();
        Thread.sleep(1000);
    }
}
```

**Key points:**
- **CountDownLatch:** one-shot, count goes down, cannot be reset. N threads count down, M threads await.
- **CyclicBarrier:** reusable, all threads must arrive before any can proceed. Barrier action runs once when all arrive.
- CountDownLatch: "wait for N things to finish" (startup, fan-out completion)
- CyclicBarrier: "all threads synchronize at a point" (phased computation, parallel iterations)

---

## Exercise 20. ThreadLocal — Per-Thread Context

```java
import java.util.concurrent.*;

record UserInfo(String userId, String role) {}

public class RequestContext {
    private static final ThreadLocal<UserInfo> context = new ThreadLocal<>();

    public static void set(UserInfo info) { context.set(info); }
    public static UserInfo get() { return context.get(); }
    public static void clear() { context.remove(); }

    // Simulated deep call stack — no need to pass UserInfo as parameter
    static void serviceLayer() {
        repositoryLayer();
    }

    static void repositoryLayer() {
        UserInfo user = RequestContext.get();
        System.out.println("[" + Thread.currentThread().getName() + "] User: " + user.userId());
    }

    public static void main(String[] args) throws Exception {
        // Correct usage: set → use → clear in try-finally
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 5; i++) {
            final int requestId = i;
            pool.submit(() -> {
                try {
                    // Set context at request entry
                    RequestContext.set(new UserInfo("user-" + requestId, "admin"));
                    serviceLayer(); // Deep in the stack, context is accessible
                } finally {
                    RequestContext.clear(); // CRITICAL: prevent leak in thread pools
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // MEMORY LEAK DEMO:
        // If you forget clear(), the ThreadLocal value stays attached to the
        // pool thread. Next request on that thread sees stale data from the
        // previous request. With thread pools (threads are reused), this is
        // both a memory leak and a correctness bug.
    }
}
```

**Key points:**
- ThreadLocal gives each thread its own isolated copy of a variable
- Primary use: request-scoped context (user info, transaction, locale) without passing through every method
- **ALWAYS** call `remove()` in a finally block when using thread pools — threads are reused!
- Without cleanup: (1) memory leak — values never GC'd, (2) data leak — next request sees previous user's data
- Alternatives in modern Java: virtual threads discourage ThreadLocal (use scoped values in Java 21+)
