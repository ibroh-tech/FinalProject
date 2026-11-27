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
  `@ControllerAdvice` so 400/404/409 responses share one documented JSON shape.
- A concurrent duplicate-email insert that passes the service check would be rejected by the
  database unique constraint and currently surface as a 500; this race is rare in the experiment
  but will be mapped to 409 when the global handler is added.
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
