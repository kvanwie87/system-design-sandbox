# spring-security-poc

A small sandbox project for learning the basics of Spring Security.

## Stack

- Java 21
- Spring Boot 4.1.1
- Spring Security 7.1.1 (via `spring-boot-starter-security`)
- Gradle (wrapper included)

> Note: Spring Boot 4.x / Spring Security 7 is a new generation. Most tutorials
> online target Spring Boot 3.x / Spring Security 6, so expect minor differences
> in the configuration API (for example, how `SecurityFilterChain` beans and the
> lambda DSL are written).

## Dependencies

Defined in `build.gradle`:

| Dependency | Purpose |
| --- | --- |
| `spring-boot-starter` | Core Spring Boot |
| `spring-boot-starter-web` | Web/MVC — provides endpoints for Security to protect |
| `spring-boot-starter-security` | Spring Security framework |
| `spring-boot-starter-data-jpa` | JPA/Hibernate — persistence for database-backed users |
| `spring-boot-starter-validation` | Bean Validation (`@Valid`) for request DTOs |
| `com.h2database:h2` | In-memory H2 database (runtime) |
| `org.projectlombok:lombok` | Boilerplate reduction (getters/builders on entities) |
| `spring-boot-starter-test` | Testing support |
| `spring-security-test` | Security test helpers (e.g. `@WithMockUser`) |

## Prerequisites

A JDK 21 installation. Verify Gradle can find it:

```bash
./gradlew --version
```

If you see an error like `JAVA_HOME is set to an invalid directory`, your
`JAVA_HOME` points somewhere that no longer exists. Set it to a valid JDK 21,
for example:

```bash
export JAVA_HOME="/c/sdkman/candidates/java/21.0.9-amzn"
```

## Running

```bash
./gradlew bootRun
```

The app runs with a selectable **security profile** (default `formlogin`). See
`SPRING-SECURITY-OVERVIEW.md` and `scripts/README.md` for the full list and how
to switch, or just use the helper scripts:

```bash
./scripts/run-basic.sh        # HTTP Basic — easiest for curl
./scripts/run-formlogin.sh    # default login form
```

### Users

Users are loaded from an in-memory **H2** database (seeded at startup by
`UserDataSeeder`), not from a generated password. Log in with:

| Username | Password | Roles |
| --- | --- | --- |
| `user` | `password` | `USER` |
| `admin` | `admin` | `ADMIN`, `USER` |

```bash
curl -u user:password http://localhost:8080/hello
```

New users can self-register via the public `POST /register` endpoint (creates a
`USER`-role account):

```bash
curl -X POST -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"secret123"}' \
     http://localhost:8080/register
# then authenticate as the new user
curl -u alice:secret123 http://localhost:8080/hello
```

The H2 console is available at `/h2-console` (JDBC URL `jdbc:h2:mem:demo`),
subject to the active profile's access rules.

## Building & Testing

```bash
./gradlew build   # compile + test
./gradlew test    # tests only
```
