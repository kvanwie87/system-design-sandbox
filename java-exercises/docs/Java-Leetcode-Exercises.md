# Core Java Interview Exercises


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