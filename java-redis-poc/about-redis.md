# About Redis

## What is Redis?

Redis (Remote Dictionary Server) is an open-source, in-memory data structure store. It functions as a database, cache, message broker, and streaming engine. Originally created by Salvatore Sanfilippo in 2009, Redis is now one of the most popular NoSQL databases in production use worldwide.

Unlike traditional databases that store data on disk and load it into memory for queries, Redis keeps the entire dataset in memory and persists to disk asynchronously. This architecture enables sub-millisecond response times for most operations.

## Key Characteristics

### In-Memory Storage
- All data resides in RAM, delivering microsecond read/write latency
- Optional persistence to disk via RDB snapshots or AOF (Append Only File) logs
- Configurable eviction policies when memory limits are reached (LRU, LFU, random, TTL-based)

### Rich Data Structures
Redis is not a simple key-value store. It supports first-class data structures:

| Data Type | Description | Example Use |
|-----------|-------------|-------------|
| **Strings** | Binary-safe strings up to 512MB | Caching, counters, flags |
| **Hashes** | Maps of field-value pairs | User profiles, object properties |
| **Lists** | Ordered collections (linked lists) | Message queues, activity feeds |
| **Sets** | Unordered collections of unique strings | Tags, unique visitors, set operations |
| **Sorted Sets** | Sets with a score per member, ordered by score | Leaderboards, priority queues, time-series |
| **Streams** | Append-only log with consumer groups | Event sourcing, message streaming |
| **Bitmaps** | Bit-level operations on strings | Feature flags, bloom filters, analytics |
| **HyperLogLog** | Probabilistic cardinality estimation | Unique visitor counting at scale |
| **Geospatial** | Longitude/latitude with radius queries | Location-based services |

### Single-Threaded Command Processing
- Commands execute sequentially on a single thread — no locks, no race conditions
- Individual commands are atomic by design
- Multi-command atomicity via MULTI/EXEC transactions or Lua scripts
- I/O multiplexing handles thousands of concurrent connections efficiently

### Performance
- 100,000+ operations per second on commodity hardware
- Sub-millisecond latency for most commands
- Pipelining allows batching multiple commands in a single network round-trip

### Persistence Options
- **RDB (Snapshotting):** Point-in-time snapshots at configurable intervals. Fast restarts, minimal performance impact, but some data loss on crash.
- **AOF (Append Only File):** Logs every write operation. Configurable fsync (every second, every write, or never). Better durability, slightly higher overhead.
- **RDB + AOF:** Combine both for durability with fast restart.
- **No persistence:** Pure in-memory cache mode.

### Replication and High Availability
- Master-replica asynchronous replication
- Redis Sentinel for automatic failover and monitoring
- Redis Cluster for horizontal partitioning (sharding) across multiple nodes
- Read replicas for scaling read-heavy workloads

### Pub/Sub Messaging
- Publish/subscribe messaging between clients
- Pattern-based channel subscriptions
- Lightweight and fast, but messages are fire-and-forget (no persistence)

### TTL and Expiration
- Any key can have a time-to-live (TTL) set in seconds or milliseconds
- Expired keys are automatically removed (lazy deletion + periodic sampling)
- Enables natural cache invalidation without application logic

## Common Use Cases

### 1. Caching
The most common use case. Redis sits between your application and database, storing frequently accessed data in memory to reduce database load and improve response times.

- **Cache-aside:** Application checks Redis first, falls back to DB on miss, writes result to Redis
- **Write-through:** Writes go to both Redis and DB simultaneously
- **Write-behind:** Writes go to Redis first, asynchronously persisted to DB

### 2. Session Store
HTTP sessions stored in Redis enable stateless application servers. Any server in the cluster can serve any user's request since session data is centralized.

### 3. Rate Limiting
Atomic increment operations with key expiration make Redis ideal for tracking request counts per time window. Common in API gateways and DDoS protection.

### 4. Leaderboards and Ranking
Sorted Sets provide O(log N) insertion and O(log N + M) range queries, making real-time leaderboards with millions of entries feasible.

### 5. Real-Time Analytics
Bitmaps and HyperLogLog enable memory-efficient counting of unique events (daily active users, feature usage) at massive scale.

### 6. Message Queues
Lists (LPUSH/BRPOP) and Streams provide lightweight queuing without the overhead of dedicated message brokers. Suitable for task queues, job scheduling, and event buffering.

### 7. Pub/Sub and Event Notification
Real-time notifications, chat systems, live updates. Messages are broadcast to all subscribers on a channel instantly.

### 8. Distributed Locks
The SETNX (SET if Not eXists) command with TTL enables distributed locking across multiple application instances. Libraries like Redlock formalize this pattern.

### 9. Geospatial Queries
Store locations and query by radius or bounding box. Used in ride-sharing, delivery tracking, and location-based search.

### 10. Full-Text Search and Secondary Indexing
With the RediSearch module, Redis supports full-text search, secondary indexing, and aggregation queries on structured data.

## Redis vs. Other Technologies

| Comparison | Redis Advantage | Trade-off |
|------------|----------------|-----------|
| vs. Memcached | Rich data types, persistence, replication | Slightly more memory overhead |
| vs. PostgreSQL | Sub-ms latency, simpler ops for cache/counter workloads | No relational queries, limited dataset size |
| vs. Kafka | Simpler setup, lower latency for small messages | No durable message replay, limited throughput for streaming |
| vs. Hazelcast | More mature ecosystem, broader language support | Hazelcast offers tighter Java integration |

## When NOT to Use Redis

- **Large datasets that exceed available RAM** — Redis requires all data to fit in memory
- **Complex relational queries** — Use a relational database instead
- **Strong transactional guarantees across multiple keys** — Redis transactions are limited compared to ACID databases
- **Long-term durable storage** — Redis is optimized for speed, not as a primary system of record
- **Large binary blobs** — Individual values up to 512MB are supported but not optimal

## Ecosystem

- **Redis Stack:** Bundles Redis with modules (Search, JSON, Time Series, Graph, Bloom)
- **Redis Insight:** Official GUI for browsing and debugging Redis data
- **Redis Commander:** Lightweight web-based management tool
- **Spring Data Redis:** First-class Spring integration with RedisTemplate and repository support
- **Lettuce / Jedis:** Popular Java client libraries (Spring Boot uses Lettuce by default)
