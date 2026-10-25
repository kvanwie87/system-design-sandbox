# Modeling User Roles with JPA

Two common ways to model user roles with JPA, from simplest to more flexible. Both produce normalized (3NF) schemas.

---

## Option 1: Roles as an enum collection (`@ElementCollection`)

A user owns a set of role **values**. There is no `Role` entity — the role is just an enum stored in a side table.

### JPA mapping

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)   // store the name, not the ordinal
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    // getters / setters
}

public enum Role {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_SELLER
}
```

### Underlying tables

**`users`**

| Column        | Type         | Constraints          |
|---------------|--------------|----------------------|
| id            | VARCHAR/UUID | PRIMARY KEY          |
| username      | VARCHAR      | UNIQUE, NOT NULL     |
| password_hash | VARCHAR      |                      |

**`user_roles`**

| Column  | Type         | Constraints                                   |
|---------|--------------|-----------------------------------------------|
| user_id | VARCHAR/UUID | NOT NULL, FK → users(id)                      |
| role    | VARCHAR      | NOT NULL (enum name, e.g. `ROLE_ADMIN`)       |
|         |              | PRIMARY KEY (user_id, role)                   |

Example rows:

```
users
id   | username | password_hash
-----+----------+---------------
u1   | alice    | $2a$...
u2   | bob      | $2a$...

user_roles
user_id | role
--------+-----------
u1      | ROLE_USER
u1      | ROLE_ADMIN
u2      | ROLE_USER
```

`user_roles` is **all-key** `(user_id, role)` with no non-key columns, so it is automatically in 3NF/BCNF.

**Use when:** roles are a fixed, compile-time set and carry no extra data. Simplest option; covers most apps.

---

## Option 2: Role as its own entity (`@ManyToMany`)

Promote `Role` to an entity so it can carry its own fields (description, etc.) and be managed in the database at runtime.

### JPA mapping

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // getters / setters
}

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String name;          // e.g. "ROLE_ADMIN"

    private String description;   // role-specific data lives here, once

    // getters / setters
}
```

### Underlying tables

**`users`**

| Column        | Type         | Constraints      |
|---------------|--------------|------------------|
| id            | VARCHAR/UUID | PRIMARY KEY      |
| username      | VARCHAR      | UNIQUE, NOT NULL |
| password_hash | VARCHAR      |                  |

**`roles`**

| Column      | Type         | Constraints          |
|-------------|--------------|----------------------|
| id          | VARCHAR/UUID | PRIMARY KEY          |
| name        | VARCHAR      | UNIQUE, NOT NULL     |
| description | VARCHAR      |                      |

**`user_roles`** (pure join table)

| Column  | Type         | Constraints                              |
|---------|--------------|------------------------------------------|
| user_id | VARCHAR/UUID | NOT NULL, FK → users(id)                 |
| role_id | VARCHAR/UUID | NOT NULL, FK → roles(id)                 |
|         |              | PRIMARY KEY (user_id, role_id)           |

Example rows:

```
users
id   | username | password_hash
-----+----------+---------------
u1   | alice    | $2a$...
u2   | bob      | $2a$...

roles
id   | name       | description
-----+------------+----------------------
r1   | ROLE_USER  | Standard user
r2   | ROLE_ADMIN | Full system access

user_roles
user_id | role_id
--------+--------
u1      | r1
u1      | r2
u2      | r1
```

Role data (`description`) is stored **once** per role in `roles`, so there is no duplication — this is exactly why you move here from Option 1 once roles need attributes.

**Use when:** roles are managed as data (created/edited at runtime) or carry their own fields.

---

## Choosing between them

| | Option 1 (enum collection) | Option 2 (Role entity) |
|---|---|---|
| Role defined in | Java enum (code) | Database row |
| Extra role data | Not supported cleanly | Supported (`roles` columns) |
| Add a role at runtime | Requires code change + redeploy | Insert a row |
| Tables | `users`, `user_roles` | `users`, `roles`, `user_roles` |
| Normalization | 3NF/BCNF | 3NF/BCNF |
| Complexity | Lowest | Moderate |

Start with Option 1 for a fixed role set. Move to Option 2 when roles need to be managed as data or carry their own attributes.
