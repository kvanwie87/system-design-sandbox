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

## API Endpoints

### Server

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/webhooks/register` | Register a callback URL |

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
│       ├── service/
│       │   ├── StockEventSimulator.java
│       │   └── WebhookRegistry.java
│       └── util/
│           └── WebhookSignatureUtils.java
├── java-webhook-poc-client/
│   └── src/main/java/com/example/webhookclient/
│       ├── WebhookClientApplication.java
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
