# Core Java Interview Exercises
Programming exercises aligned with the most common core Java interview topics, ordered by how frequently they appear in interviews (most common first).

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

### Exercise 14. Exceptions — Custom Exception Hierarchy

**Concept:** Checked vs unchecked, exception chaining, try-with-resources.

**Exercise:** Build a mini banking system with:
- `InsufficientFundsException` (checked — recoverable)
- `AccountLockedException` (unchecked — programming error)
- A `BankAccount` with `withdraw()` that throws appropriately
- A `TransactionProcessor` using try-with-resources and exception chaining

[View Solution](Answers-11-20.md#exercise-14-exceptions--custom-exception-hierarchy)

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
### Exercise 40. Serialization and Object Cloning

**Concept:** `Serializable`, `transient`, deep copy vs shallow copy, `Cloneable` pitfalls.

**Exercise:** Deep copy a `Department` containing `List<Employee>` three ways:
1. Serialization-based
2. Copy constructor
3. Clone (demonstrate the shallow copy pitfall)

[View Solution](Answers-31-40.md#exercise-40-serialization-and-object-cloning)

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