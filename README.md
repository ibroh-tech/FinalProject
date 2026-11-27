# AI-Powered Software Testing Assistant

Dissertation project by Ibrohim. The assistant reads an OpenAPI specification, asks an LLM
(Google Gemini) for structured JSON test definitions, validates them, executes them with
REST Assured against a sample Spring Boot API, and evaluates the result with JaCoCo and PIT.

**Current status: Week 2 complete** — the sample API now exposes validated Users and
Products CRUD endpoints, covered by REST Assured integration tests. The testing assistant,
AI integration, and dashboard are still planned (Weeks 5–13).

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

There are no endpoints on `/`, so it returns a 404. The API lives under `/api/users` and
`/api/products` (see below).

Stop the applications with `Ctrl+C`. Stop the database with `docker compose down`
(add `-v` only if you also want to delete the data volume).

## Sample API endpoints (Week 2)

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| POST | `/api/users` | 201 | 400 invalid body, 409 duplicate email |
| GET | `/api/users/{id}` | 200 | 404 not found |
| PUT | `/api/users/{id}` | 200 | 400 invalid body, 404 not found, 409 duplicate email |
| DELETE | `/api/users/{id}` | 204 | 404 not found |
| POST | `/api/products` | 201 | 400 invalid body |
| GET | `/api/products/{id}` | 200 | 404 not found |
| PUT | `/api/products/{id}` | 200 | 400 invalid body, 404 not found |
| DELETE | `/api/products/{id}` | 204 | 404 not found |

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

Example:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john.doe@example.com","age":25}'
```

## 4. Run the tests

REST Assured tests start the application on a random port and use the local `sample_api`
database. They delete rows from `users` and `products` before and after each test, so do not
point them at a database with data you want to keep. PostgreSQL must be running.

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
`ProductApiTest` cover the remaining CRUD endpoints.

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

See [`docs/progress.md`](docs/progress.md) for the exact commands and observed output.
