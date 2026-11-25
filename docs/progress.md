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
- [ ] Push the repository to GitHub.

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
  and manual REST Assured tests in Week 2.
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
