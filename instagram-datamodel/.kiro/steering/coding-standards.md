---
inclusion: always
---

# Coding Standards

## Java Conventions

- Use Java 21 features where appropriate (records, sealed classes, pattern matching)
- Follow standard Java naming: `PascalCase` for classes, `camelCase` for methods/fields, `UPPER_SNAKE_CASE` for constants
- Use tabs for indentation (project default)
- Package structure: `com.example.demo.<feature>`

## Spring Data Conventions

- Entity classes go in a `model` or `entity` subpackage per data store context
- Repository interfaces go in a `repository` subpackage
- Use Spring Data repository interfaces (`JpaRepository`, `Neo4jRepository`, `ElasticsearchRepository`)
- Prefer derived query methods over `@Query` annotations for simple queries
- Use `@Query` annotations for complex or custom queries

## Entity Modeling

- JPA entities use `@Entity`, `@Table`, `@Id`, and `@GeneratedValue`
- Use `Long` for primary key types on relational entities
- Neo4j nodes use `@Node` and relationships use `@Relationship`
- Elasticsearch documents use `@Document` and `@Field`
- All entities should have a no-arg constructor (required by JPA/Spring Data)

## General Practices

- No Lombok — use explicit getters/setters or Java records where applicable
- Keep classes focused and small; prefer composition over inheritance
- Use constructor injection for Spring beans
- No service or controller layer needed — this is a data modeling PoC
- Tests can validate repository queries and entity relationships directly
