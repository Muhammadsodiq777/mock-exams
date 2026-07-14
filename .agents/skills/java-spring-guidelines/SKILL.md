---
name: java-spring-guidelines
description: >
  Opinionated Java + Spring Boot guidelines for the Mock Exams backend. Covers package layout,
  DTO naming, controllers, services, mappers, JPA entities and repositories, soft-delete rules,
  validation, error handling, schema SQL updates, and Maven-based verification. Trigger whenever
  the user writes, reviews, scaffolds, or plans backend code in `backend/` for this repository.
  Do not trigger for frontend-only work or non-Java projects.
---

# Mock Exams Java + Spring Guidelines

Use this skill as the backend convention source for Mock Exams.

## Read First

Before coding or reviewing backend changes, read:

1. `docs/overview.md` if it exists
2. Relevant `docs/specs/*.md`
3. `docs/API_CONTRACT.md` when changing routes, DTOs, auth, or pagination
4. Relevant `docs/ERD/*.dbml` files when applicable
5. `docs/backend-dev.md`
6. The nearby controller, service, repository, entity, DTO, and mapper files you are changing

If `docs/overview.md` is missing, note that briefly and continue.

## Stack

- Java 21
- Spring Boot 3.5.x in this project
- Spring MVC + Validation + Data JPA
- Maven
- PostgreSQL in runtime, H2 available for tests
- springdoc OpenAPI
- Lombok on entities and selected boilerplate-heavy classes

## Verification

Prefer `just` recipes when the repo adds them. Today the working defaults are:

- Narrow backend test: `mvn -Dtest=ClassName test`
- Full backend verification: `mvn test`

Do not run the full suite during each RED/GREEN step. Run it once at the end.

## Codebase Shape

The backend package rule is class-type first, with purpose subpackages only inside those class-type packages.
All controllers belong under `controller/`; all services under `service/`; all repositories under
`repository/`; all entities under `entity/`; all DTOs under `dto/`; all mappers under `mapper/`.
When a folder becomes crowded, split it by purpose inside that folder, for example
`controller/auth/`, `controller/admin/`, `dto/tests/`, or `service/payments/`.

Do not add new feature-first package trees such as `auth/controller/` or `teachers/dto/`.
If touching legacy feature-first code, avoid opportunistic moves unless the task is explicitly
part of a repackaging plan.

- `controller/`
- `service/` and `service/impl/`
- `repository/`
- `entity/`
- `dto/`
- `mapper/`
- `config/`

Match this structure unless the user explicitly asks for a refactor.

Read these references as needed:

- API conventions: `references/api-patterns.md`
- Service and mapping patterns: `references/service-patterns.md`
- Persistence and schema patterns: `references/persistence-patterns.md`
- Build and testing rules: `references/testing-and-build.md`

## Non-Negotiable Rules

- Keep controllers thin
- Keep business rules in services
- Reuse DTO suffixes consistently: `CDO`, `UDO`, `RDO`
- Prefer record DTOs for request and response shapes
- Map entities to DTOs in dedicated mapper components, not in controllers
- Use `BusinessException(code, message)` for domain and validation failures
- Preserve soft-delete behavior where the entity already uses `deletedAt`
- Update both schema SQL files when the database shape changes
- Match existing endpoint verb style for the feature you are touching instead of forcing a new REST rule midstream

## TDD Guidance

- Add the narrowest useful test first for logic-heavy changes
- Follow existing test infrastructure instead of introducing a new framework casually
- If a feature currently has no tests, add the minimum useful coverage near the changed service or controller
- Note when the repo lacks a closer equivalent to `just test-class`

## Documentation

When backend behavior changes, update the relevant `docs/specs/*.md`.
Update `docs/API_CONTRACT.md` and `docs/ERD/*.dbml` when the change affects them.
Do not let specs drift from controller/service behavior.
