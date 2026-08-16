# User Sessions: Database Choice Analysis

## Characteristics of Session Data

**The data itself:**

- Ephemeral — sessions are created when users log in and destroyed when they log out or expire. They have a natural lifespan (hours to days), not permanent storage.
- Key-value access — every lookup is by session token or user_id. There are no complex queries, no joins, no aggregations.
- Extremely latency-sensitive — every single API request must validate the user's session. If session lookup takes 50ms, it adds 50ms to *every* request in the system.
- High throughput — with 200M daily active users, session validation happens on every request across all services. That's hundreds of thousands of lookups per second.
- Small payloads — a session record is typically a few hundred bytes: user ID, subscription tier, device info, expiration time.
- Needs automatic expiration — sessions should disappear after a timeout without manual cleanup.

---

## Why Redis Fits

- **Sub-millisecond reads** — Redis stores everything in memory. A session lookup takes < 1ms, adding negligible overhead to every API request.
- **Native TTL (Time-To-Live)** — set an expiration when creating the session and Redis automatically deletes it when it expires. No background jobs or cleanup scripts needed.
- **Simple key-value model** — sessions are a textbook key-value workload. `GET session:{token}` returns the session data. That's the entire query pattern.
- **Millions of operations per second** — a single Redis instance handles 100K+ ops/sec. A cluster handles millions. This matches the throughput needs of session validation across all services.
- **Atomic operations** — set-with-expiry (`SETEX`) is atomic, preventing race conditions when creating or refreshing sessions.
- **Cluster mode** — Redis Cluster shards data across nodes by key, scaling horizontally while maintaining the same simple interface.
- **Pub/sub for invalidation** — when a user logs out or a session is revoked, Redis pub/sub can notify all API gateway instances to drop cached session state immediately.

---

## Why the Others Don't Fit Well

### PostgreSQL

- Disk-based storage means every session lookup involves disk I/O (or hopes the row is in the buffer cache). Even with SSDs, this is 1-5ms vs Redis's < 1ms.
- Adding 1-5ms to every API request across 300K+ requests/sec is a meaningful latency penalty.
- No native TTL. You'd need a background job to periodically delete expired sessions, which adds operational complexity and can cause table bloat.
- Connection pool pressure — every service instance needs a database connection for session validation, competing with connections needed for actual business queries.
- Overkill complexity for a simple key-value lookup. You don't need ACID transactions, joins, or relational modeling for sessions.

### Cassandra

- Cassandra's write path (commit log → memtable → SSTable) adds latency compared to Redis's pure in-memory model.
- Read latency is typically 2-10ms, which is acceptable for many workloads but suboptimal when you're adding it to every single request.
- No native TTL... actually, Cassandra *does* have TTL on writes, which is a point in its favor. But the latency penalty remains.
- Operational complexity of a Cassandra cluster isn't justified for what is fundamentally a simple cache-like workload.
- Better suited for persistent, high-volume writes (like listening history) than ephemeral lookups.

### Elasticsearch

- Not designed for key-value lookups. Elasticsearch is a search engine — using it for session storage is like using a sledgehammer to hang a picture frame.
- Higher latency than Redis for simple lookups (typically 5-20ms).
- No native TTL for automatic document expiration (you'd use index lifecycle management, which is coarse-grained).
- Resource-heavy for what amounts to a hash table lookup.

### MongoDB

- Disk-based with optional in-memory engine. The default storage engine (WiredTiger) adds disk I/O latency.
- Has a TTL index feature for automatic expiration, which is useful.
- But at 200M+ active sessions with hundreds of thousands of lookups per second, MongoDB's overhead compared to Redis isn't justified for pure key-value access.
- The document model's flexibility is wasted — sessions are flat key-value pairs.

---

## What Redis Stores for Sessions

A typical session record:

```json
{
  "user_id": "u_abc123",
  "subscription_tier": "premium",
  "device_id": "d_xyz789",
  "region": "US",
  "login_time": "2024-03-15T10:30:00Z",
  "permissions": ["stream_high_quality", "offline_download"]
}
```

Key: `session:{token}`  
TTL: 24 hours (refreshed on activity)

This is ~200 bytes per session. At 200M active sessions, that's ~40GB — well within the capacity of a Redis cluster.

---

## The Bottom Line

Session data is ephemeral, accessed by key, needs sub-millisecond latency on every request, and should auto-expire. Redis is purpose-built for this exact pattern. Disk-based databases add unnecessary latency to the critical path of every API call, and none of their advanced features (relational queries, full-text search, flexible schemas) are needed for what is fundamentally a high-speed lookup table with automatic expiration.
