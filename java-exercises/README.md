# Java Interview Exercises

50 core Java programming exercises aligned with the most common interview topics, ordered by frequency. Built as a Java 21 Gradle project you can run and test locally.

## Project Setup

- **Java:** 21 (with `--enable-preview`)
- **Build:** Gradle 9.5.1 (Groovy DSL)
- **Testing:** JUnit 5

## Build & Run

```bash
./gradlew build        # Compile + run tests
./gradlew test         # Run tests only
./gradlew run          # (if you add an application plugin)
```

## Structure

```
src/
├── main/java/exercises/
│   ├── core/        # Core Java (HashMap, Strings, Streams, etc.)
│   ├── advanced/    # Advanced Java (Concurrency, JMM, Design Patterns, etc.)
│   ├── leetcode/    # Data Structures & Algorithms (ArrayList, LinkedList, LRU Cache, etc.)
│   └── lld/         # Low-Level Design (SOLID, Design Patterns, etc.)
└── test/java/exercises/
    ├── core/
    ├── advanced/
    ├── leetcode/
    └── lld/
docs/
├── Java-Core-Exercises.md
├── Java-Advanced-Exercises.md
├── Java-Leetcode-Exercises.md
├── Java-LLD-Exercises.md
└── Answers-*.md
```

## Exercises & Answers

### Exercises

| File | Content |
|------|---------|
| [Java-Core-Exercises.md](docs/Java-Core-Exercises.md) | Core Java exercises (HashMap, Strings, Streams, etc.) |
| [Java-Advanced-Exercises.md](docs/Java-Advanced-Exercises.md) | Advanced Java exercises (Concurrency, JMM, Design Patterns, etc.) |
| [Java-Leetcode-Exercises.md](docs/Java-Leetcode-Exercises.md) | Data structures & algorithm exercises (ArrayList, LinkedList, LRU Cache, etc.) |
| [Java-LLD-Exercises.md](docs/Java-LLD-Exercises.md) | Low-level design exercises (SOLID, Design Patterns, etc.) |

### Solutions

| File | Content |
|------|---------|
| [Answers-01-10.md](docs/Answers-01-10.md) | Solutions for exercises 1–10 |
| [Answers-11-20.md](docs/Answers-11-20.md) | Solutions for exercises 11–20 |
| [Answers-21-30.md](docs/Answers-21-30.md) | Solutions for exercises 21–30 |
| [Answers-31-40.md](docs/Answers-31-40.md) | Solutions for exercises 31–40 |
| [Answers-41-50.md](docs/Answers-41-50.md) | Solutions for exercises 41–50 |

## Topics Covered

| Package | Topics |
|---------|--------|
| core | HashMap internals, Strings, Concurrency (producer/consumer, singleton), Streams, Pass-by-value, Comparable/Comparator, equals/hashCode inheritance, Autoboxing, Lambdas |
| advanced | ConcurrentHashMap, CompletableFuture, volatile/JMM, ReentrantLock, CountDownLatch/CyclicBarrier, ThreadLocal, Semaphore, ForkJoinPool, Virtual Threads, Memory/GC, Serialization, Reflection/DI, Class Loading, Records/Sealed, Pattern Matching |
| leetcode | ArrayList/LinkedList impl, LRU Cache, Stack/Queue, PriorityQueue, Iterator, BST, TreeMap, String algorithms, Generics (PECS) |
| lld | SOLID principles, Design Patterns (Factory, Proxy, Template, Chain, Strategy, Observer, Builder, Decorator), Abstract vs Interface, Mini In-Memory DB |

## How to Use

1. Pick an exercise from one of the four exercise docs above
2. Implement your solution in the matching `src/main/java/exercises/{core,advanced,leetcode,lld}/` package
3. Write a test in `src/test/java/exercises/{core,advanced,leetcode,lld}/`
4. Run `./gradlew test` to verify
5. Check the answer file if you get stuck
