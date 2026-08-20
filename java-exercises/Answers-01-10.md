# Core Java Interview Exercises — Answers

Solutions for each exercise in [Java-Interview-Exercises.md](Java-Interview-Exercises.md).

---

## Exercise 1. HashMap Internals — Custom Key

```java
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Person {
    private String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public int getAge() { return age; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age && Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    public static void main(String[] args) {
        // Scenario 1: Correct usage
        Map<Person, String> map = new HashMap<>();
        Person alice = new Person("Alice", 30);
        map.put(alice, "Engineer");
        System.out.println(map.get(new Person("Alice", 30))); // "Engineer"

        // Scenario 2: Mutate key after insertion
        Person bob = new Person("Bob", 25);
        map.put(bob, "Designer");
        bob.setName("Robert"); // Mutating the key!
        System.out.println(map.get(bob)); // null — hashCode changed, wrong bucket
        System.out.println(map.get(new Person("Bob", 25))); // null — orphaned entry

        // Scenario 3: Constant hashCode
        // If hashCode() always returns 1, all entries go to one bucket → O(n) lookups
    }
}
```

**Key points:**
- `equals()` and `hashCode()` must be consistent: if `a.equals(b)` then `a.hashCode() == b.hashCode()`
- Never mutate fields used in `hashCode()` while the object is a map key
- Constant hashCode is legal but destroys performance (O(n) instead of O(1))
- Since Java 8, buckets with 8+ entries treeify (O(log n) instead of O(n))

---

## Exercise 2. String Immutability and the String Pool

```java
import java.util.Arrays;

public class StringAnalyzer {

    public String analyze(String a, String b) {
        if (a == b) return "SAME_REFERENCE";
        if (a.equals(b)) return "SAME_VALUE";
        if (isAnagram(a, b)) return "ANAGRAM";
        return "DIFFERENT";
    }

    private boolean isAnagram(String a, String b) {
        if (a.length() != b.length()) return false;
        char[] aChars = a.toLowerCase().toCharArray();
        char[] bChars = b.toLowerCase().toCharArray();
        Arrays.sort(aChars);
        Arrays.sort(bChars);
        return Arrays.equals(aChars, bChars);
    }

    public static void main(String[] args) {
        StringAnalyzer analyzer = new StringAnalyzer();
        String s1 = "hello";
        String s2 = "hello";
        System.out.println(analyzer.analyze(s1, s2)); // SAME_REFERENCE

        String s3 = new String("hello");
        System.out.println(analyzer.analyze(s1, s3)); // SAME_VALUE

        System.out.println(analyzer.analyze("listen", "silent")); // ANAGRAM
        System.out.println(analyzer.analyze("foo", "bar"));       // DIFFERENT
    }
}
```

**Key points:**
- String pool: literals are interned at compile time; `new String()` creates a new heap object
- `==` compares references; `.equals()` compares content
- Immutability enables: thread safety, security, hashCode caching, pool sharing
- `intern()` explicitly adds a string to the pool

---

## Exercise 3. Concurrency — Producer/Consumer

```java
import java.util.concurrent.*;

public class ProducerConsumer {
    private final BlockingQueue<Integer> queue;
    private static final int POISON_PILL = -1;

    public ProducerConsumer(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 1; i <= 100; i++) {
                    queue.put(i);
                }
                queue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class Consumer implements Runnable {
        private int sum = 0;

        @Override
        public void run() {
            try {
                while (true) {
                    int value = queue.take();
                    if (value == POISON_PILL) {
                        System.out.println("Sum = " + sum);
                        break;
                    }
                    sum += value;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer(10);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(pc.new Producer());
        executor.submit(pc.new Consumer());
        executor.shutdown();
    }
}
```

**Key points:**
- `put()` blocks when full; `take()` blocks when empty — built-in backpressure
- Always restore interrupt flag when catching `InterruptedException`
- Poison pill is a clean shutdown pattern
- Bounded queue prevents OOM if producer is faster than consumer

---

## Exercise 4. Java Streams — Data Processing Pipeline

```java
import java.time.LocalDate;
import java.util.*;
import java.util.stream.*;

record Transaction(String id, String category, double amount, LocalDate date, String status) {}

public class StreamExercises {
    public static void main(String[] args) {
        List<Transaction> transactions = List.of(
            new Transaction("T1", "electronics", 5000.0, LocalDate.of(2025, 1, 10), "COMPLETED"),
            new Transaction("T2", "clothing", 200.0, LocalDate.of(2025, 1, 12), "COMPLETED"),
            new Transaction("T3", "electronics", 15000.0, LocalDate.of(2025, 2, 1), "COMPLETED"),
            new Transaction("T4", "food", 50.0, LocalDate.of(2025, 2, 5), "PENDING"),
            new Transaction("T5", "clothing", 800.0, LocalDate.of(2025, 3, 1), "COMPLETED"),
            new Transaction("T6", "electronics", 12000.0, LocalDate.of(2025, 3, 15), "PENDING")
        );

        // 1. Top 3 highest-value COMPLETED
        List<Transaction> top3 = transactions.stream()
            .filter(t -> "COMPLETED".equals(t.status()))
            .sorted(Comparator.comparingDouble(Transaction::amount).reversed())
            .limit(3)
            .toList();

        // 2. Group by category, sum amounts
        Map<String, Double> sumByCategory = transactions.stream()
            .collect(Collectors.groupingBy(Transaction::category, Collectors.summingDouble(Transaction::amount)));

        // 3. First over $10,000 (short-circuit)
        Optional<Transaction> firstOver10k = transactions.stream()
            .filter(t -> t.amount() > 10_000)
            .findFirst();

        // 4. Comma-separated IDs
        String ids = transactions.stream()
            .map(Transaction::id)
            .collect(Collectors.joining(", "));

        // 5. Partition COMPLETED vs PENDING
        Map<Boolean, List<Transaction>> partitioned = transactions.stream()
            .collect(Collectors.partitioningBy(t -> "COMPLETED".equals(t.status())));

        // 6. Total with reduce
        double total = transactions.stream()
            .map(Transaction::amount)
            .reduce(0.0, Double::sum);
    }
}
```

**Key points:**
- Streams are lazy — nothing executes until a terminal operation
- `findFirst()` short-circuits processing
- `partitioningBy` always returns both `true` and `false` keys
- Avoid parallel streams unless: large dataset + stateless + no shared mutable state

---

## Exercise 5. Concurrency — Thread-Safe Singleton

```java
import java.util.Set;
import java.util.concurrent.*;

// Approach 1: Double-checked locking
class SingletonDCL {
    private static volatile SingletonDCL instance;
    private SingletonDCL() {}

    public static SingletonDCL getInstance() {
        if (instance == null) {
            synchronized (SingletonDCL.class) {
                if (instance == null) {
                    instance = new SingletonDCL();
                }
            }
        }
        return instance;
    }
}

// Approach 2: Static inner class (Bill Pugh)
class SingletonHolder {
    private SingletonHolder() {}
    private static class Holder {
        private static final SingletonHolder INSTANCE = new SingletonHolder();
    }
    public static SingletonHolder getInstance() { return Holder.INSTANCE; }
}

// Approach 3: Enum
enum SingletonEnum {
    INSTANCE;
    public void doWork() { }
}

// Verification
public class SingletonTest {
    public static void main(String[] args) throws Exception {
        Set<SingletonDCL> instances = ConcurrentHashMap.newKeySet();
        ExecutorService exec = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 100; i++) {
            exec.submit(() -> instances.add(SingletonDCL.getInstance()));
        }
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Unique instances: " + instances.size()); // 1
    }
}
```

**Key points:**
- `volatile` prevents instruction reordering (partially constructed object visible to other threads)
- Bill Pugh leverages class loading thread safety — no synchronization needed
- Enum singleton: serialization-safe, reflection-safe, the recommended approach
- Without `volatile`, another thread might see a non-null reference to an incomplete object

---

## Exercise 6. Java Pass-by-Value

```java
public class PassByValueDemo {

    static void tryToChangeInt(int x) {
        x = 99; // Modifies local copy only
    }

    static void tryToReassign(StringBuilder sb) {
        sb = new StringBuilder("new"); // Reassigns local reference copy
    }

    static void mutateObject(StringBuilder sb) {
        sb.append(" world"); // Mutates the object through the copied reference
    }

    public static void main(String[] args) {
        // 1. Primitives are copied
        int num = 42;
        tryToChangeInt(num);
        System.out.println(num); // 42 — unchanged

        // 2. References are copied — reassignment doesn't affect caller
        StringBuilder sb1 = new StringBuilder("hello");
        tryToReassign(sb1);
        System.out.println(sb1); // "hello" — unchanged

        // 3. But the object's state CAN be mutated through the copied reference
        StringBuilder sb2 = new StringBuilder("hello");
        mutateObject(sb2);
        System.out.println(sb2); // "hello world" — mutated!
    }
}
```

**Key points:**
- Java is ALWAYS pass-by-value. For objects, the value passed is the reference (memory address), not the object itself.
- Reassigning the parameter inside the method changes the local copy of the reference, not the caller's reference.
- You CAN modify the object's state through the copied reference — that's not pass-by-reference, it's pass-by-value of a reference.

---

## Exercise 7. Comparable vs Comparator

```java
import java.time.LocalDate;
import java.util.*;

public class Employee implements Comparable<Employee> {
    private final String name;
    private final double salary;
    private final LocalDate hireDate;

    public Employee(String name, double salary, LocalDate hireDate) {
        this.name = name;
        this.salary = salary;
        this.hireDate = hireDate;
    }

    public String getName() { return name; }
    public double getSalary() { return salary; }
    public LocalDate getHireDate() { return hireDate; }

    // Natural ordering: by name
    @Override
    public int compareTo(Employee other) {
        return this.name.compareTo(other.name);
    }

    @Override
    public String toString() { return name + " ($" + salary + ")"; }

    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>(List.of(
            new Employee("Charlie", 80000, LocalDate.of(2020, 3, 1)),
            new Employee("Alice", 95000, LocalDate.of(2019, 1, 15)),
            new Employee("Bob", 80000, LocalDate.of(2021, 6, 10))
        ));

        // 1. Natural ordering (Comparable) — by name
        Collections.sort(employees);
        System.out.println("By name: " + employees);
        // [Alice ($95000.0), Bob ($80000.0), Charlie ($80000.0)]

        // 2. Custom Comparator — by salary descending
        Comparator<Employee> bySalaryDesc = Comparator.comparingDouble(Employee::getSalary).reversed();
        employees.sort(bySalaryDesc);
        System.out.println("By salary desc: " + employees);
        // [Alice ($95000.0), Bob ($80000.0), Charlie ($80000.0)]

        // 3. Multi-field: salary desc, then name asc
        Comparator<Employee> multiField = Comparator
            .comparingDouble(Employee::getSalary).reversed()
            .thenComparing(Employee::getName);
        employees.sort(multiField);
        System.out.println("By salary desc, name asc: " + employees);
        // [Alice ($95000.0), Bob ($80000.0), Charlie ($80000.0)]
    }
}
```

**Key points:**
- `Comparable`: defines natural ordering, lives inside the class, one per class
- `Comparator`: external, can have many, used for alternate sort orders
- `Comparator.comparing()` + `thenComparing()` is the modern way to build multi-field comparators
- `compareTo` should be consistent with `equals` for use in TreeSet/TreeMap

---

## Exercise 8. equals() and hashCode() with Inheritance

```java
import java.util.Objects;

class Point {
    private final int x, y;

    public Point(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() { return Objects.hash(x, y); }
}

// PROBLEM: Symmetry breaks with instanceof-based equals
class ColorPointBroken extends Point {
    private final String color;

    public ColorPointBroken(int x, int y, String color) {
        super(x, y);
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColorPointBroken cp)) return false;
        return super.equals(o) && Objects.equals(color, cp.color);
    }
    // point.equals(colorPoint) → true (Point only checks x,y)
    // colorPoint.equals(point) → false (not instanceof ColorPoint)
    // SYMMETRY VIOLATED
}

// FIX: Use composition instead of inheritance
class ColorPoint {
    private final Point point;
    private final String color;

    public ColorPoint(int x, int y, String color) {
        this.point = new Point(x, y);
        this.color = color;
    }

    public Point asPoint() { return point; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorPoint that = (ColorPoint) o;
        return point.equals(that.point) && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() { return Objects.hash(point, color); }
}

public class EqualsInheritanceDemo {
    public static void main(String[] args) {
        Point p = new Point(1, 2);
        ColorPointBroken cp = new ColorPointBroken(1, 2, "red");
        System.out.println(p.equals(cp));  // true (Point.equals uses getClass(), so actually false!)
        System.out.println(cp.equals(p));  // false
        // With getClass() check: both are false (safe but breaks Liskov)
        // With instanceof check: symmetry breaks

        // Composition fix — no ambiguity
        ColorPoint cp2 = new ColorPoint(1, 2, "red");
        // cp2 and Point are different types, never compared directly
    }
}
```

**Key points:**
- `getClass()` check: strict, preserves symmetry but breaks Liskov substitution
- `instanceof` check: allows subtype substitution but can break symmetry/transitivity
- Joshua Bloch's recommendation: use composition over inheritance when adding value components
- There is no way to extend an instantiable class and add a value component while preserving the equals contract

---

## Exercise 9. Autoboxing and Unboxing Pitfalls

```java
import java.util.*;

public class AutoboxingDemo {
    public static void main(String[] args) {
        // Integer cache: -128 to 127
        Integer a = 127, b = 127;
        Integer c = 128, d = 128;
        System.out.println(a == b);  // true — same cached object
        System.out.println(c == d);  // false — different objects (outside cache)

        // NPE from unboxing null
        Integer e = null;
        try {
            int f = e; // Unboxing null → NullPointerException
        } catch (NullPointerException ex) {
            System.out.println("NPE from unboxing null");
        }

        // remove() ambiguity
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        list.remove(1);            // remove(int index) → removes element at index 1 (value: 2)
        System.out.println(list);  // [1, 3]

        List<Integer> list2 = new ArrayList<>(List.of(1, 2, 3));
        list2.remove(Integer.valueOf(1)); // remove(Object) → removes the element with value 1
        System.out.println(list2); // [2, 3]

        // Performance: autoboxing in a loop
        Long sum = 0L; // Boxed! Creates ~2 billion Long objects
        for (long i = 0; i < Integer.MAX_VALUE; i++) {
            sum += i; // Unbox, add, rebox every iteration
        }
        // Fix: use primitive long sum = 0L;
    }
}
```

**Key points:**
- Integer cache: `==` works for -128 to 127, fails beyond that range. Always use `.equals()`.
- Unboxing `null` throws NPE — common bug in maps: `map.get(key)` returns `null`, unboxing blows up.
- `list.remove(1)` calls `remove(int index)`, not `remove(Object)`. Use `Integer.valueOf()` to force Object overload.
- Autoboxing in tight loops destroys performance — use primitives.

---

## Exercise 10. Functional Interfaces and Lambdas

```java
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

record ValidationError(String message) {}
record ValidationResult(boolean isValid, List<ValidationError> errors) {
    public static ValidationResult success() { return new ValidationResult(true, List.of()); }
    public static ValidationResult failure(List<ValidationError> errors) { return new ValidationResult(false, errors); }
}

public class ValidationEngine<T> {
    private record RuleEntry<T>(Predicate<T> predicate, String errorMessage) {}
    private final List<RuleEntry<T>> rules = new ArrayList<>();

    public ValidationEngine<T> addRule(Predicate<T> rule, String errorMessage) {
        rules.add(new RuleEntry<>(rule, errorMessage));
        return this;
    }

    public ValidationResult validate(T object) {
        List<ValidationError> errors = rules.stream()
            .filter(rule -> !rule.predicate().test(object))
            .map(rule -> new ValidationError(rule.errorMessage()))
            .collect(Collectors.toList());
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    public static void main(String[] args) {
        record User(String name, String email, int age) {}

        ValidationEngine<User> engine = new ValidationEngine<User>()
            .addRule(u -> u.name() != null && !u.name().isBlank() && u.name().length() > 2,
                     "Name must be non-blank and > 2 chars")
            .addRule(u -> u.email() != null && u.email().contains("@"),
                     "Email must contain @")
            .addRule(((Predicate<User>) u -> u.age() >= 18).and(u -> u.age() <= 120),
                     "Age must be 18-120");

        ValidationResult result = engine.validate(new User("Al", "bad", 15));
        result.errors().forEach(e -> System.out.println("  - " + e.message()));
    }
}
```

**Key points:**
- `Predicate.and()`, `.or()`, `.negate()` for composition
- Fluent API via returning `this`
- Collect ALL errors, not fail-fast
- Method references (`User::name`) replace simple lambdas for readability
