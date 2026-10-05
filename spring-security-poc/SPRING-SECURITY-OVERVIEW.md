# Spring Security — Overview

A living reference for learning Spring Security in this project.
This project uses **Spring Security 7.1.1** (via Spring Boot 4.1.1, Java 21).

---

## What is Spring Security?

Spring Security is a framework for **authentication** (who are you?) and
**authorization** (what are you allowed to do?) in Spring applications. It also
provides protection against common attacks (CSRF, session fixation, clickjacking,
etc.) out of the box.

When you add `spring-boot-starter-security` to the classpath, Spring Boot
auto-configures a sensible secure default:

- All endpoints require authentication.
- A login form (for browsers) and HTTP Basic (for API clients) are enabled.
- A single user `user` is created with a random password printed to the console.
- CSRF protection and security headers are on by default.

You saw this with the `/hello` endpoint — it's locked down until you log in.

---

## Core concepts

### Authentication vs Authorization

- **Authentication** — verifying identity (login, tokens, certificates).
- **Authorization** — deciding whether an authenticated identity may perform an
  action or access a resource.

### The Filter Chain

Spring Security works as a chain of **servlet filters**. Each incoming request
passes through a `SecurityFilterChain` — an ordered list of filters that handle
concerns like CSRF, authentication, and authorization. A request only reaches
your controller (e.g. `HelloController`) if it passes through the chain.

### Key building blocks

| Concept | Role |
| --- | --- |
| `SecurityFilterChain` | Defines which requests are secured and how. The modern replacement for extending `WebSecurityConfigurerAdapter` (removed in newer versions). |
| `AuthenticationManager` | Processes an authentication request. |
| `AuthenticationProvider` | Performs a specific type of authentication. The built-in `DaoAuthenticationProvider` covers username/password; you only write a custom one for a different auth *method* (LDAP, external IdP, OTP), not just because users are in a database. |
| `UserDetailsService` | Loads user data (username, password, authorities) by username. Providing this bean is all that's needed for DB-backed users. |
| `UserDetails` | Represents a user: credentials + granted authorities. |
| `PasswordEncoder` | Hashes and verifies passwords (use BCrypt — never plaintext). |
| `SecurityContext` / `SecurityContextHolder` | Holds the current authenticated principal for the request. |
| `GrantedAuthority` / roles | Permissions granted to a principal (e.g. `ROLE_ADMIN`). |

### The SecurityContext

Once authenticated, the principal is stored in the `SecurityContextHolder`.
You can access the current user from anywhere during request processing:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

---

## Configuring security (modern style)

In Spring Security 6+ / 7, you configure security by defining beans — primarily a
`SecurityFilterChain`. The lambda DSL is the current idiom:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/hello").permitAll()   // public
                .anyRequest().authenticated()             // everything else needs auth
            )
            .httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
```

> Version note: Most tutorials online target Spring Security 6 (Spring Boot 3.x).
> The config API here is nearly identical, but watch for small differences, since
> this project is on Security 7.

### In-memory users (handy for learning)

Instead of the random generated password, define known users:

```java
@Bean
UserDetailsService users() {
    UserDetails user = User.withUsername("user")
        .password("{noop}password")   // {noop} = no encoding, for demo only
        .roles("USER")
        .build();
    return new InMemoryUserDetailsManager(user);
}
```

> `{noop}` stores the password in plaintext — fine for a sandbox, never in
> production. Use a real `PasswordEncoder` (BCrypt) for anything real.

### Password encoding

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Database-backed users (what this project uses)

This project loads users from an in-memory **H2** database rather than defining
them in code or in `application.yaml`. The moving parts:

| Piece | Role |
| --- | --- |
| `AppUser` (`user` package) | JPA entity for a user: username, BCrypt password hash, roles, enabled. |
| `UserRepository` | Spring Data JPA repo with `findByUsername`. |
| `DatabaseUserDetailsService` | `UserDetailsService` that loads an `AppUser` and maps it to Spring's `UserDetails`. |
| `PasswordConfig` | Shared `BCryptPasswordEncoder` bean. |
| `UserDataSeeder` | `CommandLineRunner` that seeds users at startup. |

The important idea: **defining a `UserDetailsService` bean is all it takes.** It
is wired into the built-in `DaoAuthenticationProvider`, which does the actual
password check via the `PasswordEncoder`. You do **not** need a custom
`AuthenticationProvider` for database-backed username/password auth — that is
only needed when the authentication *method* differs (LDAP, external IdP,
one-time codes, etc.), not merely because users live in a database.

Two consequences worth remembering:

- Once this bean exists, Spring Boot's default single user backs off, so the
  `spring.security.user.*` properties no longer apply.
- Because `DatabaseUserDetailsService` is a plain shared bean (no `@Profile`),
  **every** security profile authenticates against these database users.

Seeded credentials (see `UserDataSeeder`):

| Username | Password | Roles |
| --- | --- | --- |
| `user` | `password` | `USER` |
| `admin` | `admin` | `ADMIN`, `USER` |

> The H2 web console is enabled at `/h2-console` (JDBC URL `jdbc:h2:mem:demo`).
> Whether you can reach it depends on the active security profile's rules.

### Self-service registration

`POST /register` creates a new user (role `USER`) in the database. The flow:
`RegistrationController` → `UserService.register` (checks for a duplicate
username, BCrypt-hashes the password, saves). The response never includes the
password or its hash.

```bash
curl -X POST -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"secret123"}' \
     http://localhost:8080/register
# 201 -> {"id":...,"username":"alice","roles":"USER"}
```

Behavior:

| Case | Result |
| --- | --- |
| Valid new user | `201 Created` (id, username, roles) |
| Duplicate username | `409 Conflict` |
| Invalid input (blank/short fields) | `400 Bad Request` (Bean Validation) |

Security points this endpoint illustrates:

- **It must be public.** Each authenticated profile explicitly permits
  `POST /register` (and `/error`) so an unauthenticated visitor can sign up. The
  rest of the app still requires authentication.
- **Input validation.** `RegistrationRequest` uses Bean Validation
  (`@NotBlank`, `@Size`), enforced by `@Valid`. A violation yields `400`.
- **CSRF exemption.** On the CSRF-enabled profiles (`formlogin`,
  `basicwithcsrf`), `/register` is added to `ignoringRequestMatchers`, so an API
  client can register without first fetching a token. (A public registration
  endpoint is a reasonable, common exemption.)
- **A subtle gotcha worth remembering:** when `@Valid` fails, Spring MVC
  forwards to `/error` to render the response. If `/error` isn't permitted, that
  internal forward gets challenged by the auth entry point and you see `401`
  instead of `400`. That's why `/error` is explicitly permitted in each profile.

---

## Switching configs with Spring profiles

This project keeps multiple security setups side by side and uses **Spring
profiles** to pick which one is active per run. Each config class is annotated
with `@Profile(...)`, so only the matching one is loaded into the context.

| Profile | Class | Behavior |
| --- | --- | --- |
| `formlogin` | `FormLoginSecurityConfig` | All requests require authentication; default login form. **(active by default)** |
| `permitall` | `PermitAllSecurityConfig` | All requests allowed through, no authentication. |
| `basic` | `BasicAuthSecurityConfig` | All requests require authentication via HTTP Basic (CSRF disabled). Good for `curl`/API testing. |
| `basicwithcsrf` | `BasicWithCsrfSecurityConfig` | Like `basic` but CSRF stays ON. POSTs need a token; demonstrates CSRF behavior. |
| `stateless` | `StatelessSecurityConfig` | HTTP Basic with `SessionCreationPolicy.STATELESS` — no session, no `JSESSIONID`. Mirrors token/JWT API setup. |

Example (`permitall`):

```java
@Configuration
@EnableWebSecurity
@Profile("permitall")
public class PermitAllSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

### Selecting a profile

The default is set in `application.yaml`:

```yaml
spring:
  profiles:
    active: formlogin   # options: formlogin | permitall | basic | basicwithcsrf | stateless
```

Override it per run without editing files:

```bash
# Gradle
./gradlew bootRun --args='--spring.profiles.active=permitall'

# Env var
SPRING_PROFILES_ACTIVE=permitall ./gradlew bootRun

# Packaged jar
java -jar app.jar --spring.profiles.active=permitall
```

### What to observe

- **`formlogin`** → hitting `/hello` redirects to a login page. Log in with the
  `spring.security.user` credentials from `application.yaml`.
- **`permitall`** → `/hello` returns `Hello, World!` immediately, no login.
- **`basic`** → `/hello` returns `401` without credentials; `curl -u user:password`
  (the `spring.security.user` values) returns `Hello, World!`.
- **`basicwithcsrf`** → `GET /hello` with creds works, but `POST /echo` with creds
  and no CSRF token returns `401` (a CSRF denial on this Basic-only chain is
  handled by the Basic auth entry point, so it surfaces as 401, not 403). Fetch a
  token from `GET /csrf` and send its `token` value in the `X-XSRF-TOKEN` header
  to make the POST succeed.
- **`stateless`** → `GET /hello` returns `401` without credentials and
  `Hello, World!` with `curl -u user:password`. Unlike the other authenticated
  profiles, a successful response sets **no `JSESSIONID` cookie** — nothing is
  remembered between requests.

> Keep exactly **one** security profile active at a time. If none is active,
> you fall back to Spring Boot's secure-by-default auto-configuration. To add
> another setup later, create a new `@Profile("...")` config class and give it
> its own profile name.

---

## Authorization styles

- **URL-based** — rules in the `SecurityFilterChain` via `requestMatchers(...)`.
- **Method-based** — annotations like `@PreAuthorize("hasRole('ADMIN')")` on
  service/controller methods. Enable with `@EnableMethodSecurity`.

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }
```

---

## Common built-in protections

- **CSRF** — on by default for browser (session) based auth. Often disabled for
  stateless APIs that use tokens. See the dedicated section below.
- **Session management** — session fixation protection, concurrent session
  control. See the dedicated section below.
- **Security headers** — e.g. `X-Content-Type-Options`, frame options
  (clickjacking protection).
- **CORS** — configurable alongside the filter chain.

---

## Default session management

By default, Spring Security persists the authentication in the `HttpSession` and
restores it on subsequent requests automatically — you don't need to do anything
for this to happen. This is the standard stateful web-app behavior you get out of
the box (it's what makes `/hello` stay authenticated after you log in once).

### How authentication is persisted

- The authenticated principal lives in the `SecurityContext`, which is stored in
  the `HttpSession` between requests via a `SecurityContextRepository`.
- On each request, the context is loaded from the session into the
  `SecurityContextHolder` for the duration of that request, then cleared.
- If you write a **custom filter or controller** that sets the security context
  yourself, you must save it explicitly through the `SecurityContextRepository` —
  it is no longer persisted implicitly.

### Session creation policy

Controlled by `SessionCreationPolicy`:

| Policy | Behavior |
| --- | --- |
| `IF_REQUIRED` | **Default.** Create a session only when one is needed. |
| `ALWAYS` | Always create a session. |
| `NEVER` | Never create one, but use an existing session if present. |
| `STATELESS` | Never create or use a session. Each request must re-authenticate (typical for token/JWT APIs). |

```java
http.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

The `stateless` profile (`StatelessSecurityConfig`) applies exactly this. You can
see the effect: a successful request under that profile sets no `JSESSIONID`
cookie, whereas `formlogin` and `basicwithcsrf` (session-backed) do.

### Session fixation protection

Enabled by default. On successful authentication, Spring Security changes the
session ID (migrating existing session attributes to the new session). This
prevents a session-fixation attack where an attacker tricks a victim into using
a session ID the attacker already knows. Strategies: `changeSessionId` (default),
`newSession`, `migrateSession`, and `none` (disables protection — avoid).

```java
http.sessionManagement(session -> session
    .sessionFixation(fixation -> fixation.changeSessionId()));
```

### Concurrent session control

You can limit how many simultaneous sessions a single user may have (for example,
to prevent the same account being used in many places at once):

```java
http.sessionManagement(session -> session
    .maximumSessions(1)                 // one active session per user
    .maxSessionsPreventsLogin(false));  // false = evict the oldest session;
                                        // true  = block the new login
```

### Version note (Security 6/7 vs 5)

The internals changed meaningfully in Spring Security 6 (which this project's
Security 7 builds on):

- `SecurityContextPersistenceFilter` and `SessionManagementFilter` are **no longer
  set by default**. An app should use `SecurityContextHolderFilter` instead (never
  both the holder and persistence filters).
- Previously (Security 5), `SessionManagementFilter` detected a fresh
  authentication and invoked the `SessionAuthenticationStrategy`, which meant the
  `HttpSession` was read on **every** request. In Security 6+, the authentication
  mechanism itself invokes the strategy, so the session no longer has to be read
  on every request.
- Because `SessionManagementFilter` isn't used by default, some older
  `sessionManagement` DSL methods (e.g. `sessionAuthenticationErrorUrl`,
  `sessionAuthenticationStrategy`) have no effect and are replaced by configuring
  handlers on the authentication mechanism instead.
- A `requireExplicitSave` setting exists mainly to preserve the old Security 5
  behavior during migration.

Source: [Spring Security reference — Authentication Persistence and Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
Content was rephrased for compliance with licensing restrictions.

---

## CSRF protection

### What CSRF is

Cross-Site Request Forgery tricks an authenticated user's browser into sending a
request the user didn't intend. Because browsers automatically attach stored
credentials (notably the session cookie) to any request going to a site, a
malicious page can submit a form or fire a request to your app and the browser
will include the victim's session — so the request arrives already
authenticated. The attacker can't read the response, but the state-changing
action (transfer funds, change email, delete something) still happens.

It maps to the OWASP categories around broken access control / misconfiguration
(see [`OWASP-TOP-10.md`](./OWASP-TOP-10.md)).

### How Spring Security defends against it

Spring Security uses the **synchronizer token pattern**: it issues a random CSRF
token tied to the user's session and expects that token echoed back on every
state-changing request (`POST`, `PUT`, `PATCH`, `DELETE`). Safe, read-only
methods (`GET`, `HEAD`, `OPTIONS`, `TRACE`) are not checked. A forged cross-site
request can ride along the session cookie but cannot know the token, so it is
rejected with `403 Forbidden`.

CSRF protection is **enabled by default**. With the default login form, Spring
injects the token as a hidden field automatically — which is why
`validate-formlogin.sh` has to scrape `_csrf` from the login page before it can
POST credentials.

### Where the token comes from

- Server-rendered forms (Thymeleaf, the default login page) include the token as
  a hidden `_csrf` field automatically.
- For JavaScript front-ends, the common approach is
  `CookieCsrfTokenRepository`, which exposes the token in a cookie
  (`XSRF-TOKEN`) that the client reads and sends back in a header
  (`X-XSRF-TOKEN`):

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
```

### When to disable it (and when not to)

The common rule: **keep CSRF on for anything that relies on cookies/sessions for
auth; it is safe to disable only for a truly stateless API** where every request
carries its own credential (e.g. an `Authorization: Bearer <token>` header) and
nothing is trusted from an ambient cookie.

```java
http.csrf(csrf -> csrf.disable()); // only for stateless, token-based APIs
```

> Disabling CSRF on a session/cookie-based app re-opens the exact hole CSRF
> protection closes. Don't disable it just to make a `POST` work — add the token
> instead.

### In this project

| Profile | CSRF | Why |
| --- | --- | --- |
| `formlogin` | **On** (default) | Session/cookie based; the login form carries the token. |
| `permitall` | On (default) | No auth, but the default protection is left untouched. |
| `basic` | **Off** | HTTP Basic is used by non-browser clients that send credentials on every request, so there's no ambient-cookie risk to protect against. This is why `curl -u` works without a token. |
| `basicwithcsrf` | **On** | Same HTTP Basic auth as `basic`, but CSRF is left enabled to demonstrate the effect. A `POST` without a token gets `401` (CSRF denial handled by the Basic auth entry point). Uses `CookieCsrfTokenRepository`; `GET /csrf` returns the token to send in the `X-XSRF-TOKEN` header. Note: the token from `/csrf` is XOR-encoded and differs from the raw `XSRF-TOKEN` cookie value — send the `/csrf` value. |
| `stateless` | **Off** | No session/cookie exists for a cross-site request to ride on, so CSRF protection is unnecessary. Credentials are sent explicitly on every request. |

See `BasicAuthSecurityConfig` for the `csrf(csrf -> csrf.disable())` call and the
reasoning in its Javadoc.

Source: [Spring Security reference — CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
Content was rephrased for compliance with licensing restrictions.

---

## Learning path / topics to explore

- [x] Default auto-configuration (observe it with `/hello`)
- [x] Custom `SecurityFilterChain` — make `/hello` public (`permitall` profile)
- [x] Password encoding with BCrypt (`PasswordConfig`, DB-backed users)
- [x] Form login vs HTTP Basic (`formlogin` vs `basic` profiles)
- [x] Custom `UserDetailsService` (users from a database — H2)
- [x] CSRF: when to keep it, when to disable it (`basicwithcsrf` vs `basic`)
- [ ] In-memory users and roles (using DB-backed users instead)
- [ ] Method-level security (`@PreAuthorize`)
- [ ] Role-based authorization (`hasRole`) using the seeded `USER`/`ADMIN`
- [ ] Stateless auth with JWT / OAuth2 resource server (`stateless` profile sets the groundwork)
- [ ] Testing with `spring-security-test` (`@WithMockUser`)

---

## References

- [Spring Security reference docs](https://docs.spring.io/spring-security/reference/)
- [Spring Security project page](https://spring.io/projects/spring-security)
- See also [`OWASP-TOP-10.md`](./OWASP-TOP-10.md) for the risks Spring Security helps mitigate.

> Note: External documentation links are provided for authoritative, up-to-date
> detail. Summaries here are paraphrased for quick reference.
