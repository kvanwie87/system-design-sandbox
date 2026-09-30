# Java Webhook PoC

A proof-of-concept demonstrating a webhook pattern using two Spring Boot applications. A **server** periodically simulates stock ticker events and broadcasts them to all registered **clients**, which log the events to the console.

## Architecture

```
┌─────────────────────┐         ┌─────────────────────┐
│   Webhook Server    │         │   Webhook Client    │
│     (port 8080)     │         │     (port 8081)     │
│                     │         │                     │
│  ┌───────────────┐  │  POST   │  ┌───────────────┐  │
│  │ Stock Event   │──┼────────►│  │  Receiver     │  │
│  │ Simulator     │  │ signed  │  │  Controller   │  │
│  └───────────────┘  │         │  └───────────────┘  │
│                     │         │                     │
│  ┌───────────────┐  │  POST   │  ┌───────────────┐  │
│  │ Registration  │◄─┼─────────│  │  Auto-Register│  │
│  │ Controller    │  │         │  │  on Startup   │  │
│  └───────────────┘  │         │  └───────────────┘  │
└─────────────────────┘         └─────────────────────┘
```

## Features

- **Auto-registration**: Client registers its callback URL with the server on startup
- **Periodic events**: Server generates random stock price updates every ~5 seconds
- **Broadcast**: Server dispatches events to all registered clients
- **HMAC-SHA256 signature verification**: Server signs payloads, client verifies before processing
- **Circuit breaker state machine**: Clients transition through ACTIVE → DEGRADED → SUSPENDED → REMOVED based on delivery failures and time
- **Self-healing**: Clients can re-register at any state to reset back to ACTIVE
- **Multiple clients**: Server supports broadcasting to any number of registered clients

## Stock Ticker Event Payload

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "STOCK_PRICE_UPDATE",
  "timestamp": "2026-08-12T14:30:00Z",
  "data": {
    "symbol": "AAPL",
    "price": 198.52,
    "change": 1.23,
    "percentChange": 0.62
  }
}
```

Symbols: AAPL, GOOGL, MSFT, AMZN, TSLA

## Prerequisites

- Java 21
- Gradle (wrapper included)

## Running

### 1. Start the server

```bash
cd java-webhook-poc-server
./gradlew bootRun
```

### 2. Start the client

```bash
cd java-webhook-poc-client
./gradlew bootRun
```

To run multiple client instances, override the port:

```bash
./gradlew bootRun --args='--server.port=8082'
./gradlew bootRun --args='--server.port=8083'
```

Each instance auto-registers its own callback URL with the server using its port.

The client will automatically register with the server on startup. Every ~5 seconds, you'll see stock events logged in the client console:

```
INFO  [STOCK EVENT] TSLA $342.17 (+2.45, 0.72%) at 2026-08-12T14:30:00Z
```

## Configuration

### Server (`java-webhook-poc-server/src/main/resources/application.yaml`)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Server HTTP port |
| `webhook.event.interval` | `5000` | Event generation interval in ms |
| `webhook.secret` | `my-super-secret-webhook-key-change-me` | HMAC-SHA256 signing key |
| `webhook.auth.mode` | `hand-rolled` | Registration auth strategy: `spring-security`, `hand-rolled`, or `none` |
| `webhook.auth.private-key-location` | `classpath:keys/private_key.pem` | RSA private key (PKCS#8 PEM) used to sign tokens |
| `webhook.auth.public-key-location` | `classpath:keys/public_key.pem` | RSA public key (X.509 PEM) used to verify tokens |
| `webhook.auth.issuer` | `webhook-auth` | `iss` claim minted and validated |
| `webhook.auth.audience` | `webhook-server` | `aud` claim minted and validated |
| `webhook.auth.ttl-seconds` | `300` | Lifetime of issued tokens |
| `webhook.auth.client-id` | `webhook-client` | Client id the token endpoint accepts |
| `webhook.auth.client-secret` | `webhook-client-secret-change-me` | Client secret the token endpoint accepts |
| `webhook.circuit-breaker.max-failures` | `3` | Consecutive failures before DEGRADED |
| `webhook.circuit-breaker.degraded-timeout` | `30000` | ms in DEGRADED before SUSPENDED |
| `webhook.circuit-breaker.suspended-timeout` | `60000` | ms in SUSPENDED before REMOVED |
| `webhook.circuit-breaker.check-interval` | `10000` | How often state transitions are checked |

### Client (`java-webhook-poc-client/src/main/resources/application.yaml`)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8081` | Client HTTP port |
| `webhook.server.url` | `http://localhost:8080` | Server base URL for registration |
| `webhook.secret` | `my-super-secret-webhook-key-change-me` | HMAC-SHA256 verification key |
| `webhook.auth.mode` | `hand-rolled` | Must match the server: `spring-security`, `hand-rolled`, or `none` |
| `webhook.auth.token-url` | `http://localhost:8080/oauth/token` | Server token endpoint (client-credentials grant) |
| `webhook.auth.client-id` | `webhook-client` | This client's id (must match server) |
| `webhook.auth.client-secret` | `webhook-client-secret-change-me` | This client's secret (must match server) |

## API Endpoints

### Server

| Method | Path | Description |
|--------|------|-------------|
| POST | `/oauth/token` | Issue a JWT via the client-credentials grant (guarded by client id/secret) |
| POST | `/api/webhooks/register` | Register a callback URL (requires a Bearer JWT unless `webhook.auth.mode=none`) |

**Registration payload:**
```json
{
  "callbackUrl": "http://localhost:8081/webhook/events"
}
```

### Client

| Method | Path | Description |
|--------|------|-------------|
| POST | `/webhook/events` | Receive webhook events (requires valid signature) |

## Signature Verification

All webhook callbacks include an `X-Webhook-Signature` header containing an HMAC-SHA256 signature:

```
X-Webhook-Signature: sha256=<hex-encoded-hmac>
```

The client verifies this signature using constant-time comparison before processing the event. Requests with missing or invalid signatures receive a `401 Unauthorized` response.

## Registration Endpoint Authentication (OAuth2 client-credentials + JWT)

The registration endpoint (`POST /api/webhooks/register`) is protected with a
short-lived **RS256 JWT** sent as a standard `Authorization: Bearer <jwt>` header.

Tokens are issued using the **OAuth2 client-credentials grant**, with the server
acting as the token authority:

```
1. Client  ──POST /oauth/token (client_id + client_secret)──▶  Server (token endpoint)
2. Server  ──mints & signs an RS256 JWT (private key), returns access_token──▶  Client
3. Client  ──POST /api/webhooks/register (Authorization: Bearer <jwt>)──▶  Server
4. Server  ──validates the JWT (public key), accepts the registration──▶  200 OK
```

### Asymmetric keys (RS256)

Signing and verification use an **RSA keypair**, not a shared secret:

- The **private key** signs tokens and lives only in the token endpoint. It never
  leaves the server.
- The **public key** verifies tokens. Anything that validates a token (the
  hand-rolled filter, the Spring resource server, or a downstream service) needs
  only the public key.

The consequence is the property a shared secret cannot provide: a verifier
holding the public key can **validate but not forge** tokens. A well-formed token
signed by any other key is rejected as an invalid signature.

The **client never signs anything**. It authenticates with its
`client_id`/`client_secret`, receives a finished token, caches it until shortly
before expiry, and attaches it to the registration call.

> PoC note: the RSA keypair (including the private key) is bundled under
> `src/main/resources/keys/` so the demo is self-contained. In a real system the
> private key would live only in the issuer, be provisioned from a secret manager,
> and never be committed to source — and verifiers would typically fetch the
> public key from a JWKS endpoint.

### Token endpoint

```
POST /oauth/token            (application/x-www-form-urlencoded)
grant_type=client_credentials&client_id=<id>&client_secret=<secret>

200 OK
{ "access_token": "<jwt>", "token_type": "Bearer", "expires_in": 300 }
```

Returns `401 invalid_client` for bad credentials and `400 unsupported_grant_type`
for any grant other than `client_credentials`. This endpoint is guarded by the
client credential itself (not by a JWT), as a real token endpoint is.

### Validation modes

How the server *validates* the presented JWT is selectable at runtime via
`webhook.auth.mode`, so the two approaches can be compared side by side:

| Mode | How the server validates | Notes |
|------|--------------------------|-------|
| `spring-security` | Spring Security OAuth2 **resource server** with a `JwtDecoder` configured with the RSA **public key** | The idiomatic framework approach |
| `hand-rolled` | A plain `OncePerRequestFilter` that verifies the RS256 signature with **pure-JDK code** (`java.security.Signature` + `java.util.Base64`) | No JWT library; shows the mechanics from first principles |
| `none` | No validation | Legacy open behavior |

Both authenticated modes verify the **same RS256 token** with the same public key
(`iss`, and `aud` are also checked), so a token issued by the server's endpoint
works against either mode — only the validation plumbing differs.

> Note: HMAC payload signing (the `X-Webhook-Signature` above) and JWT caller
> authentication are **independent** mechanisms that coexist here. The JWT
> authenticates the caller of the registration endpoint; the HMAC signature
> protects the integrity of the event payloads delivered to clients.

### Token claims

```
sub = webhook-client      # the authenticated client (from client_id)
iss = webhook-auth        # issuer (validated)
aud = webhook-server      # audience (validated)
iat = <now>               # issued-at
exp = <now + ttl>         # expiry (validated)
```

### Switching modes

Set the same mode on **both** apps (defaults to `hand-rolled`):

```bash
# Server
./gradlew bootRun --args='--webhook.auth.mode=spring-security'

# Client (same mode)
./gradlew bootRun --args='--webhook.auth.mode=spring-security'
```

Requests are rejected with `401` for a missing, malformed, or wrong-key token
(and, when a token is correctly signed, for a wrong issuer/audience or an expired
`exp`). Both modes log each decision (`ACCEPTED` / `REJECTED` with a reason) — see
`webhook.auth` logger levels in `application.yaml`.

### Demo script

`auth-demo.sh` builds both modules, boots the server and client in a chosen mode,
and runs a suite of assertions:

- **Token endpoint:** valid credentials issue a token; a bad secret is rejected.
- **Registration:** the server-issued token is accepted; missing, garbage, and
  wrong-key tokens are rejected `401`.
- **End-to-end:** the client fetches a token, auto-registers, and receives events.

The happy-path token is obtained from the server's `/oauth/token` endpoint (signed
with the server's real private key). The negative-path tokens are forged locally
with `openssl` using a **throwaway RSA key the server doesn't trust**, which
demonstrates that a token the server didn't sign is rejected. The script prints
both the server-side and client-side auth logs.

```bash
./auth-demo.sh                 # hand-rolled (default)
./auth-demo.sh spring-security
./auth-demo.sh none
```

Requirements: `bash`, `curl`, `openssl`, and a JDK 21 (auto-detected, or set
`JAVA_HOME`).

## Project Structure

```
java-webhook-poc/
├── java-webhook-poc-server/
│   └── src/main/java/com/example/webhookserver/
│       ├── WebhookServerApplication.java
│       ├── config/
│       │   └── JacksonConfig.java
│       ├── controller/
│       │   └── WebhookRegistrationController.java
│       ├── model/
│       │   ├── ClientState.java
│       │   ├── StockTickerData.java
│       │   ├── WebhookClient.java
│       │   ├── WebhookEvent.java
│       │   └── WebhookRegistration.java
│       ├── auth/                          # JWT auth for the registration endpoint
│       │   ├── AuthConfig.java            # wires spring-security | hand-rolled | none
│       │   ├── AuthProperties.java        # webhook.auth.* config
│       │   ├── RsaKeyProvider.java        # loads the bundled RSA keypair (PEM)
│       │   ├── TokenController.java       # POST /oauth/token (client-credentials, signs RS256 JWT)
│       │   ├── HandRolledJwtFilter.java   # pure-JDK RS256 validation filter
│       │   └── JwtUtils.java              # pure-JDK RS256 mint/verify
│       ├── service/
│       │   ├── StockEventSimulator.java
│       │   └── WebhookRegistry.java
│       └── util/
│           └── WebhookSignatureUtils.java
├── java-webhook-poc-client/
│   └── src/main/java/com/example/webhookclient/
│       ├── WebhookClientApplication.java
│       ├── auth/                          # fetches, caches & attaches the Bearer JWT
│       │   ├── AuthProperties.java
│       │   └── TokenProvider.java         # calls /oauth/token, caches the token
│       ├── config/
│       │   └── JacksonConfig.java
│       ├── controller/
│       │   └── WebhookReceiverController.java
│       ├── model/
│       │   ├── StockTickerData.java
│       │   └── WebhookEvent.java
│       ├── service/
│       │   └── WebhookRegistrationService.java
│       └── util/
│           └── WebhookSignatureUtils.java
├── auth-demo.sh                           # boots both apps and exercises the auth
└── README.md
```

## Notes: Failure Handling Strategies for Webhook Systems

This PoC implements **Option 5 (Exponential Backoff + State Machine)** with accelerated timeframes for demo purposes. Below are the five strategies considered for handling unresponsive clients:

### Option 1: Exponential Backoff with Circuit Breaker

Mark a client as "degraded" after delivery failures and back off attempts for that specific client (skip 1, then 2, then 4 dispatch cycles). Avoids hammering a temporarily-down client while still eventually removing truly dead endpoints.

### Option 2: Time-Window Based Failure Tracking

Track failures within a sliding time window rather than consecutive counts — e.g., "if 5 out of the last 10 deliveries in the past hour failed, disable." More forgiving of intermittent network blips while still catching dead endpoints.

### Option 3: Dead Letter Queue + Health Check Ping

Before fully unregistering, move the client to a "suspended" list. Periodically ping suspended clients with a lightweight `GET /health` request. If they respond, re-activate them. Only permanently remove after they've been suspended for a configurable period (e.g., 24 hours).

### Option 4: Client-Initiated Heartbeat

Flip the responsibility — require clients to periodically call a `POST /api/webhooks/heartbeat` endpoint. If a client misses N heartbeats (determined by the timeout window), the server suspends delivery. This decouples failure detection from event delivery entirely.

**Pros:** Simple, clean separation of concerns, no retry logic needed on the server.  
**Cons:** Requires client cooperation. A misbehaving client that heartbeats but rejects events won't be caught.

### Option 5: State Machine with Time-Based Escalation (implemented)

Each client transitions through states based on delivery failures and elapsed time:

```
ACTIVE → DEGRADED → SUSPENDED → REMOVED
         (3 fails)   (30s)       (60s)
```

- **ACTIVE**: Normal delivery. After 3 consecutive failures → DEGRADED
- **DEGRADED**: Still receives events (allows recovery). Successful delivery → back to ACTIVE. No recovery after 30s → SUSPENDED
- **SUSPENDED**: No events dispatched. No re-registration after 60s → REMOVED (permanently evicted)
- **Re-registration**: Client can call `POST /api/webhooks/register` at any state to reset to ACTIVE

**PoC timeframes** (configurable via `webhook.circuit-breaker.*`):
- 3 failures to degrade
- 30 seconds degraded before suspension
- 60 seconds suspended before removal
- State checked every 10 seconds

**Production timeframes** would typically be:
- 5-10 failures to degrade
- 1 hour degraded before suspension
- 24 hours suspended before removal
