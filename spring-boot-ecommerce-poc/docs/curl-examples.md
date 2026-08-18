# cURL Examples

Sample commands for every service endpoint. All examples assume services are running locally on their default ports.

---

## Product Service (port 8081)

### List products (paginated)
```bash
curl -s http://localhost:8081/products?page=0&size=5 | python3 -m json.tool
```

### List products filtered by category
```bash
curl -s "http://localhost:8081/products?category=Electronics&page=0&size=10" | python3 -m json.tool
```

### Get product by ID
```bash
curl -s http://localhost:8081/products/prod-wireless-headphones | python3 -m json.tool
```

### Create a product
```bash
curl -s -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "USB-C Hub",
    "description": "7-in-1 USB-C hub with HDMI, SD card, and USB-A ports",
    "category": "Electronics",
    "price": 45.99,
    "rating": 4.3,
    "reviewCount": 512,
    "sellerId": "seller_001",
    "attributes": {"ports": 7, "brand": "HubMax"}
  }' | python3 -m json.tool
```

### Update a product
```bash
curl -s -X PUT http://localhost:8081/products/prod-wireless-headphones \
  -H "Content-Type: application/json" \
  -d '{
    "price": 139.99,
    "description": "Updated: Premium noise-cancelling wireless headphones with 40hr battery"
  }' | python3 -m json.tool
```

### Health check
```bash
curl -s http://localhost:8081/actuator/health | python3 -m json.tool
```

---

## Search Service (port 8082)

### Search by keyword
```bash
curl -s "http://localhost:8082/products/search?query=headphones" | python3 -m json.tool
```

### Search with filters
```bash
curl -s "http://localhost:8082/products/search?query=wireless&category=Electronics&minPrice=30&maxPrice=200&minRating=4.0" | python3 -m json.tool
```

### Search sorted by price (ascending)
```bash
curl -s "http://localhost:8082/products/search?query=&sortBy=price_asc&limit=5" | python3 -m json.tool
```

### Search sorted by rating
```bash
curl -s "http://localhost:8082/products/search?query=&sortBy=rating&limit=10" | python3 -m json.tool
```

### Health check
```bash
curl -s http://localhost:8082/actuator/health | python3 -m json.tool
```

---

## Cart Service (port 8083)

### Add item to cart
```bash
curl -s -X POST http://localhost:8083/cart/user-001/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-wireless-headphones",
    "productName": "Wireless Headphones",
    "quantity": 2,
    "unitPrice": 149.99
  }' | python3 -m json.tool
```

### Add another item
```bash
curl -s -X POST http://localhost:8083/cart/user-001/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-bluetooth-speaker",
    "productName": "Bluetooth Speaker",
    "quantity": 1,
    "unitPrice": 49.99
  }' | python3 -m json.tool
```

### Get cart
```bash
curl -s http://localhost:8083/cart/user-001 | python3 -m json.tool
```

### Update item quantity
```bash
curl -s -X PUT http://localhost:8083/cart/user-001/items/prod-wireless-headphones \
  -H "Content-Type: application/json" \
  -d '{"quantity": 3}' | python3 -m json.tool
```

### Remove item from cart
```bash
curl -s -X DELETE http://localhost:8083/cart/user-001/items/prod-bluetooth-speaker | python3 -m json.tool
```

### Clear entire cart
```bash
curl -s -X DELETE http://localhost:8083/cart/user-001 -w "\nHTTP %{http_code}\n"
```

### Health check
```bash
curl -s http://localhost:8083/actuator/health | python3 -m json.tool
```

---

## Order Service (port 8084)

### Place an order (checkout)
First add items to cart (see Cart Service above), then:
```bash
curl -s -X POST http://localhost:8084/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "shippingAddressId": "addr-123",
    "idempotencyKey": "unique-key-001"
  }' | python3 -m json.tool
```

### Get order by ID
```bash
curl -s http://localhost:8084/orders/ORDER_ID_HERE | python3 -m json.tool
```

### Get orders by user
```bash
curl -s http://localhost:8084/orders/user/user-001 | python3 -m json.tool
```

### Idempotent retry (same key returns same order)
```bash
curl -s -X POST http://localhost:8084/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "shippingAddressId": "addr-123",
    "idempotencyKey": "unique-key-001"
  }' | python3 -m json.tool
```

### Health check
```bash
curl -s http://localhost:8084/actuator/health | python3 -m json.tool
```

---

## Inventory Service (port 8085)

### Check availability
```bash
curl -s http://localhost:8085/inventory/prod-wireless-headphones/availability | python3 -m json.tool
```

### Reserve inventory
```bash
curl -s -X POST http://localhost:8085/inventory/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-wireless-headphones",
    "quantity": 5,
    "orderId": "ord-test-001"
  }' | python3 -m json.tool
```

### Confirm reservation
```bash
curl -s -X POST http://localhost:8085/inventory/confirm \
  -H "Content-Type: application/json" \
  -d '{"orderId": "ord-test-001"}' -w "\nHTTP %{http_code}\n"
```

### Release reservation
```bash
curl -s -X POST http://localhost:8085/inventory/release \
  -H "Content-Type: application/json" \
  -d '{"orderId": "ord-test-001"}' -w "\nHTTP %{http_code}\n"
```

### Health check
```bash
curl -s http://localhost:8085/actuator/health | python3 -m json.tool
```

---

## Payment Service (port 8086)

### Process a payment (success)
```bash
curl -s -X POST http://localhost:8086/payments/charge \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ord-test-001",
    "amount": 159.99,
    "currency": "USD",
    "cardLast4": "4242",
    "idempotencyKey": "pay-key-001"
  }' | python3 -m json.tool
```

### Process a payment (forced failure — card ending in 0000)
```bash
curl -s -X POST http://localhost:8086/payments/charge \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ord-test-002",
    "amount": 99.99,
    "currency": "USD",
    "cardLast4": "0000",
    "idempotencyKey": "pay-key-002"
  }' | python3 -m json.tool
```

### Idempotent retry (same key returns cached result)
```bash
curl -s -X POST http://localhost:8086/payments/charge \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ord-test-001",
    "amount": 159.99,
    "currency": "USD",
    "cardLast4": "4242",
    "idempotencyKey": "pay-key-001"
  }' | python3 -m json.tool
```

### Get payment by ID
```bash
curl -s http://localhost:8086/payments/PAYMENT_ID_HERE | python3 -m json.tool
```

### Refund a payment
```bash
curl -s -X POST http://localhost:8086/payments/refund/PAYMENT_ID_HERE | python3 -m json.tool
```

### Health check
```bash
curl -s http://localhost:8086/actuator/health | python3 -m json.tool
```

---

## Full Purchase Flow (end-to-end)

Run these in sequence to simulate a complete purchase:

```bash
# 1. Browse products
curl -s "http://localhost:8081/products?page=0&size=3" | python3 -m json.tool

# 2. Check availability
curl -s http://localhost:8085/inventory/prod-wireless-headphones/availability | python3 -m json.tool

# 3. Add to cart
curl -s -X POST http://localhost:8083/cart/user-demo/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-wireless-headphones","productName":"Wireless Headphones","quantity":1,"unitPrice":149.99}' | python3 -m json.tool

# 4. View cart
curl -s http://localhost:8083/cart/user-demo | python3 -m json.tool

# 5. Place order
curl -s -X POST http://localhost:8084/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-demo","shippingAddressId":"addr-456","idempotencyKey":"demo-order-001"}' | python3 -m json.tool

# 6. Verify cart is cleared
curl -s http://localhost:8083/cart/user-demo | python3 -m json.tool

# 7. Check order history
curl -s http://localhost:8084/orders/user/user-demo | python3 -m json.tool

# 8. Verify inventory reduced
curl -s http://localhost:8085/inventory/prod-wireless-headphones/availability | python3 -m json.tool

# 9. Search for the product
curl -s "http://localhost:8082/products/search?query=headphones" | python3 -m json.tool
```
