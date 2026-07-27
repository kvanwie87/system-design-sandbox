---
inclusion: always
---

# Data Modeling Guide

## PostgreSQL (via Spring Data JPA + H2)

The relational store holds core application entities. Use H2 in-memory database to simulate PostgreSQL.

### Entities

| Entity    | Key Fields                                          | Relationships                     |
|-----------|-----------------------------------------------------|-----------------------------------|
| User      | id, username, email, password_hash                  | One-to-many: Posts, Comments, Shares |
| Post      | id, user_id, likes_count                            | Many-to-one: User; One-to-many: Media, Comments, Shares |
| Media     | id, post_id, media_type (photo/video), media_url    | Many-to-one: Post               |
| Comment   | id, post_id, user_id, comment_text                  | Many-to-one: Post, User         |
| Share     | id, post_id, user_id                                | Many-to-one: Post, User         |
| Follower  | id, followee_id, created_at                         | Many-to-one: User (followee)    |

### Guidelines

- Use `@ManyToOne` / `@OneToMany` JPA annotations for relationships
- Use `FetchType.LAZY` by default for collections
- Consider `@JoinColumn` to name foreign key columns explicitly
- Use `media_type` as an enum: `PHOTO`, `VIDEO`

## Redis (User Feed Cache)

- Model feed entries as a sorted set or list per user
- Key pattern: `feed:{userId}`
- Values: serialized post references (post IDs or lightweight DTOs)
- Use Spring Data Redis or a simple `RedisTemplate` approach
- Since this is a PoC, an embedded Redis mock (e.g., using a `Map`-based stub) is acceptable

Example
{
  "user_id": 56789,
  "feed": [
    {"post_id": 111, "user_id": 123, "media_url": "s3://path1", "caption": "Hello world"},
    {"post_id": 112, "user_id": 234, "media_url": "s3://path2", "caption": "Sunset view"}
  ]
}

## Neo4j (Social Graph)

- Model `User` as a node with label `User`
- Model `FOLLOWS` as a relationship between User nodes
- Store engagement score on the relationship for feed ranking
- Use Spring Data Neo4j `@Node` and `@Relationship` annotations

### Example Structure

```
(:User {userId, username}) -[:FOLLOWS {engagementScore}]-> (:User {userId, username})
```

Example "People You May Know"
```
MATCH (me:User {id:12345})-[:FOLLOWS]->(friend)-[:FOLLOWS]->(suggested)
WHERE NOT (me)-[:FOLLOWS]->(suggested)
RETURN suggested LIMIT 5
```
## Elasticsearch (Search Indexes)

### Search Index
- Index posts, users, and comments for full-text search
- Document fields should include text content and relevant metadata

Example
{
  "user_id": 12345,
  "username": "john_doe",
  "full_name": "John Doe",
  "bio": "Photographer | Traveler"
}

### Hashtags Index
- Index hashtags with associated post references
- Support trending topic queries and hashtag-based search

Example
{
  "hashtag": "#travel",
  "post_count": 1500000,
  "last_used": "2025-03-20T12:00:00Z"
}

### Guidelines

- Use `@Document(indexName = "...")` for each index
- Use `@Field(type = FieldType.Text, analyzer = "standard")` for searchable text fields
- Keep Elasticsearch documents denormalized for fast search access
