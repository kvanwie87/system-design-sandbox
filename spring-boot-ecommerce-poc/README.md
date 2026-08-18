# Spring Boot E-Commerce PoC

A proof-of-concept e-commerce system with 6 independent Spring Boot services communicating via REST and Kafka, backed by PostgreSQL, Redis, Elasticsearch, and Kafka (KRaft mode).

## Architecture

```
Product Service  :8081  - Catalog management, PostgreSQL + Redis cache
Search Service   :8082  - Elasticsearch full-text search, Kafka consumer
Cart Service     :8083  - Redis-backed shopping cart
Order Service    :8084  - Saga-based checkout orchestration
Inventory Service:8085  - Stock management with optimistic locking
Payment Service  :8086  - Mocked payment processor
```

## Tech Stack

- Java 21, Spring Boot 4.1, Gradle 9.5.1
- PostgreSQL 16 (separate schemas per service)
- Redis 7 (caching + cart storage)
- Elasticsearch 8.15 (product search)
- Apache Kafka 3.8 (KRaft mode, event streaming)

## Quick Start (Local Development)

### Prerequisites
- Java 21+
- Docker & Docker Compose

### 1. Start infrastructure
```powershell
.\scripts\start-infra.ps1
```
This starts PostgreSQL, Redis, Elasticsearch, and Kafka. Services run locally via Gradle.

### 2. Build all modules
```bash
./gradlew build -x test
```

### 3. Run services individually
```bash
./gradlew :product-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :cart-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :order-service:bootRun
./gradlew :search-service:bootRun
```

Each service uses Spring Boot Docker Compose integration to auto-discover the running containers.

## Full Stack (Docker Compose)

Build and run everything in containers using the `apps` profile:
```powershell
.\scripts\start-all.ps1
```

Or directly:
```bash
docker compose --profile apps up --build -d
```

## Helper Scripts

| Script | Description |
|--------|-------------|
| `scripts\start-infra.ps1` | Start infrastructure only (for local dev) |
| `scripts\start-all.ps1` | Start infra + all 6 services containerized |
| `scripts\stop-all.ps1` | Stop all containers |
| `scripts\clean.ps1` | Stop all and remove volumes (full reset) |

## E2E Test

After all services are running:
```powershell
.\e2e-test.ps1
```

The test exercises the full purchase flow:
1. Health check all services
2. Browse products
3. Check inventory
4. Add items to cart
5. Place order (saga checkout)
6. Verify order created, cart cleared
7. Test idempotency
8. Search products

## API Examples

See [docs/curl-examples.md](docs/curl-examples.md) for sample cURL commands covering every endpoint across all 6 services, including a full end-to-end purchase flow.

## Key Design Patterns

### Saga Pattern (Order Service)
The checkout flow uses an orchestration-based saga:
1. Fetch cart from Cart Service
2. Reserve inventory (Inventory Service)
3. Process payment (Payment Service)
4. Create order record (local DB)
5. Confirm inventory reservation
6. Clear cart
7. Publish OrderCreatedEvent to Kafka

Compensation on failure:
- Payment fails → release inventory reservation
- Order creation fails → refund payment + release inventory

### Optimistic Locking (Inventory Service)
Uses `@Version` for concurrent reservation handling with automatic retry (up to 3 attempts).

### Reservation with TTL
Inventory reservations expire after 10 minutes. A scheduler runs every 60 seconds to release expired reservations back to available stock.

### Event-Driven Search Indexing
Product creates/updates publish `ProductUpdatedEvent` to Kafka. The Search Service consumes these events and updates the Elasticsearch index.

### Dead Letter Topics
Failed Kafka messages (e.g., deserialization errors) are routed to DLT topics after 3 retry attempts.

## API Endpoints

| Service | Endpoint | Description |
|---------|----------|-------------|
| Product | `GET /products` | List products (paginated) |
| Product | `GET /products/{id}` | Get product detail |
| Product | `POST /products` | Create product |
| Search | `GET /products/search?query=&category=&minPrice=&maxPrice=` | Full-text search |
| Cart | `GET /cart/{userId}` | Get cart |
| Cart | `POST /cart/{userId}/items` | Add to cart |
| Cart | `PUT /cart/{userId}/items/{productId}` | Update quantity |
| Cart | `DELETE /cart/{userId}/items/{productId}` | Remove item |
| Inventory | `GET /inventory/{productId}/availability` | Check stock |
| Inventory | `POST /inventory/reserve` | Reserve stock |
| Inventory | `POST /inventory/confirm` | Confirm reservation |
| Inventory | `POST /inventory/release` | Release reservation |
| Payment | `POST /payments/charge` | Process payment |
| Payment | `POST /payments/refund/{paymentId}` | Refund payment |
| Order | `POST /orders` | Place order (checkout) |
| Order | `GET /orders/{orderId}` | Get order |
| Order | `GET /orders/user/{userId}` | Order history |

## Configuration

### Payment Mock
Configure in `payment-service/src/main/resources/application.yaml`:
- `payment.mock.failure-rate`: Percentage of random failures (0-100)
- `payment.mock.latency-ms`: Simulated network latency
- `payment.mock.fail-card-suffix`: Card suffix that always fails (default: "0000")
