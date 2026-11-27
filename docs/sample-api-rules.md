# Sample API rules

Status: **Week 3**. This file is the authoritative list of endpoints and business rules for
`sample-api`. It is used when writing manual tests, prompts, validation rules, and injected
defects. There are exactly **15 endpoints**: 4 users, 4 products, 4 orders, 3 authentication.

Base URL in development: `http://localhost:8080`. All request and response bodies are JSON.

## 1. Endpoint inventory

| # | Method | Path | Success | Error statuses |
|---|--------|------|---------|----------------|
| 1 | POST | `/api/users` | 201 | 400 invalid body, 409 duplicate email |
| 2 | GET | `/api/users/{id}` | 200 | 404 |
| 3 | PUT | `/api/users/{id}` | 200 | 400, 404, 409 |
| 4 | DELETE | `/api/users/{id}` | 204 | 404 |
| 5 | POST | `/api/products` | 201 | 400 |
| 6 | GET | `/api/products/{id}` | 200 | 404 |
| 7 | PUT | `/api/products/{id}` | 200 | 400, 404 |
| 8 | DELETE | `/api/products/{id}` | 204 | 404 |
| 9 | POST | `/api/orders` | 201 | 400, 404 unknown user/product, 409 insufficient stock |
| 10 | GET | `/api/orders/{id}` | 200 | 404 |
| 11 | PUT | `/api/orders/{id}` | 200 | 400, 404, 409 invalid transition/quantity/stock |
| 12 | POST | `/api/orders/{id}/cancel` | 200 | 404, 409 shipped/already cancelled |
| 13 | POST | `/api/auth/register` | 201 | 400, 409 duplicate account |
| 14 | POST | `/api/auth/login` | 200 | 400, 401 invalid credentials |
| 15 | POST | `/api/auth/refresh` | 200 | 400, 401 invalid/expired/reused token |

There are deliberately no list or pagination endpoints, so the total stays within the
10–15 endpoint target.

## 2. Users

Request body for create and update:

```json
{"name":"John Doe","email":"john.doe@example.com","age":25}
```

| Field | Rule |
|-------|------|
| `name` | Required, not blank, at most 120 characters. Stored trimmed. |
| `email` | Required, valid email, at most 254 characters, unique. Stored lowercase. |
| `age` | Required integer, 1–120 inclusive. |

- Duplicate email returns **409 Conflict**, including the same address in different case.
- Invalid input returns **400 Bad Request**.
- Successful creation returns **201 Created**; update and get return **200**; delete returns **204**.
- A missing user returns **404 Not Found**.

## 3. Products

Request body for create and update:

```json
{"name":"Keyboard","description":"Mechanical keyboard","price":49.99,"stock":10}
```

| Field | Rule |
|-------|------|
| `name` | Required, not blank, at most 120 characters. Stored trimmed. |
| `description` | Optional, at most 500 characters. Blank or whitespace-only becomes `null`. |
| `price` | Required decimal, at least 0, at most 8 integer digits and 2 decimal places. |
| `stock` | Required integer, at least 0. |

- Invalid input returns **400**; missing product returns **404**.
- Product names are not unique; duplicates are allowed.
- Stock is a stored value and is **not decremented** when an order is created (sample API
  simplification).

## 4. Orders

### 4.1 Create

```json
{"userId":1,"productId":2,"quantity":3}
```

| Field | Rule |
|-------|------|
| `userId` | Required, must exist (otherwise **404**). |
| `productId` | Required, must exist (otherwise **404**). |
| `quantity` | Required integer, 1–1000 inclusive (otherwise **400**). |

- If `quantity` is greater than the product's current `stock`, the request returns
  **409 Conflict**.
- The order stores a snapshot of the product's price as `unitPrice`.
- `totalPrice = unitPrice × quantity`, rounded to 2 decimal places.
- A new order always starts as `PENDING`.
- Response fields: `id`, `userId`, `productId`, `quantity`, `unitPrice`, `totalPrice`, `status`.

### 4.2 Status model

```
PENDING ──► PAID ──► SHIPPED
   │          │
   └──────────┴──► CANCELLED
```

| Current status | Allowed update target |
|----------------|-----------------------|
| `PENDING` | `PENDING`, `PAID` |
| `PAID` | `PAID`, `SHIPPED` |
| `SHIPPED` | none (order cannot be modified) |
| `CANCELLED` | none (order cannot be modified) |

`CANCELLED` is reached only through the cancel endpoint, never through `PUT`.

### 4.3 Update

```json
{"quantity":5,"status":"PAID"}
```

- An order can be updated only while its status is `PENDING` or `PAID`; otherwise **409**.
- `quantity` can change only while the current status is `PENDING`; changing quantity on a
  `PAID` order returns **409**.
- The requested status must be an allowed transition; otherwise **409**.
- The new quantity must not exceed the product's current stock; otherwise **409**.
- The unit price snapshot is kept, and `totalPrice` is recalculated for the new quantity.
- The body is a full replacement: both `quantity` and `status` are required.

### 4.4 Cancel

`POST /api/orders/{id}/cancel` with no body.

- `PENDING` and `PAID` orders become `CANCELLED` and return **200**.
- A `SHIPPED` order returns **409 Conflict** with a message that a shipped order cannot be
  cancelled.
- Cancelling an already `CANCELLED` order returns **409 Conflict**.
- Missing order returns **404**.

### 4.5 Intentionally simple behaviour

- Creating or cancelling an order does **not** modify product stock.
- There is no payment processing; `PAID` is a state set through the update endpoint.
- Orders store `userId` and `productId` values; deleting a user or product does not delete or
  block existing orders.

## 5. Authentication

Authentication accounts are separate from the domain `users` table. They exist to exercise the
register/login/refresh workflow; no endpoint currently requires an access token.

### 5.1 Register

```json
{"name":"Test User","email":"test@example.com","password":"Password123!"}
```

| Field | Rule |
|-------|------|
| `name` | Required, not blank, at most 120 characters. |
| `email` | Required, valid email, at most 254 characters, unique among accounts. Stored lowercase. |
| `password` | Required, 8–72 characters. Stored only as a BCrypt hash. |

- Successful registration returns **201** with `accessToken`, `refreshToken`,
  `tokenType` (`Bearer`), and `expiresInSeconds` (900).
- Duplicate account email returns **409** (case-insensitive).
- Invalid input returns **400**.

### 5.2 Login

```json
{"email":"test@example.com","password":"Password123!"}
```

- Valid credentials return **200** with a new token pair.
- Unknown email or wrong password returns **401 Unauthorized** with the same message,
  `Invalid email or password`.
- Missing fields return **400**.

### 5.3 Refresh

```json
{"refreshToken":"<token>"}
```

- A valid refresh token returns **200** with a new access token and a new refresh token.
- The old session is revoked, so reusing the old refresh token returns **401**.
- Unknown, revoked, or expired refresh tokens return **401**.
- Access token lifetime: 15 minutes (900 seconds). Refresh token lifetime: 7 days.
- Expired session rows are not cleaned up automatically in this sample.

## 6. Error format

Every error produced by the application uses this JSON shape, including validation errors,
not-found errors, conflicts, authentication failures, unknown routes, and unexpected errors:

```json
{
  "timestamp": "2026-11-27T13:15:30.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "A shipped order cannot be cancelled",
  "path": "/api/orders/1/cancel",
  "details": []
}
```

- `details` is always present and is an array. For validation failures it contains entries such
  as `"quantity: quantity must be at least 1"`.
- `timestamp` is an ISO-8601 instant in UTC.
- Unexpected errors return **500** with a generic message; the stack trace is logged but never
  returned to the client.

## 7. Business rules available for later defect injection

These rules are deliberately testable and are candidates for the 5–8 injected defects in
Week 11:

1. Email uniqueness for users, case-insensitive.
2. User age must be between 1 and 120.
3. Product price must not be negative.
4. Order quantity must be between 1 and 1000.
5. Order quantity cannot exceed product stock.
6. A shipped order cannot be cancelled.
7. A cancelled order cannot be cancelled again.
8. A shipped or cancelled order cannot be modified.
9. Quantity cannot change after an order becomes `PAID`.
10. Status transitions are restricted to `PENDING → PAID → SHIPPED` (and cancellation before shipping).
11. Duplicate account email is rejected.
12. Registration password must be 8–72 characters.
13. Refresh tokens rotate and the old token is rejected.
