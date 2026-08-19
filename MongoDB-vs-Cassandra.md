# MongoDB vs Cassandra

## Overview

Both are NoSQL databases designed for horizontal scalability, but they solve fundamentally different problems and make different trade-offs.

- **MongoDB** — Document-oriented. Optimized for flexible schemas, rich queries, and developer productivity.
- **Cassandra** — Wide-column. Optimized for write-heavy workloads, high availability, and predictable performance at massive scale.

---

## Architecture

| Aspect | MongoDB | Cassandra |
|--------|---------|-----------|
| Topology | Primary-secondary (replica sets) | Peer-to-peer (ring, no master) |
| Sharding | Manual shard key selection | Automatic via consistent hashing |
| Single point of failure | Primary node (auto-failover via election) | None (all nodes are equal) |
| Write path | Writes go to primary, replicated to secondaries | Writes go to any node, replicated to N peers |
| Read path | Reads from primary (strong) or secondary (eventual) | Reads from any replica (tunable consistency) |
| Consensus | Raft-based replica set elections | Gossip protocol + tunable quorum |

---

## Data Model

| Aspect | MongoDB | Cassandra |
|--------|---------|-----------|
| Structure | JSON-like documents (nested, variable shape) | Rows within partitions (flat, fixed schema per table) |
| Schema | Flexible (schema-on-read, optional validation) | Rigid (schema-on-write, ALTER TABLE for changes) |
| Nesting | Deep nesting, embedded arrays and objects | No nesting — denormalize into separate tables |
| Primary key | `_id` field (any type, auto-generated ObjectId) | Composite: partition key + clustering columns |
| Design approach | Model your domain naturally, query flexibly | Model your queries first, denormalize aggressively |

### Same Data, Different Models

**A user's recent orders:**

MongoDB — one query, flexible filtering:
```json
db.orders.find({ customerId: "cust-881", status: "shipped" })
         .sort({ createdAt: -1 }).limit(10)
```

Cassandra — requires a pre-designed table for this exact query:
```sql
-- Must have a table specifically for this access pattern
SELECT * FROM orders_by_customer
WHERE customer_id = 'cust-881'
ORDER BY created_at DESC
LIMIT 10;
-- Cannot filter by status unless it's part of the primary key
```

---

## Query Capabilities

| Capability | MongoDB | Cassandra |
|-----------|---------|-----------|
| Ad-hoc queries | Yes — query on any field, any combination | No — must query by partition key |
| Secondary indexes | Rich (compound, multikey, text, geo, hashed) | Limited (local/global secondary indexes, expensive) |
| Aggregation | Full pipeline (group, project, unwind, lookup) | Basic (COUNT, SUM, AVG) — no complex aggregations |
| Joins | `$lookup` (left outer join between collections) | Not supported |
| Full-text search | Native text indexes + Atlas Search (Lucene-based) | Not supported (use Elasticsearch/Solr alongside) |
| Sorting | On any indexed field | Only by clustering column within a partition |
| Filtering | Any field, regex, array operators, nested paths | Only on primary key columns (ALLOW FILTERING is an anti-pattern) |

---

## Write and Read Performance

| Aspect | MongoDB | Cassandra |
|--------|---------|-----------|
| Write throughput | High (single-primary bottleneck per shard) | Very high (distributed writes, no single bottleneck) |
| Write latency | Low (single-digit ms typical) | Very low (sub-ms possible, append-only LSM) |
| Read latency | Low for indexed queries | Very low for partition-key lookups |
| Read flexibility | Any query pattern if indexed | Only pre-modeled query patterns |
| Hot spots | Possible with poor shard key choice | Possible with poor partition key choice |
| Storage engine | WiredTiger (B-tree based) | LSM trees (optimized for sequential writes) |

**Key insight:** Cassandra's LSM tree storage makes writes essentially sequential I/O (append-only), which is why it excels at sustained write throughput. MongoDB's B-tree storage is more balanced between reads and writes but can't match Cassandra's raw write speed at extreme scale.

---

## Consistency and Availability

| Aspect | MongoDB | Cassandra |
|--------|---------|-----------|
| CAP theorem | CP (consistency + partition tolerance) | AP (availability + partition tolerance) — tunable |
| Default consistency | Strong (reads from primary) | Eventual (ONE read/write) |
| Tunable consistency | Read/write concern levels | Per-query consistency level (ONE, QUORUM, ALL) |
| Failover | Automatic election (10-30s window) | No failover needed (all nodes serve traffic) |
| Multi-DC replication | Supported (replica set members across DCs) | First-class (designed for multi-DC from day one) |
| Conflict resolution | Last-write-wins (primary arbitrates) | Last-write-wins (timestamp-based) |

**Key insight:** Cassandra is designed to never go down — it sacrifices consistency guarantees for availability. MongoDB prioritizes consistency — during a primary election, writes are briefly unavailable.

---

## Operational Characteristics

| Aspect | MongoDB | Cassandra |
|--------|---------|-----------|
| Scaling | Add shards (manual shard key planning) | Add nodes (automatic data rebalancing) |
| Node addition | Requires balancer migration | Streams data automatically from neighbors |
| Backup | mongodump, file snapshots, Atlas backups | Snapshots per node, incremental backups |
| Schema changes | No-op (flexible schema) | ALTER TABLE (online, but must be compatible) |
| Compaction | Background (WiredTiger) | Configurable strategy (size-tiered, leveled) |
| Managed options | Atlas (MongoDB Inc.) | Astra (DataStax), Amazon Keyspaces |
| Learning curve | Lower (familiar JSON, flexible queries) | Higher (query-first modeling, CQL limitations) |

---

## When to Choose MongoDB

- Your schema evolves frequently or varies between records.
- You need rich, ad-hoc queries and aggregations.
- Your read patterns are diverse and unpredictable.
- Development speed and query flexibility matter more than extreme write throughput.
- You want a single database that handles both transactional and analytical queries.
- Your workload is read-heavy or balanced read/write.

## When to Choose Cassandra

- Write throughput is the dominant concern (logs, events, time-series, IoT).
- You need always-on availability with zero downtime (no single point of failure).
- Multi-datacenter / multi-region replication is a core requirement.
- Your access patterns are known upfront and won't change often.
- You're dealing with massive scale (petabytes) and predictable query patterns.
- You can afford to duplicate data across multiple tables for different query patterns.

## When Either Could Work

- High-throughput event ingestion with simple queries → slight edge to Cassandra.
- User-facing apps with moderate scale and known query patterns → either works, MongoDB is typically easier to develop against.
- Time-series data → Cassandra if write-heavy at extreme scale, MongoDB if you need flexible aggregations (or consider a dedicated time-series DB).

---

## Summary

| | MongoDB | Cassandra |
|-|---------|-----------|
| Philosophy | "Model your data, query it any way you want" | "Model your queries, optimize for that access pattern" |
| Strength | Flexibility and query power | Write throughput and availability |
| Weakness | Write throughput ceiling per shard | Query rigidity |
| Mindset | Developer-first | Operations-first |
