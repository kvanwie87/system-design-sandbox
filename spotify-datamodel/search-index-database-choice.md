# Search Index: Database Choice Analysis

## Characteristics of Search Data

**The data itself:**

- Denormalized view of the catalog — pulls fields from songs, artists, albums, and lyrics into a single searchable document.
- Read-only from the search perspective — the search index receives updates from the primary store (PostgreSQL) but never writes back.
- Text-heavy — song titles, artist names, album names, lyrics, genre tags. The core operation is matching user-typed text against this corpus.
- Requires fuzzy matching — users misspell names constantly ("Ariana Grandi," "bohemain rapsody"). The system must tolerate typos and partial input.
- Needs relevance scoring — raw text matches aren't enough. A search for "queen" should rank the legendary rock band above every song with "queen" in the title.
- Latency-critical — users expect results within 100ms, including autocomplete suggestions under 50ms as they type.

---

## Why Elasticsearch Fits

- **Inverted indexes** are purpose-built for text search. They map terms to the documents containing them, enabling instant lookups across 100M+ songs.
- **Fuzzy queries** handle typos natively. "bohemain" with edit distance 1 matches "bohemian" without extra application logic.
- **Phonetic matching** — analyzers like Soundex and Metaphone index words by how they sound, so "Cue" matches "Queue."
- **N-gram tokenization** powers autocomplete. "bohe" matches "bohemian" because the index stores partial prefixes.
- **Relevance scoring (BM25)** — Elasticsearch ranks results by how well they match, combining text relevance with custom signals like popularity and recency.
- **Horizontal scalability** — indexes are sharded across nodes. As the catalog grows, you add nodes to the cluster.
- **Near real-time indexing** — new songs are searchable within seconds of being added to the primary catalog.

---

## Why the Others Don't Fit Well

### PostgreSQL

- `LIKE '%query%'` doesn't use indexes and requires full table scans across 100M rows — far too slow for interactive search.
- PostgreSQL has full-text search (`tsvector`/`tsquery`), but it lacks fuzzy matching, phonetic analysis, and sophisticated relevance tuning out of the box.
- No n-gram tokenization for autocomplete without significant custom engineering.
- Can't easily combine text relevance with signals like popularity in a single ranked query.

### Cassandra

- Cassandra has no full-text search capability whatsoever. It's designed for key-based lookups, not text matching.
- You can't query "find all songs where title contains 'bohemian'" — Cassandra requires you to know the partition key upfront.
- No relevance scoring, no fuzzy matching, no text analysis.

### Redis

- Redis supports basic pattern matching on keys but has no concept of full-text search, relevance ranking, or fuzzy matching.
- Could cache *results* of popular searches (and should), but cannot perform the search itself.
- No inverted indexes, no text analysis pipeline.

### MongoDB

- MongoDB has a text search feature, but it's limited compared to Elasticsearch: basic tokenization, no phonetic matching, limited fuzzy support.
- Relevance scoring is rudimentary — no easy way to blend text relevance with popularity or personalization signals.
- At 100M documents with complex text queries, performance degrades compared to a purpose-built search engine.

---

## Relationship to PostgreSQL

Elasticsearch is **not** a replacement for PostgreSQL — it's a complement:

1. PostgreSQL remains the system of record for song metadata (source of truth).
2. An indexing pipeline listens for changes in PostgreSQL (new songs, updated metadata).
3. Changes are transformed into denormalized search documents and pushed to Elasticsearch.
4. Search queries hit Elasticsearch; detailed song data is fetched from PostgreSQL (or cache) after results are identified.

This separation means each system is optimized for its specific workload without compromise.

---

## The Bottom Line

Search is a specialized workload that requires inverted indexes, text analysis pipelines, fuzzy matching, and relevance scoring. Elasticsearch is purpose-built for exactly this. General-purpose databases can do basic text matching, but none can deliver the sub-100ms, typo-tolerant, relevance-ranked experience users expect from a modern search interface.
