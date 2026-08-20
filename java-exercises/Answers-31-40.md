# Answers — Exercises 31–40

Solutions for exercises 31–40 in [Java-Interview-Exercises.md](Java-Interview-Exercises.md).

---

## Exercise 31. SOLID — Liskov Substitution and Interface Segregation

```java
// --- LSP VIOLATION: Rectangle / Square ---

class Rectangle {
    protected int width, height;

    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

class Square extends Rectangle {
    // Square overrides to keep sides equal — violates LSP
    @Override
    public void setWidth(int w) { this.width = w; this.height = w; }
    @Override
    public void setHeight(int h) { this.width = h; this.height = h; }
}

// Client code assumes independent width/height:
// Rectangle r = new Square();
// r.setWidth(5);
// r.setHeight(3);
// assert r.area() == 15; // FAILS — area is 9 because Square enforces w==h
// Subtituting Square for Rectangle breaks the caller's expectations → LSP violated

// LSP FIX: Use immutable shapes with no setters
interface Shape {
    int area();
}

record RectangleFixed(int width, int height) implements Shape {
    public int area() { return width * height; }
}

record SquareFixed(int side) implements Shape {
    public int area() { return side * side; }
}
// No inheritance relationship — no substitution problem

// --- ISP VIOLATION ---

// BAD: one fat interface forces robots to implement eat/sleep
interface WorkerBad {
    void work();
    void eat();   // Robots don't eat
    void sleep(); // Robots don't sleep
}

// ISP FIX: Split into focused interfaces
interface Workable { void work(); }
interface Feedable { void eat(); }
interface Restable { void sleep(); }

class HumanWorker implements Workable, Feedable, Restable {
    public void work() { System.out.println("Human working"); }
    public void eat() { System.out.println("Human eating"); }
    public void sleep() { System.out.println("Human sleeping"); }
}

class RobotWorker implements Workable {
    public void work() { System.out.println("Robot working 24/7"); }
    // No eat() or sleep() — not forced to throw UnsupportedOperationException
}

public class LspIspDemo {
    public static void main(String[] args) {
        // LSP demo
        Shape rect = new RectangleFixed(5, 3);
        Shape square = new SquareFixed(4);
        System.out.println("Rectangle area: " + rect.area());   // 15
        System.out.println("Square area: " + square.area());     // 16

        // ISP demo
        Workable human = new HumanWorker();
        Workable robot = new RobotWorker();
        human.work(); // Human working
        robot.work(); // Robot working 24/7
    }
}
```

**Key points:**
- **LSP:** If substituting a subtype breaks caller expectations, the inheritance is wrong. Fix with composition or immutability.
- **ISP:** No client should be forced to depend on methods it doesn't use. Split fat interfaces into role-specific ones.
- The Rectangle/Square problem is the classic LSP example — mutable setters create the contract violation.
- Immutable value objects (records) sidestep LSP issues entirely — no behavioral surprises.

---

## Exercise 32. SOLID — Dependency Inversion

```java
// --- BEFORE: High-level depends directly on low-level ---
/*
class NotificationServiceBad {
    private final SmtpEmailSender sender = new SmtpEmailSender(); // Tight coupling
    void notify(String user, String msg) { sender.send(user, msg); }
}
*/

// --- AFTER: Depend on abstraction ---

// Abstraction (owned by the high-level module)
interface MessageSender {
    void send(String recipient, String message);
}

// Low-level implementations
class EmailSender implements MessageSender {
    @Override
    public void send(String recipient, String message) {
        System.out.println("[EMAIL] To: " + recipient + " | " + message);
    }
}

class SmsSender implements MessageSender {
    @Override
    public void send(String recipient, String message) {
        System.out.println("[SMS] To: " + recipient + " | " + message);
    }
}

class PushNotificationSender implements MessageSender {
    @Override
    public void send(String recipient, String message) {
        System.out.println("[PUSH] To: " + recipient + " | " + message);
    }
}

// High-level module: depends on abstraction, not concretion
class NotificationService {
    private final MessageSender sender;

    // Constructor injection — swap implementations at runtime
    public NotificationService(MessageSender sender) {
        this.sender = sender;
    }

    public void notify(String user, String message) {
        sender.send(user, message);
    }
}

// Composite sender — send via multiple channels
class MultiChannelSender implements MessageSender {
    private final java.util.List<MessageSender> senders;

    public MultiChannelSender(MessageSender... senders) {
        this.senders = java.util.List.of(senders);
    }

    @Override
    public void send(String recipient, String message) {
        senders.forEach(s -> s.send(recipient, message));
    }
}

public class DependencyInversionDemo {
    public static void main(String[] args) {
        // Swap implementation without changing NotificationService
        NotificationService emailService = new NotificationService(new EmailSender());
        emailService.notify("alice@example.com", "Your order shipped");

        NotificationService smsService = new NotificationService(new SmsSender());
        smsService.notify("+1234567890", "Your code is 4829");

        // Multi-channel
        NotificationService multiService = new NotificationService(
            new MultiChannelSender(new EmailSender(), new PushNotificationSender())
        );
        multiService.notify("bob@example.com", "New login detected");
    }
}
```

**Key points:**
- **DIP:** High-level modules (NotificationService) should not depend on low-level modules (SmtpEmailSender). Both should depend on abstractions (MessageSender).
- Constructor injection makes dependencies explicit and testable (inject mocks in tests).
- The abstraction is owned by the high-level module, not the low-level one.
- This is the foundation of dependency injection frameworks (Spring, Guice).

---

## Exercise 33. Factory and Abstract Factory

```java
import java.util.*;

// --- Simple Factory ---

interface Notification {
    void send(String recipient, String message);
}

class EmailNotification implements Notification {
    public void send(String recipient, String message) {
        System.out.println("[EMAIL] " + recipient + ": " + message);
    }
}

class SmsNotification implements Notification {
    public void send(String recipient, String message) {
        System.out.println("[SMS] " + recipient + ": " + message);
    }
}

class PushNotification implements Notification {
    public void send(String recipient, String message) {
        System.out.println("[PUSH] " + recipient + ": " + message);
    }
}

class NotificationFactory {
    public static Notification create(String type) {
        return switch (type.toLowerCase()) {
            case "email" -> new EmailNotification();
            case "sms" -> new SmsNotification();
            case "push" -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}

// --- Abstract Factory: families of related objects ---

interface Button {
    void render();
}
interface TextField {
    void render();
}

// Web family
class WebButton implements Button {
    public void render() { System.out.println("<button>Click</button>"); }
}
class WebTextField implements TextField {
    public void render() { System.out.println("<input type='text'/>"); }
}

// Mobile family
class MobileButton implements Button {
    public void render() { System.out.println("[Mobile Button]"); }
}
class MobileTextField implements TextField {
    public void render() { System.out.println("[Mobile TextField]"); }
}

// Abstract factory interface
interface UIFactory {
    Button createButton();
    TextField createTextField();
}

class WebUIFactory implements UIFactory {
    public Button createButton() { return new WebButton(); }
    public TextField createTextField() { return new WebTextField(); }
}

class MobileUIFactory implements UIFactory {
    public Button createButton() { return new MobileButton(); }
    public TextField createTextField() { return new MobileTextField(); }
}

// Client code works with ANY factory — doesn't know concrete types
class LoginForm {
    private final Button submitButton;
    private final TextField usernameField;

    public LoginForm(UIFactory factory) {
        this.submitButton = factory.createButton();
        this.usernameField = factory.createTextField();
    }

    public void render() {
        usernameField.render();
        submitButton.render();
    }
}

public class FactoryDemo {
    public static void main(String[] args) {
        // Simple factory
        Notification n = NotificationFactory.create("email");
        n.send("alice@example.com", "Hello");

        // Abstract factory — consistent families
        System.out.println("\n--- Web ---");
        new LoginForm(new WebUIFactory()).render();

        System.out.println("\n--- Mobile ---");
        new LoginForm(new MobileUIFactory()).render();
    }
}
```

**Key points:**
- **Simple Factory:** Encapsulates creation logic in one place. Client doesn't know concrete classes.
- **Abstract Factory:** Produces families of related objects. Guarantees consistency (web button + web text field, never web button + mobile text field).
- Client code depends on factory interface → swap entire product families by swapping the factory.
- Switch expressions (Java 14+) make factory methods concise.

---

## Exercise 34. Proxy Pattern — Lazy Loading and Access Control

```java
import java.lang.reflect.*;
import java.util.*;

// --- Virtual Proxy: Lazy loading ---

interface Image {
    void display();
}

class RealImage implements Image {
    private final String filename;
    private final byte[] data;

    public RealImage(String filename) {
        this.filename = filename;
        this.data = loadFromDisk(filename); // Expensive!
    }

    private byte[] loadFromDisk(String filename) {
        System.out.println("Loading image from disk: " + filename);
        return new byte[1024 * 1024]; // Simulate 1MB load
    }

    public void display() { System.out.println("Displaying: " + filename); }
}

class LazyImage implements Image {
    private final String filename;
    private RealImage realImage; // Loaded on first use

    public LazyImage(String filename) { this.filename = filename; }

    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename); // Load only when needed
        }
        realImage.display();
    }
}

// --- Protection Proxy: Access control ---

interface DocumentService {
    String readDocument(String docId);
    void deleteDocument(String docId);
}

class RealDocumentService implements DocumentService {
    public String readDocument(String docId) { return "Content of " + docId; }
    public void deleteDocument(String docId) { System.out.println("Deleted: " + docId); }
}

class SecureDocumentService implements DocumentService {
    private final DocumentService delegate;
    private final String userRole;

    public SecureDocumentService(DocumentService delegate, String userRole) {
        this.delegate = delegate;
        this.userRole = userRole;
    }

    public String readDocument(String docId) {
        // All roles can read
        return delegate.readDocument(docId);
    }

    public void deleteDocument(String docId) {
        if (!"admin".equals(userRole)) {
            throw new SecurityException("Only admins can delete documents");
        }
        delegate.deleteDocument(docId);
    }
}

// --- Dynamic Proxy: Logging for any interface ---

class LoggingProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T create(T target, Class<T> iface) {
        return (T) Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class[]{iface},
            (proxy, method, args) -> {
                System.out.println("[LOG] Calling: " + method.getName() +
                    " with args: " + (args != null ? Arrays.toString(args) : "[]"));
                long start = System.nanoTime();
                Object result = method.invoke(target, args);
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                System.out.println("[LOG] " + method.getName() + " returned in " + elapsed + "ms");
                return result;
            }
        );
    }
}

public class ProxyDemo {
    public static void main(String[] args) {
        // Virtual proxy — image not loaded until display()
        Image img = new LazyImage("photo.jpg");
        System.out.println("Image object created (not loaded yet)");
        img.display(); // NOW it loads
        img.display(); // Already loaded, no reload

        System.out.println();

        // Protection proxy
        DocumentService adminService = new SecureDocumentService(new RealDocumentService(), "admin");
        DocumentService userService = new SecureDocumentService(new RealDocumentService(), "viewer");
        System.out.println(adminService.readDocument("doc-1"));
        adminService.deleteDocument("doc-1"); // Works
        try {
            userService.deleteDocument("doc-2"); // Throws SecurityException
        } catch (SecurityException e) {
            System.out.println("Blocked: " + e.getMessage());
        }

        System.out.println();

        // Dynamic proxy — logs all calls to any interface
        DocumentService logged = LoggingProxyFactory.create(new RealDocumentService(), DocumentService.class);
        logged.readDocument("doc-3");
    }
}
```

**Key points:**
- **Virtual Proxy:** Delays expensive creation until first use. Common for images, DB connections, remote objects.
- **Protection Proxy:** Same interface, intercepts calls to enforce access control.
- **Dynamic Proxy (`java.lang.reflect.Proxy`):** Creates a proxy for ANY interface at runtime. This is how Spring AOP, Hibernate lazy-loading, and mock frameworks (Mockito) work internally.
- Dynamic proxies only work with interfaces, not concrete classes (for classes, use CGLIB/ByteBuddy).

---

## Exercise 35. Template Method Pattern

```java
import java.util.*;

abstract class DataProcessor {
    // Template method — final prevents subclass from changing the algorithm structure
    public final List<String> process() {
        beforeProcess();
        List<String> raw = read();
        List<String> transformed = transform(raw);
        validate(transformed);
        write(transformed);
        afterProcess();
        return transformed;
    }

    // Abstract steps — subclass MUST implement
    protected abstract List<String> read();
    protected abstract List<String> transform(List<String> data);
    protected abstract void write(List<String> data);

    // Concrete step with default behavior
    protected void validate(List<String> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("No data to process");
        }
    }

    // Hook methods — optional overrides
    protected void beforeProcess() { }
    protected void afterProcess() { }
}

class CsvProcessor extends DataProcessor {
    @Override
    protected List<String> read() {
        System.out.println("Reading CSV file...");
        return List.of("name,age", "Alice,30", "Bob,25");
    }

    @Override
    protected List<String> transform(List<String> data) {
        // Skip header, uppercase names
        return data.stream()
            .skip(1)
            .map(line -> line.split(",")[0].toUpperCase())
            .toList();
    }

    @Override
    protected void write(List<String> data) {
        System.out.println("Writing CSV output: " + data);
    }

    @Override
    protected void beforeProcess() {
        System.out.println("[CSV] Starting processing...");
    }
}

class JsonProcessor extends DataProcessor {
    @Override
    protected List<String> read() {
        System.out.println("Reading JSON API...");
        return List.of("{\"name\":\"Charlie\"}", "{\"name\":\"Diana\"}");
    }

    @Override
    protected List<String> transform(List<String> data) {
        // Extract names from JSON (simplified)
        return data.stream()
            .map(json -> json.replaceAll("[{}\"]", "").split(":")[1])
            .toList();
    }

    @Override
    protected void write(List<String> data) {
        System.out.println("Writing to database: " + data);
    }
}

public class TemplateMethodDemo {
    public static void main(String[] args) {
        System.out.println("=== CSV Processing ===");
        new CsvProcessor().process();

        System.out.println("\n=== JSON Processing ===");
        new JsonProcessor().process();
    }
}
```

**Key points:**
- Template method defines the algorithm skeleton; subclasses fill in specific steps.
- `final` on the template method prevents subclasses from altering the sequence.
- Hook methods (beforeProcess, afterProcess) provide optional extension points without forcing implementation.
- Used extensively in frameworks: `HttpServlet.service()`, Spring's `JdbcTemplate`, JUnit's test lifecycle.

---

## Exercise 36. Chain of Responsibility

```java
import java.util.*;

record Request(String token, String userId, String role, String path) {}
record Response(int status, String message) {}

abstract class Handler {
    private Handler next;

    public Handler setNext(Handler next) {
        this.next = next;
        return next; // Fluent chaining
    }

    public Response handle(Request request) {
        Response response = doHandle(request);
        if (response != null) return response; // Rejected — stop chain
        if (next != null) return next.handle(request); // Pass to next
        return new Response(200, "OK"); // End of chain — success
    }

    protected abstract Response doHandle(Request request);
}

class AuthenticationHandler extends Handler {
    private static final Set<String> VALID_TOKENS = Set.of("token-abc", "token-xyz");

    @Override
    protected Response doHandle(Request request) {
        if (request.token() == null || !VALID_TOKENS.contains(request.token())) {
            return new Response(401, "Unauthorized: Invalid token");
        }
        return null; // Pass to next handler
    }
}

class RateLimitHandler extends Handler {
    private final Map<String, Integer> requestCounts = new HashMap<>();
    private final int limit;

    public RateLimitHandler(int limit) { this.limit = limit; }

    @Override
    protected Response doHandle(Request request) {
        int count = requestCounts.merge(request.userId(), 1, Integer::sum);
        if (count > limit) {
            return new Response(429, "Too Many Requests");
        }
        return null;
    }
}

class AuthorizationHandler extends Handler {
    private final Map<String, Set<String>> rolePermissions = Map.of(
        "admin", Set.of("/users", "/admin", "/reports"),
        "user", Set.of("/users", "/reports"),
        "guest", Set.of("/reports")
    );

    @Override
    protected Response doHandle(Request request) {
        Set<String> allowed = rolePermissions.getOrDefault(request.role(), Set.of());
        if (!allowed.contains(request.path())) {
            return new Response(403, "Forbidden: " + request.role() + " cannot access " + request.path());
        }
        return null;
    }
}

class LoggingHandler extends Handler {
    @Override
    protected Response doHandle(Request request) {
        System.out.println("[LOG] " + request.userId() + " → " + request.path());
        return null; // Always passes through
    }
}

public class ChainOfResponsibilityDemo {
    public static void main(String[] args) {
        // Build the chain
        Handler chain = new AuthenticationHandler();
        chain.setNext(new RateLimitHandler(3))
             .setNext(new AuthorizationHandler())
             .setNext(new LoggingHandler());

        // Valid request
        Request req1 = new Request("token-abc", "user-1", "admin", "/admin");
        System.out.println(chain.handle(req1)); // 200 OK

        // Invalid token
        Request req2 = new Request("bad-token", "user-2", "user", "/users");
        System.out.println(chain.handle(req2)); // 401 Unauthorized

        // Forbidden path
        Request req3 = new Request("token-xyz", "user-3", "guest", "/admin");
        System.out.println(chain.handle(req3)); // 403 Forbidden

        // Rate limited (send 4 requests)
        for (int i = 0; i < 4; i++) {
            Request req = new Request("token-abc", "user-4", "user", "/reports");
            Response resp = chain.handle(req);
            System.out.println("Request " + (i+1) + ": " + resp);
        }
        // 4th request → 429 Too Many Requests
    }
}
```

**Key points:**
- Each handler decides: handle and stop, or pass to next.
- Decouples sender from receivers — sender doesn't know which handler will process the request.
- Easy to reorder, add, or remove handlers without changing others.
- Used in: servlet filters, Spring Security filter chain, logging middleware, validation pipelines.

---

## Exercise 37. Inheritance vs Composition — Design Exercise

```java
// --- BROKEN: Penguin extends Bird but can't fly ---
/*
class Bird {
    void fly() { System.out.println("Flying"); }
    void eat() { System.out.println("Eating"); }
}
class Penguin extends Bird {
    void fly() { throw new UnsupportedOperationException(); } // LSP violation
}
*/

// --- FIX: Interface segregation + Composition ---

// Step 1: Segregated interfaces
interface Flyable { void fly(); }
interface Swimmable { void swim(); }
interface Eatable { void eat(); }
interface Diveable { void dive(); }

// Step 2: Behavior implementations (Strategy objects)
class WingFlight implements Flyable {
    public void fly() { System.out.println("Flying with wings"); }
}

class GlideFlight implements Flyable {
    public void fly() { System.out.println("Gliding on currents"); }
}

class SurfaceSwim implements Swimmable {
    public void swim() { System.out.println("Swimming on surface"); }
}

class DeepDive implements Diveable {
    public void dive() { System.out.println("Diving deep"); }
}

// Step 3: Birds compose behaviors — only implement what applies
class Sparrow implements Flyable, Eatable {
    private final Flyable flight = new WingFlight();
    public void fly() { flight.fly(); }
    public void eat() { System.out.println("Eating seeds"); }
}

class Penguin implements Swimmable, Diveable, Eatable {
    private final Swimmable swim = new SurfaceSwim();
    private final Diveable dive = new DeepDive();
    public void swim() { swim.swim(); }
    public void dive() { dive.dive(); }
    public void eat() { System.out.println("Eating fish"); }
    // No fly() — doesn't implement Flyable. Compile-time safe.
}

class Albatross implements Flyable, Swimmable, Eatable {
    private final Flyable flight = new GlideFlight();
    private final Swimmable swim = new SurfaceSwim();
    public void fly() { flight.fly(); }
    public void swim() { swim.swim(); }
    public void eat() { System.out.println("Eating squid"); }
}

public class CompositionDemo {
    public static void main(String[] args) {
        Penguin penguin = new Penguin();
        penguin.swim(); penguin.dive(); penguin.eat();
        // penguin.fly() — doesn't compile! Type-safe.

        Albatross albatross = new Albatross();
        albatross.fly(); albatross.swim();

        // Polymorphism still works
        List<Eatable> birds = List.of(new Sparrow(), new Penguin(), new Albatross());
        birds.forEach(Eatable::eat);
    }
}
```

**Key points:**
- Compile-time safety: Penguin doesn't implement Flyable, so calling `.fly()` is a compile error — not a runtime surprise.
- Adding `Diveable` required zero changes to existing Sparrow or Albatross classes (Open/Closed).
- Strategy objects (WingFlight, GlideFlight) can be swapped at runtime if needed.
- "Favor composition over inheritance" — Joshua Bloch, Effective Java Item 18.

---

## Exercise 38. Design Patterns — Strategy, Observer, Builder, Decorator Combined

```java
import java.util.*;

// --- Builder: Notification ---
enum Priority { LOW, MEDIUM, HIGH }

class Notification {
    private final String to;
    private final String subject;
    private final String body;
    private final Priority priority;

    private Notification(Builder b) { this.to = b.to; this.subject = b.subject; this.body = b.body; this.priority = b.priority; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Priority getPriority() { return priority; }
    public static Builder builder() { return new Builder(); }

    static class Builder {
        private String to, subject, body;
        private Priority priority = Priority.MEDIUM;
        public Builder to(String to) { this.to = to; return this; }
        public Builder subject(String s) { this.subject = s; return this; }
        public Builder body(String b) { this.body = b; return this; }
        public Builder priority(Priority p) { this.priority = p; return this; }
        public Notification build() { Objects.requireNonNull(to); return new Notification(this); }
    }
}

// --- Strategy: Channel ---
interface NotificationChannel {
    void send(Notification notification);
}

class EmailChannel implements NotificationChannel {
    public void send(Notification n) { System.out.println("[EMAIL] To: " + n.getTo() + " | " + n.getSubject()); }
}

class SmsChannel implements NotificationChannel {
    public void send(Notification n) { System.out.println("[SMS] To: " + n.getTo() + " | " + n.getBody()); }
}

// --- Decorator: wraps any channel ---
class LoggingDecorator implements NotificationChannel {
    private final NotificationChannel delegate;
    public LoggingDecorator(NotificationChannel delegate) { this.delegate = delegate; }
    public void send(Notification n) {
        System.out.println("[LOG] Sending " + n.getPriority() + " notification to " + n.getTo());
        delegate.send(n);
    }
}

class RetryDecorator implements NotificationChannel {
    private final NotificationChannel delegate;
    private final int maxRetries;
    public RetryDecorator(NotificationChannel delegate, int maxRetries) {
        this.delegate = delegate; this.maxRetries = maxRetries;
    }
    public void send(Notification n) {
        for (int i = 1; i <= maxRetries; i++) {
            try { delegate.send(n); return; }
            catch (RuntimeException e) { System.out.println("[RETRY] Attempt " + i + " failed"); }
        }
        throw new RuntimeException("All " + maxRetries + " attempts failed");
    }
}

// --- Observer ---
interface NotificationListener {
    void onSent(Notification notification);
}

class NotificationService {
    private final List<NotificationListener> listeners = new ArrayList<>();
    private final NotificationChannel channel;

    public NotificationService(NotificationChannel channel) { this.channel = channel; }
    public void subscribe(NotificationListener listener) { listeners.add(listener); }

    public void send(Notification notification) {
        channel.send(notification);
        listeners.forEach(l -> l.onSent(notification));
    }
}

public class PatternsDemo {
    public static void main(String[] args) {
        // Builder
        Notification notification = Notification.builder()
            .to("user@example.com")
            .subject("Order Shipped")
            .body("Your package is on the way")
            .priority(Priority.HIGH)
            .build();

        // Decorator stack: Retry → Logging → Email
        NotificationChannel channel = new RetryDecorator(
            new LoggingDecorator(new EmailChannel()), 3
        );

        // Observer
        NotificationService service = new NotificationService(channel);
        service.subscribe(n -> System.out.println("[AUDIT] Sent to " + n.getTo()));

        service.send(notification);
    }
}
```

**Key points:**
- **Builder:** Solves telescoping constructors, enforces required fields, immutable result.
- **Strategy:** Swap channels without changing client code.
- **Decorator:** Stack behaviors transparently — same interface, added functionality.
- **Observer:** Decouple side effects (auditing, analytics) from core logic.
- These compose naturally — that's the goal of good OO design.

---

## Exercise 39. Memory and GC — Identify the Leak

```java
import java.util.*;

public class CacheComparison {
    // Version 1: HashMap — never evicts (leak)
    static Map<Object, byte[]> leakyCache = new HashMap<>();

    // Version 2: WeakHashMap — entries GC'd when key has no strong refs
    static Map<Object, byte[]> weakCache = new WeakHashMap<>();

    // Version 3: Bounded LRU
    static Map<Object, byte[]> lruCache = new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Object, byte[]> eldest) {
            return size() > 1000;
        }
    };

    public static void main(String[] args) throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        System.out.println("--- HashMap (leaky) ---");
        fillCache(leakyCache);
        System.gc(); Thread.sleep(100);
        System.out.println("Size after GC: " + leakyCache.size()); // 100,000 — nothing collected
        System.out.println("Used MB: " + usedMB(runtime));

        System.out.println("\n--- WeakHashMap ---");
        fillCache(weakCache);
        System.gc(); Thread.sleep(100);
        System.out.println("Size after GC: " + weakCache.size()); // ~0 — keys had no strong refs
        System.out.println("Used MB: " + usedMB(runtime));

        System.out.println("\n--- LRU LinkedHashMap ---");
        fillCache(lruCache);
        System.gc(); Thread.sleep(100);
        System.out.println("Size after GC: " + lruCache.size()); // 1000 — capped
        System.out.println("Used MB: " + usedMB(runtime));
    }

    private static void fillCache(Map<Object, byte[]> cache) {
        for (int i = 0; i < 100_000; i++) {
            cache.put(new String("key-" + i), new byte[100]);
        }
    }

    private static long usedMB(Runtime r) {
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
    }
}
```

**Key points:**
- **HashMap leak:** Strong references keep entries alive forever. If keys are abandoned, entries are orphaned but not collected.
- **WeakHashMap:** Keys are `WeakReference`. When no other strong ref exists to the key, entry becomes eligible for GC.
- **LRU LinkedHashMap:** Bounded size, evicts oldest/least-accessed entries.
- Common leak sources: static collections, listeners never unregistered, inner classes holding outer refs, unclosed streams.
- In real apps, use Caffeine or Guava Cache instead of rolling your own.

---

## Exercise 40. Serialization and Object Cloning

```java
import java.io.*;
import java.util.*;

class Employee implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient String ssn; // Not serialized

    public Employee(String name, String ssn) { this.name = name; this.ssn = ssn; }
    public Employee(Employee other) { this.name = other.name; this.ssn = other.ssn; } // Copy constructor
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    @Override public String toString() { return "Employee{" + name + ", ssn=" + ssn + "}"; }

    @Override
    public Employee clone() {
        try { return (Employee) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }
}

class Department implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private String name;
    private List<Employee> employees;

    public Department(String name, List<Employee> employees) {
        this.name = name;
        this.employees = new ArrayList<>(employees);
    }

    // Copy constructor — deep copy
    public Department(Department other) {
        this.name = other.name;
        this.employees = new ArrayList<>();
        for (Employee emp : other.employees) {
            this.employees.add(new Employee(emp));
        }
    }

    // Clone — must deep-copy mutable fields
    @Override
    public Department clone() {
        try {
            Department cloned = (Department) super.clone(); // Shallow
            // Deep copy the list
            cloned.employees = new ArrayList<>();
            for (Employee emp : this.employees) {
                cloned.employees.add(emp.clone());
            }
            return cloned;
        } catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }

    public List<Employee> getEmployees() { return employees; }
    @Override public String toString() { return "Department{" + name + ", " + employees + "}"; }
}

public class DeepCopyUtils {
    // Approach 1: Serialization-based deep copy
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deepCopy(T object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new ObjectOutputStream(bos).writeObject(object);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            return (T) new ObjectInputStream(bis).readObject();
        } catch (Exception e) { throw new RuntimeException("Deep copy failed", e); }
    }

    public static void main(String[] args) {
        Department original = new Department("Engineering", List.of(
            new Employee("Alice", "123-45-6789"),
            new Employee("Bob", "987-65-4321")
        ));

        // Approach 1: Serialization
        Department copy1 = deepCopy(original);
        copy1.getEmployees().get(0).setName("MODIFIED");
        System.out.println("After serialization copy modified:");
        System.out.println("  Original: " + original.getEmployees().get(0).getName()); // Alice
        System.out.println("  Copy: " + copy1.getEmployees().get(0).getName());         // MODIFIED

        // Approach 2: Copy constructor
        Department copy2 = new Department(original);
        copy2.getEmployees().get(0).setName("CHANGED");
        System.out.println("\nAfter copy-constructor modified:");
        System.out.println("  Original: " + original.getEmployees().get(0).getName()); // Alice
        System.out.println("  Copy: " + copy2.getEmployees().get(0).getName());         // CHANGED

        // Approach 3: Clone
        Department copy3 = original.clone();
        copy3.getEmployees().get(0).setName("CLONED");
        System.out.println("\nAfter clone modified:");
        System.out.println("  Original: " + original.getEmployees().get(0).getName()); // Alice
        System.out.println("  Copy: " + copy3.getEmployees().get(0).getName());         // CLONED

        // transient demo
        System.out.println("\nOriginal employee: " + original.getEmployees().get(0)); // ssn=123...
        Department serialized = deepCopy(original);
        System.out.println("After serialization: " + serialized.getEmployees().get(0)); // ssn=null (transient)
    }
}
```

**Key points:**
- `transient` fields are skipped during serialization (passwords, SSNs, caches)
- **Serialization copy:** Easy, works for any graph, but slow and requires `Serializable`
- **Copy constructor:** Explicit, fast, no magic — preferred in modern Java
- **Clone pitfall:** `super.clone()` does a shallow copy. Mutable fields (lists, arrays, nested objects) must be manually deep-copied.
- `serialVersionUID` prevents `InvalidClassException` when class evolves
