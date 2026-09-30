# Servlets Prep Plan

A focused, hands-on plan to get interview- and job-ready on Java Servlets. Tailored to the role's stack: J2EE, WAR/EAR packaging, Maven, Db2, REST consumption, and document/binary streaming.

**Suggested pace:** ~1 to 2 hours/day over 5 days, or compress into a focused weekend. Each phase ends with a small buildable exercise so the knowledge sticks.

> **Short on time?** See the [2-Day Crash Plan](#2-day-crash-plan) below for a compressed version.

---

## 2-Day Crash Plan

When you only have two days, the goal shifts from "know everything" to "cover what's most likely to come up and prove you can build with servlets." Strategy:

- **Protect** the streaming (Phase 5) and integration (Phase 6) topics — they map directly to the role's requirements.
- **Compress** fundamentals and lifecycle into a fast first pass.
- **Cut** the deep corners: extension/default URL patterns, listeners, URL rewriting, eager vs lazy loading nuances, and multiple config styles (pick annotations only).
- **Build 2 things, not 6** — a JSON echo servlet and a file-stream + OAuth2-to-REST servlet. These two demos exercise most of what an interviewer probes.

### Day 1 — Foundations + Core Handling (build a working servlet)

**Morning (~3 hrs)**
- [ ] Phase 0 setup, but pick **one** container/package pair and don't look back (Tomcat 10 + `jakarta.servlet` recommended). Get a Hello World WAR running.
- [ ] Phase 1 fundamentals, condensed: `HttpServlet`, `doGet`/`doPost`, `@WebServlet` mapping only (skip `web.xml` deep dive — just know it exists).
- [ ] Phase 2 lifecycle: memorize `init → service → destroy` + the "single instance, many threads → instance fields aren't thread-safe" point. This is the most common interview question — nail it.

**Afternoon (~3 hrs)**
- [ ] Phase 3 request/response: `getParameter`, reading a JSON body, status codes, `getWriter()` vs `getOutputStream()`, forward vs redirect (one sentence each).
- [ ] **Build #1:** a servlet that accepts JSON and returns JSON with correct status codes.
- [ ] Phase 4, minimal: just `HttpSession` basics and what a `Filter` is for (auth/logging). Skip listeners and cookies detail.

### Day 2 — The Role-Specific Skills (build the demo that matters)

**Morning (~3 hrs) ⭐**
- [ ] Phase 5 streaming — spend real time here. Binary `getOutputStream()`, `Content-Type` + `Content-Disposition`, buffered copy for large files (don't load into memory), multipart upload with `@MultipartConfig` + `request.getPart`.
- [ ] **Build #2a:** a file-download streaming servlet + a multipart upload servlet.

**Afternoon (~3 hrs)**
- [ ] Phase 6 integration, prioritized: OAuth2 client-credentials token fetch → attach `Bearer` header → call a downstream REST API with `HttpClient` → stream the response back.
- [ ] **Build #2b:** the OAuth2 → REST → stream servlet.
- [ ] Know the one-liners: WAR packaging via Maven, `WEB-INF` structure, and "Spring's DispatcherServlet is just a servlet."
- [ ] Phase 7 Q&A — run through the interview questions out loud. This is the highest-ROI final hour.

### What you're skipping (and why it's safe)
- `web.xml` config style — annotations cover the same ground; just acknowledge `web.xml` exists.
- Listeners, URL rewriting, cookie internals — rarely the focus, easy to mention conceptually.
- Db2/JDBC hands-on — you can describe connection/`DataSource`/resource-closing without a live DB if time runs out.

### If you only had ONE day
Do Day 1 morning (fundamentals + lifecycle) → jump straight to Phase 5 streaming → Phase 7 Q&A. Skip Build #1 and the OAuth2 demo; just read through the integration concepts.

---

## Phase 0 — Environment Setup (30–45 min)

Get a working loop before learning theory, so every concept can be tried immediately.

- [ ] Install a JDK (17+ recommended) and confirm `java -version`
- [ ] Install Maven and confirm `mvn -version`
- [ ] Install a servlet container: **Apache Tomcat 10.x** (note: Tomcat 10+ uses `jakarta.servlet.*`; Tomcat 9 uses `javax.servlet.*`)
- [ ] Create a skeleton Maven web app that packages a `.war`
- [ ] Deploy the WAR to Tomcat and hit a "Hello World" servlet in the browser

**Key gotcha:** `javax.servlet` (Java EE 8 / Tomcat 9 and earlier) vs `jakarta.servlet` (Jakarta EE 9+ / Tomcat 10+). Pick one and be consistent. Know that this rename exists — it trips people up.

**Deliverable:** a running `hello-servlet.war` on localhost.

---

## Phase 1 — Servlet Fundamentals (2–3 hrs)

The core mental model: what a servlet is and how the container drives it.

- [ ] What a servlet is: a Java class the container invokes to handle HTTP requests
- [ ] The **servlet container** role (Tomcat) — request routing, threading, lifecycle management
- [ ] `Servlet` interface → `GenericServlet` → `HttpServlet` hierarchy
- [ ] Overriding `doGet`, `doPost`, `doPut`, `doDelete`, and when each is called
- [ ] The `service()` method and how it dispatches to the `doXxx` methods
- [ ] Mapping servlets to URLs two ways:
  - `web.xml` (`<servlet>` + `<servlet-mapping>`)
  - `@WebServlet` annotation
- [ ] URL patterns: exact match, path prefix (`/api/*`), extension (`*.do`), default

**Deliverable:** one servlet that responds differently to GET vs POST.

---

## Phase 2 — Servlet Lifecycle (1–2 hrs)

A very common interview topic. Be able to explain it cleanly and draw it.

- [ ] The three lifecycle phases: `init()` → `service()` (repeated) → `destroy()`
- [ ] When each is called and how many times
- [ ] **Single instance, multiple threads** — one servlet instance serves concurrent requests, so instance fields are shared state (thread-safety matters)
- [ ] `ServletConfig` (per-servlet) vs `ServletContext` (per-application)
- [ ] Init parameters vs context parameters
- [ ] Lazy vs eager loading (`<load-on-startup>`)

**Interview drill:** "Walk me through the servlet lifecycle" and "Are servlets thread-safe? Why or why not?"

**Deliverable:** a servlet that logs in `init`/`destroy` and uses an init parameter.

---

## Phase 3 — Request & Response Handling (2–3 hrs)

The bread and butter of day-to-day servlet work.

- [ ] `HttpServletRequest`: query params (`getParameter`), headers, path info, method, body
- [ ] `HttpServletResponse`: status codes, headers, content type, `getWriter()` vs `getOutputStream()`
- [ ] Reading request bodies (form-encoded vs JSON vs raw)
- [ ] Setting proper `Content-Type` and character encoding
- [ ] Redirects (`sendRedirect`) vs forwards (`RequestDispatcher.forward`) — client vs server side
- [ ] Sending error responses (`sendError`, status codes)

**Deliverable:** a small servlet that accepts JSON input and returns JSON output with correct status codes.

---

## Phase 4 — Sessions, State & Filters (2 hrs)

- [ ] `HttpSession` — creating, reading, invalidating; cookies vs URL rewriting
- [ ] Cookies API
- [ ] Request/response scopes: request vs session vs application attributes
- [ ] **Filters** (`Filter` interface) — cross-cutting concerns: auth, logging, compression
- [ ] Filter chains and ordering
- [ ] **Listeners** (`ServletContextListener`, `HttpSessionListener`) — lifecycle hooks

**Deliverable:** a logging filter that times every request, plus a session-based counter.

---

## Phase 5 — Streaming Documents & Binary Data (2–3 hrs) ⭐

Directly maps to the role's "Document/binary streaming over REST" requirement. Prioritize this.

- [ ] Writing binary data with `response.getOutputStream()`
- [ ] Setting `Content-Type`, `Content-Disposition` (attachment vs inline), `Content-Length`
- [ ] Streaming large files without loading fully into memory (buffered copy loop)
- [ ] Chunked transfer encoding — when the container uses it and why
- [ ] Handling multipart uploads (`@MultipartConfig`, `request.getPart(s)`)
- [ ] `InputStream`/`OutputStream` copy patterns and closing streams properly (try-with-resources)

**Deliverable:** a servlet that streams a file download and a second one that accepts a multipart file upload.

---

## Phase 6 — Integration with the Role's Stack (2–3 hrs)

Tie servlets to the broader tech listed in required-skills.

- [ ] **Db2/JDBC:** a servlet that runs a query and returns results (connection handling, `DataSource`, closing resources)
- [ ] **REST consumption from a servlet:** call an external API from `doGet` using `HttpClient` (Java 11+) or `RestTemplate`
- [ ] **OAuth2 token handling:** fetch a token (client credentials), cache it, attach as a `Bearer` header
- [ ] **WAR/EAR packaging & deployment:** Maven `war` packaging, structure of `WEB-INF`, deploy to Tomcat
- [ ] How Servlets relate to **Spring MVC** (DispatcherServlet is just a servlet) — good framing for the "nice to have" Spring skill

**Deliverable:** a servlet that authenticates via OAuth2, calls a downstream REST API, and streams the response back to the client.

---

## Phase 7 — Interview Q&A Review (1 hr)

Rapid-fire questions to self-test:

- [ ] Explain the servlet lifecycle.
- [ ] Difference between `ServletConfig` and `ServletContext`?
- [ ] Are servlets thread-safe? How do you handle shared state?
- [ ] Forward vs redirect — when to use each?
- [ ] `getWriter()` vs `getOutputStream()` — can you use both in one response?
- [ ] How do filters differ from servlets?
- [ ] How does `web.xml` compare to annotation-based config?
- [ ] How would you stream a large file to avoid memory issues?
- [ ] What is the DispatcherServlet in Spring MVC?
- [ ] How do you handle a multipart file upload?

---

## Reference Materials

- Jakarta Servlet Specification (official docs)
- Apache Tomcat documentation
- Baeldung servlet tutorials (practical, code-first)
- Oracle Java EE / Jakarta EE tutorials

## Progress Tracker

| Phase | Topic | Status |
|-------|-------|--------|
| 0 | Environment setup | ⬜ |
| 1 | Servlet fundamentals | ⬜ |
| 2 | Servlet lifecycle | ⬜ |
| 3 | Request & response | ⬜ |
| 4 | Sessions, state, filters | ⬜ |
| 5 | Streaming documents/binary ⭐ | ⬜ |
| 6 | Integration (Db2, REST, OAuth2, WAR) | ⬜ |
| 7 | Interview Q&A review | ⬜ |
