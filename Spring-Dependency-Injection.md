# Spring Dependency Injection: Types of Injection

## Summary

Spring supports three ways to inject dependencies into a bean. The mechanics are
unchanged across Spring Framework 6/7 and Spring Boot 3.x — constructor injection
remains the strongly recommended default.

- **Constructor injection** — dependencies passed as constructor arguments.
  Preferred. Enables `final` fields, explicit dependencies, easy testing.
- **Setter injection** — dependencies set through setter methods. Good for
  genuinely optional or reconfigurable dependencies.
- **Field injection** — dependencies set directly on fields via reflection.
  Discouraged outside legacy code.

The choice of injection *mechanism* is separate from the choice of injection
*annotation* (`@Autowired`, `@Inject`, `@Resource`, etc.) — see the last section.

## 1. Constructor Injection (recommended)

Dependencies are provided through the class constructor.

```java
@Service
public class OrderService {
    private final PaymentGateway paymentGateway;
    private final InventoryRepository inventoryRepository;

    // @Autowired is optional when there's a single constructor (Spring 4.3+)
    public OrderService(PaymentGateway paymentGateway,
                        InventoryRepository inventoryRepository) {
        this.paymentGateway = paymentGateway;
        this.inventoryRepository = inventoryRepository;
    }
}
```

Why it's preferred:

- Fields can be `final` → immutable and thread-safe.
- Dependencies are explicit and guaranteed present at construction → no `null` surprises.
- Easy to unit test: `new OrderService(mock1, mock2)` with no Spring container.
- Fails fast at startup if a dependency is missing, and makes circular
  dependencies obvious (they throw instead of silently resolving).

## 2. Setter Injection

Dependencies are injected via setter methods.

```java
@Service
public class ReportService {
    private NotificationClient notificationClient;

    @Autowired(required = false)  // optional dependency
    public void setNotificationClient(NotificationClient client) {
        this.notificationClient = client;
    }
}
```

Use when: the dependency is genuinely optional, or must be changeable after
construction.

## 3. Field Injection (discouraged)

Dependencies injected directly into fields via reflection.

```java
@Service
public class LegacyService {
    @Autowired
    private UserRepository userRepository;  // discouraged
}
```

Why it's discouraged:

- The field cannot be `final`.
- Hides dependencies — harder to spot when a class takes on too many.
- Can't instantiate without a DI container, so unit testing needs reflection or Spring.
- Both Spring and IntelliJ warn against it.

## `private final` vs `@Autowired`

These are not alternatives — they answer different questions:

- `private final` → how the field is **declared** (visibility + immutability).
- `@Autowired` → how Spring knows to **inject** it.

You can drop `@Autowired` and still have `private final` fields injected **when you
use constructor injection with a single constructor**. Since Spring 4.3, a class
with exactly one constructor has that constructor used for injection automatically.

```java
@Service
public class OrderService {
    private final PaymentGateway paymentGateway;   // final, no @Autowired
    private final InventoryRepository inventory;

    // single constructor → @Autowired is implicit
    public OrderService(PaymentGateway paymentGateway,
                        InventoryRepository inventory) {
        this.paymentGateway = paymentGateway;
        this.inventory = inventory;
    }
}
```

### Why `final` requires the constructor

`final` fields must be assigned exactly once, at construction time — a Java
language rule, not a Spring one.

- Constructor injection → value available when the object is built → `final` works.
- Field injection → Spring sets the field *after* the object exists, via
  reflection → the field **cannot** be `final`.
- Setter injection → set after construction → **cannot** be `final`.

### When you still need `@Autowired`

When there is **more than one constructor** and you want Spring to pick one:

```java
@Service
public class ReportService {
    private final DataSource dataSource;

    public ReportService() { this.dataSource = null; }

    @Autowired   // required: tells Spring which constructor to use
    public ReportService(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
```

## Comparison

| Type | `final` fields | Testability | `@Autowired` needed? | Best for |
|------|:---:|:---:|:---:|----------|
| Constructor | ✅ | Easy | No, if single constructor | Required dependencies (default) |
| Setter | ❌ | Moderate | Yes (on the setter) | Optional / changeable dependencies |
| Field | ❌ | Hard | Yes (on the field) | Legacy code only |

## Injection Annotations

Beyond the mechanism, you choose *which* annotation drives the wiring:

- `@Autowired` — Spring's native annotation.
- `@Inject` — JSR-330 standard (needs `jakarta.inject`).
- `@Resource` — JSR-250; resolves by name first, then type.
- `@Qualifier` — disambiguates when multiple beans of the same type exist.
- `@Value` — injects config/property values rather than beans.

## Bottom Line

Default to constructor injection with `final` fields, skip `@Autowired` when there's
a single constructor, and use setter injection for the rare optional dependency.
Avoid field injection outside legacy code. Lombok's `@RequiredArgsConstructor` can
generate the constructor to cut boilerplate.
