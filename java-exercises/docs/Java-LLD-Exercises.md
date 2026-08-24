# Core Java Interview Exercises

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