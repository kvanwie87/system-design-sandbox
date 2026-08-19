# OAuth 2.0 and JWT

## OAuth 2.0

OAuth 2.0 is an **authorization framework** (not authentication) that allows a third-party application to access a user's resources on another service without the user giving their credentials to that third party.

The classic example: "Sign in with Google" on a third-party app. The app never sees your Google password — instead it gets a token that grants limited access.

### Why "Framework" and Not "Protocol"

OAuth 2.0 is called a "framework" because it defines **roles, grant types, endpoints, and token concepts** but leaves many implementation details unspecified. It's a set of rules for how parties interact, not a complete plug-and-play solution.

#### What the framework specifies:

**1. Roles (who's involved)**
- Resource Owner, Client, Authorization Server, Resource Server

**2. Grant Types (how to get a token)**
- Authorization Code, Client Credentials, Device Code, Refresh Token
- These are the "flows" — step-by-step interaction patterns

**3. Endpoints (where to send requests)**
- `/authorize` — where the user is redirected to consent
- `/token` — where the client exchanges credentials for tokens
- The framework says these must exist but doesn't dictate exact URLs or response shapes beyond minimum required fields

**4. Token Concepts (what you get back)**
- Access token (short-lived, grants API access)
- Refresh token (long-lived, gets new access tokens)
- Scopes (limit what the token can do)
- The framework says tokens must exist but doesn't specify their format — could be JWT, opaque string, whatever

**5. Security Requirements (guardrails)**
- HTTPS required, redirect URI validation, state parameter for CSRF protection, etc.

#### What the framework does NOT specify:

- Token format (JWT? Opaque? Up to you)
- Token storage (database? In-memory? Your call)
- How the user authenticates (password? Biometric? Not OAuth's concern)
- User identity claims (that's OIDC's job)
- Token revocation mechanism (optional extension, RFC 7009)
- Introspection (optional extension, RFC 7662)
- Exact error response formats beyond a minimum set

**In short:** OAuth 2.0 gives you the skeleton — the actors, the choreography, and the constraints — but you fill in the muscles (token format, storage, auth mechanism, etc.). That's why implementations like Auth0, Keycloak, and Google's OAuth all behave somewhat differently while still being "OAuth 2.0 compliant." A true protocol (like HTTP) specifies exact byte-level behavior. OAuth 2.0 intentionally leaves room for decisions.

---

### Core Concepts

- **Resource Owner** — The user who owns the data.
- **Client** — The application requesting access (e.g., a mobile app, SPA).
- **Authorization Server** — Issues tokens after the user consents (e.g., Google's auth server).
- **Resource Server** — The API that holds the user's data and accepts tokens.

### How It Works (Authorization Code Flow)

This is the most common flow for server-side apps:

```
1. User clicks "Login with Google" on the Client
2. Client redirects user to Authorization Server with:
   - client_id, redirect_uri, scope, response_type=code
3. User authenticates with Google and consents to the scopes
4. Authorization Server redirects back to Client with an authorization code
5. Client exchanges the code for tokens (server-to-server call):
   - Sends: code + client_id + client_secret
   - Receives: access_token (+ optionally refresh_token)
6. Client uses the access_token to call the Resource Server API
7. Resource Server validates the token and returns data
```

### Other Flows

| Flow | Use Case |
|------|----------|
| Authorization Code + PKCE | SPAs, mobile apps (no client secret) |
| Client Credentials | Service-to-service (no user involved) |
| Device Code | Smart TVs, CLI tools (limited input) |
| Implicit (deprecated) | Was used for SPAs, replaced by PKCE |

---

## Where JWT Fits In

**OAuth 2.0 doesn't mandate a token format.** Access tokens could be opaque strings, database-backed references, or anything else. The spec doesn't care.

**JWT (JSON Web Token)** is a specific *token format* that many OAuth implementations choose to use for access tokens and ID tokens. It's a signed, base64-encoded JSON payload.

```
Header.Payload.Signature

eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyLTEyMyIsInNjb3BlIjoicmVhZCJ9.signature...
```

### Why Use JWT as the OAuth Token Format?

- **Stateless validation** — The Resource Server can verify the token by checking the signature (using the Authorization Server's public key) without making a network call back to the auth server.
- **Self-contained** — The token carries claims (user ID, scopes, expiration) inside itself.
- **Interoperable** — Standard format understood across languages and platforms.

### The Relationship

```
OAuth 2.0          = The protocol (who talks to whom, what flows exist)
JWT                = A token format (how the token is structured)
Access Token       = The concept (permission to access resources)

OAuth 2.0 + JWT    = Using JWT as the format for OAuth access tokens
```

They are **independent specs that are often used together** but don't require each other:
- You can do OAuth without JWT (use opaque tokens validated via introspection endpoint).
- You can use JWT without OAuth (e.g., a simple API that issues its own JWTs for authentication).

---

## OpenID Connect (OIDC)

One more piece that ties them together: **OIDC** is a layer on top of OAuth 2.0 that adds **authentication**. It introduces the `id_token`, which is *always* a JWT containing identity claims (who the user is).

- OAuth 2.0 = authorization (what can you access?)
- OIDC = authentication (who are you?) built on OAuth 2.0
- OIDC mandates JWT for the `id_token`
