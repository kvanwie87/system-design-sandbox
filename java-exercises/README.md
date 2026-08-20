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
│   ├── tier1/    # Exercises 1–10  (asked in almost every interview)
│   ├── tier2/    # Exercises 11–20 (very common)
│   ├── tier3/    # Exercises 21–30 (frequently asked)
│   └── tier4/    # Exercises 31–50 (senior / architect level)
└── test/java/exercises/
    ├── tier1/
    ├── tier2/
    ├── tier3/
    └── tier4/
```

## Exercises & Answers

| File | Content |
|------|---------|
| [Java-Interview-Exercises.md](Java-Interview-Exercises.md) | All 50 exercises with starter code |
| [Answers-01-10.md](Answers-01-10.md) | Solutions for exercises 1–10 |
| [Answers-11-20.md](Answers-11-20.md) | Solutions for exercises 11–20 |
| [Answers-21-30.md](Answers-21-30.md) | Solutions for exercises 21–30 |
| [Answers-31-40.md](Answers-31-40.md) | Solutions for exercises 31–40 |
| [Answers-41-50.md](Answers-41-50.md) | Solutions for exercises 41–50 |

## Topics Covered

| Tier | Topics |
|------|--------|
| 1 | HashMap internals, Strings, Concurrency (producer/consumer, singleton), Streams, Pass-by-value, Comparable/Comparator, equals/hashCode inheritance, Autoboxing, Lambdas |
| 2 | ArrayList/LinkedList impl, Generics (PECS), Exceptions, ConcurrentHashMap, CompletableFuture, volatile/JMM, ReentrantLock, CountDownLatch/CyclicBarrier, ThreadLocal |
| 3 | LRU Cache, Stack/Queue, PriorityQueue, Iterator, Semaphore, ForkJoinPool, String algorithms, Abstract vs Interface, SOLID principles |
| 4 | Design Patterns (Factory, Proxy, Template, Chain, Strategy, Observer, Builder, Decorator), Memory/GC, Serialization, Reflection/DI, Class Loading, BST, Records/Sealed, Pattern Matching, Optional, Virtual Threads, TreeMap, DCL deep dive, Mini In-Memory DB |

## How to Use

1. Read the exercise in [Java-Interview-Exercises.md](Java-Interview-Exercises.md)
2. Implement your solution in the appropriate `src/main/java/exercises/tierN/` package
3. Write a test in `src/test/java/exercises/tierN/`
4. Run `./gradlew test` to verify
5. Check the answer file if you get stuck
