# Frontend Teacher Test List

> Build teacher-owned test management list.

## Depends On

- `0011-backend-test-builder-core.md`

## Context

Teachers need an owner-only management table for all tests before entering the builder. It should expose lifecycle status, moderation notes, allowed actions, search/filter controls, and creation entry points without embedding the builder itself.

## Scope

### In scope

- `/teacher/tests`.
- List drafts, submitted, published, rejected, and archived tests.
- Search and status filter.
- Create new test entry point.
- Edit, preview, submit, archive actions where allowed.

### Out of scope

- Builder internals.
- Admin moderation decisions.

## Key Decisions

- Use teacher-owned test APIs and lifecycle statuses from the backend; do not infer editable/submittable actions only from frontend state.
- Keep filters and pagination in URL search params where practical.
- Action menu availability follows backend status/permission rules: edit draft/rejected, preview allowed owner content, submit when readiness passes, archive where supported.
- Rejected tests must surface moderation note and a path back into editing.
- Submitted/published tests open read-only preview unless backend supports clone/new-version later.

## What To Implement

- Teacher tests service and hooks.
- Status badges.
- Action menu with role/status rules.
- Empty state for new teachers.
- `/teacher/tests` page with list/table, search, status filter, sort, pagination, and create button.
- Mutations for create, archive, submit/submit navigation, and any allowed status actions exposed by backend.
- Confirmation dialog for archive/destructive actions.
- Status-specific empty/action states for draft, submitted, rejected, published, archived.
- Query invalidation after create/archive/submit.

## Tests

- Rejected test shows rejection note.
- Submitted test edit action is disabled.
- Archive confirmation is required.
- Search/status filters call API with expected params and preserve URL state.
- Action menu hides actions forbidden by current status.
- Empty teacher sees create-first-test CTA.

## Documentation Sync

- Keep lifecycle statuses aligned with backend.
- Mirror teacher test list/summary DTOs in frontend types.
- Keep builder routes aligned with `0050`.

## Acceptance Criteria

- Teacher can manage all owned tests from one page.
- Teacher can see what state each test is in and which action is safe next.
