# Testing Backend Unit Integration

> Define backend RED/GREEN coverage for services, controllers, and persistence.

## Depends On

- Backend plans `0005` through `0035`
- `0067-live-teacher-speaking-schedules.md` when schedule work begins

## Context

Backend implementation plans rely on domain rules that are easy to break: auth/role access, teacher approval, builder lifecycle, rich-text sanitization, scoring idempotency, payments, wallets, AI usage, admin moderation, and schedule booking. This plan defines the baseline RED/GREEN coverage implementers should add as features land.

## Scope

### In scope

- Service tests for business rules.
- Controller tests for HTTP contract and authorization.
- Repository or integration tests for critical queries.
- Migration verification.
- Concurrency tests for booking/reservation flows when live speaking schedules are implemented.

### Out of scope

- Browser tests.
- Load testing.
- Full provider sandbox calls; provider adapters are covered by mock/contract tests in `0064`.

## Key Decisions

- Service tests protect business invariants first; controller tests protect HTTP status, envelope, auth, and validation contracts.
- Repository/integration tests are required for complex queries, locking, uniqueness, migrations, and concurrency-sensitive flows.
- Use H2 where existing test infrastructure supports it, but add PostgreSQL/Testcontainers only through a deliberate test-infrastructure decision if H2 cannot cover a PostgreSQL-specific behavior.
- Each feature implementation should run focused tests during development and the full backend suite at feature completion.
- Tests should assert stable business error codes, not prose messages.
- Concurrency tests are mandatory for payment confirmation, wallet earning idempotency, and live speaking booking reservation.

## What To Implement

- Add focused tests per feature before production code.
- Use `mvn -Dtest=ClassName test` during feature loops.
- Run `mvn test` after backend feature completion.
- Use transactional tests for slot reservation, payment confirmation, and Google sync job idempotency in `0067`.
- Establish naming/location conventions for service, controller, mapper, repository, migration, and provider adapter tests.
- Add test fixtures/builders for users, roles, teacher profiles, tests, sections, questions, attempts, payments, wallets, AI jobs, and bookings as those domains land.
- Add migration verification that applies Flyway migrations from empty schema.
- Add authorization negative tests for every student/teacher/admin route group.
- Add rich-text sanitizer tests for safe content, empty content, disallowed tags, disallowed embeds, and rendered output.

## Required Coverage

- Auth, roles, teacher approval, test lifecycle, attempts, scoring, AI limits, payments, wallet, withdrawals, moderation, settings, rich-text validation, builder preview, and live speaking schedule booking.
- For every backend feature: success path, validation failure, forbidden role, inactive account where relevant, not-found visibility, invalid status transition, idempotent retry where relevant, and persistence constraints.

## Documentation Sync

- Update `docs/backend-dev.md` with test commands.
- Add Google Calendar/Meet mock test setup when `0067` is implemented.
- Update feature plans when test strategy reveals a missing invariant or endpoint behavior.
- Update `docs/API_CONTRACT.md` examples when controller tests define/lock a response shape.

## Acceptance Criteria

- Each backend feature has tests for success, permission failure, and key negative cases.
- Live speaking schedule tests prove no double booking, no duplicate wallet earning, and retry-safe provider sync.
- Full backend verification passes before a backend feature is considered complete, or every exception is documented with a specific failing test and reason.
