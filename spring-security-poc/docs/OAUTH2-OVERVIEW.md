# OAuth2 — High-Level Overview

A learning reference for OAuth2: what it is, the roles and flows involved, how it
relates to OpenID Connect and JWT, and where this project fits.

---

## What is OAuth2?

OAuth2 is an **authorization framework** — a standard way to grant an application
**limited access** to resources **without sharing passwords**. The classic
example is "Log in with Google" or allowing an app to read your Google Calendar:
you authorize scoped access, and the app receives a **token**, never your
credentials.

The key distinction up front:

> OAuth2 is about **authorization** (what an app is allowed to do), not
> **authentication** (proving who you are). It's commonly used for login too, but
> that is technically **OpenID Connect (OIDC)** — a layer built on top of OAuth2
> (see below).

Instead of handing an app your password, a trusted **authorization server**
issues it an **access token** representing specific, limited permissions. The app
presents that token to access protected resources.

---

## The four roles

OAuth2 defines four participants:

| Role | Who it is | Example |
| --- | --- | --- |
| **Resource Owner** | The user who owns the data | You |
| **Client** | The app wanting access | A photo-printing website |
| **Authorization Server** | Authenticates the user and issues tokens | Google's login/consent server |
| **Resource Server** | Hosts the protected data/API | The Google Photos API |

The flow in one sentence: the **client** asks the **resource owner** for
permission; the owner authenticates with the **authorization server**; the server
issues the client an **access token**; the client uses that token to call the
**resource server**.

```
Resource Owner (you)
      │  1. authenticate + consent
      ▼
Authorization Server ──2. issues access token──► Client (app)
                                                   │
                                   3. calls API with token
                                                   ▼
                                             Resource Server (API)
                                                   │
                                   4. validates token, returns data
```

---

## Tokens

- **Access token** — the credential the client sends to the resource server to
  access protected resources. Short-lived. May be a JWT or an opaque string.
- **Refresh token** — a longer-lived credential used to obtain a new access
  token without the user logging in again.
- **ID token** (OIDC only) — a JWT describing *who the user is* (used for login,
  not for API access).

Scopes limit what a token can do (e.g. `read:calendar` vs `write:calendar`), so
access is narrow rather than all-or-nothing.

---

## Grant types (how a token is obtained)

OAuth2 defines several flows ("grants") for different situations:

| Grant | Use case |
| --- | --- |
| **Authorization Code** (+ **PKCE**) | The main flow for web, mobile, and SPA apps with a user. The client is redirected to the auth server, the user logs in and consents, and the client exchanges a one-time code for a token. PKCE is the modern default that makes this safe for public clients. |
| **Client Credentials** | Machine-to-machine; no user involved. One service authenticating to another. |
| **Refresh Token** | Exchange a refresh token for a fresh access token without re-login. |
| *Implicit*, *Resource Owner Password* | Legacy grants, now discouraged. |

---

## OAuth2 vs OIDC vs JWT (common confusions)

These three get mixed up constantly. They operate at different levels:

- **OAuth2** — the authorization *framework*: roles, grants, access tokens.
- **OIDC (OpenID Connect)** — an *authentication* layer on top of OAuth2. It adds
  an **ID token** (a JWT about the user) so OAuth2 can be used for login.
  "Sign in with Google" is really OIDC.
- **JWT** — just a token *format*. OAuth2 access tokens are *often* JWTs, but
  don't have to be — they can be opaque strings the resource server validates by
  calling the authorization server. See [`JWT-OVERVIEW.md`](./JWT-OVERVIEW.md).

| | What it is | Answers |
| --- | --- | --- |
| OAuth2 | Authorization framework | "What may this app do?" |
| OIDC | Auth layer on OAuth2 | "Who is this user?" |
| JWT | Token format | (a container, not a protocol) |

---

## Opaque tokens vs JWT (self-contained) tokens

How the resource server validates an access token depends on its format:

- **JWT (self-contained):** the token is signed and carries its claims. The
  resource server verifies the signature **locally** with the issuer's public key
  (fetched once from a JWKS endpoint) — no network call per request. Scales well.
- **Opaque:** the token is a random string with no readable content. The resource
  server must call the authorization server's **introspection** endpoint to check
  validity on each use. Simpler to revoke, but adds a round-trip.

This project uses JWTs, so verification is local and stateless.

---

## Where this project fits

In `JwtSecurityConfig`, the `.oauth2ResourceServer(...)` call makes this app a
**Resource Server** in OAuth2 terms — a service that accepts and validates access
tokens (JWTs) to protect its endpoints. That is one of the four roles above.

This sandbox is a **simplified, self-contained** case: the same app plays two
roles at once —

- **Authorization Server** — `POST /login` (`AuthController` + `TokenService`)
  authenticates the user and issues a signed token.
- **Resource Server** — `JwtSecurityConfig` validates that token on each request.

In a typical production setup those are **separate**: a dedicated authorization
server (Keycloak, Auth0, Cognito, Entra ID, or Spring Authorization Server) issues
tokens, and your API is *purely* a resource server that verifies them — fetching
the issuer's public keys from its JWKS endpoint:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-issuer/   # auto-discovers the JWKS
```

That separation is the "one issuer, many verifiers" model: many resource-server
instances verify tokens with the public key and never hold a signing key —
which is also what makes horizontal scaling and central key rotation clean.

> Note: the sandbox's `/login` is a minimal stand-in for an authorization server,
> not a full OAuth2 authorization-code flow. It demonstrates the resource-server
> half (token validation) with a convenient way to mint a token.

---

## References

- [OAuth 2.0 (oauth.net)](https://oauth.net/2/)
- [RFC 6749 — The OAuth 2.0 Authorization Framework](https://datatracker.ietf.org/doc/html/rfc6749)
- [OpenID Connect](https://openid.net/connect/)
- [Spring Security — OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- See also [`JWT-OVERVIEW.md`](./JWT-OVERVIEW.md),
  [`SPRING-SECURITY-OVERVIEW.md`](./SPRING-SECURITY-OVERVIEW.md), and
  [`ENCRYPTION-BASICS.md`](./ENCRYPTION-BASICS.md).

> Note: External links are provided for authoritative detail. Summaries here are
> paraphrased for quick reference.
