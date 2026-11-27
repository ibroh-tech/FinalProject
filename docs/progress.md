# Progress log

This file records what each week delivered, decisions, verification, and known problems.
Per the project brief, each week ends with a five-line summary that can be pasted into the
next week's chat.

## Week 1 — Setup and research

**Goal:** a clean repository, a running database, and a written architecture.

**Dates:** 2026-11-25

### Tasks from the brief

- [x] Create the monorepo with `sample-api`, `assistant-backend`, and `docs`.
- [x] Generate both Spring Boot projects (Maven, Java 17, Web, JPA, Validation, PostgreSQL).
- [x] Add `docker-compose.yml` with PostgreSQL and separate databases for both applications.
- [x] Write `docs/architecture.md` and `docs/progress.md`.
- [x] Collect and summarize five papers on LLM test generation in `docs/literature.md`
      (notes based on abstracts/metadata; full-text reading still pending).
- [x] Push the repository to GitHub.
      (https://github.com/ibroh-tech/FinalProject)

### What was built

```
FinalProject/
├── sample-api/          Spring Boot 3.5.16, port 8080, database sample_api
├── assistant-backend/   Spring Boot 3.5.16, port 8081, database assistant_db
├── docs/
│   ├── architecture.md
│   ├── literature.md
│   └── progress.md
├── docker/initdb/01-create-databases.sql
├── docker-compose.yml   PostgreSQL 16-alpine, port 5432, healthcheck
├── .gitignore
└── README.md
```

Both projects include the Maven Wrapper (`mvnw`, `mvnw.cmd`) and a single
`@SpringBootApplication` entry point. No controllers, entities, repositories, or AI code yet.
The React dashboard is deferred to Week 13 as planned.

### Verification record

Environment: Windows 11, Temurin JDK 25.0.5.1 (projects target Java 17), Maven 3.10.0,
Docker 29.9.1, Docker Compose v5.6.0.

| Check | Command | Result |
|-------|---------|--------|
| sample-api build | `mvn clean package` in `sample-api/` | BUILD SUCCESS; `target/sample-api-0.0.1-SNAPSHOT.jar` |
| assistant-backend build | `mvn clean package` in `assistant-backend/` | BUILD SUCCESS; `target/assistant-backend-0.0.1-SNAPSHOT.jar` |
| PostgreSQL starts | `docker compose up -d` | Container `ai-testing-postgres` reached `healthy` |
| Databases exist | `psql -d sample_api` / `psql -d assistant_db` | `current_database()` returned `sample_api` and `assistant_db`; PostgreSQL 16.15 |
| sample-api connects | `java -jar sample-api-0.0.1-SNAPSHOT.jar` | `HikariPool-1 - Start completed.`; `Tomcat started on port 8080`; started in 2.622 s |
| assistant-backend connects | `java -jar assistant-backend-0.0.1-SNAPSHOT.jar` | `HikariPool-1 - Start completed.`; `Tomcat started on port 8081`; started in 2.96 s |
| Database sessions | `pg_stat_activity` filtered on both databases | JDBC connections present for both `sample_api` and `assistant_db` |
| HTTP smoke check | `curl http://localhost:8080/` and `:8081/` | HTTP 404 on both, expected because no controllers exist yet |
| Maven Wrapper | `./mvnw -v` | Maven 3.10.0 resolved and ran |
| Applications stop cleanly | stop both processes | Ports 8080/8081 released |

Build warnings: Maven printed a non-fatal `certificate_unknown` warning while checking metadata
for the OW2/Sonatype snapshot repositories. Normal dependency resolution from Maven Central
succeeded and both builds passed.

### Decisions made

1. One PostgreSQL instance with two databases (`sample_api`, `assistant_db`) instead of two
   containers: simpler development setup and complete data isolation between apps.
2. Spring Boot 3.5.16 with a Java 17 target: current Spring Boot 3.x, project baseline Java 17.
3. Ports 8080 and 8081 so both services run simultaneously; the dashboard will use 5173.
4. Datasource settings come from environment variables with development defaults.
5. Maven Wrapper is committed so the project builds without a global Maven installation.
6. No domain code in Week 1, following the brief's rule to do only the requested task.
7. JPA uses `ddl-auto: update` for development. This will be revisited before evaluation runs.

### Known problems / risks

- Docker Desktop must be running before the database can start. No automated fallback yet.
- Default database credentials (`postgres`/`postgres`) are for local development only; they
  must be overridden outside a development machine.
- The init script runs only on the first volume creation. Recreating the databases requires
  `docker compose down -v`, which deletes local data.
- `ddl-auto: update` is convenient now but not a migration strategy; schema management must be
  tightened before the experiment (Weeks 8–12).
- No test classes yet, so `mvn package` reports "No tests to run". Unit tests begin in Week 5
  and manual REST Assured tests in Week 2. (Resolved in Week 2.)
- `docs/literature.md` is based on abstracts and metadata; full-text reading is still needed
  for the literature-review chapter.

### Week 1 summary (5 lines)

1. Built the monorepo with two Spring Boot 3.5.16 Maven projects, Maven Wrappers, and docs.
2. Added PostgreSQL 16 via Docker Compose with isolated `sample_api` and `assistant_db` databases.
3. Verified both applications build and start, connect to their databases, and release their ports.
4. Documented the target architecture, trust boundaries, data model, five key papers, and decisions.
5. Known gaps: no domain code or tests yet, dev-only DB credentials, and a temporary `ddl-auto: update`.

### Next week (Week 2)

Users and Products CRUD in `sample-api` with DTOs, `@Valid`, the specified validation rules,
and five manual REST Assured tests for `POST /api/users`.

## Week 2 — Sample API: users and products

**Goal:** the first working endpoints with validation.

**Dates:** 2026-11-27

### Tasks from the brief

- [x] Entities, repositories, services, and controllers for Users and Products
      (create, get, update, delete).
- [x] `POST /api/users` rules: name not empty; valid unique email (409 on duplicate);
      age 1–120; 400 on invalid input; 201 on success.
- [x] Write five manual REST Assured tests for `POST /api/users`.
- [x] All endpoints respond correctly.

### What was built

Eight endpoints in `sample-api` (four per resource):

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| POST | `/api/users` | 201 | 400, 409 |
| GET | `/api/users/{id}` | 200 | 404 |
| PUT | `/api/users/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/users/{id}` | 204 | 404 |
| POST | `/api/products` | 201 | 400 |
| GET | `/api/products/{id}` | 200 | 404 |
| PUT | `/api/products/{id}` | 200 | 400, 404 |
| DELETE | `/api/products/{id}` | 204 | 404 |

- `user` package: `User`, `UserRepository`, `UserService`, `UserController`, `UserRequest`,
  `UserResponse`, `UserNotFoundException`, `DuplicateEmailException`.
- `product` package: `Product`, `ProductRepository`, `ProductService`, `ProductController`,
  `ProductRequest`, `ProductResponse`, `ProductNotFoundException`.
- `UserApiTest` — exactly five REST Assured tests for `POST /api/users`:
  valid creation, blank name, invalid email, age outside 1–120, duplicate email.
- `UserCrudApiTest` — one end-to-end user CRUD flow.
- `ProductApiTest` — one end-to-end product CRUD flow.
- `pom.xml` — added `io.rest-assured:rest-assured` in test scope (version managed by Spring Boot).
- `README.md` and `docs/architecture.md` — updated endpoint, validation, and test documentation.

Implementation notes:

- Controllers use DTO records with Jakarta Bean Validation; entities stay in the persistence layer.
- `UserService` normalises emails to lowercase and trims names; duplicate checks are
  case-insensitive and the database has a unique constraint on `email`.
- `PUT` is a full replacement. Products support a blank description, stored as `null`.
- Services own transaction boundaries (`@Transactional`); reads use `readOnly = true`.
- Missing resources return 404 via `@ResponseStatus` exceptions; duplicates return 409.
- Eight endpoints were chosen instead of ten so Week 3 (orders + auth) reaches 15 total.

### Verification record

| Check | Command | Result |
|-------|---------|--------|
| Full build with tests | `mvn -B -ntp clean package` in `sample-api/` | BUILD SUCCESS; 7 tests, 0 failures, 0 errors |
| Five required POST tests | `UserApiTest` | 5 tests passed |
| User CRUD flow | `UserCrudApiTest` | 1 test passed |
| Product CRUD flow | `ProductApiTest` | 1 test passed |
| Manual HTTP checks | Python/curl smoke script against the running jar | 30/30 checks passed |
| App startup | `java -jar sample-api-0.0.1-SNAPSHOT.jar` | Hikari connected; Tomcat on 8080; started in 2.744 s |
| Database schema | `\d users`, `\d products` in `sample_api` | Tables created; unique constraint on `users.email` |

The manual checks covered: valid user 201, blank name 400, invalid email 400, age 0 and 121
400, duplicate email (different case) 409, user GET/PUT/DELETE, product POST/GET/PUT/DELETE,
404 after deletion, and negative product price 400. These are the same requests that can be
replayed in Postman using the README examples.

Maven Central was briefly unreachable during the session because of a DNS/network outage.
Dependencies and the test run were completed after connectivity returned; no build workaround
remains in the repository.

### Decisions made

1. DTO records at the API boundary so entities are never serialised directly.
2. Validation annotations on DTOs, with service-level uniqueness rules.
3. Email stored lowercase, giving case-insensitive uniqueness through a database constraint.
4. Full-replacement `PUT` semantics, no `PATCH`.
5. Eight endpoints now, leaving the total at 15 after Week 3.
6. Integration tests use the local development database and clean only `users` and `products`.
7. Five required POST tests plus two CRUD flow tests, so every endpoint is exercised.

### Known problems / risks

- The REST Assured tests run against the development `sample_api` database and delete its
  `users` and `products` rows. They must not be pointed at data that matters. A dedicated test
  database or Testcontainers setup is a later improvement.
- Error bodies still use Spring Boot's default error format. Week 3 adds a global
  `@ControllerAdvice` so 400/404/409 responses share one documented JSON shape. (Resolved in Week 3.)
- A concurrent duplicate-email insert that passes the service check would be rejected by the
  database unique constraint and currently surface as a 500; this race is rare in the experiment
  but will be mapped to 409 when the global handler is added. (Resolved in Week 3.)
- No list/pagination endpoints by design; tests fetch known IDs.
- Product fields (`description`, `price`, `stock`) were chosen because the brief did not specify
  them; Week 3 orders will build on `price` and `stock`.
- No authentication/authorisation on the sample API yet (Week 3).

### Week 2 summary (5 lines)

1. Implemented Users and Products CRUD with DTO validation, services, repositories, and entities.
2. Enforced user rules: non-blank name, valid unique email (409), age 1–120 (400), 201 on success.
3. Added five required REST Assured tests for `POST /api/users` plus two CRUD flow tests.
4. All 7 tests and 30 manual HTTP checks pass; `mvn clean package` succeeds.
5. Known gaps: tests share the dev database, default error shape, and no auth/orders yet.

### Next week (Week 3)

Add Orders (create, get, update, cancel) and Authentication (register, login, refresh token),
a global exception handler with one JSON error shape, and documented business rules.

## Week 3 — Sample API: orders, auth, error handling

**Goal:** 10–15 endpoints in total and consistent errors.

**Dates:** 2026-11-27

**Branch:** `develop` (not merged into `main`; `main` remains at Week 2).

### Tasks from the brief

- [x] Orders: create, get, update, cancel.
- [x] Authentication: register, login, refresh token.
- [x] `@ControllerAdvice` global exception handler with one JSON error body
      (400 / 401 / 404 / 405 / 409 / 500).
- [x] Business rules that tests can later detect, including "a shipped order cannot be cancelled".
- [x] 10–15 endpoints work; exactly 15 are implemented.
- [x] Rules listed in `docs/sample-api-rules.md`.

### What was built

| Area | Files / behaviour |
|------|-------------------|
| Orders | `order` package: `Order`, `OrderStatus`, repository, service, controller, create/update DTOs, response, 404/409 exceptions. 4 endpoints. |
| Authentication | `auth` package: `Account`, `AuthSession`, repositories, `AuthService`, `AuthController`, DTOs, BCrypt config, 401/409 exceptions. 3 endpoints. |
| Errors | `common` package: `ApiException`, `ErrorResponse`, `GlobalExceptionHandler`. All errors share one JSON shape. |
| Rules | `docs/sample-api-rules.md` lists all 15 endpoints, validation, status transitions, token rules, and 13 testable business rules. |
| Tests | `OrderApiTest` (8), `AuthApiTest` (7), `ErrorResponseTest` (2), plus the 7 Week 2 tests. |

Order model:

- Create stores a unit-price snapshot and `totalPrice = unitPrice × quantity`.
- New orders are `PENDING`.
- `PENDING → PAID → SHIPPED`; cancellation is allowed from `PENDING` and `PAID`.
- A shipped order cannot be cancelled or modified.
- Quantity cannot change after the order becomes `PAID`.
- Quantity must be 1–1000 and cannot exceed product stock.
- Stock is validated but deliberately not decremented.

Authentication model:

- Separate `accounts` table; domain `users` remain unchanged.
- Passwords are BCrypt hashes; password rule is 8–72 characters.
- Register/login return opaque UUID access and refresh tokens.
- Access tokens last 15 minutes, refresh tokens 7 days.
- Refresh rotates the pair and revokes the old session; reusing an old refresh token returns 401.
- No endpoint requires a token yet; this is the standalone authentication workflow.

Unified error body:

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

### Verification record

| Check | Command | Result |
|-------|---------|--------|
| Full build with tests | `mvn -B -ntp clean package` in `sample-api/` | BUILD SUCCESS; 24 tests, 0 failures, 0 errors |
| Order lifecycle tests | `OrderApiTest` | 8 passed |
| Auth flow tests | `AuthApiTest` | 7 passed |
| Error format tests | `ErrorResponseTest` | 2 passed |
| Week 2 regression tests | `UserApiTest`, `UserCrudApiTest`, `ProductApiTest` | 7 passed |
| Endpoint count | rules inventory | 15 endpoints (4 + 4 + 4 + 3) |
| Error shape | assertions on 400, 401, 404, 405, 409 | same keys and status/error/path values |
| Database schema | Hibernate update against `sample_api` | `orders`, `accounts`, `auth_sessions` added alongside `users`, `products` |

### Decisions made

1. Exactly 15 endpoints, leaving no room for extra list/pagination routes in this week.
2. Order lifecycle `PENDING → PAID → SHIPPED` with `CANCELLED`; update cannot jump from
   `PENDING` to `SHIPPED`.
3. Price snapshot on the order so later product price changes do not rewrite history.
4. Stock is checked but not decremented, keeping order tests independent and repeatable.
5. Authentication uses a separate `accounts` table so the Week 2 user contract stays unchanged.
6. BCrypt through `spring-security-crypto`, without the full Spring Security filter chain.
7. Opaque UUID tokens stored in `auth_sessions`, with refresh rotation and revocation.
8. A single `ApiException` base and `GlobalExceptionHandler` for every error response.
9. Validation errors include a `details` array; all other errors include an empty array.
10. Week 3 development stays on `develop`.

### Known problems / risks

- Access tokens are issued but not yet required or validated by any endpoint. Protected-resource
  behaviour will matter when the assistant calls the sample API in Week 9.
- Authentication accounts and domain users are separate; registering does not create a `users` row.
- Token lifetimes are constants in `AuthService`, not configuration. Move them to
  `application.yml` if experiments need different values.
- Expired auth sessions are not purged automatically; the table will grow slowly.
- Orders reference users/products by ID without foreign keys, so deleting a user or product leaves
  historical orders with dangling IDs.
- Stock is not reserved or decremented, and there is no payment processing; `PAID` is a state set
  through the update endpoint.
- Integration tests still use the development `sample_api` database and delete rows from
  `users`, `products`, `orders`, `accounts`, and `auth_sessions`.
- The generic 500 handler logs and hides programming errors; check application logs when a 500 occurs.
- Access and refresh tokens are stored as plaintext UUIDs in `auth_sessions`; acceptable for a
  local sample API, but production systems should hash or encrypt stored bearer tokens.
- The error body intentionally exposes domain messages such as "Order not found"; fine for a
  sample API, not necessarily for production.

### Week 3 summary (5 lines)

1. Added 4 order endpoints with a testable `PENDING → PAID → SHIPPED` lifecycle and cancel rules.
2. Added register, login, and rotating refresh-token endpoints with BCrypt password hashing.
3. Replaced scattered error responses with one `@RestControllerAdvice` JSON shape for all errors.
4. Reached exactly 15 endpoints; 24 REST Assured tests pass and the full build succeeds.
5. Work is on `develop`; `main` stays at Week 2. Tokens are not enforced yet, and stock/order data is simplified.

### Next week (Week 4)

Add springdoc-openapi, document every endpoint and status code, save `docs/openapi.json`, write
the 25–30 manual JUnit 5 + REST Assured baseline, configure JaCoCo, and record baseline coverage.
