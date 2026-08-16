# Listening History: Database Choice Analysis

## Characteristics of Listening History Data

**The data itself:**

- Append-only — events are written once and never modified. A user played a song at a specific time; that fact doesn't change.
- Extremely high write volume — 1 billion streams per day = 11,500+ writes per second sustained, with peaks of 3x during commute hours (~35,000 writes/sec).
- Time-series nature — data is naturally ordered by timestamp. The most common query is "what did this user listen to recently?"
- User-partitioned access — queries always include a user_id. "Show me my recent plays," "what did I listen to last week?" You never query across all users in real-time.
- Grows continuously — unlike song metadata (which is mostly static), listening history accumulates forever. 500M users × years of history = massive and growing.
- Feeds downstream systems — listening history is the input to recommendation models, royalty calculations, and analytics pipelines.
- Tolerates eventual consistency — if a play event takes a few seconds to appear in "recently played," users won't notice.

---

## Why Cassandra Fits

- **Designed for high write throughput** — Cassandra's write path (commit log + memtable, flushed to SSTables) is optimized for fast appends. No locks, no write-ahead log contention. Each node can handle tens of thousands of writes per second.
- **Partition key = user_id** — all of a user's listening history lives on the same nodes. "Last 100 songs I played" is a single partition scan — Cassandra's most efficient operation.
- **Clustering key = timestamp (DESC)** — within a user's partition, events are sorted by time (newest first). Range queries like "plays from the last 7 days" are efficient sequential reads.
- **Linear horizontal scaling** — as write volume grows (more users, more plays), add nodes. Cassandra distributes load automatically. No downtime, no application changes.
- **TTL for automatic cleanup** — old listening history (beyond 6-12 months) can expire automatically using Cassandra's native TTL feature, preventing unbounded storage growth.
- **Multi-datacenter replication** — listening events are replicated across regions for durability and low-latency reads globally.
- **Handles the write spike pattern** — during peak hours (3x average), Cassandra absorbs the load without backpressure because writes are distributed across the cluster.

---

## Why the Others Don't Fit Well

### PostgreSQL

- 11,500+ writes per second (35,000+ at peak) on a single primary is pushing PostgreSQL's limits, especially with indexes that need updating on each insert.
- The table grows by ~1 billion rows per day. Even with partitioning (by date or user), this creates operational challenges: vacuum pressure, index bloat, partition management.
- You'd need to shard by user_id to distribute writes, but PostgreSQL has no native sharding. Manual sharding adds significant complexity.
- The strengths of PostgreSQL (joins, complex queries, ACID) aren't needed here. Listening history queries are simple: "get recent plays for user X."
- Read replicas don't help with write throughput — all writes still go to one primary (per shard).

### Redis

- Listening history is persistent, long-lived data. Storing months of history for 500M users in memory is prohibitively expensive.
- At ~8KB per user (6 months of history) × 500M users = 4TB in RAM. That's an enormous and expensive Redis cluster for data that doesn't need sub-millisecond reads.
- Redis persistence (RDB snapshots, AOF) isn't designed for this volume of writes. AOF rewrite and RDB fork operations would be extremely expensive.
- Redis works well for caching the *most recent* plays (last 10-20 songs) for quick display, but not as the persistent store.

### Elasticsearch

- Elasticsearch can ingest high write volumes, but it's optimized for search, not time-series append workloads.
- Segment merging under heavy write load causes latency spikes and increased resource usage.
- No native TTL per document (you'd use index lifecycle management with daily/weekly indexes, which is viable but adds operational complexity).
- The query patterns (get by user + time range) don't benefit from Elasticsearch's text search capabilities.
- More expensive to operate per GB than Cassandra for data that doesn't need full-text search.

### MongoDB

- Could handle this workload with sharding (shard key = user_id), but Cassandra is more naturally suited to time-series append patterns.
- MongoDB's WiredTiger storage engine performs well for writes, but Cassandra's log-structured merge tree is specifically optimized for append-heavy workloads.
- At this scale (billions of documents, growing daily), MongoDB's sharding and balancer introduce more operational complexity than Cassandra's consistent hashing.
- MongoDB's flexible schema is unnecessary — listening history events have a fixed, well-known structure.

### DynamoDB (for comparison)

- A viable alternative to Cassandra for this use case. Partition key = user_id, sort key = timestamp works identically.
- Trade-offs: managed service (less operational burden) vs. potentially higher cost at this scale and less control over cluster topology.
- If you're on AWS and prefer managed services, DynamoDB is a defensible choice here.

---

## Schema Design in Cassandra

```sql
CREATE TABLE listening_history (
    user_id UUID,
    listened_at TIMESTAMP,
    song_id UUID,
    artist_id UUID,
    duration_played_ms INT,
    context VARCHAR,  -- 'playlist', 'album', 'radio', 'search'
    PRIMARY KEY (user_id, listened_at)
) WITH CLUSTERING ORDER BY (listened_at DESC)
  AND default_time_to_live = 15552000;  -- 180 days
```

- **Partition key:** `user_id` — all of a user's history on the same nodes
- **Clustering key:** `listened_at DESC` — newest first, so "recent plays" is a simple sequential read
- **TTL:** 180 days — old data expires automatically, keeping storage bounded

Common queries:
- "Last 50 songs I played" → single partition read, limit 50
- "What I listened to last Tuesday" → partition read with timestamp range filter
- "Total plays this month" → partition scan with count (or pre-aggregated separately)

---

## The Bottom Line

Listening history is a high-volume, append-only, time-series workload with user-partitioned access patterns. Cassandra is purpose-built for this: fast appends without locks, data sorted by timestamp within user partitions, linear scaling as write volume grows, and native TTL for automatic data lifecycle management. PostgreSQL can't handle the write throughput without complex sharding, Redis is too expensive for persistent storage at this scale, and Elasticsearch's search capabilities go unused.
