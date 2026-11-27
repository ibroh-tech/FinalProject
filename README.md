# AI-Powered Software Testing Assistant

Dissertation project by Ibrohim. The assistant reads an OpenAPI specification, asks an LLM
(Google Gemini) for structured JSON test definitions, validates them, executes them with
REST Assured against a sample Spring Boot API, and evaluates the result with JaCoCo and PIT.

**Current status: Week 3 complete on the `develop` branch** — the sample API exposes 15
validated endpoints (users, products, orders, authentication), a unified JSON error format,
and 24 REST Assured integration tests. `main` currently contains Weeks 1–2; Week 3 is not
merged into `main` yet.

## Repository layout

```
FinalProject/
├── sample-api/          Spring Boot app under test (port 8080)
├── assistant-backend/   Spring Boot testing assistant (port 8081)
├── docs/                architecture, literature notes, weekly progress
├── docker/initdb/       PostgreSQL init script (creates both databases)
├── docker-compose.yml   PostgreSQL 16 development database
└── README.md
```

The React dashboard (`assistant-ui/`) is scheduled for Week 13 and is not created yet.

## Prerequisites

| Tool           | Version used |
|----------------|--------------|
| JDK            | 17 or newer (built and verified with JDK 25) |
| Docker Desktop | with Docker Compose v2+ |
| Git            | any recent version |
| Maven          | optional — Maven Wrapper (`mvnw` / `mvnw.cmd`) is included in both projects |

## 1. Start PostgreSQL

From the repository root:

```bash
docker compose up -d
docker compose ps
```

The init script creates two databases on the same PostgreSQL instance:

| Database         | Used by           | Local URL |
|------------------|-------------------|-----------|
| `sample_api`     | sample-api        | `jdbc:postgresql://localhost:5432/sample_api` |
| `assistant_db`   | assistant-backend | `jdbc:postgresql://localhost:5432/assistant_db` |

Check the databases:

```bash
docker compose exec postgres psql -U postgres -d sample_api -c "SELECT current_database();"
docker compose exec postgres psql -U postgres -d assistant_db -c "SELECT current_database();"
```

The init script only runs the first time the volume is created. To recreate the databases
from scratch (this deletes local development data):

```bash
docker compose down -v
docker compose up -d
```

## 2. Build both applications

Windows (PowerShell or cmd):

```powershell
cd sample-api
.\mvnw.cmd clean package

cd ..\assistant-backend
.\mvnw.cmd clean package
```

Bash / Git Bash / macOS / Linux:

```bash
cd sample-api && ./mvnw clean package
cd ../assistant-backend && ./mvnw clean package
```

## 3. Run both applications

Open two terminals.

```powershell
# Terminal 1
cd sample-api
.\mvnw.cmd spring-boot:run

# Terminal 2
cd assistant-backend
.\mvnw.cmd spring-boot:run
```

Expected startup evidence:

- `HikariPool-1 - Start completed.` — the app connected to PostgreSQL.
- `Tomcat started on port 8080 (http)` for sample-api.
- `Tomcat started on port 8081 (http)` for assistant-backend.

There are no endpoints on `/`, so it returns a 404. The API lives under `/api/users`,
`/api/products`, `/api/orders`, and `/api/auth` (see below).

Stop the applications with `Ctrl+C`. Stop the database with `docker compose down`
(add `-v` only if you also want to delete the data volume).

## Sample API endpoints (Week 3)

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| POST | `/api/users` | 201 | 400 invalid body, 409 duplicate email |
| GET | `/api/users/{id}` | 200 | 404 not found |
| PUT | `/api/users/{id}` | 200 | 400, 404, 409 duplicate email |
| DELETE | `/api/users/{id}` | 204 | 404 not found |
| POST | `/api/products` | 201 | 400 invalid body |
| GET | `/api/products/{id}` | 200 | 404 not found |
| PUT | `/api/products/{id}` | 200 | 400, 404 |
| DELETE | `/api/products/{id}` | 204 | 404 not found |
| POST | `/api/orders` | 201 | 400, 404 unknown user/product, 409 insufficient stock |
| GET | `/api/orders/{id}` | 200 | 404 not found |
| PUT | `/api/orders/{id}` | 200 | 400, 404, 409 invalid transition/quantity/stock |
| POST | `/api/orders/{id}/cancel` | 200 | 404, 409 shipped/already cancelled |
| POST | `/api/auth/register` | 201 | 400, 409 duplicate account |
| POST | `/api/auth/login` | 200 | 400, 401 invalid credentials |
| POST | `/api/auth/refresh` | 200 | 400, 401 invalid/expired/reused token |

The full rule set is in [`docs/sample-api-rules.md`](docs/sample-api-rules.md).

### Validation rules

**User**

- `name`: required, not blank, at most 120 characters.
- `email`: required, valid address, at most 254 characters, unique. Email is stored in
  lowercase, so duplicates are rejected case-insensitively with `409 Conflict`.
- `age`: required, integer from 1 to 120 inclusive.
- Invalid input returns `400 Bad Request`; successful creation returns `201 Created`.

**Product**

- `name`: required, not blank, at most 120 characters.
- `description`: optional, at most 500 characters; blank descriptions are stored as `null`.
- `price`: required, at least 0, at most 8 integer digits and 2 decimal places.
- `stock`: required, integer at least 0.

**Order**

- `userId` and `productId` must exist; missing IDs return 404.
- `quantity` is 1–1000 and cannot exceed product stock (409).
- New orders start as `PENDING`.
- Allowed transitions: `PENDING → PAID → SHIPPED`; cancellation is allowed before shipping.
- A shipped order cannot be cancelled (409). A shipped or cancelled order cannot be modified.
- Quantity cannot change after the order becomes `PAID`.
- Creating or cancelling an order does not modify product stock.

**Authentication**

- Register requires a non-blank name, unique valid email, and a password of 8–72 characters.
- Passwords are stored as BCrypt hashes; duplicate account email returns 409.
- Login with bad credentials returns 401.
- Refresh tokens rotate; reusing an old token returns 401.
- Access tokens expire after 15 minutes; refresh tokens after 7 days.

Example:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john.doe@example.com","age":25}'
```

All errors use one shape:

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

## 4. Run the tests

REST Assured tests start the application on a random port and use the local `sample_api`
database. They delete rows from `users`, `products`, `orders`, `accounts`, and `auth_sessions`
before and after each test, so do not point them at a database with data you want to keep.
PostgreSQL must be running.

Windows:

```powershell
cd sample-api
.\mvnw.cmd test
```

Bash:

```bash
cd sample-api && ./mvnw test
```

`UserApiTest` contains the five required tests for `POST /api/users` (valid creation, blank
name, invalid email, age out of range, duplicate email). `UserCrudApiTest` and
`ProductApiTest` cover the remaining user/product CRUD endpoints. `OrderApiTest` covers order
creation, totals, stock conflicts, status transitions, cancellation rules, and error shape.
`AuthApiTest` covers register, login, refresh rotation, duplicate accounts, and 401/400 errors.
`ErrorResponseTest` verifies the unified 404 and 405 responses. The suite currently has
**24 tests**.

To build without starting PostgreSQL, skip the integration tests:

```powershell
.\mvnw.cmd clean package -DskipTests
```

## Configuration

All settings have local development defaults. Override them with environment variables
(no secrets are committed):

| Variable            | Default                                          | Purpose |
|---------------------|--------------------------------------------------|---------|
| `SAMPLE_API_DB_URL` | `jdbc:postgresql://localhost:5432/sample_api`    | sample-api datasource |
| `ASSISTANT_DB_URL`  | `jdbc:postgresql://localhost:5432/assistant_db`  | assistant-backend datasource |
| `POSTGRES_USER`     | `postgres`                                       | database user (also read by Docker Compose) |
| `POSTGRES_PASSWORD` | `postgres`                                       | database password (also read by Docker Compose) |
| `POSTGRES_PORT`     | `5432`                                           | host port for PostgreSQL |

The default credentials are for local development only. Set `POSTGRES_PASSWORD` before
running anything outside a development machine.

Planned for Week 6: `GEMINI_API_KEY`, model name, base URL, timeout, and retry limits.
The API key will never be stored in this repository.

## Documentation

- [`docs/architecture.md`](docs/architecture.md) — planned architecture, components, data flow, decisions.
- [`docs/sample-api-rules.md`](docs/sample-api-rules.md) — all 15 endpoints, validation rules, error format, business rules.
- [`docs/literature.md`](docs/literature.md) — notes on five LLM test-generation papers.
- [`docs/progress.md`](docs/progress.md) — weekly progress log and verification records.

## Verification

### Week 1

| Requirement | Status |
|-------------|--------|
| Monorepo with `sample-api`, `assistant-backend`, `docs` | Done |
| Two Spring Boot Maven projects (Web, JPA, Validation, PostgreSQL) | Done |
| `docker-compose.yml` with separate databases | Done |
| `docs/architecture.md`, `docs/progress.md`, `docs/literature.md` | Done |
| Both projects build with `mvn clean package` | Done |
| PostgreSQL starts with Docker | Done |
| Both apps connect to their database | Done |
| Repository pushed to GitHub | Done |

### Week 2

| Requirement | Status |
|-------------|--------|
| Users CRUD endpoints (`POST`, `GET`, `PUT`, `DELETE`) | Done |
| Products CRUD endpoints (`POST`, `GET`, `PUT`, `DELETE`) | Done |
| `POST /api/users`: name, email, age validation | Done — 400 on invalid input |
| Duplicate email rejected | Done — 409, case-insensitive |
| Five REST Assured tests for `POST /api/users` | Done — all pass |
| All endpoints work | Done — 7 integration tests pass; 30/30 manual curl checks pass |
| `mvn clean package` | BUILD SUCCESS with 7 tests |

### Week 3

| Requirement | Status |
|-------------|--------|
| Orders: create, get, update, cancel | Done — 8 REST Assured tests |
| Authentication: register, login, refresh token | Done — 7 REST Assured tests |
| Global exception handler with consistent JSON errors | Done — handler covers 400/401/404/405/409/500; tested for 400/401/404/405/409 |
| Consistent error body for 400, 404, 409 | Done — `timestamp`, `status`, `error`, `message`, `path`, `details` |
| 10–15 endpoints total | Done — exactly 15 |
| Business rules documented | Done — `docs/sample-api-rules.md` |
| All endpoints work | Done — 24 tests pass |
| `mvn clean package` | BUILD SUCCESS with 24 tests |
| Branch policy | Week 3 is on `develop`; `main` is unchanged |

See [`docs/progress.md`](docs/progress.md) for the exact commands and observed output.
