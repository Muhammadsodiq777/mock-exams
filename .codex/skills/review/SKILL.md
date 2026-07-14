---
name: review
description: >
  Unified change review skill for Mock Exams. Use when reviewing a diff, branch, plan output, or
  completed task that spans backend and frontend, or when the user asks for a review without
  naming a layer. Routes backend changes through backend-review and frontend changes through
  frontend-review, then adds cross-layer contract, schema, and docs checks. For single-layer
  changes prefer backend-review or frontend-review directly.
---

# Mock Exams Change Review

Review the actual changes and directly relevant nearby code, not the whole repository.

## Scope Detection

1. Identify the changed files (diff, branch, task output, or files the user names).
2. Changes under `backend/` only: apply `backend-review` and the Cross-Layer Checks that involve docs.
3. Changes under `frontend/` only: apply `frontend-review` and the Cross-Layer Checks that involve docs.
4. Changes in both, or in `docs/`, `docker-compose.yml`, or CI: apply both review skills plus all Cross-Layer Checks.

## Required Skills

- Use `backend-review` for Java/Spring changes.
- Use `frontend-review` for React/TypeScript changes.
- Use `java-spring-guidelines` and `react-typescript-guidelines` as the convention sources.
- Use `dependency-refresh` when dependency or platform versions changed.

## Cross-Layer Checks

- API contract: backend DTOs, endpoints, and error codes match `docs/API_CONTRACT.md` and the frontend `types/backend.ts`, `services/` clients, and `apiRoutes`.
- Schema: persistence changes ship as new Flyway migrations under `backend/src/main/resources/db/migration/` and never edit applied migrations.
- Boundaries: changes respect `docs/architecture-boundaries.md`.
- Docs: behavior changes update the relevant `docs/specs/*.md`, and `docs/backend-dev.md` / `docs/frontend-dev.md` stay accurate when commands or environment variables change.
- Config: new environment variables appear in `application.properties` defaults, `.env.example`, or `docker-compose.yml` as appropriate.

## Verification

Confirm the matching checks ran (or run them) before signing off:

- Backend changes: `./mvnw test` in `backend/`
- Frontend changes: `npm run lint` and `npm run build` in `frontend/`
- Cross-layer changes: both sets

## Output

Return findings first, ordered by severity, with file references. Separate backend, frontend, and
cross-layer findings. If no issues are found, say so and mention residual verification risk.
