# PUT vs PATCH

## Summary

- **PUT** — Replace the entire resource with the provided representation.
- **PATCH** — Apply a partial modification to a resource.

## Key Differences

| Aspect | PUT | PATCH |
|--------|-----|-------|
| Semantics | Full replacement | Partial update |
| Idempotent | Yes | Not guaranteed |
| Payload | Complete resource representation | Only the fields to change |
| Missing fields | Typically set to null/default | Left unchanged |
| Typical success code | `200 OK` or `204 No Content` | `200 OK` or `204 No Content` |
| Create if missing | May create (`201 Created`) | Usually does not create |

## Examples

Given an existing resource at `GET /users/42`:

```json
{
  "id": 42,
  "name": "Alice",
  "email": "alice@example.com",
  "role": "admin"
}
```

### PUT — Full Replacement

```http
PUT /users/42
Content-Type: application/json

{
  "id": 42,
  "name": "Alice",
  "email": "alice@newdomain.com",
  "role": "admin"
}
```

You must send the entire resource. If you omit `role`, the server may null it out.

### PATCH — Partial Update

```http
PATCH /users/42
Content-Type: application/json

{
  "email": "alice@newdomain.com"
}
```

Only the `email` field is updated; everything else remains untouched.

## When to Use Which

**Use PUT when:**
- The client owns the full representation and can always send it completely.
- You want simple, idempotent semantics (retries are safe by definition).
- The resource is small and sending the whole thing is cheap.

**Use PATCH when:**
- You only know (or care about) a subset of fields.
- The resource is large and sending the full payload is wasteful.
- You need fine-grained operations (e.g., appending to an array, incrementing a counter).

## Common Pitfalls

1. **Accidental data loss with PUT** — Forgetting a field in a PUT request can reset it to null. Clients must always send the complete, current state.
2. **Assuming PATCH is idempotent** — `PATCH` operations like "increment counter by 1" are not idempotent. Design accordingly if retries are possible.
3. **Inconsistent PATCH formats** — There's no single standard for PATCH bodies. JSON Merge Patch (RFC 7396) and JSON Patch (RFC 6902) are both valid; pick one and document it.
4. **PUT for partial updates** — Using PUT but only sending changed fields violates the spec and confuses API consumers. Use PATCH instead.
