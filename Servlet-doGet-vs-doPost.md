# Servlet `doGet` vs `doPost`

## Summary

- **`doGet`** — the method `HttpServlet` dispatches to for an HTTP `GET` request.
  Meant for **safe, idempotent** retrieval: reading or displaying data with no
  state change.
- **`doPost`** — the method `HttpServlet` dispatches to for an HTTP `POST` request.
  Meant for operations that **change state**: creating records, submitting forms,
  uploading files, triggering actions.

The difference isn't the Java API — the two methods have identical signatures.
The difference is the **semantics of the HTTP methods** they serve.

## Identical Signatures

```java
protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException { ... }

protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException { ... }
```

`doPut`, `doDelete`, `doHead`, `doOptions`, and `doTrace` share the same shape —
only the name differs. (See [Why the signatures look alike](#why-the-signatures-look-alike).)

## GET vs POST: the Real Differences

| Aspect | GET (`doGet`) | POST (`doPost`) |
|--------|---------------|-----------------|
| Where data lives | URL query string (`/search?q=foo`) | Request body |
| Visibility | In address bar, history, bookmarks, server logs | Not in the URL (not encrypted — only TLS does that) |
| Size limit | Bounded by URL length (~2–8 KB typical) | Effectively unbounded (server-configured) |
| Safe? (no side effects) | Yes (by contract) | No |
| Idempotent? (repeat = same effect) | Yes | No |
| Cacheable | Yes (browsers, proxies, CDNs) | Generally not |
| Bookmark / replay | Yes | No (browser warns "resend form?") |
| Has a request body | No | Yes |

## Reading the Data in the Servlet

- **Simple form parameters** come from `req.getParameter(...)` for **both** verbs.
  The container parses the query string for GET and an
  `application/x-www-form-urlencoded` body for POST.
- **Raw / JSON bodies** are read with `req.getReader()` or `req.getInputStream()` —
  only meaningful for POST (and PUT); a GET has no body.

## Which to Override

Match the HTTP method to intent, not convenience:

- **`doGet`** — retrieving/displaying data: show a page, run a search, fetch a
  resource. Should be safe and repeatable.
- **`doPost`** — submitting data that changes state: create a record, process a
  form, upload a file, trigger an action.

```java
// GET /stocks?symbol=AAPL  -> read-only lookup
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String symbol = req.getParameter("symbol");          // from the query string
    resp.getWriter().write(lookupPrice(symbol));
}

// POST /orders  (body: JSON or form fields) -> creates something
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String body = req.getReader().lines().collect(Collectors.joining());  // read the body
    Order created = createOrder(body);
    resp.setStatus(HttpServletResponse.SC_CREATED);       // 201
}
```

**Caveat:** none of this is enforced by the servlet API. You *can* mutate state in
`doGet` or return data from `doPost` — the method just mirrors the HTTP verb. The
GET-is-safe/idempotent contract is a convention that browsers, proxies, caches, and
crawlers rely on. Breaking it (e.g., a "delete" link behind a GET that a crawler
then follows) causes real bugs.

## Why the Signatures Look Alike

All the `doXxx` methods are variations of one template method on `HttpServlet`,
deliberately given the same shape so the dispatch mechanism can treat them
uniformly.

Every request first hits `HttpServlet.service(...)`, which reads the HTTP method
and routes to the matching handler:

```java
protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    String method = req.getMethod();
    if (method.equals("GET"))         doGet(req, resp);
    else if (method.equals("POST"))   doPost(req, resp);
    else if (method.equals("PUT"))    doPut(req, resp);
    else if (method.equals("DELETE")) doDelete(req, resp);
    // ... doHead, doOptions, doTrace
}
```

For `service` to call any of them interchangeably, they must accept the same
inputs and throw the same checked exceptions. The deeper reasons:

- **Two abstractions model every exchange.** `HttpServletRequest` represents *any*
  incoming request and `HttpServletResponse` *any* outgoing response, regardless of
  verb. The method, headers, query string, and body all live *inside* the `request`
  object — so the handler never needs different parameters per verb. The
  differences are accessed through methods on `req` (`getParameter` vs `getReader`),
  not expressed in the signature.
- **It's the Template Method pattern.** `HttpServlet` defines the fixed algorithm
  (`service` does the dispatch) and leaves per-verb hooks for you to override.
  Uniform hooks let the base class invoke them through one call site. The default
  implementations return `405 Method Not Allowed` — so an un-overridden `doPost`
  answers a POST with a 405 automatically.
- **The signature isn't where the verb lives.** The HTTP method is *data*
  (`req.getMethod()` returns `"GET"`/`"POST"`), and the servlet API turns that data
  into a *method name* via dispatch. The verb is encoded in **which** method is
  called, not in **what** it accepts.

A practical corollary: because the signatures are identical, a common idiom is to
have one verb delegate to the other when you want identical handling:

```java
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    doGet(req, resp);   // handle GET and POST identically
}
```

That trick only works *because* the signatures match.

## Common Misconceptions

1. **"POST is encrypted / more secure than GET."** Neither is encrypted on its own;
   only HTTPS/TLS encrypts the transport. POST just keeps data out of the URL (and
   thus out of logs and history).
2. **"GET can't send data."** It can, via the query string — just limited in size
   and visible in the URL.
3. **"The servlet enforces safe/idempotent GET."** It does not. That contract is a
   convention your code must honor; the API won't stop you from breaking it.
4. **"Different verbs need different method signatures."** They don't — the verb is
   the method *name*; the inputs are always `(HttpServletRequest, HttpServletResponse)`.
