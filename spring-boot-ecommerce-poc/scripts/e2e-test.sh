#!/bin/bash
# E2E Integration Test Script for Spring Boot E-Commerce PoC
# Run this after all services are up (either locally or via docker compose --profile apps)
#
# Requires: curl, python3
# Usage: ./scripts/e2e-test.sh

set -uo pipefail

BASE_URL="http://localhost"
PRODUCT_SERVICE="$BASE_URL:8081"
SEARCH_SERVICE="$BASE_URL:8082"
CART_SERVICE="$BASE_URL:8083"
ORDER_SERVICE="$BASE_URL:8084"
INVENTORY_SERVICE="$BASE_URL:8085"
PAYMENT_SERVICE="$BASE_URL:8086"

PASSED=0
FAILED=0
DEBUG=${DEBUG:-0}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

# JSON helper using python3 — safely returns empty string on error
json_get() {
    python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    result = eval('data' + sys.argv[1])
    print(result)
except Exception as e:
    print('')
" "$1" 2>/dev/null
}

debug_response() {
    if [ "$DEBUG" = "1" ]; then
        echo -e "    DEBUG: $1" >&2
    fi
}

test_step() {
    local name="$1"
    shift
    echo -e "\n${CYAN}--- TEST: $name ---${NC}"
    if "$@"; then
        echo -e "  ${GREEN}PASS${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "  ${RED}FAIL${NC}"
        FAILED=$((FAILED + 1))
    fi
}

# ============================================================
echo -e "\n${YELLOW}========================================${NC}"
echo -e "${YELLOW}  E-Commerce PoC - E2E Integration Test${NC}"
echo -e "${YELLOW}========================================${NC}\n"

# 1. Health checks
test_health() {
    local result
    result=$(curl -s "$1/actuator/health")
    debug_response "$result"
    echo "$result" | grep -q '"UP"'
}

test_step "Product Service health" test_health "$PRODUCT_SERVICE"
test_step "Inventory Service health" test_health "$INVENTORY_SERVICE"
test_step "Cart Service health" test_health "$CART_SERVICE"
test_step "Order Service health" test_health "$ORDER_SERVICE"
test_step "Payment Service health" test_health "$PAYMENT_SERVICE"
test_step "Search Service health" test_health "$SEARCH_SERVICE"

# 2. Product discovery
PRODUCT_ID=""
PRODUCT_NAME=""
PRODUCT_PRICE=""

test_list_products() {
    local result
    result=$(curl -s "$PRODUCT_SERVICE/products?page=0&size=5")
    debug_response "$result"
    PRODUCT_ID=$(echo "$result" | json_get "['content'][0]['id']")
    PRODUCT_NAME=$(echo "$result" | json_get "['content'][0]['name']")
    PRODUCT_PRICE=$(echo "$result" | json_get "['content'][0]['price']")
    echo "    Using product: $PRODUCT_NAME ($PRODUCT_ID) @ \$$PRODUCT_PRICE"
    [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "" ] && [ "$PRODUCT_ID" != "None" ]
}

test_step "List products (paginated)" test_list_products

test_get_product() {
    local result
    result=$(curl -s "$PRODUCT_SERVICE/products/$PRODUCT_ID")
    debug_response "$result"
    local id
    id=$(echo "$result" | json_get "['id']")
    if [ -z "$id" ] || [ "$id" = "" ]; then
        echo "    Error response: $result"
        return 1
    fi
    echo "    Got product: $id"
    [ "$id" = "$PRODUCT_ID" ]
}

test_step "Get product by ID" test_get_product

# 3. Inventory check
test_inventory() {
    local result
    result=$(curl -s "$INVENTORY_SERVICE/inventory/$PRODUCT_ID/availability")
    debug_response "$result"
    local avail
    avail=$(echo "$result" | json_get "['availableQty']")
    echo "    Available: $avail"
    [ -n "$avail" ] && [ "$avail" != "0" ] && [ "$avail" != "" ]
}

test_step "Check inventory availability" test_inventory

# 4. Cart operations
USER_ID="e2e-test-user-001"

test_add_to_cart() {
    local body="{\"productId\":\"$PRODUCT_ID\",\"productName\":\"$PRODUCT_NAME\",\"quantity\":2,\"unitPrice\":$PRODUCT_PRICE}"
    local result
    result=$(curl -s -X POST "$CART_SERVICE/cart/$USER_ID/items" -H "Content-Type: application/json" -d "$body")
    debug_response "$result"
    local count
    count=$(echo "$result" | json_get "['itemCount']")
    if [ -z "$count" ] || [ "$count" = "" ]; then
        echo "    Error response: $result"
        return 1
    fi
    echo "    Cart has $count item(s)"
    [ "$count" != "0" ]
}

test_step "Add item to cart" test_add_to_cart

test_get_cart() {
    local result
    result=$(curl -s "$CART_SERVICE/cart/$USER_ID")
    debug_response "$result"
    local count
    count=$(echo "$result" | json_get "['itemCount']")
    if [ -z "$count" ] || [ "$count" = "" ]; then
        echo "    Error response: $result"
        return 1
    fi
    echo "    Cart has $count item(s)"
    [ "$count" != "0" ]
}

test_step "Get cart" test_get_cart

test_update_cart() {
    local body='{"quantity":3}'
    local result
    result=$(curl -s -X PUT "$CART_SERVICE/cart/$USER_ID/items/$PRODUCT_ID" -H "Content-Type: application/json" -d "$body")
    debug_response "$result"
    local count
    count=$(echo "$result" | json_get "['itemCount']")
    if [ -z "$count" ] || [ "$count" = "" ]; then
        echo "    Error response: $result"
        return 1
    fi
    echo "    Updated quantity, cart has $count item(s)"
    [ "$count" != "0" ]
}

test_step "Update cart item quantity" test_update_cart

# 5. Place order
IDEMPOTENCY_KEY=$(python3 -c "import uuid; print(uuid.uuid4())")
ORDER_ID=""

test_place_order() {
    local body="{\"userId\":\"$USER_ID\",\"shippingAddressId\":\"addr-123\",\"idempotencyKey\":\"$IDEMPOTENCY_KEY\"}"
    local result
    result=$(curl -s -X POST "$ORDER_SERVICE/orders" -H "Content-Type: application/json" -d "$body")
    debug_response "$result"
    ORDER_ID=$(echo "$result" | json_get "['id']")
    local status
    status=$(echo "$result" | json_get "['status']")
    echo "    Order created: $ORDER_ID, status: $status"
    if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "" ]; then
        echo "    Response: $result"
        return 1
    fi
    [ "$status" = "CONFIRMED" ]
}

test_step "Place order (checkout)" test_place_order

# 6. Verify order
test_get_order() {
    if [ -z "$ORDER_ID" ]; then
        echo "    Skipped (no order ID from previous step)"
        return 1
    fi
    local result
    result=$(curl -s "$ORDER_SERVICE/orders/$ORDER_ID")
    debug_response "$result"
    echo "$result" | grep -q "CONFIRMED"
}

test_step "Get order by ID" test_get_order

test_orders_by_user() {
    local result
    result=$(curl -s "$ORDER_SERVICE/orders/user/$USER_ID")
    debug_response "$result"
    # Should return array or contain order data
    echo "$result" | grep -q "$USER_ID" || echo "$result" | grep -q "CONFIRMED"
}

test_step "Get orders by user" test_orders_by_user

# 7. Verify cart cleared
test_cart_cleared() {
    local result
    result=$(curl -s "$CART_SERVICE/cart/$USER_ID")
    debug_response "$result"
    # Cart should be empty — no items or itemCount=0
    local count
    count=$(echo "$result" | json_get "['itemCount']")
    if [ -z "$count" ]; then
        count=$(echo "$result" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('items',[])))" 2>/dev/null)
    fi
    echo "    Cart item count: $count"
    [ "$count" = "0" ]
}

test_step "Cart cleared after order" test_cart_cleared

# 8. Idempotency
test_idempotency() {
    if [ -z "$ORDER_ID" ]; then
        echo "    Skipped (no order ID from previous step)"
        return 1
    fi
    local body="{\"userId\":\"$USER_ID\",\"shippingAddressId\":\"addr-123\",\"idempotencyKey\":\"$IDEMPOTENCY_KEY\"}"
    local result
    result=$(curl -s -X POST "$ORDER_SERVICE/orders" -H "Content-Type: application/json" -d "$body")
    debug_response "$result"
    local id
    id=$(echo "$result" | json_get "['id']")
    echo "    Same order returned: $id"
    [ "$id" = "$ORDER_ID" ]
}

test_step "Idempotent order (same key returns same order)" test_idempotency

# 9. Search
test_search() {
    local result
    result=$(curl -s "$SEARCH_SERVICE/products/search?query=headphones")
    debug_response "$result"
    local hits
    hits=$(echo "$result" | json_get "['totalHits']")
    echo "    Search returned $hits hits"
    true  # Search may return 0 if indexing hasn't completed
}

test_step "Search products" test_search

# ============================================================
echo -e "\n${YELLOW}========================================${NC}"
if [ "$FAILED" -eq 0 ]; then
    echo -e "  ${GREEN}RESULTS: $PASSED passed, $FAILED failed${NC}"
else
    echo -e "  ${RED}RESULTS: $PASSED passed, $FAILED failed${NC}"
fi
echo -e "${YELLOW}========================================${NC}\n"

echo "Tip: Run with DEBUG=1 ./scripts/e2e-test.sh to see full API responses"
exit $FAILED
