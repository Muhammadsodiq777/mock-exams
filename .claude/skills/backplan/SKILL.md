---
name: backplan
description: >
  Architecture and implementation planning skill for Mock Exams backend work and API-led changes.
  Use when the user asks for a backend plan, design, architecture, implementation approach,
  module breakdown, API design, schema design, or a plan file for new functionality. Also use
  when a request spans backend plus frontend and the backend contract or business rules drive
  the work. Do not trigger for direct coding tasks, trivial fixes, or purely visual frontend work.
---

# Mock Exams Backend Planning

Produce a plan that tells implementers what to build, what constraints matter, and what docs
must stay in sync. Do not write code in the plan.

## Understand First

Before planning, read:

1. `docs/overview.md` if it exists
2. Relevant files in `docs/specs/`
3. `docs/API_CONTRACT.md` when the task affects endpoints, DTOs, auth, pagination, or frontend/backend coupling
4. Relevant files in `docs/ERD/` when the task changes persistence-heavy flows
5. `docs/backend-dev.md`
6. The actual controllers, services, repositories, DTOs, schema files, and frontend API consumers touched by the change

If `docs/overview.md` is missing, say so briefly and continue.

Use `java-spring-guidelines` for backend conventions. If frontend/API shape is involved, also use
`react-typescript-guidelines` so the plan matches current client patterns.

## What the Plan Must Decide

- Which existing package or feature area owns the change
- DTO and endpoint changes, including tenant-scoped routes
- Schema changes as new Flyway migrations under `backend/src/main/resources/db/migration/` if persistence changes
- Business invariants, validation rules, and soft-delete implications
- Query, locking, or transaction concerns
- Frontend impact when API responses, query keys, or UX flows change
- Which docs/specs need updates

Make decisions when the sensible path is clear. Only present multiple options when the trade-off
is real and consequential.

## Plan Output

If the user wants a persisted plan, create `docs/plans/{NNNN}-{slug}.md`.
If `docs/plans/` does not exist yet, create it.
If the user only wants discussion, present the same structure inline.

Use this structure:

```md
# {Title}

> One-line summary.

## Context

## Scope

### In scope

### Out of scope

## Key Decisions

## What To Implement

## Documentation Sync

## Acceptance Criteria
```

## Writing Rules

- Keep units of work meaningful; do not reduce them to code chores
- Do not prescribe git workflow
- Do not prescribe exact agent sequencing unless the user asks for orchestration
- Include concrete negative cases, not generic "handle edge cases"
- When docs are missing, call that out and plan the minimal docs needed

## Documentation Sync

Every plan must say which living docs change:

- `docs/specs/{domain}.md`
- `docs/API_CONTRACT.md` when HTTP contract or DTO conventions change
- `docs/ERD/{name}.dbml` when persistence model understanding changes materially
- `docs/backend-dev.md` or `docs/frontend-dev.md` when workflow changes
- `docs/overview.md` if the repo adds it later

## Quality Check

Before presenting the plan, verify:

- You read the current code and relevant specs
- The plan matches Mock Exams' actual Java/Spring + React/Vite structure
- Schema changes mention the new Flyway migration file when required
- Acceptance criteria are observable and specific
- Documentation updates are listed explicitly
