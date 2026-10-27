# Spring Security: Method-Level Security

Method-level security lets you enforce authorization rules directly on individual
methods using annotations, rather than only at the URL/endpoint level. The checks
run via Spring AOP proxies, so they apply to calls that pass through a
Spring-managed bean.

---

## Enabling Method Security

Method security is off by default. Turn it on with a configuration class:

```java
@Configuration
@EnableMethodSecurity   // Spring Security 6+ (prePostEnabled is true by default)
public class SecurityConfig {
}
```

For older Spring Security 5.x applications:

```java
@EnableGlobalMethodSecurity(prePostEnabled = true)
```

---

## The Core Annotations

### `@PreAuthorize`

Evaluates a SpEL (Spring Expression Language) expression **before** the method
runs. If it evaluates to `false`, the call is blocked with an
`AccessDeniedException`.

```java
@Service
public class AccountService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAccount(Long id) { ... }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<Account> listAll() { ... }

    @PreAuthorize("hasAuthority('account:write')")
    public void update(Account a) { ... }
}
```

### `@PostAuthorize`

Checks **after** the method runs. Useful when the decision depends on the
returned value, referenced as `returnObject`.

```java
@PostAuthorize("returnObject.owner == authentication.name")
public Document getDocument(Long id) { ... }
```

### `@PreFilter` / `@PostFilter`

Filter elements out of a collection argument (`@PreFilter`) or a returned
collection (`@PostFilter`) instead of blocking the whole call.

```java
@PostFilter("filterObject.owner == authentication.name")
public List<Document> getDocuments() { ... }
```

### `@Secured`

Older, simpler annotation. Only supports role checks — no SpEL expressions.

```java
@Secured("ROLE_ADMIN")
public void deleteAccount(Long id) { ... }
```

---

## Using Method Arguments in Expressions

SpEL expressions can reference method parameters with `#paramName` and the
current principal via `authentication`.

```java
// Allow only if the id passed in matches the logged-in user's id
@PreAuthorize("#userId == authentication.principal.id")
public User getProfile(Long userId) { ... }

// Reference a parameter's properties
@PreAuthorize("#account.owner == authentication.name")
public void closeAccount(Account account) { ... }

// Combine conditions
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public User getUser(Long userId) { ... }
```

- `#paramName` is resolved from the **method's arguments** at call time.
- `authentication.principal` comes from the stored authentication (see below).

---

## Where the Annotations Get Their Data

The annotations hold no data themselves. At evaluation time they read from the
`Authentication` object stored in the `SecurityContext`:

```java
SecurityContextHolder.getContext().getAuthentication()
```

That `Authentication` object exposes two things the annotations rely on:

- `getPrincipal()` — usually a `UserDetails`, available in SpEL as
  `authentication.principal`.
- `getAuthorities()` — a `Collection<GrantedAuthority>`, which is what
  `hasRole` / `hasAuthority` check against.

So `hasRole('ADMIN')` is effectively asking: *does the current Authentication's
authorities collection contain `ROLE_ADMIN`?*

---

## How the Data Gets Into the Authentication

During login, Spring Security populates the `Authentication` through this chain:

```
Login request
   │
   ▼
AuthenticationManager
   │
   ▼
AuthenticationProvider (e.g. DaoAuthenticationProvider)
   │
   ▼
UserDetailsService.loadUserByUsername(name)   ◄── your data source
   │
   ▼
returns a UserDetails (username, password, authorities)
   │
   ▼
Authentication created + stored in SecurityContext
```

The key hook is **`UserDetailsService`**. This is where your application decides
what authorities a user has, loading them from your data source (database,
directory service, etc.) and converting them into `GrantedAuthority` objects.

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));

        // Map stored roles to Spring authorities
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            authorities
        );
    }
}
```

End-to-end, the data path is:

```
Data store (e.g. user/role tables)
   → repository / lookup
   → UserDetailsService maps roles to GrantedAuthority ("ROLE_ADMIN")
   → stored in Authentication → SecurityContext
   → @PreAuthorize("hasRole('ADMIN')") reads it back at method-call time
```

---

## Key Points to Remember

- **The exact name is `@PreAuthorize`** (not `@PreAuthorized`).
- `hasRole('ADMIN')` automatically prepends `ROLE_`, so it matches the authority
  `ROLE_ADMIN`. `hasAuthority('ROLE_ADMIN')` is the equivalent without the
  auto-prefix. If your store keeps the role as `ADMIN`, persist/map it as the
  authority `ROLE_ADMIN`, or use `hasAuthority('ADMIN')` instead. Prefix
  mismatches are the most common cause of unexpected denials.
- Checks run through **Spring AOP proxies**, so an internal self-call
  (`this.method()`) bypasses them. The call must come through the proxied bean.
- A denied call throws `AccessDeniedException`, which Spring Security typically
  translates to an HTTP 403 response in a web application.
- `#param` values come from the **incoming method arguments**;
  `authentication.*` values come from the **stored authentication** in the
  `SecurityContext`.
