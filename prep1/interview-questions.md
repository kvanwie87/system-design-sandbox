# Interview Questions by Required Skill

Common interview questions mapped to each skill in `required-skills.md`, with concise answers you can review before an interview. Servlets is covered most deeply since that's the flagged prep focus.

**How to use:** cover the answers, ask yourself the question, then check. The starred (⭐) questions are the ones most likely to come up.

---

## 1. J2EE Platform Fundamentals (WAR/EAR, Maven, deployment)

**Q: What's the difference between a WAR and an EAR?** ⭐
A WAR (Web Application Archive) packages a single web module — servlets, JSPs, static resources, and `WEB-INF/`. An EAR (Enterprise Archive) bundles multiple modules (one or more WARs plus EJB JARs and shared libraries) for deployment as one enterprise application. Most modern apps ship as WARs (or executable JARs with embedded servers); EARs are legacy-heavy.

**Q: What lives under `WEB-INF/` and why does it matter?**
`WEB-INF/` holds `web.xml`, `classes/` (compiled code), and `lib/` (dependency JARs). It's **not** directly served to clients — resources there can't be requested over HTTP, which makes it the safe place for code and config.

**Q: What does Maven scope `provided` mean, and when do you use it?** ⭐
A `provided` dependency is needed to compile but is supplied by the runtime (container), so it's not bundled into the artifact. The servlet API is the classic example — Tomcat provides it, so bundling it would cause classloader conflicts.

**Q: What's the difference between `mvn package` and `mvn install`?**
`package` builds the artifact into `target/`. `install` does that and also copies it into your local `~/.m2` repository so other local projects can depend on it.

**Q: What is the Maven build lifecycle?**
An ordered sequence of phases — `validate → compile → test → package → verify → install → deploy`. Running a phase runs all prior phases too.

---

## 2. Java Servlets ⭐ (primary focus)

**Q: Walk me through the servlet lifecycle.** ⭐⭐
Three phases: `init()` is called once when the container loads/instantiates the servlet (one-time setup); `service()` is called for every request and dispatches to `doGet`/`doPost`/etc.; `destroy()` is called once before the container removes the servlet (cleanup). One instance handles many requests.

**Q: Are servlets thread-safe?** ⭐⭐
No, not by default. The container creates **one servlet instance** and uses a thread pool to serve concurrent requests through it. Instance/static fields are shared across threads, so mutable shared state must be synchronized or avoided. Local variables inside `doGet`/`doPost` are thread-safe because each thread gets its own stack.

**Q: Difference between `ServletConfig` and `ServletContext`?** ⭐
`ServletConfig` is per-servlet — init parameters for one servlet. `ServletContext` is per-application (per web app) — shared across all servlets, used for app-wide init params and attributes. One context, many configs.

**Q: Forward vs redirect?** ⭐
`RequestDispatcher.forward()` is server-side: control passes to another resource within the same app, the URL doesn't change, and the request/response are reused. `sendRedirect()` is client-side: a 302 tells the browser to make a new request to a new URL (can be external). Forward is one round trip; redirect is two.

**Q: `getWriter()` vs `getOutputStream()` — can you use both?** ⭐
`getWriter()` returns a `PrintWriter` for character/text output; `getOutputStream()` returns a `ServletOutputStream` for binary data. You can use only **one** per response — calling both throws `IllegalStateException`.

**Q: How do filters differ from servlets?** ⭐
Filters intercept requests/responses for cross-cutting concerns (auth, logging, compression, encoding) without generating the primary response. They run in a chain (`doFilter` calls `chain.doFilter` to continue). Servlets produce the response; filters wrap around them.

**Q: `web.xml` vs `@WebServlet` annotations?**
Both map servlets to URL patterns. `web.xml` is centralized XML config (easier to see all mappings at once, override without recompiling); annotations keep config next to code (less boilerplate). They can coexist; `web.xml` can override annotations.

**Q: What is `load-on-startup`?**
By default servlets are lazily instantiated on first request. `<load-on-startup>` (or the annotation attribute) forces eager `init()` at container startup; the number sets ordering (lower loads first).

**Q: What's the DispatcherServlet in Spring MVC?** ⭐
It's a single front-controller servlet that receives all requests and dispatches them to controllers. Spring MVC is built on top of the Servlet API — the DispatcherServlet is just a servlet, which is why servlet fundamentals transfer directly to Spring.

**Q: How do sessions work without server-side memory of the client?**
`HttpSession` is keyed by a session ID sent to the client, usually via a `JSESSIONID` cookie (or URL rewriting as fallback). The server stores session state and looks it up by that ID on each request.

**Q: `javax.servlet` vs `jakarta.servlet`?** ⭐
Same API, renamed package. Java EE moved to the Eclipse Foundation as Jakarta EE; from Jakarta EE 9 the namespace changed `javax.*` → `jakarta.*`. Tomcat 9 and earlier use `javax`; Tomcat 10+ use `jakarta`. Mixing them causes `ClassNotFoundException`/`NoClassDefFoundError`.

---

## 3. SQL Proficiency (Db2)

**Q: Difference between `WHERE` and `HAVING`?** ⭐
`WHERE` filters rows before aggregation; `HAVING` filters groups after `GROUP BY`. Aggregate functions (`SUM`, `COUNT`) can be used in `HAVING` but not `WHERE`.

**Q: Explain the join types.** ⭐
`INNER JOIN` returns matching rows from both tables. `LEFT/RIGHT OUTER JOIN` returns all rows from one side plus matches (NULLs where none). `FULL OUTER JOIN` returns all rows from both. `CROSS JOIN` is the Cartesian product.

**Q: How do you read/trace a complex legacy query?**
Start from the `FROM`/`JOIN` clauses to map the data sources and relationships, then `WHERE` for filtering, then `GROUP BY`/`HAVING`, then the `SELECT` list, and finally `ORDER BY`. Read subqueries/CTEs innermost-first. Identify what each join and filter contributes to the final row set.

**Q: What's a CTE and why use one?**
A Common Table Expression (`WITH name AS (...)`) names a subquery so it can be referenced and reused, improving readability of complex queries. Db2 supports recursive CTEs for hierarchical data.

**Q: What's the logical order of SQL execution?**
`FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT/FETCH`. This is why you can't reference a `SELECT` alias in `WHERE` (SELECT runs later).

**Q: `FETCH FIRST n ROWS ONLY` — what is it?**
Db2's standard way to limit result rows (equivalent to `LIMIT` in MySQL/Postgres). Often paired with `ORDER BY` for deterministic results.

---

## 4. REST API Consumption from Java (RestTemplate / HttpClient, OAuth2)

**Q: RestTemplate vs the Java 11 HttpClient vs WebClient?** ⭐
`RestTemplate` is Spring's classic synchronous client (maintenance mode, still widely used). Java 11's `HttpClient` is the built-in modern client supporting sync and async, HTTP/2, no external deps. `WebClient` is Spring's reactive, non-blocking client and the recommended choice for new Spring code.

**Q: Explain the OAuth2 client credentials flow.** ⭐⭐
Machine-to-machine auth with no user involved. The client POSTs its `client_id`, `client_secret`, `grant_type=client_credentials`, and `scope` to the token endpoint. It gets back an access token, then sends that as `Authorization: Bearer <token>` on API calls. Used for service-to-service integration.

**Q: How do you handle token expiry and caching?** ⭐
Tokens have a lifetime (`expires_in`). Cache the token and reuse it until shortly before expiry (refresh a bit early to avoid races), then request a new one. Don't fetch a token per request — that's slow and can hit rate limits.

**Q: How do you handle errors from a REST call?**
Check status codes: 4xx = client error (bad request, auth, not found), 5xx = server error. Implement retries with backoff for transient 5xx/timeouts, but not for 4xx. Set connect and read timeouts so a hung downstream doesn't block your threads.

**Q: Where should secrets like client_id/secret live?**
Never hardcoded or committed. Use environment variables, a secrets manager, or externalized config — injected at runtime.

---

## 5. Document/Binary Streaming over REST ⭐

**Q: How do you stream a large file without loading it into memory?** ⭐⭐
Read from the source `InputStream` and write to `response.getOutputStream()` in a fixed-size buffer loop (e.g., 8KB), rather than reading the whole file into a `byte[]`. This keeps memory constant regardless of file size. Use try-with-resources to close streams.

**Q: What headers matter for a file download?** ⭐
`Content-Type` (the MIME type), `Content-Disposition: attachment; filename="..."` (prompts download vs inline display), and `Content-Length` when the size is known. Without `Content-Length`, the server typically uses chunked transfer encoding.

**Q: What is chunked transfer encoding and when is it used?**
When the response size isn't known up front, the server sends the body in chunks with `Transfer-Encoding: chunked` instead of `Content-Length`. Common when streaming generated or piped content.

**Q: How do you handle a multipart file upload in a servlet?** ⭐
Annotate the servlet with `@MultipartConfig`, then read parts via `request.getPart("field")` or `request.getParts()`. Each `Part` exposes its `InputStream`, filename, and headers. `multipart/form-data` is the encoding used for file uploads.

**Q: Why use buffered streams?**
They reduce the number of underlying I/O system calls by batching reads/writes, which significantly improves throughput for large transfers.

**Q: Why is closing streams important, and how do you guarantee it?**
Unclosed streams leak file handles/connections and can exhaust resources. Use try-with-resources (`try (InputStream in = ...)`) so streams close automatically even on exceptions.

---

## 6. Legacy Code Comprehension

**Q: How do you approach unfamiliar, undocumented code before changing it?** ⭐
Read before editing: trace the entry points and follow the execution path. Identify inputs, outputs, and side effects. Reconstruct intended behavior from tests (if any), naming, and data flow. Make the smallest possible change, verify against existing behavior, and avoid refactoring unrelated code in the same change.

**Q: How do you safely change behavior you don't fully understand?**
Add characterization tests that capture current behavior first, so you have a safety net. Change incrementally and verify at each step. Preserve existing behavior unless the change is the explicit goal.

**Q: How do you reconstruct intent when there are no comments?**
Follow the data: where values come from, how they're transformed, where they go. Check callers and callees, version history (git blame/log) for the "why," and any related config or SQL.

---

## 7. Git-Based Version Control Workflow

**Q: `merge` vs `rebase`?** ⭐
`merge` combines branches with a merge commit, preserving true history. `rebase` replays your commits on top of another branch for a linear history. Rule of thumb: don't rebase commits that have been pushed/shared, since it rewrites history.

**Q: What's a typical feature-branch workflow?** ⭐
Branch off `main`, commit focused changes, push the branch, open a pull request for review, address feedback, then merge (often squash) back into `main`. Never commit directly to `main` on a team.

**Q: `git fetch` vs `git pull`?**
`fetch` downloads remote changes without modifying your working branch. `pull` is `fetch` + `merge` (or `+ rebase`) into your current branch.

**Q: How do you resolve a merge conflict?**
Git marks conflicting regions with `<<<<<<<`, `=======`, `>>>>>>>`. Edit to the desired result, remove the markers, `git add` the resolved files, then complete the merge/rebase.

**Q: How do you undo a commit that's already pushed?**
Use `git revert` to create a new commit that undoes the change — safe for shared history. Avoid `reset --hard` + force-push on shared branches.

---

## 8. Nice to Have: Spring / Spring Boot

**Q: What problem does Spring Boot solve over plain Spring?** ⭐
Auto-configuration, sensible defaults, embedded servers (no external Tomcat needed), and starter dependencies. It removes most boilerplate XML/config so you can run an app with minimal setup.

**Q: What is dependency injection and why does Spring use it?** ⭐
The framework supplies (injects) an object's dependencies rather than the object creating them. This decouples components, makes them easier to test (inject mocks), and centralizes wiring in the container.

**Q: How does a Spring `@RestController` relate to servlets?**
It's handled by the DispatcherServlet (a servlet). Spring maps request paths to controller methods and handles JSON serialization, so you write methods instead of raw `doGet`/`doPost` — but it's servlets underneath.

**Q: What are common Spring stereotype annotations?**
`@Component` (generic bean), `@Service` (business logic), `@Repository` (data access, with exception translation), `@Controller`/`@RestController` (web layer). All are `@Component` specializations picked up by component scanning.

---

## Rapid-Fire Self-Test

Cover the answers above and run through these out loud:

1. Servlet lifecycle — three methods, when each fires?
2. Are servlets thread-safe? Why/why not?
3. `ServletConfig` vs `ServletContext`?
4. Forward vs redirect?
5. Stream a 2GB file — how, without OOM?
6. OAuth2 client credentials — the four request fields?
7. `WHERE` vs `HAVING`?
8. `merge` vs `rebase` — when not to rebase?
9. `javax.servlet` vs `jakarta.servlet` — what changed and which Tomcat?
10. What is the DispatcherServlet?
