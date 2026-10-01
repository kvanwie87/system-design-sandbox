# Servlet Lifecycle: `init`, `service`, `destroy`

## Summary

These three are the servlet **lifecycle methods**, declared on the `Servlet`
interface and managed by the container (Tomcat, Jetty, etc.). You don't call them —
the container does, at defined points in the servlet's life.

- **`init()`** — called **once** when the servlet is loaded; set up resources.
- **`service()`** — called **per request** (concurrently); handle each request.
- **`destroy()`** — called **once** when the servlet is unloaded; release resources.

There is one servlet instance (the singleton): one `init`, one `destroy`, and
`service` in between for every request.

## The Lifecycle

```
   load (startup or first request)
            │
            ▼
       init()          ── called ONCE ──  set up resources
            │
            ▼
   ┌──  service()  ──┐  ── called MANY times, CONCURRENTLY ──  handle each request
   │   (per request) │
   └─────────────────┘
            │
            ▼
      destroy()        ── called ONCE ──  release resources
            │
            ▼
   instance eligible for GC
```

## `init(ServletConfig config)`

- **Called once**, when the container first loads the servlet. By default that is on
  the **first request**; with `<load-on-startup>` (or `@WebServlet(loadOnStartup = 1)`)
  it happens at **application startup** instead.
- **Purpose:** one-time setup — open a database connection pool, load configuration,
  build an expensive shared resource, read init parameters.
- Runs **before** any request can enter `service`, so setup done here is visible to all
  later requests. This is the safe place to establish read-only shared state.
- If `init` throws `ServletException` (or `UnavailableException`), the servlet is **not**
  put into service.
- Read init parameters via `getServletConfig().getInitParameter(...)`.

```java
private DataSource dataSource;   // set once in init, read-only afterward

@Override
public void init(ServletConfig config) throws ServletException {
    super.init(config);                        // keep the default behavior
    String jndi = config.getInitParameter("dataSourceJndi");
    this.dataSource = lookupDataSource(jndi);  // expensive, done once
}
```

There is also a convenience no-arg `init()` (from `GenericServlet`) you can override
instead, so you don't have to call `super.init(config)` yourself.

## `service(ServletRequest req, ServletResponse resp)`

- **Called once per request**, and concurrently on the container's thread pool.
- **Purpose:** handle the request. In `HttpServlet`, `service` is the **dispatcher**: it
  reads the HTTP method and routes to `doGet`, `doPost`, `doPut`, etc.
- **You usually don't override `service`** — override the `doXxx` methods instead.
  Overriding `service` directly bypasses that dispatch (and HTTP niceties like automatic
  `HEAD`/`OPTIONS` and `405` handling), so only do it when you deliberately want to
  handle all methods uniformly.

```java
// HttpServlet.service, conceptually:
protected void service(HttpServletRequest req, HttpServletResponse resp) {
    switch (req.getMethod()) {
        case "GET"  -> doGet(req, resp);
        case "POST" -> doPost(req, resp);
        // ... doPut, doDelete, doHead, doOptions, doTrace
    }
}
```

## `destroy()`

- **Called once**, when the container takes the servlet out of service — application
  shutdown, undeploy, or redeploy/reload.
- **Purpose:** clean up what `init` created — close connection pools, stop background
  threads, flush/close files, release caches. The mirror image of `init`.
- The container only calls `destroy` **after** in-flight `service` calls have finished
  (or timed out). Any **background threads you started yourself** must be stopped here,
  or you leak them across redeploys.
- **Not guaranteed on a hard kill:** if the JVM is killed abruptly (`kill -9`, power
  loss), `destroy` does **not** run. It is for graceful cleanup, not a durability
  guarantee.

```java
@Override
public void destroy() {
    scheduler.shutdownNow();   // stop threads you started
    closeQuietly(dataSource);  // release resources from init
}
```

## Quick Comparison

| Method | How often | When | Typical use |
|--------|-----------|------|-------------|
| `init()` | Once | On load (first request, or startup with `load-on-startup`) | Set up shared, read-only resources |
| `service()` | Per request | Every request | Dispatch to `doGet`/`doPost`; handle the request |
| `destroy()` | Once | On unload / shutdown / redeploy | Release resources, stop your threads |

## Mental Model

Set up once (`init`), serve many times (`service`), tear down once (`destroy`).
