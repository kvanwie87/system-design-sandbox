# Song Metadata: Database Choice Analysis

## Characteristics of Song Metadata

**The data itself:**

- Highly relational — songs belong to albums, albums belong to artists, artists collaborate on songs. These are natural foreign key relationships.
- Relatively small — 100M songs × ~2KB each = ~200GB. This fits comfortably in a single well-provisioned database.
- Read-heavy, write-light — songs are added to the catalog daily (not per second). Once added, metadata rarely changes. The read-to-write ratio is extreme.
- Requires complex queries — "all songs by artist X," "albums released in 2024," "top 50 songs in genre Y," "songs on this album sorted by track number." These are multi-table joins with filtering and sorting.
- Needs strong consistency — if you update a song's title or an artist's profile, every user should see the same data immediately. There's no tolerance for one user seeing stale metadata.

---

## Why PostgreSQL Fits

- **Relational model maps naturally** to the domain. Songs, artists, and albums are distinct entities with well-defined relationships. Foreign keys enforce data integrity (you can't have a song pointing to a non-existent artist).
- **Rich query language** — SQL handles the diverse query patterns easily: joins across tables, aggregations, filtering by genre/date/popularity, pagination with OFFSET/LIMIT.
- **ACID transactions** — when you add a new album (insert album row + insert 12 song rows + update artist's discography), either all of it succeeds or none of it does.
- **Indexes** — B-tree indexes on artist_id, album_id, genre, release_date give you fast lookups without full table scans.
- **Read replicas** handle the read-heavy pattern naturally. One primary handles the low write volume; multiple replicas serve the high read traffic. No need for sharding complexity at 200GB.
- **Mature tooling** — migrations, backups, monitoring, and operational patterns are well-established.

---

## Why the Others Don't Fit Well

### Cassandra

- Designed for high write throughput and horizontal scaling — neither of which song metadata needs. The write volume is trivial.
- Requires you to design tables around query patterns. Each new query pattern might need a separate denormalized table. Song metadata has *many* query patterns (by artist, by album, by genre, by release date, by popularity), which would mean maintaining many duplicate tables.
- No joins. To get "all songs on this album with artist names," you'd either denormalize everything or make multiple round trips.
- Eventually consistent by default. A user could see stale metadata after an update.
- Overkill complexity for 200GB of mostly-static data.

### Redis

- In-memory only — 200GB of metadata in RAM is expensive and wasteful for data that doesn't need sub-millisecond latency on every access (caching the hot subset is fine, but it's not your primary store).
- No relational queries. You can look up by key, but "all songs in genre X released after 2020 sorted by play count" isn't something Redis does.
- No durability guarantees comparable to a relational database. Redis is a cache or session store, not a system of record.
- No schema enforcement — no way to ensure referential integrity between songs and artists.

### Elasticsearch

- Great for search, but not a primary data store. It can lose data during cluster issues and doesn't guarantee write durability the way PostgreSQL does.
- No transactional writes. Adding an album with its songs isn't atomic.
- Eventual consistency — writes aren't immediately visible to reads.
- No foreign key relationships or referential integrity.
- That said, it *complements* PostgreSQL perfectly as a read-optimized search index that receives updates from the primary store.

### MongoDB (Document Store)

- Could work for simple lookups, but the relational nature of the data means you'd either embed everything (duplicating artist data across thousands of songs) or use references (losing the join capability and doing multiple round trips).
- Schema flexibility is a liability here, not a benefit. Song metadata has a well-known, stable schema — you *want* the database to enforce it.
- Doesn't handle complex multi-entity queries as naturally as SQL.

---

## The Bottom Line

Song metadata is a textbook relational workload: stable schema, clear entity relationships, diverse query patterns, low write volume, strong consistency needs, and modest size. PostgreSQL was designed for exactly this kind of data. The other databases solve different problems — high write throughput (Cassandra), sub-millisecond caching (Redis), full-text search (Elasticsearch) — that don't align with what song metadata actually demands.
