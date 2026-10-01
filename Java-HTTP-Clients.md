# Java HTTP Clients: HttpClient vs RestTemplate vs RestClient vs WebClient vs Feign vs @HttpExchange

## Summary

All six answer the same question — *how does my Java/Spring app make an outbound
HTTP call to another service?* — but they differ on two axes:

- **Imperative vs declarative** — do you write the call step by step, or declare an
  interface and let the framework generate the call?
- **Blocking vs reactive** — does the calling thread wait for the response, or is
  the call non-blocking?

- **JDK `HttpClient`** — Built into Java 11+. No dependencies. Sync and async, HTTP/2.
- **RestTemplate** — Spring's classic synchronous client. In maintenance mode.
- **RestClient** — Spring 6.1+ synchronous client with a modern fluent API. The
  current replacement for RestTemplate.
- **WebClient** — Spring's reactive, non-blocking client (WebFlux). Can block if needed.
- **Feign** — Spring Cloud's declarative client: annotate an interface, no call code.
- **`@HttpExchange`** — Spring 6+ declarative HTTP Interface built into core Spring
  (Feign-style, no Spring Cloud needed).

## The Two Axes

```
                 IMPERATIVE                        DECLARATIVE
             (you write the call)            (you declare an interface)

  BLOCKING   RestTemplate                    Feign
             RestClient                      @HttpExchange (sync proxy)
             JDK HttpClient (sync)

  REACTIVE   WebClient                        @HttpExchange (reactive proxy)
             JDK HttpClient (async*)
```

\* The JDK client's async mode returns `CompletableFuture`, which is non-blocking
but not reactive streams (no backpressure) the way WebClient's `Mono`/`Flux` are.

## Core Comparison

| Client | Style | Blocking / Reactive | Module / Dependency | Status | Best fit |
|--------|-------|---------------------|---------------------|--------|----------|
| **JDK `HttpClient`** | Imperative | Both (sync + `CompletableFuture`) | `java.net.http` (JDK 11+) | Current | No-dependency calls; libraries; non-Spring code |
| **RestTemplate** | Imperative | Blocking | `spring-web` | Maintenance mode | Existing code; simple sync calls |
| **RestClient** | Imperative (fluent) | Blocking | `spring-web` (6.1+) | Current | New synchronous Spring code (RestTemplate's successor) |
| **WebClient** | Imperative (fluent) | Reactive (optional `.block()`) | `spring-webflux` | Current | Reactive apps, streaming, high concurrency |
| **Feign** | Declarative | Blocking | `spring-cloud-openfeign` | Current (Spring Cloud) | Microservices with discovery / load balancing |
| **`@HttpExchange`** | Declarative | Both (sync or reactive proxy) | `spring-web` (6+) | Current | New declarative clients without Spring Cloud |

## What Each Looks Like

**JDK HttpClient** — imperative, no framework. (This is what the webhook PoC uses in
`TokenProvider` and the auth tests.)

```java
HttpClient http = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/oauth/token"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form))
        .build();
HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
```

**RestTemplate** — the classic imperative Spring client.

```java
StockEvent event = restTemplate.getForObject(url, StockEvent.class);
```

**RestClient** — modern fluent synchronous Spring client. (The webhook PoC's
`WebhookRegistrationService` and `StockEventSimulator` use `RestClient`.)

```java
StockEvent event = restClient.get()
        .uri(url)
        .retrieve()
        .body(StockEvent.class);
```

**WebClient** — reactive, non-blocking.

```java
Mono<StockEvent> event = webClient.get()
        .uri(url)
        .retrieve()
        .bodyToMono(StockEvent.class);   // .block() to go synchronous
```

**Feign** — declarative; the implementation is generated from the interface.

```java
@FeignClient(name = "pricing", url = "http://localhost:8080")
interface PricingClient {
    @GetMapping("/stocks/{symbol}")
    StockEvent getStock(@PathVariable String symbol);
}
```

**`@HttpExchange`** — declarative, built into core Spring (no Spring Cloud).

```java
interface PricingClient {
    @GetExchange("/stocks/{symbol}")
    StockEvent getStock(@PathVariable String symbol);
}
// Backed at runtime by an HttpServiceProxyFactory over a RestClient/WebClient.
```

## How to Choose

**JDK `HttpClient`** — You want zero dependencies, you're writing a library, or
you're outside Spring. Good default for simple calls and for code that shouldn't
pull in a web framework.

**RestTemplate** — You're maintaining existing code that already uses it. Fine to
keep, but prefer RestClient for anything new (RestTemplate gets no new features).

**RestClient** — Your default for **new synchronous** Spring code. Modern fluent
API, same blocking model as RestTemplate, no reactive stack required.

**WebClient** — You're in a reactive (WebFlux) app, need streaming, or want
non-blocking I/O for high-concurrency fan-out. Also usable synchronously via
`.block()`, but don't adopt the reactive stack just for that — use RestClient.

**Feign** — You have **many** downstream clients in a microservice fleet and want
them as tidy annotated interfaces, especially with service discovery (Eureka) and
client-side load balancing (Spring Cloud LoadBalancer).

**`@HttpExchange`** — You want Feign-style declarative clients but **without**
Spring Cloud. It's core Spring (6+/Boot 3+), so it's the forward-looking choice for
new declarative clients in a plain Spring Boot app.

## Decision Shortcuts

- New sync call, plain Spring Boot → **RestClient**.
- New declarative client, no Spring Cloud → **`@HttpExchange`**.
- Declarative client in a Spring Cloud microservice mesh → **Feign**.
- Reactive / streaming / non-blocking → **WebClient**.
- No framework or a library with no deps → **JDK `HttpClient`**.
- Already have working RestTemplate code → leave it; migrate to RestClient when touched.

## Common Misconceptions

1. **"RestTemplate is deprecated."** It is in **maintenance mode** (no new features),
   not removed. Existing code keeps working; new code should prefer RestClient.
2. **"You need WebClient to make modern Spring calls."** RestClient gives the same
   fluent style synchronously, without pulling in the reactive stack.
3. **"Feign and `@HttpExchange` are unrelated."** They solve the same problem
   (declarative interface-based clients). `@HttpExchange` is the core-Spring take;
   Feign is the Spring Cloud one that predates it.
4. **"Declarative clients are a different transport."** They are not — Feign and
   `@HttpExchange` sit **on top of** a lower-level client (RestClient/WebClient or
   OkHttp/Apache HttpClient). You're choosing an abstraction level, not a protocol.
5. **"The JDK client is too low-level to bother with."** For simple calls it's
   perfectly ergonomic, ships with the JDK, and avoids a dependency — which is why
   this workspace's webhook PoC uses it for token fetching and tests.

## Related / Out of Scope

These round out the landscape but sit outside the six app-level Spring choices above:

- **OkHttp** (Square) and **Apache HttpClient** — mature low-level transports. Rarely
  used directly in app code today; they more often sit *underneath* RestClient,
  WebClient, or Feign as the configured HTTP engine.
- **Retrofit** (Square) — declarative like Feign/`@HttpExchange`, but from the
  Android/Square ecosystem rather than Spring.
