# Instagram Data Model PoC

A proof-of-concept demonstrating polyglot persistence for an Instagram-like application using Spring Boot and Spring Data.

## Overview

This project models core Instagram entities across multiple data stores, each chosen for its strengths in handling specific access patterns:

| Data Store | Purpose | Implementation |
|---|---|---|
| PostgreSQL (H2) | Core entities and relationships | Spring Data JPA |
| Neo4j | Social graph (follow relationships) | Spring Data Neo4j |
| Elasticsearch | Full-text search and hashtags | Spring Data Elasticsearch |
| Redis | User feed cache | Spring Data Redis (RedisTemplate) |

No external infrastructure is required. All data stores run as Docker containers via Testcontainers.

## Prerequisites

- Java 21
- Docker (for Testcontainers)

## Running

Run the application locally with Testcontainers. This starts all containers, seeds sample data, logs query results, and shuts down:

```shell
.\gradlew.bat bootTestRun
```

On Linux/Mac:

```shell
./gradlew bootTestRun
```

## Testing

```shell
.\gradlew.bat test
```

Tests use `@SpringBootTest` with Testcontainers for Neo4j, Elasticsearch, and Redis. H2 is used in-memory for JPA.

## Building

```shell
.\gradlew.bat build
```

## Project Structure

```
src/main/java/com/example/demo/
├── DemoApplication.java          # Spring Boot entry point
├── DataLoader.java               # Seeds data and logs queries on startup
├── config/
│   └── JpaConfig.java            # Explicit JPA transaction manager
├── entity/                       # JPA entities (PostgreSQL via H2)
│   ├── User.java
│   ├── Post.java
│   ├── Media.java
│   ├── MediaType.java
│   ├── Comment.java
│   ├── Share.java
│   └── Follower.java
├── repository/                   # JPA repositories
│   ├── UserRepository.java
│   ├── PostRepository.java
│   ├── MediaRepository.java
│   ├── CommentRepository.java
│   ├── ShareRepository.java
│   └── FollowerRepository.java
├── graph/                        # Neo4j social graph
│   ├── Neo4jConfig.java
│   ├── node/
│   │   ├── UserNode.java
│   │   └── FollowsRelationship.java
│   └── repository/
│       └── UserNodeRepository.java
├── search/                       # Elasticsearch indexes
│   ├── document/
│   │   ├── PostDocument.java
│   │   └── HashtagDocument.java
│   └── repository/
│       ├── PostDocumentRepository.java
│       └── HashtagDocumentRepository.java
└── cache/                        # Redis feed cache
    ├── RedisConfig.java
    └── FeedCacheService.java
```

## Data Model

### JPA Entities (Relational)

- **User** - id, username, email, password_hash
- **Post** - id, user (FK), likes_count
- **Media** - id, post (FK), media_type (PHOTO/VIDEO), media_url
- **Comment** - id, post (FK), user (FK), comment_text
- **Share** - id, post (FK), user (FK)
- **Follower** - id, followee (FK), created_at

### Neo4j Graph

```
(:User {userId, username}) -[:FOLLOWS {engagementScore}]-> (:User {userId, username})
```

### Elasticsearch Documents

- **posts** index - postId, username, caption (full-text), likesCount
- **hashtags** index - hashtag, postIds, postCount

### Redis Cache

- Key pattern: `feed:{userId}` - list of post IDs (most recent first)

## Sample Output

On startup, the DataLoader seeds sample data and runs queries across all stores:

```
=== Starting Data Seeding ===
--- Seeding JPA (H2) Data ---
Seeded 3 users: alice, bob, charlie
Seeded 3 posts
Seeded 3 media items
Seeded 3 comments
Seeded 2 shares
Seeded 2 follower records
--- Seeding Neo4j Data ---
Seeded 3 user nodes with FOLLOWS relationships
--- Seeding Elasticsearch Data ---
Seeded 3 post documents
Seeded 5 hashtag documents
--- Seeding Redis Cache ---
Seeded feed cache for bob (2 posts) and charlie (2 posts)

=== Running Sample Queries ===
--- JPA Queries ---
findByUsername('alice'): User{id=1, username='alice', email='alice@example.com'}
findByUser(alice): 2 posts
findByLikesCountGreaterThan(20): 2 posts
findByMediaType(PHOTO): 2 items
findByPost(post1): 2 comments
findByUser(charlie) shares: 1
findByFollowee(alice): 2 followers
--- Neo4j Queries ---
findFollowing(bob): 1 -> [UserNode{userId=1, username='alice'}]
findFollowers(alice): 2 -> [bob, charlie]
--- Elasticsearch Queries ---
findByCaptionContaining('sunset'): 1 result
findByUsername('alice'): 2 results
findByHashtag('coding'): HashtagDocument{hashtag='coding', postCount=1}
--- Redis Cache Queries ---
getFeed(bob): 2 posts -> [2, 1]
getFeed(charlie): 2 posts -> [2, 1]

=== Data Seeding & Queries Complete ===
```

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Data JPA + H2
- Spring Data Neo4j
- Spring Data Elasticsearch
- Spring Data Redis
- Testcontainers (Neo4j 5, Elasticsearch 9, Redis 7)
- Gradle
