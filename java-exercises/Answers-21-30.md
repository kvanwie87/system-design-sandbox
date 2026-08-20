# Answers — Exercises 21–30

Solutions for exercises 21–30 in [Java-Interview-Exercises.md](Java-Interview-Exercises.md).

---

## Exercise 21. Implement an LRU Cache

```java
import java.util.*;

// Approach 1: LinkedHashMap (simple)
class LRUCacheSimple<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCacheSimple(int capacity) {
        super(capacity, 0.75f, true); // accessOrder = true
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}

// Approach 2: HashMap + Doubly Linked List (classic interview version)
class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head; // Most recently used
    private final Node<K, V> tail; // Least recently used

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev, next;

        Node(K key, V value) { this.key = key; this.value = value; }
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.head = new Node<>(null, null); // Sentinel
        this.tail = new Node<>(null, null); // Sentinel
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;
        moveToHead(node);
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
        } else {
            Node<K, V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            addToHead(newNode);
            if (map.size() > capacity) {
                Node<K, V> lru = tail.prev;
                removeNode(lru);
                map.remove(lru.key);
            }
        }
    }

    private void addToHead(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }

    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.get(1);           // Access 1 → moves to head
        cache.put(4, "four");   // Evicts 2 (least recently used)

        System.out.println(cache.get(2)); // null — evicted
        System.out.println(cache.get(1)); // "one" — still present
        System.out.println(cache.get(3)); // "three"
        System.out.println(cache.get(4)); // "four"
    }
}
```

**Key points:**
- Both get() and put() must be O(1) — HashMap for lookup, doubly-linked list for ordering
- Sentinel nodes (dummy head/tail) eliminate null checks at boundaries
- On access: move to head. On eviction: remove from tail.
- LinkedHashMap approach is production-ready; the manual approach is what interviewers want to see

---

## Exercise 22. Implement a Stack and Queue

```java
import java.util.*;

// Stack using array
class MyStack<T> {
    private Object[] data;
    private int top = -1;

    public MyStack(int capacity) { data = new Object[capacity]; }

    public void push(T item) {
        if (top == data.length - 1) throw new IllegalStateException("Stack full");
        data[++top] = item;
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) throw new NoSuchElementException("Stack empty");
        T item = (T) data[top];
        data[top--] = null;
        return item;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("Stack empty");
        return (T) data[top];
    }

    public boolean isEmpty() { return top == -1; }
}

// Queue using two stacks — amortized O(1) dequeue
class MyQueue<T> {
    private final Deque<T> inbox = new ArrayDeque<>();
    private final Deque<T> outbox = new ArrayDeque<>();

    public void enqueue(T item) {
        inbox.push(item);
    }

    public T dequeue() {
        if (outbox.isEmpty()) {
            if (inbox.isEmpty()) throw new NoSuchElementException("Queue empty");
            while (!inbox.isEmpty()) {
                outbox.push(inbox.pop()); // Transfer reverses order
            }
        }
        return outbox.pop();
    }

    public boolean isEmpty() { return inbox.isEmpty() && outbox.isEmpty(); }
}

// Balanced parentheses using stack
class ParenthesesValidator {
    public static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        Map<Character, Character> pairs = Map.of(')', '(', ']', '[', '}', '{');

        for (char c : s.toCharArray()) {
            if (pairs.containsValue(c)) {
                stack.push(c);
            } else if (pairs.containsKey(c)) {
                if (stack.isEmpty() || stack.pop() != pairs.get(c)) return false;
            }
        }
        return stack.isEmpty();
    }
}

public class StackQueueDemo {
    public static void main(String[] args) {
        // Stack demo
        MyStack<Integer> stack = new MyStack<>(10);
        stack.push(1); stack.push(2); stack.push(3);
        System.out.println(stack.pop());  // 3 (LIFO)
        System.out.println(stack.peek()); // 2

        // Queue with two stacks
        MyQueue<Integer> queue = new MyQueue<>();
        queue.enqueue(1); queue.enqueue(2); queue.enqueue(3);
        System.out.println(queue.dequeue()); // 1 (FIFO)
        System.out.println(queue.dequeue()); // 2

        // Parentheses
        System.out.println(ParenthesesValidator.isValid("({[]})")); // true
        System.out.println(ParenthesesValidator.isValid("({[}])"));  // false
        System.out.println(ParenthesesValidator.isValid("(("));      // false
    }
}
```

**Key points:**
- Two-stack queue: inbox holds new items, outbox holds reversed items for dequeue. Transfer only when outbox is empty → amortized O(1).
- In Java, prefer `ArrayDeque` over `Stack` class (Stack extends Vector — synchronized overhead, legacy).
- Balanced parentheses is one of the most common stack interview questions.

---

## Exercise 23. PriorityQueue and Heap Concepts

```java
import java.util.*;

// Top-K finder using min-heap of size K
class TopKFinder {
    private final PriorityQueue<Integer> minHeap;
    private final int k;

    public TopKFinder(int k) {
        this.k = k;
        this.minHeap = new PriorityQueue<>(k); // Min-heap by default
    }

    public void add(int value) {
        if (minHeap.size() < k) {
            minHeap.offer(value);
        } else if (value > minHeap.peek()) {
            minHeap.poll();   // Remove smallest
            minHeap.offer(value); // Add new larger value
        }
    }

    public List<Integer> getTopK() {
        return new ArrayList<>(minHeap); // Unordered, but all are top-K
    }
}

// Merge K sorted lists
class KWayMerge {
    public static List<Integer> merge(List<List<Integer>> lists) {
        // Min-heap of (value, listIndex, elementIndex)
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));

        // Seed with first element of each list
        for (int i = 0; i < lists.size(); i++) {
            if (!lists.get(i).isEmpty()) {
                heap.offer(new int[]{lists.get(i).get(0), i, 0});
            }
        }

        List<Integer> result = new ArrayList<>();
        while (!heap.isEmpty()) {
            int[] entry = heap.poll();
            result.add(entry[0]);
            int listIdx = entry[1], elemIdx = entry[2];
            if (elemIdx + 1 < lists.get(listIdx).size()) {
                heap.offer(new int[]{lists.get(listIdx).get(elemIdx + 1), listIdx, elemIdx + 1});
            }
        }
        return result;
    }
}

// Simple task scheduler
class TaskScheduler {
    record Task(String name, int priority) implements Comparable<Task> {
        @Override
        public int compareTo(Task other) {
            return Integer.compare(other.priority, this.priority); // Higher priority first
        }
    }

    private final PriorityQueue<Task> queue = new PriorityQueue<>();

    public void submit(String name, int priority) { queue.offer(new Task(name, priority)); }
    public Task next() { return queue.poll(); }
}

public class PriorityQueueDemo {
    public static void main(String[] args) {
        // Top-K
        TopKFinder finder = new TopKFinder(3);
        for (int v : new int[]{5, 2, 9, 1, 7, 3, 8}) finder.add(v);
        System.out.println("Top 3: " + finder.getTopK()); // Contains 7, 8, 9

        // K-way merge
        List<List<Integer>> lists = List.of(
            List.of(1, 4, 7),
            List.of(2, 5, 8),
            List.of(3, 6, 9)
        );
        System.out.println("Merged: " + KWayMerge.merge(lists)); // [1,2,3,4,5,6,7,8,9]

        // Task scheduler
        TaskScheduler scheduler = new TaskScheduler();
        scheduler.submit("low-task", 1);
        scheduler.submit("critical-task", 10);
        scheduler.submit("medium-task", 5);
        System.out.println("Next: " + scheduler.next().name()); // critical-task
    }
}
```

**Key points:**
- Java's `PriorityQueue` is a min-heap by default. For max-heap: `new PriorityQueue<>(Comparator.reverseOrder())`
- Top-K with min-heap: keep only K elements. If new value > min, swap it in. O(n log k) total.
- K-way merge: O(n log k) where n = total elements, k = number of lists
- PriorityQueue is NOT thread-safe — use `PriorityBlockingQueue` for concurrent access

---

## Exercise 24. Iterator and Iterable — Custom Implementation

```java
import java.util.*;
import java.util.function.Predicate;

// FilteredIterator: wraps an iterator, yields only matching elements
class FilteredIterator<T> implements Iterator<T> {
    private final Iterator<T> source;
    private final Predicate<T> predicate;
    private T nextItem;
    private boolean hasNext;

    public FilteredIterator(Iterator<T> source, Predicate<T> predicate) {
        this.source = source;
        this.predicate = predicate;
        advance();
    }

    private void advance() {
        hasNext = false;
        while (source.hasNext()) {
            T candidate = source.next();
            if (predicate.test(candidate)) {
                nextItem = candidate;
                hasNext = true;
                return;
            }
        }
    }

    @Override
    public boolean hasNext() { return hasNext; }

    @Override
    public T next() {
        if (!hasNext) throw new NoSuchElementException();
        T result = nextItem;
        advance();
        return result;
    }
}

// FlatMapIterator: flattens Iterator<Iterator<T>> into Iterator<T>
class FlatMapIterator<T> implements Iterator<T> {
    private final Iterator<Iterator<T>> outer;
    private Iterator<T> current;

    public FlatMapIterator(Iterator<Iterator<T>> outer) {
        this.outer = outer;
        this.current = Collections.emptyIterator();
        advanceOuter();
    }

    private void advanceOuter() {
        while (!current.hasNext() && outer.hasNext()) {
            current = outer.next();
        }
    }

    @Override
    public boolean hasNext() {
        advanceOuter();
        return current.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        T item = current.next();
        advanceOuter();
        return item;
    }
}

public class IteratorDemo {
    public static void main(String[] args) {
        // FilteredIterator
        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8);
        Iterator<Integer> evens = new FilteredIterator<>(nums.iterator(), n -> n % 2 == 0);
        while (evens.hasNext()) System.out.print(evens.next() + " "); // 2 4 6 8
        System.out.println();

        // FlatMapIterator
        List<Iterator<String>> iterators = List.of(
            List.of("a", "b").iterator(),
            List.of("c").iterator(),
            List.of("d", "e", "f").iterator()
        );
        Iterator<String> flat = new FlatMapIterator<>(iterators.iterator());
        while (flat.hasNext()) System.out.print(flat.next() + " "); // a b c d e f
        System.out.println();
    }
}
```

**Key points:**
- Iterator is lazy — elements are computed on demand, not stored
- `advance()` pattern: pre-compute the next valid element in the constructor and after each `next()`
- FlatMapIterator is the basis of `Stream.flatMap()` internally
- Fail-fast vs fail-safe: ArrayList iterator is fail-fast (checks modCount); ConcurrentHashMap iterator is fail-safe (works on a snapshot)

---

## Exercise 25. Semaphore — Rate Limiter

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class ConnectionPool {
    private final Semaphore semaphore;
    private final BlockingQueue<Connection> pool;

    public ConnectionPool(int size) {
        this.semaphore = new Semaphore(size, true); // Fair ordering
        this.pool = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            pool.add(new Connection(i));
        }
    }

    public Connection borrowConnection(long timeout, TimeUnit unit) throws Exception {
        if (!semaphore.tryAcquire(timeout, unit)) {
            throw new TimeoutException("Could not acquire connection within timeout");
        }
        return pool.take(); // Safe — semaphore guarantees availability
    }

    public void returnConnection(Connection conn) {
        pool.offer(conn);
        semaphore.release();
    }

    public int available() { return semaphore.availablePermits(); }
}

class Connection {
    private final int id;
    public Connection(int id) { this.id = id; }
    public void execute(String query) {
        System.out.println("[Conn-" + id + "] Executing: " + query);
    }
    @Override
    public String toString() { return "Connection-" + id; }
}

public class SemaphoreDemo {
    public static void main(String[] args) throws Exception {
        ConnectionPool pool = new ConnectionPool(3);
        ExecutorService exec = Executors.newFixedThreadPool(10);
        AtomicInteger completed = new AtomicInteger();

        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            exec.submit(() -> {
                try {
                    Connection conn = pool.borrowConnection(2, TimeUnit.SECONDS);
                    try {
                        conn.execute("SELECT * FROM task_" + taskId);
                        Thread.sleep(500); // Simulate work
                    } finally {
                        pool.returnConnection(conn);
                    }
                    completed.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("Task " + taskId + " timed out");
                }
            });
        }

        exec.shutdown();
        exec.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("Completed: " + completed.get() + "/10");
        System.out.println("Available connections: " + pool.available()); // 3
    }
}
```

**Key points:**
- `Semaphore(n)` allows at most n concurrent permits — perfect for resource pools
- `tryAcquire(timeout)` avoids indefinite blocking — returns false if not available in time
- Fair semaphore (`true`) grants permits in FIFO order — prevents starvation
- Always release in a finally block to prevent permit leaks
- Semaphore vs Lock: Lock is binary (1 permit), Semaphore is counting (N permits)

---

## Exercise 26. ForkJoinPool — Parallel Divide and Conquer

```java
import java.util.Arrays;
import java.util.concurrent.*;

public class ParallelMergeSort extends RecursiveTask<int[]> {
    private final int[] array;
    private static final int THRESHOLD = 1000;

    public ParallelMergeSort(int[] array) {
        this.array = array;
    }

    @Override
    protected int[] compute() {
        if (array.length <= THRESHOLD) {
            // Base case: sort sequentially
            int[] copy = Arrays.copyOf(array, array.length);
            Arrays.sort(copy);
            return copy;
        }

        int mid = array.length / 2;
        int[] left = Arrays.copyOfRange(array, 0, mid);
        int[] right = Arrays.copyOfRange(array, mid, array.length);

        ParallelMergeSort leftTask = new ParallelMergeSort(left);
        ParallelMergeSort rightTask = new ParallelMergeSort(right);

        leftTask.fork();  // Execute left in another thread
        int[] rightResult = rightTask.compute(); // Execute right in current thread
        int[] leftResult = leftTask.join();      // Wait for left

        return merge(leftResult, rightResult);
    }

    private int[] merge(int[] a, int[] b) {
        int[] result = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length) {
            result[k++] = (a[i] <= b[j]) ? a[i++] : b[j++];
        }
        while (i < a.length) result[k++] = a[i++];
        while (j < b.length) result[k++] = b[j++];
        return result;
    }

    public static void main(String[] args) {
        int[] data = new int[100_000];
        for (int i = 0; i < data.length; i++) data[i] = (int)(Math.random() * 1_000_000);

        ForkJoinPool pool = ForkJoinPool.commonPool();

        long start = System.currentTimeMillis();
        int[] sorted = pool.invoke(new ParallelMergeSort(data));
        long parallel = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        int[] copy = Arrays.copyOf(data, data.length);
        Arrays.sort(copy);
        long sequential = System.currentTimeMillis() - start;

        System.out.println("Parallel: " + parallel + "ms");
        System.out.println("Sequential: " + sequential + "ms");
        System.out.println("Sorted correctly: " + Arrays.equals(sorted, copy));
    }
}
```

**Key points:**
- `fork()` submits task to pool; `compute()` runs in current thread; `join()` waits for result
- Always `fork()` one side and `compute()` the other — avoid forking both (wastes a thread)
- Work-stealing: idle threads steal tasks from busy threads' deques
- THRESHOLD prevents over-forking — below it, sequential sort is faster
- `RecursiveTask<V>` returns a value; `RecursiveAction` returns void

---

## Exercise 27. String Manipulation — Reverse and Palindrome

```java
public class StringProblems {

    // 1. Reverse a string using char array
    public String reverse(String s) {
        char[] chars = s.toCharArray();
        int left = 0, right = chars.length - 1;
        while (left < right) {
            char temp = chars[left];
            chars[left] = chars[right];
            chars[right] = temp;
            left++;
            right--;
        }
        return new String(chars);
    }

    // 2. Reverse words: "hello world" → "world hello"
    public String reverseWords(String s) {
        String[] words = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    // 3. Palindrome check (ignoring case and non-alphanumeric)
    public boolean isPalindrome(String s) {
        int left = 0, right = s.length() - 1;
        while (left < right) {
            while (left < right && !Character.isLetterOrDigit(s.charAt(left))) left++;
            while (left < right && !Character.isLetterOrDigit(s.charAt(right))) right--;
            if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    // 4. Longest palindromic substring (expand from center)
    public String longestPalindrome(String s) {
        if (s == null || s.length() < 2) return s;
        int start = 0, maxLen = 1;

        for (int i = 0; i < s.length(); i++) {
            // Odd length palindromes
            int len1 = expandFromCenter(s, i, i);
            // Even length palindromes
            int len2 = expandFromCenter(s, i, i + 1);
            int len = Math.max(len1, len2);
            if (len > maxLen) {
                maxLen = len;
                start = i - (len - 1) / 2;
            }
        }
        return s.substring(start, start + maxLen);
    }

    private int expandFromCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return right - left - 1;
    }

    public static void main(String[] args) {
        StringProblems sp = new StringProblems();
        System.out.println(sp.reverse("hello"));                  // "olleh"
        System.out.println(sp.reverseWords("hello world foo"));   // "foo world hello"
        System.out.println(sp.isPalindrome("A man, a plan, a canal: Panama")); // true
        System.out.println(sp.longestPalindrome("babad"));        // "bab" or "aba"
    }
}
```

**Key points:**
- Two-pointer technique for in-place reversal — O(n) time, O(1) extra space
- Palindrome: skip non-alphanumeric chars, compare case-insensitive
- Longest palindrome: expand from center is O(n²) — better than brute force O(n³)
- String is immutable in Java, so "in-place" means working with char[] then creating new String

---

## Exercise 28. String Compression and Duplicate Detection

```java
import java.util.*;

public class StringAlgorithms {

    // 1. Compress: "aabcccccaaa" → "a2b1c5a3"
    public String compress(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (int i = 1; i <= s.length(); i++) {
            if (i < s.length() && s.charAt(i) == s.charAt(i - 1)) {
                count++;
            } else {
                sb.append(s.charAt(i - 1)).append(count);
                count = 1;
            }
        }
        // Only return compressed if shorter
        return sb.length() < s.length() ? sb.toString() : s;
    }

    // 2. First non-repeating character
    public char firstNonRepeating(String s) {
        // LinkedHashMap preserves insertion order
        LinkedHashMap<Character, Integer> counts = new LinkedHashMap<>();
        for (char c : s.toCharArray()) {
            counts.merge(c, 1, Integer::sum);
        }
        for (Map.Entry<Character, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 1) return entry.getKey();
        }
        throw new NoSuchElementException("No non-repeating character");
    }

    // 3. Remove duplicate characters preserving order
    public String removeDuplicates(String s) {
        Set<Character> seen = new LinkedHashSet<>();
        for (char c : s.toCharArray()) seen.add(c);
        StringBuilder sb = new StringBuilder();
        for (char c : seen) sb.append(c);
        return sb.toString();
    }

    // 4. Check if two strings are rotations: "abcde" / "cdeab"
    public boolean isRotation(String s1, String s2) {
        if (s1.length() != s2.length()) return false;
        // Key insight: if s2 is a rotation of s1, then s2 is a substring of s1+s1
        String doubled = s1 + s1;
        return doubled.contains(s2);
    }

    public static void main(String[] args) {
        StringAlgorithms sa = new StringAlgorithms();
        System.out.println(sa.compress("aabcccccaaa"));    // "a2b1c5a3"
        System.out.println(sa.compress("abc"));             // "abc" (compressed is longer)
        System.out.println(sa.firstNonRepeating("aabcbd")); // 'c'
        System.out.println(sa.removeDuplicates("abracadabra")); // "abrcd"
        System.out.println(sa.isRotation("abcde", "cdeab")); // true
        System.out.println(sa.isRotation("abcde", "abced")); // false
    }
}
```

**Key points:**
- Compression: only return compressed if actually shorter — edge case many candidates miss
- First non-repeating: LinkedHashMap gives O(n) with insertion-order traversal
- Rotation trick: `s1 + s1` contains all rotations of s1 as substrings. One of those "aha" interview insights.
- LinkedHashSet preserves insertion order while deduplicating — useful for "remove duplicates preserving order"

---

## Exercise 29. Abstract Class vs Interface

```java
import java.util.UUID;

// Abstract class: shared state + template behavior
abstract class PaymentProcessor {
    private int retryCount = 3;

    // Shared state: transaction ID generation
    protected String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Template method: defines algorithm skeleton
    public final void process(double amount) {
        String txnId = generateTransactionId();
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                executePayment(txnId, amount);
                System.out.println("Payment " + txnId + " succeeded on attempt " + attempt);
                return;
            } catch (RuntimeException e) {
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
            }
        }
        throw new RuntimeException("Payment failed after " + retryCount + " attempts");
    }

    // Subclass fills in the specifics
    protected abstract void executePayment(String txnId, double amount);
}

// Interface: capability contract (no state)
interface Auditable {
    // Default method: provides behavior without forcing implementation
    default void audit(String action, String details) {
        System.out.println("[AUDIT] " + action + ": " + details);
    }
}

// Interface: optional capability
interface Refundable {
    void refund(String transactionId, double amount);
}

// Concrete: supports everything
class CreditCardProcessor extends PaymentProcessor implements Auditable, Refundable {
    @Override
    protected void executePayment(String txnId, double amount) {
        audit("CHARGE", txnId + " $" + amount);
        // Simulate credit card charge
        if (Math.random() < 0.3) throw new RuntimeException("Network timeout");
    }

    @Override
    public void refund(String transactionId, double amount) {
        audit("REFUND", transactionId + " $" + amount);
        System.out.println("Refunded " + amount + " for " + transactionId);
    }
}

// Concrete: no refund support
class CryptoProcessor extends PaymentProcessor implements Auditable {
    @Override
    protected void executePayment(String txnId, double amount) {
        audit("CRYPTO_TRANSFER", txnId + " $" + amount);
        // Crypto payments are non-reversible
    }
    // Does NOT implement Refundable — crypto can't be refunded
}

public class PaymentDemo {
    public static void main(String[] args) {
        CreditCardProcessor cc = new CreditCardProcessor();
        cc.process(99.99);

        // Can refund credit card
        if (cc instanceof Refundable r) {
            r.refund("TXN-abc123", 99.99);
        }

        // Crypto processor — no refund
        CryptoProcessor crypto = new CryptoProcessor();
        crypto.process(0.5);
        System.out.println("Crypto is refundable: " + (crypto instanceof Refundable)); // false
    }
}
```

**Key points:**
- **Abstract class:** use when you have shared state (retryCount, txnId generator) or template algorithms. A class can only extend ONE abstract class.
- **Interface:** use for capabilities/contracts with no state. A class can implement MANY interfaces.
- **Default methods (Java 8+):** add behavior to interfaces without breaking existing implementations. Used for backward-compatible API evolution.
- **When to choose:** If it has state or a core algorithm skeleton → abstract class. If it's a capability or you need multiple inheritance → interface.

---

## Exercise 30. SOLID — Single Responsibility and Open/Closed

```java
import java.util.*;

// --- BEFORE: Monolithic class with multiple responsibilities ---
/*
class OrderProcessorBad {
    void process(Order order) {
        // Validates
        if (order.items().isEmpty()) throw new IllegalArgumentException("Empty order");
        // Calculates discount
        double discount = order.total() > 100 ? 0.1 : 0;
        // Persists
        database.save(order);
        // Sends email
        emailService.send(order.customer(), "Order confirmed");
    }
}
*/

// --- AFTER: Single Responsibility ---

record OrderItem(String name, double price, int quantity) {}
record Order(String id, String customer, List<OrderItem> items) {
    public double total() { return items.stream().mapToDouble(i -> i.price() * i.quantity()).sum(); }
}

// Responsibility 1: Validation
class OrderValidator {
    public void validate(Order order) {
        if (order.items() == null || order.items().isEmpty())
            throw new IllegalArgumentException("Order must have items");
        if (order.customer() == null || order.customer().isBlank())
            throw new IllegalArgumentException("Customer required");
    }
}

// Responsibility 2: Discount calculation (Open/Closed via Strategy)
interface DiscountStrategy {
    double calculateDiscount(Order order);
}

class NoDiscount implements DiscountStrategy {
    @Override
    public double calculateDiscount(Order order) { return 0; }
}

class PercentageDiscount implements DiscountStrategy {
    private final double threshold;
    private final double percentage;
    public PercentageDiscount(double threshold, double percentage) {
        this.threshold = threshold;
        this.percentage = percentage;
    }
    @Override
    public double calculateDiscount(Order order) {
        return order.total() > threshold ? order.total() * percentage : 0;
    }
}

// NEW discount type — no modification to existing classes!
class BuyOneGetOneFree implements DiscountStrategy {
    @Override
    public double calculateDiscount(Order order) {
        return order.items().stream()
            .filter(i -> i.quantity() >= 2)
            .mapToDouble(OrderItem::price)
            .sum();
    }
}

// Responsibility 3: Persistence (interface)
interface OrderRepository {
    void save(Order order);
}

// Responsibility 4: Notification (interface)
interface NotificationService {
    void notify(String recipient, String message);
}

// Orchestrator: composes single-responsibility components
class OrderProcessor {
    private final OrderValidator validator;
    private final DiscountStrategy discountStrategy;
    private final OrderRepository repository;
    private final NotificationService notificationService;

    public OrderProcessor(OrderValidator validator, DiscountStrategy discountStrategy,
                          OrderRepository repository, NotificationService notificationService) {
        this.validator = validator;
        this.discountStrategy = discountStrategy;
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public double process(Order order) {
        validator.validate(order);
        double discount = discountStrategy.calculateDiscount(order);
        double finalTotal = order.total() - discount;
        repository.save(order);
        notificationService.notify(order.customer(), "Order " + order.id() + " confirmed. Total: $" + finalTotal);
        return finalTotal;
    }
}

public class SolidDemo {
    public static void main(String[] args) {
        Order order = new Order("ORD-1", "alice@example.com", List.of(
            new OrderItem("Widget", 50.0, 3),
            new OrderItem("Gadget", 25.0, 1)
        ));

        OrderProcessor processor = new OrderProcessor(
            new OrderValidator(),
            new PercentageDiscount(100, 0.1), // 10% off orders over $100
            o -> System.out.println("Saved: " + o.id()),
            (recipient, msg) -> System.out.println("Email to " + recipient + ": " + msg)
        );

        double total = processor.process(order);
        System.out.println("Final total: $" + total); // $175 - $17.5 = $157.5
    }
}
```

**Key points:**
- **SRP:** Each class has one reason to change. Validator changes for validation rules, DiscountStrategy changes for pricing logic, etc.
- **OCP:** Adding `BuyOneGetOneFree` required zero changes to existing classes — just implement the interface.
- Strategy pattern is the classic OCP enabler — new behavior via new classes, not modifications.
- The orchestrator (`OrderProcessor`) coordinates but doesn't implement business logic itself.
