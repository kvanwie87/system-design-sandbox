# Recommendations: Database Choice Analysis

## Characteristics of Recommendation Data

**The data itself:**

- Pre-computed — recommendations (like "Discover Weekly") are generated offline by ML pipelines, not computed on-the-fly when users request them. The heavy lifting happens in batch jobs hours or days before serving.
- Read-heavy, write-infrequent — recommendations are written once per refresh cycle (daily or weekly) and read thousands of times by the user across sessions.
- Latency-critical on reads — when a user opens the app, their personalized recommendations must load instantly (< 10ms). Any delay makes the home screen feel sluggish.
- Simple access pattern — always fetched by user_id. "Get recommendations for user X" is the entire query. No joins, no filtering, no sorting at read time.
- Ephemeral with scheduled refresh — recommendations have a natural lifespan. "Discover Weekly" refreshes every Monday. "Daily Mix" refreshes every day. Old recommendations are replaced, not preserved.
- Moderate payload size — a recommendation set is typically 20-100 song IDs with metadata (reason, score), roughly 2-5KB per user per feature.

---

## Why Redis Fits

- **Sub-millisecond reads** — recommendations are the first thing users see when opening the app. Redis serves them from memory instantly, making the home screen feel responsive.
- **Simple key-value model** — `GET recommendations:{user_id}:discover_weekly` returns the pre-computed list. The access pattern is pure key-based lookup.
- **Native TTL** — set expiration aligned with refresh cycles. Discover Weekly gets a 7-day TTL, Daily Mix gets 24 hours. Stale recommendations auto-expire without cleanup logic.
- **Efficient for the data volume** — 500M users × 5KB of recommendations = ~2.5TB. Spread across a Redis cluster, this is manageable and cost-effective given the access frequency.
- **High throughput** — during peak hours when millions of users open the app simultaneously, Redis handles millions of reads per second across the cluster.
- **Data structures** — Redis sorted sets can store recommendations with scores, enabling quick "top N" retrieval or re-ranking without deserializing the entire payload.
- **Write pattern matches batch pipelines** — the ML pipeline generates recommendations in bulk and writes them to Redis in batch. Redis handles burst writes well.

---

## Why the Others Don't Fit Well

### PostgreSQL

- Adding 10ms+ of disk-based read latency to the home screen load path degrades user experience for no benefit.
- The query pattern (single key lookup, no joins, no filtering) uses none of PostgreSQL's strengths.
- Connection pool pressure — the home screen is loaded by every user on every app open. That's an enormous volume of simple lookups competing for database connections that are better used for complex catalog queries.
- No native TTL — you'd need a cron job to purge stale recommendations, adding operational complexity.
- PostgreSQL's ACID guarantees are unnecessary for data that's regenerated from scratch on every refresh cycle.

### Cassandra

- Could work — partition key = user_id, clustering key = recommendation_type. The access pattern fits.
- But Cassandra's read latency (2-10ms) is higher than Redis (< 1ms) for what is a latency-critical path.
- Cassandra shines for high write throughput and large datasets. Recommendations are small (~2.5TB) with infrequent writes (once per refresh cycle). The write optimization is wasted.
- Operational overhead of a Cassandra cluster isn't justified for data that fits in memory and benefits from in-memory speed.
- That said, Cassandra could serve as a **fallback** behind Redis — if the cache misses, fetch from Cassandra rather than recomputing.

### Elasticsearch

- Full-text search, relevance scoring, and fuzzy matching are completely irrelevant for serving pre-computed recommendation lists.
- Higher latency than Redis for simple key-based lookups.
- Resource-heavy for what amounts to "store this blob, return it when asked."
- No native TTL per document.

### MongoDB

- Could store recommendations as documents per user, but disk-based reads add latency to the critical home screen path.
- The flexible document model is unnecessary — recommendation data has a fixed, simple structure.
- Doesn't offer advantages over Redis for this specific access pattern (single key lookup, needs speed, has natural expiration).

---

## Architecture: How Recommendations Get Into Redis

```
ML Pipeline (Spark) → Batch Write → Redis Cluster → Serve to Users
                                         ↑
                              TTL handles expiration
```

1. **ML pipeline runs** (daily/weekly on Spark or similar):
   - Processes all user listening history
   - Trains/updates collaborative filtering and content-based models
   - Generates top-N recommendations per user

2. **Batch write to Redis:**
   - Pipeline writes results directly to Redis using bulk SET operations
   - Each key gets a TTL matching the refresh cycle
   - Old recommendations are overwritten (or expire naturally)

3. **Serving:**
   - User opens app → API calls `GET recommendations:{user_id}:discover_weekly`
   - Redis returns pre-computed list in < 1ms
   - On cache miss (new user, expired TTL), fall back to popularity-based defaults or trigger on-demand computation

---

## What Redis Stores

```json
// Key: recommendations:u_abc123:discover_weekly
// TTL: 604800 (7 days)
{
  "generated_at": "2024-03-11T00:00:00Z",
  "songs": [
    {"song_id": "s_001", "score": 0.95, "reason": "Similar to artists you love"},
    {"song_id": "s_002", "score": 0.91, "reason": "Popular in your genre"},
    {"song_id": "s_003", "score": 0.88, "reason": "Fans of Artist X also like this"}
  ]
}
```

Multiple recommendation types per user:
- `recommendations:{user_id}:discover_weekly` (refreshed weekly)
- `recommendations:{user_id}:daily_mix_1` (refreshed daily)
- `recommendations:{user_id}:release_radar` (refreshed weekly)

---

## The Bottom Line

Recommendations are pre-computed, accessed by key, need instant reads on the app's most visible surface, and have natural expiration cycles. Redis is the obvious fit: in-memory speed for the critical read path, native TTL for lifecycle management, and simple key-value semantics that match the access pattern perfectly. Disk-based databases add unnecessary latency to what should be the fastest read in the system, and their advanced features (queries, search, transactions) go entirely unused.
