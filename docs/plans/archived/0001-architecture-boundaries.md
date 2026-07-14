# Architecture Boundaries

> Define the application boundaries before feature work starts.

## Depends On

- `docs/specs/project-overview.md`
- `docs/ERD/platform-architecture.dbml`

## Scope

### In scope

- Keep the backend as a Spring Boot modular monolith.
- Keep the frontend as React + Vite unless a later migration plan explicitly changes it.
- Define backend package ownership for auth, users, teachers, tests, attempts, AI, marketplace, payments, admin, notifications, files, and growth.
- Define frontend ownership for app shell, student, teacher, admin, services, hooks, store, and shared UI.

### Out of scope

- Implementing code.
- Choosing exact payment and speech-to-text providers.

## What To Implement Later

- Backend packages under `controller`, `service`, `service/impl`, `repository`, `entity`, `dto`, `mapper`, and `config`.
- Frontend folders for `app`, `pages`, `components`, `services`, `hooks`, `store`, `types`, `utils`, and `constants`.
- Cross-cutting error, auth, file, payment, and AI integration boundaries.

## Documentation Sync

- Update `docs/specs/project-overview.md` if scope changes.
- Create or update `docs/API_CONTRACT.md` when route conventions are decided.

## Acceptance Criteria

- Each product module has a clear backend owner.
- Each route group has a clear frontend owner.
- No feature plan needs to invent its own architecture rules.
