# Playlists: Database Choice Analysis

## Characteristics of Playlist Data

**The data itself:**

- User-centric access — every query includes a user_id. "Show me my playlists," "load this playlist," "add a song to this playlist." You never query playlists across all users.
- High read and write throughput — 500M users actively creating, editing, and loading playlists. Users add songs, reorder tracks, and share playlists frequently.
- Ordered data — songs in a playlist have a position. The order matters and must be preserved.
- Variable size — playlists range from 3 songs to thousands. Some power users maintain playlists with 10,000+ tracks.
- Availability over consistency — if a playlist edit takes a second to propagate to another device, that's acceptable. Losing the edit is not.
- Needs horizontal scaling — as the user base grows from 100M to 500M, the database must scale without rewriting the application.

---

## Why Cassandra Fits

- **Partition key = user_id** — all of a user's playlists live on the same set of nodes. Loading "my playlists" is a single partition read, which is Cassandra's fastest operation.
- **Clustering keys preserve order** — within a partition, data is sorted by clustering columns. Playlist songs are clustered by `position`, so loading a playlist returns songs in correct order without application-side sorting.
- **Linear horizontal scaling** — need to handle 2x the users? Add nodes. Cassandra rebalances data automatically using consistent hashing. No downtime, no application changes.
- **High write throughput** — adding songs to playlists, reordering, creating new playlists — Cassandra handles this without locks or write contention.
- **Tunable consistency** — write with quorum for durability, read with ONE for speed. Or use LOCAL_QUORUM for a balance. You choose per query.
- **Multi-region replication** — built-in support for replicating across data centers, so users worldwide get low-latency access to their playlists.
- **Fault tolerant** — no single point of failure. Nodes can go down without affecting availability (replication factor = 3 means any 2 out of 3 replicas can serve reads).

---

## Why the Others Don't Fit Well

### PostgreSQL

- At 500M users with 5-10 playlists each, that's 2.5-5 billion playlist rows plus billions of playlist_song rows. A single PostgreSQL instance can't handle this volume.
- You'd need to shard, which PostgreSQL doesn't do natively. Manual sharding (by user_id) adds significant application complexity and operational burden.
- Write throughput becomes a bottleneck. A single primary handles all writes, and playlist editing is write-heavy.
- PostgreSQL's strengths (complex joins, rich queries) aren't needed here — playlist queries are simple and always scoped to one user.

### Redis

- Could handle the access pattern (key-value by user), but 2.5TB of playlist data doesn't belong in RAM. The cost would be astronomical.
- No built-in persistence guarantees. Redis persistence (RDB/AOF) is an afterthought, not a primary design goal. Losing a user's playlists is unacceptable.
- No native support for ordered lists within complex structures (you'd model it with sorted sets, but it gets awkward at scale).
- Redis works well as a *cache* in front of Cassandra for hot playlists, not as the primary store.

### Elasticsearch

- Not designed for CRUD operations. Elasticsearch is a search engine, not a transactional data store.
- No guarantees on write durability — segments can be lost during failures.
- Updating a single song's position in a playlist means reindexing the entire document.
- Overkill for simple key-based lookups that don't need text search or relevance scoring.

### MongoDB

- Could work for this use case — document model fits playlists reasonably well (a playlist document with an array of song references).
- However, MongoDB struggles with very large arrays. A playlist with 10,000 songs in a single document pushes against document size limits and makes partial updates expensive.
- Sharding in MongoDB is more complex operationally than Cassandra's approach.
- MongoDB's query flexibility (ad-hoc queries on any field) isn't needed — playlist access patterns are predictable and simple.

---

## Schema Design in Cassandra

**Playlists by user:**

```
PRIMARY KEY (user_id, created_at)
```

Partitioned by user, sorted by creation date. One query returns all playlists for a user.

**Songs in a playlist:**

```
PRIMARY KEY (playlist_id, position)
```

Partitioned by playlist, sorted by position. One query returns all songs in order.

This design means the two most common operations — "show my playlists" and "load this playlist's songs" — are each a single partition read.

---

## The Bottom Line

Playlist data has a clear partition key (user_id), needs high read/write throughput, must scale horizontally with user growth, and has simple, predictable query patterns. Cassandra is designed for exactly this workload. PostgreSQL would require complex sharding for scale, Redis is too expensive for persistent storage at this volume, and Elasticsearch isn't a transactional data store.
