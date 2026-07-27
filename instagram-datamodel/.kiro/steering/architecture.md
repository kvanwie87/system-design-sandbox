---
inclusion: always
---

# Architecture

## Polyglot Persistence

This project demonstrates polyglot persistence — using different data stores for different access patterns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                    │
├─────────────┬──────────────┬──────────────┬─────────────────┤
│  Spring     │  Spring      │  Spring      │  Spring         │
│  Data JPA   │  Data Neo4j  │  Data ES     │  Data Redis     │
├─────────────┼──────────────┼──────────────┼─────────────────┤
│  H2         │  Embedded    │  Embedded    │  In-memory      │
│  (PostgreSQL│  Neo4j       │  Elastic-    │  Redis mock     │
│   compat.)  │              │  search      │                 │
└─────────────┴──────────────┴──────────────┴─────────────────┘
```

## Data Store Responsibilities

| Data Store      | Purpose                            | Access Pattern                        |
|-----------------|-------------------------------------|---------------------------------------|
| PostgreSQL (H2) | Core entities and relationships     | CRUD, joins, transactional writes     |
| Neo4j           | Social graph (follow relationships) | Graph traversals, path queries        |
| Elasticsearch   | Search index and hashtags           | Full-text search, aggregations        |
| Redis           | User feed cache                     | Fast reads, sorted sets, TTL-based    |

## Package Structure

```
com.example.demo
├── entity/          # JPA entities (User, Post, Media, Comment, Share, Follower)
├── repository/      # JPA repositories
├── graph/
│   ├── node/        # Neo4j node entities
│   └── repository/  # Neo4j repositories
├── search/
│   ├── document/    # Elasticsearch documents
│   └── repository/  # Elasticsearch repositories
└── cache/           # Redis cache configuration and feed cache logic
```

## Design Principles

- Each data store is independent — no cross-store transactions
- Entities in different stores may share the same logical ID (e.g., userId) but are not JPA-linked across stores
- The JPA layer is the source of truth for core entities
- Neo4j mirrors follow relationships for efficient graph queries
- Elasticsearch indexes are denormalized projections of JPA data for search
- Redis caches are ephemeral and can be rebuilt from the JPA/Neo4j layers
