# AI-Powered Software Testing Assistant

Dissertation project by Ibrohim. The assistant reads an OpenAPI specification, asks an LLM
(Google Gemini) for structured JSON test definitions, validates them, executes them with
REST Assured against a sample Spring Boot API, and evaluates the result with JaCoCo and PIT.

**Current status: Week 1 complete** — monorepo, buildable Spring Boot applications,
PostgreSQL development environment, and initial documentation. No controllers, entities,
or AI integration yet (those start in Weeks 2–8).

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

There are no controllers yet, so `/` returns a 404; this is expected in Week 1.

Stop the applications with `Ctrl+C`. Stop the database with `docker compose down`
(add `-v` only if you also want to delete the data volume).

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
- [`docs/progress.md`](docs/progress.md) — weekly progress log and Week 1 verification record.

## Week 1 verification

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

See [`docs/progress.md`](docs/progress.md) for the exact commands and observed output.
