# MongoDB

## What Is MongoDB?

MongoDB is a general-purpose, document-oriented NoSQL database. Instead of storing data in rows and columns (like relational databases), it stores data as flexible JSON-like documents (BSON internally). Each document can have a different structure, and related data is typically embedded within a single document rather than normalized across tables.

## Defining Characteristics

- **Document model** — Data is stored as rich, nested documents (JSON/BSON). No rigid schema required; fields can vary between documents in the same collection.
- **Schema flexibility** — Schema-on-read rather than schema-on-write. You can evolve your data model without migrations or downtime.
- **Horizontal scalability** — Native sharding distributes data across machines. Designed to scale out rather than up.
- **Rich query language** — Supports ad-hoc queries, secondary indexes, aggregation pipelines, full-text search, and geospatial queries.
- **Replication** — Replica sets provide automatic failover and data redundancy with tunable read/write concerns.
- **Flexible indexing** — Compound, multikey (array), text, geospatial, hashed, and TTL indexes.
- **ACID transactions** — Multi-document transactions supported since v4.0 (single-document operations have always been atomic).

## Comparisons to Other Datastores

| Aspect | MongoDB | PostgreSQL (Relational) | DynamoDB (Key-Value/Doc) | Cassandra (Wide-Column) |
|--------|---------|------------------------|--------------------------|------------------------|
| Data model | Nested documents | Normalized tables | Flat documents / key-value | Wide-column families |
| Schema | Flexible (schema-on-read) | Rigid (schema-on-write) | Schemaless (per-item) | Schema-per-table |
| Query flexibility | Rich queries, aggregations | Full SQL, joins, CTEs | Limited (partition/sort key) | Limited (partition key required) |
| Scaling model | Horizontal (sharding) | Primarily vertical | Horizontal (managed) | Horizontal (ring-based) |
| Joins | Embedded docs, `$lookup` | Native joins | Not supported | Not supported |
| Transactions | Multi-document ACID | Full ACID | Limited (single-item or 25-item batch) | Lightweight transactions only |
| Operational model | Self-managed or Atlas (managed) | Self-managed or managed (RDS, etc.) | Fully managed (AWS) | Self-managed or managed |
| Best for | Flexible, evolving schemas with complex queries | Relational data with complex joins and strong consistency | High-throughput key-based access at massive scale | Write-heavy time-series, append-only workloads |

## Sample Data Models

The same domain — an e-commerce order — modeled in each database to illustrate structural differences.

### MongoDB (Document)

A single document embeds the customer reference, line items, and shipping info together. One read fetches everything needed to display an order.

```json
{
  "_id": ObjectId("64a7f2..."),
  "orderNumber": "ORD-10042",
  "status": "shipped",
  "customer": {
    "customerId": "cust-881",
    "name": "Alice Park",
    "email": "alice@example.com"
  },
  "items": [
    { "sku": "WIDGET-A", "name": "Widget A", "qty": 2, "price": 14.99 },
    { "sku": "GADGET-B", "name": "Gadget B", "qty": 1, "price": 49.99 }
  ],
  "shipping": {
    "address": "123 Main St, Springfield",
    "carrier": "FedEx",
    "trackingNumber": "FX123456789"
  },
  "totals": {
    "subtotal": 79.97,
    "tax": 6.40,
    "total": 86.37
  },
  "createdAt": ISODate("2025-03-15T10:30:00Z")
}
```

**Key trait:** Nested, denormalized. Related data lives together. No joins needed at query time.

---

### PostgreSQL (Relational)

Data is normalized across multiple tables with foreign keys. An order is assembled via joins.

```sql
-- customers table
| id       | name       | email              |
|----------|------------|--------------------|
| cust-881 | Alice Park | alice@example.com  |

-- orders table
| id         | order_number | status  | customer_id | subtotal | tax  | total | created_at          |
|------------|--------------|---------|-------------|----------|------|-------|---------------------|
| order-1001 | ORD-10042    | shipped | cust-881    | 79.97    | 6.40 | 86.37 | 2025-03-15 10:30:00 |

-- order_items table
| id | order_id   | sku      | name     | qty | price |
|----|------------|----------|----------|-----|-------|
| 1  | order-1001 | WIDGET-A | Widget A | 2   | 14.99 |
| 2  | order-1001 | GADGET-B | Gadget B | 1   | 49.99 |

-- shipments table
| id | order_id   | address              | carrier | tracking_number |
|----|------------|----------------------|---------|-----------------|
| 1  | order-1001 | 123 Main St, Spring… | FedEx   | FX123456789     |
```

**Key trait:** Normalized, join-dependent. Each entity has its own table. Integrity enforced via foreign keys and constraints.

---

### DynamoDB (Key-Value / Document)

Data modeled around access patterns using a single-table design with composite keys.

```
Table: Orders

| PK (Partition Key) | SK (Sort Key)       | Attributes                                          |
|--------------------|---------------------|-----------------------------------------------------|
| ORDER#ORD-10042    | META                | status=shipped, customerId=cust-881, total=86.37    |
| ORDER#ORD-10042    | ITEM#WIDGET-A       | name=Widget A, qty=2, price=14.99                   |
| ORDER#ORD-10042    | ITEM#GADGET-B       | name=Gadget B, qty=1, price=49.99                   |
| ORDER#ORD-10042    | SHIPPING            | address=123 Main St…, carrier=FedEx, tracking=FX…   |
| CUSTOMER#cust-881  | ORDER#ORD-10042     | orderNumber=ORD-10042, status=shipped, total=86.37  |
```

**Key trait:** Access-pattern-driven. All data for one order shares a partition key. A GSI on `CUSTOMER#` enables "get all orders for customer" queries. No joins, no ad-hoc queries on arbitrary fields without an index.

---

### Cassandra (Wide-Column)

Tables are designed per query. Denormalization is expected — the same data may appear in multiple tables.

```
-- Table optimized for: "Get order details by order number"
CREATE TABLE orders_by_id (
    order_number  text,
    item_sku      text,
    customer_name text STATIC,
    status        text STATIC,
    total         decimal STATIC,
    item_name     text,
    qty           int,
    price         decimal,
    PRIMARY KEY (order_number, item_sku)
);

| order_number | item_sku | customer_name | status  | total | item_name | qty | price |
|--------------|----------|---------------|---------|-------|-----------|-----|-------|
| ORD-10042    | GADGET-B | Alice Park    | shipped | 86.37 | Gadget B  | 1   | 49.99 |
| ORD-10042    | WIDGET-A | Alice Park    | shipped | 86.37 | Widget A  | 2   | 14.99 |

-- Table optimized for: "Get all orders for a customer, most recent first"
CREATE TABLE orders_by_customer (
    customer_id   text,
    created_at    timestamp,
    order_number  text,
    status        text,
    total         decimal,
    PRIMARY KEY (customer_id, created_at)
) WITH CLUSTERING ORDER BY (created_at DESC);

| customer_id | created_at          | order_number | status  | total |
|-------------|---------------------|--------------|---------|-------|
| cust-881    | 2025-03-15 10:30:00 | ORD-10042    | shipped | 86.37 |
```

**Key trait:** Query-first design. Each table serves one query. Data is duplicated across tables. Partition key determines data distribution; clustering key determines sort order within a partition.

---

## Sample Use Cases

### 1. Content Management System

**Scenario:** A media company stores articles, each with different metadata — some have video embeds, others have image galleries, polls, or live tickers.

**Why MongoDB over a relational DB:**
Relational databases would require either a wide table full of nullable columns or a complex EAV (Entity-Attribute-Value) pattern to handle varying content types. MongoDB's document model naturally accommodates heterogeneous content within a single collection. Each article document embeds exactly the fields it needs, no joins or nullable columns required.

### 2. Product Catalog (E-Commerce)

**Scenario:** An online marketplace sells electronics, clothing, and furniture — each category has completely different attributes (screen size vs. fabric type vs. dimensions).

**Why MongoDB over a relational DB:**
A relational schema would need per-category tables or an attribute table with generic key-value rows, making queries awkward. MongoDB lets each product document carry its own category-specific attributes while still supporting indexed queries across all products (e.g., filter by price, search by name).

### 3. Real-Time Analytics Dashboard

**Scenario:** An IoT platform ingests sensor events with varying payloads (temperature, humidity, vibration) and serves dashboards with aggregated views.

**Why MongoDB over Cassandra:**
Both handle high write throughput, but MongoDB's aggregation pipeline makes it straightforward to compute rollups, percentiles, and time-bucketed summaries without a separate analytics layer. Cassandra would require pre-computed materialized views or an external tool (Spark) for ad-hoc aggregations.

### 4. User Profile / Personalization Service

**Scenario:** A SaaS platform stores user profiles with preferences, feature flags, notification settings, and activity history — all read together in a single API call.

**Why MongoDB over DynamoDB:**
DynamoDB excels at single-key lookups but becomes cumbersome when you need to query across multiple attributes or run aggregations (e.g., "find all users who enabled feature X in the last 30 days"). MongoDB supports secondary indexes and expressive queries on any field without redesigning access patterns upfront.

### 5. Activity Feeds

**Scenario:** A social platform stores activity events (posts, likes, comments, shares) and renders personalized feeds in real time.

**Why MongoDB over PostgreSQL:**
Events have varying shapes (a "like" event differs from a "share" event). Embedding related context (author info, preview data) into each event document avoids expensive joins at read time. MongoDB's flexible schema accommodates new event types without migrations, and its change streams enable real-time feed updates pushed to clients.

PostgreSQL can handle polymorphic events via JSONB columns and offers LISTEN/NOTIFY for basic change notification, but MongoDB's change streams are more capable for tailing updates across collections at scale. The document model also avoids the impedance mismatch of storing variably-shaped events in a relational schema.

**Note:** This use case is about serving read-optimized activity feeds, not true event sourcing (append-only logs replayed to rebuild state). For that pattern, a dedicated event store or Kafka is typically a better fit.

> **A note on polyglot persistence:** These use cases compare MongoDB against a single alternative for clarity, but production systems often use multiple databases together — MongoDB for the catalog, PostgreSQL for payments, Redis for sessions, Elasticsearch for search, etc. The right question isn't "which one database?" but "where does each database fit best?" Choose the simplest stack that serves your access patterns, and add stores only when a workload clearly outgrows what you have.

## When MongoDB Is Not the Best Fit

- **Heavy relational joins** — If your domain is highly normalized and join-heavy (e.g., ERP, accounting), a relational database with mature query planning is a better choice.
- **Strict ACID across many entities** — While MongoDB supports transactions, workloads that rely on complex cross-collection transactions at high throughput may be better served by PostgreSQL or a traditional RDBMS.
- **Simple key-value access at extreme scale** — If you only ever read/write by a single key and need single-digit-ms latency at millions of RPS, a purpose-built key-value store (Redis, DynamoDB) is more efficient.
