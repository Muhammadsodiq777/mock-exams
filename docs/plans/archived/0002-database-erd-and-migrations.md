# Database ERD and Migrations

> Turn the target DBML into ordered, reviewable Flyway migrations.

## Depends On

- `docs/ERD/platform-architecture.dbml`
- `0001-architecture-boundaries.md`

## Scope

### In scope

- Replace the current seed `mock_exam` shape with the target platform schema through Flyway.
- Split migrations by domain: identity, teacher, file, test engine, attempts/results, AI, marketplace, payments, admin, growth.
- Define seed data for roles, platform settings, and optional demo content.

### Out of scope

- Entity class implementation.
- Data migration from production users, because there is no production data model yet.

## What To Implement Later

- Ordered `V2__...sql` migrations.
- Indexes for lookup, moderation queues, attempts, purchases, and dashboards.
- Constraints for non-negative money, valid scores, ordered sections/questions, and one result per attempt.

## Documentation Sync

- Keep `docs/ERD/platform-architecture.dbml` in sync with migrations.
- Add domain ERD notes if the schema becomes too large for one DBML file.

## Acceptance Criteria

- A fresh PostgreSQL database can migrate from empty to target schema.
- The DBML parses successfully.
- Migration order avoids circular creation failures.
