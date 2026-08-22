# RESTful Endpoint Naming

RESTful endpoints must represent resources (nouns), not actions (verbs). Map your CRUD operations directly to standard HTTP methods rather than encoding them into the URI path.

---

## Core Naming Rules

- **Use nouns, not verbs** — Never include words like get, create, update, or delete in the URL.
- **Prefer plural nouns** — Use plural formatting for resource collections to maintain consistent structure.
- **Stick to lowercase** — Treat URLs as case-sensitive and enforce lowercase to avoid routing errors.
- **Separate with hyphens** — Use kebab-case (`/order-items`) instead of camelCase or snake_case for multi-word paths.
- **No file extensions** — Omit indicators like `.json` or `.xml` from the path; use `Accept`/`Content-Type` headers instead.
- **No trailing slashes** — Leave the final forward slash off the URL to avoid indexing duplicate paths.

---

## Standard Endpoint Mapping

| Action | HTTP Method | REST Endpoint | Bad Example |
|--------|-------------|---------------|-------------|
| Get All Items | GET | `/v1/products` | `GET /v1/getAllProducts` |
| Get One Item | GET | `/v1/products/{id}` | `GET /v1/product?id={id}` |
| Create Item | POST | `/v1/products` | `POST /v1/createProduct` |
| Update Item | PUT / PATCH | `/v1/products/{id}` | `POST /v1/updateProduct/{id}` |
| Delete Item | DELETE | `/v1/products/{id}` | `GET /v1/products/{id}/delete` |


---

## Filtering, Sorting, and Searching

Do not create distinct endpoints for variations of a resource collection. Use query parameters to modify the response dynamically.

| Operation | Example |
|-----------|---------|
| Filter | `GET /v1/products?category=electronics` |
| Sort | `GET /v1/products?sort=price_desc` |
| Paginate | `GET /v1/products?page=2&limit=50` |


---

## Handling Relationships and Nesting

Limit your URL hierarchy to a maximum of two to three levels to prevent long, unreadable endpoints. Use forward-slash syntax to express parent-child relationships.

**Good (sub-resource):**
```
GET /v1/authors/{id}/books
```
Fetches books belonging to a specific author.

**Bad (deep nesting):**
```
GET /v1/authors/{id}/books/{id}/chapters/{id}/paragraphs
```
Too complex — flatten or use query parameters instead.


---


## REST Verb Philosophies

Three main schools of thought on how to handle actions that don't fit clean CRUD.

### 1. Resource Purist — "Everything is a noun"

Every action can be modeled as creating or modifying a resource. Verbs in URLs are never acceptable.

```
Cancel order    → POST /v1/orders/{id}/cancellations
Send email      → POST /v1/emails/{id}/deliveries
Lock account    → POST /v1/accounts/{id}/locks
Approve request → POST /v1/requests/{id}/approvals
```

**Pros:**
- Every action becomes queryable and auditable (`GET /v1/orders/{id}/cancellations`)
- Consistent API surface — no special cases
- HATEOAS-friendly

**Cons:**
- Forced abstractions feel awkward ("create a cancellation" vs "cancel it")
- Lots of one-off resources that only exist to model actions
- API consumers need to learn the vocabulary

**Used by:** Teams that follow REST literally, hypermedia APIs, APIs designed for long-term evolvability.

---

### 2. Pragmatic REST — "Verbs when necessary"

Use nouns by default, but use verbs for actions that have significant side effects or don't produce a meaningful resource.

```
CRUD on resource → POST/GET/PUT/DELETE /v1/orders
State transition → POST /v1/orders/{id}/cancel
Process trigger  → POST /v1/reports/generate
Computation      → POST /v1/loans/calculate
```

**Pros:**
- Intuitive — API reads like what it does
- Reduces forced abstractions
- Clear signal: "this endpoint has side effects"

**Cons:**
- Inconsistency — some endpoints are nouns, some are verbs
- Requires judgment calls
- Documentation needs to explain the distinction

**Used by:** Most production APIs (Stripe, GitHub, Twilio). The industry standard in practice.

---

### 3. Action-Oriented / RPC-over-HTTP — "URLs describe what you're doing"

Endpoints describe operations. HTTP is just a transport layer.

```
POST /v1/sendEmail
POST /v1/createOrder
POST /v1/cancelOrder/{id}
POST /v1/calculateShipping
```

**Pros:**
- Extremely clear — no ambiguity
- Easy to discover and document
- Natural fit for service-to-service communication

**Cons:**
- Not RESTful (it's RPC with HTTP verbs)
- Loses HTTP method semantics (everything is POST)
- Caching and idempotency harder to reason about
- Endpoint explosion

**Used by:** Internal microservices, gRPC-influenced APIs, teams prioritizing speed over design purity.

---

### Same Action, Three Styles

"User approves a loan application"

| Philosophy | Endpoint | Mental Model |
|-----------|----------|-------------|
| Purist | `POST /v1/applications/{id}/approvals` | "Create an approval resource" |
| Pragmatic | `POST /v1/applications/{id}/approve` | "Approve this application" |
| RPC-style | `POST /v1/approveLoanApplication` | "Call the approve function" |

---

### Which to Choose?

| Factor | Recommendation |
|--------|---------------|
| Public API (external consumers) | Pragmatic REST |
| Internal microservice-to-microservice | RPC-style is fine |
| API needing long-term stability (years) | Purist |
| Small team moving fast | Pragmatic |
| Event-sourced system | Purist (actions are already events/resources) |

Most teams land on Pragmatic REST — nouns for CRUD, verbs for side-effect actions, and internal consistency matters more than philosophical purity.
