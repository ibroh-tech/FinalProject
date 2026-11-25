# Architecture

Status: **Week 1 baseline**. This document describes the target architecture and clearly marks
what exists today. Only the monorepo, the two Spring Boot skeletons, PostgreSQL, and the
documentation exist so far.

## 1. Purpose

The system generates API tests with an LLM and measures whether they are any good. It reads
an OpenAPI specification, asks Google Gemini for structured JSON test definitions, validates
them, converts approved definitions into REST Assured requests, executes them against a
sample API, and reports coverage, mutation score, detected defects, validity, and timing.

Research question: how effective is AI-assisted test generation compared with manual test
generation for Java REST APIs, in terms of coverage, fault detection, test validity, and
development time?

## 2. System context

```
User
 │  uploads OpenAPI spec, reviews tests, starts runs
 ▼
Assistant backend (Spring Boot, :8081) ──► Gemini API (external, free tier)
 │                                          returns JSON test definitions
 │  builds REST Assured requests
 ▼
Sample API (Spring Boot, :8080)  ◄── tests execute against it
 │
 ├── JaCoCo agent  → line/branch coverage
 └── PIT           → mutation score
```

The LLM never produces Java code that the system executes. It only returns data (test
definitions). Java code validates that data and builds the HTTP requests itself.

## 3. Components

| # | Component | Responsibility | Status |
|---|-----------|----------------|--------|
| 1 | React dashboard (`assistant-ui`) | Upload spec, generate, review, run, view results | Planned, Week 13 |
| 2 | Assistant backend | Projects, workflow coordination, REST API | Skeleton, Week 1 |
| 3 | API Analyzer | Parse OpenAPI into endpoint models (Swagger Parser) | Planned, Week 5 |
| 4 | AI Service | Prompt building, provider call, response parsing | Planned, Weeks 6–7 |
| 5 | Validation Engine | Schema check, endpoint/method allowlist, sanity checks | Planned, Week 8 |
| 6 | Execution Engine | Build REST Assured calls, run, record results | Planned, Week 9 |
| 7 | Evaluation Engine | JaCoCo, PIT, defect detection, timings | Planned, Weeks 10–12 |
| 8 | PostgreSQL | Projects, endpoints, generations, tests, executions, reports | Running, Week 1 |
| 9 | Sample API | 10–15 endpoints used as the system under test | Skeleton, Weeks 2–4 |

## 4. Target data flow

1. **Create project** — `POST /api/projects`.
2. **Upload specification** — `POST /api/projects/{id}/specification`; the analyzer extracts
   endpoints, parameters, request schemas, required fields, and documented response statuses.
3. **Generate** — `POST /api/projects/{id}/generate`; the AI Service builds a prompt for one
   endpoint, calls the configured `AiProvider`, parses the JSON array, and stores the raw
   request/response in `test_generations`.
4. **Validate** — the Validation Engine checks required fields, types, category, endpoint and
   method allowlist, documented `expectedStatus`, body size, and duplicates. Invalid or
   duplicate definitions are stored with `INVALID` status and a reason, never deleted.
5. **Review** — `PUT /api/tests/{id}/review`; a human approves or rejects each valid test.
6. **Execute** — `POST /api/projects/{id}/execute`; the Execution Engine builds a REST Assured
   request from each approved definition, enforces the base URL allowlist, method allowlist,
   timeout, and request limit, then records expected vs actual status, duration, and result
   (`PASSED`, `FAILED`, `INVALID`, `ERROR`).
7. **Evaluate** — coverage (JaCoCo), mutation score (PIT), defect detection, validity rate,
   and timings are stored in `evaluation_reports` and exposed through
   `GET /api/projects/{id}/evaluation`.
8. **Report** — the dashboard displays results.

## 5. Monorepo layout

```
FinalProject/
├── sample-api/                  System under test (Spring Boot, :8080)
│   ├── src/main/java/com/example/sampleapi/
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── assistant-backend/           Testing assistant (Spring Boot, :8081)
│   ├── src/main/java/com/example/assistant/
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── docs/                        Architecture, literature notes, progress, later results
│   ├── architecture.md
│   ├── literature.md
│   └── progress.md
├── docker/
│   └── initdb/01-create-databases.sql
├── docker-compose.yml           PostgreSQL 16 with two databases
├── .gitignore
└── README.md
```

`assistant-ui/` will be added in Week 13.

## 6. Runtime topology (development)

| Service | Port | Database | Notes |
|---------|------|----------|-------|
| PostgreSQL | 5432 | — | Docker container `ai-testing-postgres` |
| sample-api | 8080 | `sample_api` | Spring Boot |
| assistant-backend | 8081 | `assistant_db` | Spring Boot |
| assistant-ui | 5173 | — | Vite dev server, Week 13 |

Both applications read connection details from environment variables, with local development
defaults. The API key for Gemini will be read from `GEMINI_API_KEY` in Week 6; it is never
committed.

## 7. Data model (planned)

The schema follows the project documentation:

| Table | Main information |
|-------|------------------|
| `users` | Application users |
| `projects` | Testing projects |
| `api_endpoints` | Extracted method, path, parameters, schemas, responses |
| `test_generations` | Provider, model name, prompt version, raw response, duration, status |
| `test_cases` | Definition JSON, category, origin (`MANUAL` / `CLOUD_AI` / `LOCAL_AI`), validation and review status |
| `test_executions` | Expected vs actual status, result, duration, run id |
| `evaluation_reports` | Line/branch coverage, mutation score, defects detected, validity rate, times |

Relations: `users → projects → api_endpoints → test_cases → test_executions → evaluation_reports`.

JPA entities start in Week 5. Until then `ddl-auto: update` produces no tables.

## 8. AI provider abstraction (planned)

```java
public interface AiProvider {
    String generateTests(String prompt);
}
```

- `GeminiAiProvider` — the primary provider; all Gemini-specific code stays here.
- `OllamaAiProvider` — optional local provider, later.
- `MockAiProvider` — fixed JSON for unit tests; tests never call the real API.

Model name, base URL, timeout, and retry limits will live in `application.yml`, not in code.
Every generation is recorded with provider, exact model name, prompt version, raw response,
and duration so results are reproducible.

## 9. Trust boundaries and safety rules

| Risk | Control |
|------|---------|
| LLM returns executable code | Output is data only; Java builds every request |
| Hallucinated endpoints | Analyzer allowlist of method + path from the uploaded spec |
| Undocumented status codes | `expectedStatus` must be documented for that operation |
| Huge or malformed payloads | JSON schema validation, size limits, request timeout |
| Secret leakage | API key from environment variable only; `.env` ignored |
| Silent data loss | Invalid/duplicate definitions persisted with status and reason |
| Unrepeatable runs | Database reset before execution; raw generations stored; 3+ runs |

A failed test is not automatically a product defect: it may be a wrong AI expectation.
Results are classified as `PASSED`, `FAILED` (needs review), `INVALID`, or `ERROR`.

## 10. Evaluation design (planned)

Compare A manual tests, B Gemini-generated tests, and optionally C local-model tests and
D AI tests after human review, all on the same sample API, database state, and injected
defects. Metrics: JaCoCo line/branch coverage, PIT mutation score, defects detected out of
5–8 injected defects, test validity, and development time. Repeat AI generation at least
three times because output varies. Report failures honestly.

## 11. Week 1 decisions

| Decision | Rationale |
|----------|-----------|
| One PostgreSQL instance, two databases | Keeps the sample API's test data isolated from assistant data while using one Docker service. |
| Spring Boot 3.5.16, Java 17 target | Current Spring Boot 3.x line; Java 17 is the project baseline and runs on newer JDKs. |
| Ports 8080 (sample) and 8081 (assistant) | Both can run at the same time during development. |
| Environment variables with dev defaults | Local setup works immediately; production secrets can override without code changes. |
| Maven Wrapper committed | Build works on machines without a global Maven installation. |
| No controllers/entities in Week 1 | Matches the Week 1 brief; domain code begins in Week 2. |
| Human review before execution | AI output is untrusted; review is part of the research design. |

## 12. Deliberately not implemented yet

- Any controller, entity, repository, service, or AI call.
- Springdoc/OpenAPI publishing (Week 4), Swagger Parser (Week 5), Gemini provider (Week 6).
- JaCoCo, PIT, REST Assured suite, evaluation schema, dashboard.
- Local Ollama provider (bonus, only if schedule allows).
