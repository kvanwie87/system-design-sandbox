# Spotify System Design: Database Choice Analysis

A breakdown of why each data type in a Spotify-like music streaming platform maps to a specific database, based on its access patterns, consistency needs, and scale characteristics.

## Data Types

| Data Type | Database | Analysis |
|-----------|----------|----------|
| Song Metadata | PostgreSQL | [Read more](./song-metadata-database-choice.md) |
| Search Index | Elasticsearch | [Read more](./search-index-database-choice.md) |
| Playlists | Cassandra | [Read more](./playlists-database-choice.md) |
| User Sessions | Redis | [Read more](./user-sessions-database-choice.md) |
| Listening History | Cassandra | [Read more](./listening-history-database-choice.md) |
| Recommendations | Redis | [Read more](./recommendations-database-choice.md) |

## Key Concept: Polyglot Persistence

Rather than forcing all data into a single database, this design uses the right tool for each job:

- **PostgreSQL** — relational data with complex queries and strong consistency
- **Cassandra** — high write throughput with user-partitioned access patterns
- **Redis** — sub-millisecond reads for ephemeral or pre-computed data
- **Elasticsearch** — full-text search with fuzzy matching and relevance scoring

## Source

Based on the system design walkthrough at [AlgoMaster - Design Spotify](https://algomaster.io/learn/system-design-interviews/design-spotify).
