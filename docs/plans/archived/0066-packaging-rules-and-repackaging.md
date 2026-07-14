# Packaging Rules and Repackaging

> Standardize backend and frontend package/folder ownership before more feature plans add files.

## Context

The archived architecture plan defines broad boundaries, but the current backend already mixes root layered packages (`controller`, `dto`, `service`) with feature-first packages (`auth/controller`, `teachers/dto`, `users/entity`, `admin/service`). Future plans will add many files, so package drift should be corrected before implementation volume increases.

The frontend is still small and mostly follows role-first folders, but it needs the same rule before pages, feature components, hooks, services, and types multiply.

## Scope

### In scope

- Document backend package rules in developer docs and agent skills.
- Document frontend folder rules in developer docs and agent skills.
- Audit current backend packages and choose a safe repackaging order.
- Repackage backend classes into class-type packages with purpose subpackages where useful.
- Keep frontend role-first folders and add purpose subfolders as features grow.
- Update imports, package declarations, tests, docs, and API examples affected by moved classes.

### Out of scope

- Renaming API routes or DTO payload fields.
- Changing database schema only because packages moved.
- Introducing a generated client or monorepo-wide module system.
- Large unrelated cleanup while moving files.
- Moving files merely for aesthetics without a verification path.

## Key Decisions

- Backend packages are class-type first: `controller`, `service`, `service/impl`, `repository`, `entity`, `dto`, `mapper`, `config`, plus cross-cutting packages such as `api` and `error`.
- If a backend class-type package becomes too large, divide it by purpose inside that package, for example `controller/auth`, `controller/admin`, `dto/tests`, `service/payments`, or `entity/users`.
- New backend work must not add feature-first trees like `auth/controller` or `teachers/dto`.
- Frontend folders are role first: `pages`, `components`, `services`, `hooks`, `store`, `types`, `utils`, and `constants`.
- If a frontend role folder becomes too large, divide it by purpose inside that folder, for example `pages/student`, `components/test-builder`, `services/admin`, `hooks/attempts`, or `types/payments`.
- Every backend/frontend implementation must be reviewed with the matching review skill or an equivalent self-review checklist before completion.
- Repackaging must preserve public API behavior and database behavior.
- If a legacy feature-first package cannot be moved safely in one pass, track the remaining package explicitly instead of partially hiding it.

## What To Implement

1. Freeze new packaging rules in `.claude`, `.codex`, `.agents`, `docs/backend-dev.md`, and `docs/frontend-dev.md`.
2. Produce a backend package inventory grouped by current package and target package.
3. Move one backend purpose group at a time, starting with low-risk DTO/mapper/repository/entity groups before controllers and services that are heavily imported.
4. Update Java `package` declarations, imports, component scanning assumptions, tests, and any OpenAPI or docs references.
5. Keep behavior unchanged while repackaging; any failing test should indicate an import, scanning, or accidental behavior regression.
6. For frontend work, keep the current role-first structure and create purpose subfolders only when adding enough files to justify them.
7. After each group, run targeted backend tests or frontend lint/build checks relevant to the moved files.
8. After all backend moves, run full backend verification.
9. Update plan files or implementation notes if a package move changes the practical ownership of a feature area.

## Documentation Sync

- Update `docs/backend-dev.md` when backend package rules or verification commands change.
- Update `docs/frontend-dev.md` when frontend folder rules or verification commands change.
- Update `docs/API_CONTRACT.md` only if package moves uncover stale endpoint examples or DTO naming.
- Update `.claude`, `.codex`, and `.agents` skills/prompts when implementation or review rules change.
- No ERD update is expected because repackaging should not change persistence shape.

## Acceptance Criteria

- New backend files follow class-type packages with optional purpose subpackages inside them.
- Existing feature-first backend packages are either migrated or tracked with explicit remaining work.
- New frontend files follow role-first folders with optional purpose subfolders inside them.
- Developer and reviewer skills require clean-code, correctness, performance, packaging, architecture, and verification review.
- Backend tests pass after repackaging, or every exception has a concrete file and follow-up owner.
- No API route, request, response, or database behavior changes solely because classes moved.
- Any remaining legacy package shape is documented with a clear follow-up path.
