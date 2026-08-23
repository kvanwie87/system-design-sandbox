# About Redis

## What is Redis?

Redis (Remote Dictionary Server) is an open-source, in-memory data structure store. It functions as a database, cache, message broker, and streaming engine. Originally created by Salvatore Sanfilippo in 2009, Redis is now one of the most popular NoSQL databases in production use worldwide.

Unlike traditional databases that store data on disk and load it into memory for queries, Redis keeps the entire dataset in memory and persists to disk asynchronously. This architecture enables sub-millisecond response times for most operations.

## Key Characteristics

### In-Memory Storage
- All data resides in RAM, delivering microsecond read/write latency
- Optional persistence to disk via RDB snapshots or AOF (Append Only File) logs
- Configurable eviction policies when memory limits are reached (LRU, LFU, random, TTL-based)

### Keys

Everything in Redis is accessed through a key. Understanding key design is fundamental to using Redis effectively.

#### Key Properties
- Keys are binary-safe — any binary sequence works, from a simple string like `"user:1"` to the contents of a JPEG file
- Maximum key size is 512MB (but keep them short in practice)
- Empty strings are valid keys

#### Key Naming Conventions
The Redis community follows a colon-separated naming convention to create a natural namespace hierarchy:

```
object-type:id:field
```

Examples:
```
user:1001:profile          # Hash storing user 1001's profile
user:1001:sessions         # Set of active session IDs for user 1001
order:20240115:items       # List of items in an order
rate_limit:api-key-xyz     # Counter for rate limiting
cache:product:sku-123      # Cached product data
session:abc123             # Session attributes
leaderboard:weekly         # Sorted set for weekly rankings
```

#### Key Design Best Practices

| Practice | Good | Avoid |
|----------|------|-------|
| Use colons as separators | `user:1001:email` | `user_1001_email` or `user.1001.email` |
| Keep keys reasonably short | `u:1001:e` (if millions of keys) | `the-user-with-id-1001-email-address` |
| Include the object type | `comment:4321:votes` | `4321:votes` |
| Use consistent casing | `user:1001` | mixing `User:1001` and `user:1001` |
| Avoid very long keys | 100–200 bytes typical | Multi-KB keys waste memory and bandwidth |

#### Key Expiration (TTL)
- Any key can have an expiration set via `EXPIRE`, `PEXPIRE`, `EXPIREAT`, or as part of `SET` (`EX`/`PX` options)
- Check remaining TTL with `TTL` (seconds) or `PTTL` (milliseconds)
- Remove expiration with `PERSIST`
- `-1` from TTL means no expiration set; `-2` means the key doesn't exist

```
SET session:abc123 "data" EX 3600     # Expires in 1 hour
EXPIRE user:1001:cache 300            # Expire existing key in 5 minutes
TTL session:abc123                    # Returns remaining seconds
PERSIST session:abc123                # Remove the expiration
```

#### Key Hashing (Redis Cluster)

In a single Redis instance, all keys live on one server. In a Redis Cluster (multiple nodes), keys are distributed across 16,384 **hash slots** using CRC16:

```
slot = CRC16(key) % 16384
```

Each node in the cluster owns a range of slots. When you issue a command, Redis hashes the key to determine which node holds it.

**Hash Tags** — forcing related keys to the same slot:

By default, the entire key is hashed. If you need multiple keys on the same node (for multi-key operations like `SINTER`, `SUNION`, or transactions), use hash tags — the part between `{` and `}`:

```
user:{1001}:profile    → hashes "1001"
user:{1001}:sessions   → hashes "1001"  (same slot!)
user:{1001}:cart       → hashes "1001"  (same slot!)

user:{2002}:profile    → hashes "2002"  (different slot)
```

Only the substring inside `{}` is used for slot calculation, so all keys sharing the same hash tag land on the same node.

**Why this matters:**
- Multi-key commands (`MGET`, `SINTER`, `SDIFF`, `SUNION`) only work when all keys are on the same node
- Transactions (`MULTI/EXEC`) and Lua scripts can only operate on keys in the same slot
- Without hash tags, related keys may scatter across nodes, breaking these operations

**Example — our PoC keys in a cluster context:**
```
# These would need hash tags to work together in a cluster:
tags:{alice}           → all of alice's tag operations on one node
session:{abc123}       → session operations are already safe (single key)
leaderboard            → single key, always on one node

# If you needed to compare two users' tags:
tags:{alice}   and   tags:{bob}   → different slots! SINTER would fail in cluster mode
```

**Best practices for cluster-ready keys:**
- Use hash tags when you need multi-key operations on related data
- Keep hash tags consistent within a logical group
- Avoid putting all keys in one hash tag (defeats the purpose of sharding)
- Single-key operations (GET, SET, HGETALL) work fine without hash tags

#### Key Scanning and Pattern Matching
- `KEYS pattern` — Find all keys matching a glob pattern (blocking, avoid in production)
- `SCAN cursor [MATCH pattern] [COUNT hint]` — Iterative, non-blocking key scanning
- Patterns support `*` (any chars), `?` (single char), `[abc]` (character class)

```
SCAN 0 MATCH user:*:profile COUNT 100    # Iterate user profiles
SCAN 0 MATCH cache:product:*             # Find all cached products
```

#### Key Space Notifications
Redis can publish events when keys are modified or expired. Enable via config:
```
CONFIG SET notify-keyspace-events KEA
```
Clients can subscribe to channels like `__keyevent@0__:expired` to react when keys expire — useful for session timeouts, cache invalidation cascades, or delayed job processing.

#### Memory Considerations
- Each key has overhead beyond its name and value (~50–70 bytes per key for metadata)
- Millions of short-lived keys are fine — Redis handles key creation/deletion efficiently
- For very large key counts, consider using Hashes to group related small values (memory optimization via ziplist encoding)

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
