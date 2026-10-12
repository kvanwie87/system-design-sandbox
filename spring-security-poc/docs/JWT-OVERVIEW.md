# JWT — High-Level Overview

A learning reference for JSON Web Tokens (JWT) and how they're used for
authentication. JWTs build on general cryptography concepts — encryption,
symmetric vs asymmetric keys, and digital signatures — which are covered
separately in [`ENCRYPTION-BASICS.md`](./ENCRYPTION-BASICS.md).

---

## 1. Cryptography foundations (prerequisite)

JWTs rely on **digital signatures**, which in turn rely on symmetric or
asymmetric keys. If those terms aren't familiar, read
[`ENCRYPTION-BASICS.md`](./ENCRYPTION-BASICS.md) first. The essentials for JWTs:

- A **digital signature** proves a message's **integrity** (not altered) and
  **authenticity** (who produced it). It does **not** hide the message.
- **Symmetric** signing (one shared secret — JWT `HS256`): anyone who can verify
  can also sign.
- **Asymmetric** signing (private key signs, public key verifies — JWT `RS256` /
  `ES256`): only the private-key holder can sign, but anyone can verify. This
  gives **"one signer, many verifiers"** without sharing the signing secret.

> Signed, not encrypted: most JWTs are **signed, not encrypted**. The signature
> guarantees the claims weren't *changed*, not that they're *hidden* — the
> payload is readable by anyone.

---

## 2. What is a JWT?

A **JSON Web Token** is a compact, URL-safe token that carries a set of claims
(statements about a user or session) in a way that can be verified. It is widely
used for **stateless authentication**: the token itself holds the identity, so
the server doesn't need to store a session.

A JWT has **three parts**, separated by dots:

```
header.payload.signature
```

Each part is Base64URL-encoded:

- **Header** — metadata, including the signing algorithm (`alg`, e.g. `RS256`)
  and token type.
- **Payload** — the **claims**: data such as the subject (`sub`), issuer
  (`iss`), expiry (`exp`), issued-at (`iat`), and any custom fields like roles.
- **Signature** — a digital signature over the header + payload, created with
  the signing key, used to verify the token wasn't tampered with.

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhbGljZSIsImV4cCI6...}.<signature>
└──── header ─────┘ └────────── payload ──────────┘ └ signature ┘
```

> Signed, not secret: in a **signed** JWT (the usual kind), the payload is only
> **encoded**, not encrypted — anyone can decode and read the claims. The
> signature guarantees the claims weren't *changed*, not that they're *hidden*,
> so never put secrets in the payload. (Encrypted JWTs do exist and hide the
> payload, but they're far less common and out of scope here.)

---

## 3. Signing a JWT: symmetric vs asymmetric

The `alg` in the header decides which model is used.

- **Symmetric (`HS256`)** — one shared secret signs and verifies. Good when the
  **same party** both issues and validates tokens (a single self-contained app).
  Simpler, but every verifier must hold the signing secret.
- **Asymmetric (`RS256`, `ES256`)** — the issuer signs with a **private key**;
  any number of services verify with the **public key**. Good for **one issuer,
  many verifiers** (microservices, or an external identity provider). Verifiers
  can never mint tokens.

In OAuth2 / OpenID Connect, the issuer typically publishes its public keys at a
**JWKS** endpoint (`/.well-known/jwks.json`) so resource servers can fetch them
and verify tokens automatically, with no shared secret to distribute.

Which is more common? In production and the OAuth2/OIDC ecosystem (Auth0, Okta,
Keycloak, Cognito, Entra ID), **asymmetric `RS256` is the norm**. Symmetric
`HS256` shows up mostly in single-app setups and tutorials.

---

## 4. How JWT authentication flows

```
1. Login:    client sends credentials → server verifies, SIGNS a JWT, returns it
2. Request:  client sends the JWT on each call  →  Authorization: Bearer <jwt>
3. Verify:   server checks the signature + expiry, reads the claims, authorizes
             (no session lookup — the token carries the identity)
```

Because the server only needs the (public or shared) key to verify, it stores no
per-user session. This is the **stateless** model — contrast with session-based
auth, where the server keeps session state and the browser holds only a
`JSESSIONID` cookie.

### Relation to CSRF

JWTs are usually sent in the `Authorization` header, which the browser does
**not** attach automatically. That means a cross-site page can't forge an
authenticated request the way it can with an ambient session cookie, so
header-based JWT APIs generally don't need CSRF protection. (See the CSRF
section in `SPRING-SECURITY-OVERVIEW.md`.)

---

## 5. Tradeoffs and gotchas

- **Not encrypted by default.** Claims are readable by anyone. Don't store
  secrets in them; use HTTPS in transit.
- **Hard to revoke.** A signed JWT is valid until it expires. There's no
  server-side session to delete. Mitigations: short lifetimes plus refresh
  tokens, or a server-side denylist (which reintroduces some state).
- **Expiry matters.** Always set and check `exp`. Keep access-token lifetimes
  short.
- **Validate everything.** Verify the signature, the issuer (`iss`), the
  audience (`aud`), and expiry — not just that the token parses.
- **Algorithm confusion.** Reject unexpected `alg` values (e.g. a token claiming
  `alg: none`, or `HS256` where you expected `RS256`). Pin the expected
  algorithm.

---

## 6. In this project

The **`jwt` profile** implements this end to end with **RS256** (asymmetric),
self-issuing tokens — no external identity provider. The pieces:

| Piece | Role |
| --- | --- |
| `JwtKeyConfig` | Generates an RSA key pair at startup; exposes a `JwtEncoder` (signs with the private key) and `JwtDecoder` (verifies with the public key). |
| `AuthController` (`POST /login`) | Authenticates username/password against the DB-backed `UserDetailsService`, then issues a signed token. |
| `TokenService` | Builds the claims (`iss`, `sub`, `iat`, `exp`, `roles`) and signs them RS256. |
| `JwtSecurityConfig` | Stateless filter chain; validates incoming tokens as an OAuth2 **resource server**; maps the `roles` claim to authorities. |

Run it and try the flow:

```bash
./scripts/run-jwt.sh          # start under the jwt profile
# then: POST /login for a token, send it as Authorization: Bearer <token>
./scripts/validate-jwt.sh     # exercises the full flow + rejection cases
```

The RSA key pair is **hardcoded** in `JwtKeyConfig` (with a fixed `kid`). This is
a sandbox shortcut, but a deliberate one: a *stable* key means tokens survive
restarts and, crucially, every instance uses the **same** key — which is what
makes verification work when the app is **scaled horizontally** (a token issued
by one instance verifies on any other). A per-startup generated key would break
the moment you run more than one instance behind a load balancer.

> **Never hardcode a private key in production** — a committed key is a leaked
> secret. Real systems load key material from **shared secure storage** (a
> secrets manager / vault, identical across instances) or use a dedicated
> **issuer + JWKS** (see below), where these services only verify and never hold
> the signing key. Both approaches also enable central key rotation via the
> `kid` header.

### How a real resource server differs

Here the app both *issues* and *verifies* tokens with its own key. In a typical
production setup an external issuer (Keycloak, Auth0, Cognito, Entra ID) signs
the tokens, and your service only *verifies* them — configured declaratively,
with Spring fetching the issuer's public keys from its JWKS endpoint:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-issuer/   # auto-discovers the JWKS (asymmetric)
```

That's the "one issuer, many verifiers" model — your service never holds a
signing key, only the public key needed to verify.

---

## References

- [JWT introduction (jwt.io)](https://jwt.io/introduction)
- [RFC 7519 — JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)
- [Spring Security — OAuth2 Resource Server (JWT)](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- See also [`SPRING-SECURITY-OVERVIEW.md`](./SPRING-SECURITY-OVERVIEW.md) and
  [`OWASP-TOP-10.md`](./OWASP-TOP-10.md).

> Note: External links are provided for authoritative detail. Summaries here are
> paraphrased for quick reference.
