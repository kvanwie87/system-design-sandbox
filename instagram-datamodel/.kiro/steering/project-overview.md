---
inclusion: always
---

# Project Overview

This is a proof-of-concept project for data modeling an Instagram-like application using Spring Boot and Spring Data.

## Goals

- Model core Instagram entities (users, posts, media, comments, shares, followers)
- Demonstrate polyglot persistence using multiple data stores
- Use in-memory databases or mocks for all data sources (no external infrastructure required)
- Focus on data modeling and access patterns, not production deployment

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Data JPA (PostgreSQL entities via H2 in-memory)
- Spring Data Neo4j (social graph)
- Spring Data Elasticsearch (search and hashtag indexes)
- Redis (user feed cache)
- Gradle build system

## Key Reference

- #[[file:project.md]] contains the full entity specification and data store assignments
