# Path Parameters vs Request (Query) Parameters

## Path Parameters

Part of the URL path itself. Used to **identify a specific resource**.

### Format
```
/api/resource/{id}
```

### Examples

| URL | What it identifies |
|-----|-------------------|
| `GET /api/users/42` | User with ID 42 |
| `GET /api/orders/abc-123` | Order with ID abc-123 |
| `GET /api/users/42/posts/7` | Post 7 belonging to user 42 |
| `DELETE /api/products/99` | Product with ID 99 |
| `GET /api/categories/electronics/items` | Items in the "electronics" category |

### Spring Boot Example

```java
@GetMapping("/users/{userId}/posts/{postId}")
public Post getPost(
        @PathVariable Long userId,
        @PathVariable Long postId) {
    return postService.findByUserAndId(userId, postId);
}
```

```java
@DeleteMapping("/orders/{orderId}")
public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
    orderService.cancel(orderId);
    return ResponseEntity.noContent().build();
}
```

---

## Request Parameters (Query Parameters)

Appended after `?` in the URL. Used to **filter, sort, paginate, or modify** the response.

### Format
```
/api/resource?key=value&key2=value2
```

### Examples

| URL | What it does |
|-----|-------------|
| `GET /api/users?role=admin` | Filter users by role |
| `GET /api/products?category=shoes&minPrice=50` | Filter products by category and minimum price |
| `GET /api/orders?status=pending&page=2&size=20` | Paginate pending orders |
| `GET /api/posts?sortBy=date&order=desc` | Sort posts by date descending |
| `GET /api/search?q=spring+boot&lang=java` | Search with a query string |

### Spring Boot Example

```java
@GetMapping("/products")
public List<Product> getProducts(
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name") String sortBy) {
    return productService.find(category, page, size, sortBy);
}
```

```java
@GetMapping("/search")
public List<SearchResult> search(
        @RequestParam String q,
        @RequestParam(required = false) String lang) {
    return searchService.search(q, lang);
}
```

---

## Comparison

| | Path Parameter | Query Parameter |
|--|----------------|-----------------|
| **Purpose** | Identify a resource | Filter/modify the response |
| **Required?** | Always (part of the route) | Usually optional |
| **Position** | Inside the URL path | After `?` in the URL |
| **Annotation** | `@PathVariable` | `@RequestParam` |
| **Cardinality** | Identifies one thing | Can have many |
| **Caching** | Each path is a distinct cacheable URL | Same path, different query = different cache entries |
| **SEO/REST** | Clean, hierarchical URLs | Not part of resource identity |

---

## Rule of Thumb

- **Path parameter**: If removing it makes the URL meaningless — you don't know *which* resource → it's a path parameter.
  - `/api/users` (all users) vs `/api/users/42` (specific user)

- **Query parameter**: If removing it still returns a valid (just unfiltered) response → it's a query parameter.
  - `/api/users?role=admin` → without `?role=admin` you still get users, just all of them.

---

## Combined Example

```
GET /api/users/42/orders?status=shipped&page=1&size=10
         ──┬──          ──────────────┬──────────────
      path param         query parameters
    (which user)       (filter + paginate)
```

```java
@GetMapping("/users/{userId}/orders")
public Page<Order> getUserOrders(
        @PathVariable Long userId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    return orderService.findByUser(userId, status, page, size);
}
```
