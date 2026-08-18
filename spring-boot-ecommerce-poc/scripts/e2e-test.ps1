# E2E Integration Test Script for Spring Boot E-Commerce PoC
# Run this after all services are up (either locally or via docker-compose.services.yaml)
#
# Usage: .\e2e-test.ps1

$ErrorActionPreference = "Stop"
$BASE_URL = "http://localhost"
$PRODUCT_SERVICE = "$BASE_URL`:8081"
$SEARCH_SERVICE = "$BASE_URL`:8082"
$CART_SERVICE = "$BASE_URL`:8083"
$ORDER_SERVICE = "$BASE_URL`:8084"
$INVENTORY_SERVICE = "$BASE_URL`:8085"
$PAYMENT_SERVICE = "$BASE_URL`:8086"

$passed = 0
$failed = 0

function Test-Step {
    param([string]$Name, [scriptblock]$Block)
    Write-Host "`n--- TEST: $Name ---" -ForegroundColor Cyan
    try {
        & $Block
        Write-Host "  PASS" -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host "  FAIL: $_" -ForegroundColor Red
        $script:failed++
    }
}

function Invoke-Api {
    param([string]$Method, [string]$Url, [object]$Body)
    $params = @{ Uri = $Url; Method = $Method; ContentType = "application/json" }
    if ($Body) { $params.Body = ($Body | ConvertTo-Json -Depth 10) }
    Invoke-RestMethod @params
}

# ============================================================
Write-Host "`n========================================" -ForegroundColor Yellow
Write-Host "  E-Commerce PoC - E2E Integration Test" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Yellow

# 1. Health checks
Test-Step "Product Service health" {
    $r = Invoke-Api GET "$PRODUCT_SERVICE/actuator/health"
    if ($r.status -ne "UP") { throw "Not UP" }
}

Test-Step "Inventory Service health" {
    $r = Invoke-Api GET "$INVENTORY_SERVICE/actuator/health"
    if ($r.status -ne "UP") { throw "Not UP" }
}

Test-Step "Cart Service health" {
    $r = Invoke-Api GET "$CART_SERVICE/actuator/health"
    if ($r.status -ne "UP") { throw "Not UP" }
}

Test-Step "Order Service health" {
    $r = Invoke-Api GET "$ORDER_SERVICE/actuator/health"
    if ($r.status -ne "UP") { throw "Not UP" }
}

Test-Step "Payment Service health" {
    $r = Invoke-Api GET "$PAYMENT_SERVICE/actuator/health"
    if ($r.status -ne "UP") { throw "Not UP" }
}

Test-Step "Search Service health" {
    $r = Invoke-Api GET "$SEARCH_SERVICE/actuator/health"
    if ($r.status -ne "UP") { throw "Not UP" }
}

# 2. Product discovery
Test-Step "List products (paginated)" {
    $r = Invoke-Api GET "$PRODUCT_SERVICE/products?page=0&size=5"
    if ($r.content.Count -eq 0) { throw "No products found" }
    Write-Host "    Found $($r.totalElements) products"
    $script:productId = $r.content[0].id
    $script:productName = $r.content[0].name
    $script:productPrice = $r.content[0].price
    Write-Host "    Using product: $script:productName ($script:productId) @ `$$script:productPrice"
}

Test-Step "Get product by ID" {
    $r = Invoke-Api GET "$PRODUCT_SERVICE/products/$script:productId"
    if ($r.id -ne $script:productId) { throw "Wrong product returned" }
}

# 3. Inventory check
Test-Step "Check inventory availability" {
    $r = Invoke-Api GET "$INVENTORY_SERVICE/inventory/$script:productId/availability"
    Write-Host "    Available: $($r.availableQty), Reserved: $($r.reservedQty)"
    if ($r.availableQty -le 0) { throw "No stock available" }
}

# 4. Cart operations
$userId = "e2e-test-user-001"

Test-Step "Add item to cart" {
    $body = @{
        productId = $script:productId
        productName = $script:productName
        quantity = 2
        unitPrice = $script:productPrice
    }
    $r = Invoke-Api POST "$CART_SERVICE/cart/$userId/items" $body
    if ($r.itemCount -eq 0) { throw "Cart is empty after add" }
    Write-Host "    Cart has $($r.itemCount) item(s), subtotal: `$$($r.subtotal)"
}

Test-Step "Get cart" {
    $r = Invoke-Api GET "$CART_SERVICE/cart/$userId"
    if ($r.itemCount -eq 0) { throw "Cart is empty" }
}

Test-Step "Update cart item quantity" {
    $body = @{ quantity = 3 }
    $r = Invoke-Api PUT "$CART_SERVICE/cart/$userId/items/$script:productId" $body
    $item = $r.items | Where-Object { $_.productId -eq $script:productId }
    if ($item.quantity -ne 3) { throw "Quantity not updated" }
    Write-Host "    Updated quantity to 3"
}

# 5. Place order (happy path)
Test-Step "Place order (checkout)" {
    $idempotencyKey = [guid]::NewGuid().ToString()
    $body = @{
        userId = $userId
        shippingAddressId = "addr-123"
        idempotencyKey = $idempotencyKey
    }
    $r = Invoke-Api POST "$ORDER_SERVICE/orders" $body
    if ($r.status -ne "CONFIRMED") { throw "Order not confirmed, got: $($r.status)" }
    $script:orderId = $r.id
    Write-Host "    Order created: $script:orderId, total: `$$($r.total)"
    $script:idempotencyKey = $idempotencyKey
}

# 6. Verify order
Test-Step "Get order by ID" {
    $r = Invoke-Api GET "$ORDER_SERVICE/orders/$script:orderId"
    if ($r.id -ne $script:orderId) { throw "Wrong order" }
    if ($r.status -ne "CONFIRMED") { throw "Wrong status: $($r.status)" }
}

Test-Step "Get orders by user" {
    $r = Invoke-Api GET "$ORDER_SERVICE/orders/user/$userId"
    if ($r.Count -eq 0) { throw "No orders found for user" }
}

# 7. Verify cart cleared
Test-Step "Cart cleared after order" {
    $r = Invoke-Api GET "$CART_SERVICE/cart/$userId"
    if ($r.itemCount -ne 0) { throw "Cart not cleared, has $($r.itemCount) items" }
    Write-Host "    Cart is empty (cleared after checkout)"
}

# 8. Idempotency check
Test-Step "Idempotent order (same key returns same order)" {
    # Add item back to cart first for the request to work
    # Actually the saga will fail because cart is empty — but idempotency check happens first
    $body = @{
        userId = $userId
        shippingAddressId = "addr-123"
        idempotencyKey = $script:idempotencyKey
    }
    $r = Invoke-Api POST "$ORDER_SERVICE/orders" $body
    if ($r.id -ne $script:orderId) { throw "Idempotency failed: got different order" }
    Write-Host "    Same order returned: $($r.id)"
}

# 9. Search
Test-Step "Search products" {
    $r = Invoke-Api GET "$SEARCH_SERVICE/products/search?query=headphones"
    Write-Host "    Search returned $($r.totalHits) hits"
}

# ============================================================
Write-Host "`n========================================" -ForegroundColor Yellow
Write-Host "  RESULTS: $passed passed, $failed failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "========================================`n" -ForegroundColor Yellow

if ($failed -gt 0) { exit 1 }
