---
inclusion: always
---

# Implementation Plan — Instagram Data Model PoC

Build a Spring Boot proof-of-concept demonstrating polyglot persistence for an Instagram-like data model. All data stores use in-memory or embedded alternatives. On startup, the application seeds sample data into every repository and logs basic query results.

## File Structure

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── DataLoader.java
├── entity/
│   ├── User.java
│   ├── Post.java
│   ├── Media.java
│   ├── MediaType.java
│   ├── Comment.java
│   ├── Share.java
│   └── Follower.java
├── repository/
│   ├── UserRepository.java
│   ├── PostRepository.java
│   ├── MediaRepository.java
│   ├── CommentRepository.java
│   ├── ShareRepository.java
│   └── FollowerRepository.java
├── graph/
│   ├── node/
│   │   ├── UserNode.java
│   │   └── FollowsRelationship.java
│   └── repository/
│       └── UserNodeRepository.java
├── search/
│   ├── document/
│   │   ├── PostDocument.java
│   │   └── HashtagDocument.java
│   └── repository/
│       ├── PostDocumentRepository.java
│       └── HashtagDocumentRepository.java
└── cache/
    └── FeedCacheService.java
```

```
src/main/resources/
└── application.yaml
```

## Phase 1: Project Configuration

### 1.1 Update `build.gradle`
- Add `spring-boot-starter-data-redis`
- Add `com.h2database:h2` as runtimeOnly
- Add `spring-boot-starter-test` to testImplementation
- Keep existing Elasticsearch, JPA, Neo4j starters and test starters

### 1.2 Configure `application.yaml`
- H2 in-memory datasource (`jdbc:h2:mem:instagramdb`)
- JPA: `ddl-auto: create-drop`, `show-sql: true`
- H2 console enabled for debugging
- Neo4j bolt connection (embedded for tests)
- Elasticsearch URI
- Redis host/port
- Logging level `DEBUG` for `com.example.demo`

## Phase 2: JPA Entities (PostgreSQL via H2)

Package: `com.example.demo.entity`

| Entity   | Fields                                    | Relationships                              |
|----------|-------------------------------------------|--------------------------------------------|
| User     | id, username, email, passwordHash         | @OneToMany → Post, Comment, Share          |
| Post     | id, user, likesCount                      | @ManyToOne → User; @OneToMany → Media, Comment, Share |
| Media    | id, post, mediaType (enum), mediaUrl      | @ManyToOne → Post                          |
| Comment  | id, post, user, commentText               | @ManyToOne → Post, User                    |
| Share    | id, post, user                            | @ManyToOne → Post, User                    |
| Follower | id, followee, createdAt                   | @ManyToOne → User                          |

Supporting type: `MediaType` enum (`PHOTO`, `VIDEO`)

Conventions:
- `FetchType.LAZY` on all collections
- Explicit `@JoinColumn` names
- No Lombok — manual getters/setters
- `toString()` on all entities for logging

## Phase 3: JPA Repositories

Package: `com.example.demo.repository`

| Repository         | Key Query Methods                          |
|--------------------|--------------------------------------------|
| UserRepository     | findByUsername, findByEmail                 |
| PostRepository     | findByUser, findByLikesCountGreaterThan    |
| MediaRepository    | findByPost, findByMediaType                |
| CommentRepository  | findByPost, findByUser                     |
| ShareRepository    | findByPost, findByUser                     |
| FollowerRepository | findByFollowee                             |

## Phase 4: Neo4j Social Graph

### 4.1 Node Entity (`com.example.demo.graph.node`)
- `UserNode` (`@Node("User")`): userId, username, following list
- `FollowsRelationship` (`@RelationshipProperties`): engagementScore, targetUser

### 4.2 Repository (`com.example.demo.graph.repository`)
- `UserNodeRepository` extends `Neo4jRepository<UserNode, Long>`
- Methods: `findByUserId`, `findByUsername`
- Custom `@Query`: `findFollowing(userId)`, `findFollowers(userId)`

## Phase 5: Elasticsearch Indexes

### 5.1 Documents (`com.example.demo.search.document`)

| Document         | Index Name  | Fields                                   |
|------------------|-------------|------------------------------------------|
| PostDocument     | `posts`     | postId, username (Text), caption (Text), likesCount |
| HashtagDocument  | `hashtags`  | hashtag (Keyword), postIds, postCount    |

### 5.2 Repositories (`com.example.demo.search.repository`)

| Repository                | Key Query Methods                     |
|---------------------------|---------------------------------------|
| PostDocumentRepository    | findByUsername, findByCaptionContaining |
| HashtagDocumentRepository | findByHashtag                          |

## Phase 6: Redis Feed Cache

Package: `com.example.demo.cache`

- `FeedCacheService` (`@Service`) — in-memory `ConcurrentHashMap` stub
- Key pattern: `feed:{userId}` → list of post IDs
- Methods: `addToFeed`, `getFeed`, `clearFeed`, `getFeedSize`

## Phase 7: Data Seeding & Query Logging

`DataLoader` implements `ApplicationRunner` with constructor injection of all repositories.

### Seed Data

| Store         | Sample Data                                                |
|---------------|------------------------------------------------------------|
| JPA Users     | alice, bob, charlie                                        |
| JPA Posts     | 3 posts (2 by alice, 1 by bob) with varying likes         |
| JPA Media     | 2 photos, 1 video attached to posts                       |
| JPA Comments  | 3 comments across posts from different users              |
| JPA Shares    | 2 shares                                                   |
| JPA Followers | bob follows alice, charlie follows alice                   |
| Neo4j         | 3 UserNodes, FOLLOWS relationships with engagement scores |
| Elasticsearch | PostDocuments for all posts, HashtagDocuments (#sunset, #coding) |
| Redis Cache   | Feed entries for bob and charlie (posts from alice)        |

### Logged Queries

| Repository               | Query                              | Purpose                    |
|--------------------------|------------------------------------|----------------------------|
| UserRepository           | findByUsername("alice")            | Lookup user by username    |
| PostRepository           | findByUser(alice)                  | Posts by a user            |
| PostRepository           | findByLikesCountGreaterThan(20)    | Popular posts filter       |
| MediaRepository          | findByMediaType(PHOTO)             | Filter media by type       |
| CommentRepository        | findByPost(post1)                  | Comments on a post         |
| ShareRepository          | findByUser(charlie)                | Shares by a user           |
| FollowerRepository       | findByFollowee(alice)              | Who follows alice          |
| UserNodeRepository       | findFollowing(bob.userId)          | Bob's following (graph)    |
| UserNodeRepository       | findFollowers(alice.userId)        | Alice's followers (graph)  |
| PostDocumentRepository   | findByCaptionContaining("sunset")  | Full-text search           |
| HashtagDocumentRepository| findByHashtag("sunset")            | Hashtag lookup             |
| FeedCacheService         | getFeed(bob.id)                    | Cached feed for bob        |

## Phase 8: Verification

- `.\gradlew.bat build` — confirm compilation
- `.\gradlew.bat bootRun` — confirm startup, seeding, and query logs
