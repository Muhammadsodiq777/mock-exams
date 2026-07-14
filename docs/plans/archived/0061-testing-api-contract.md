# Testing API Contract

> Keep backend DTOs and frontend types aligned.

## Depends On

- `0003-api-contract-conventions.md`
- `0038-frontend-api-client-types.md`

## Context

The backend and frontend share many DTO-heavy flows. Contract drift would break forms, tables, builders, players, dashboards, and admin screens. This plan keeps `docs/API_CONTRACT.md`, backend controller behavior, and `frontend/src/types/backend.ts` aligned.

## Scope

### In scope

- Contract examples in `docs/API_CONTRACT.md`.
- Controller tests for response status and error shape.
- Frontend type mirrors.
- Build-time TypeScript checks.
- Rich-text payload examples and preview DTO examples.
- Live speaking schedule, booking, payment target, notification, and Google sync DTO examples when `0067` lands.

### Out of scope

- Full generated client pipeline.
- Public OpenAPI publishing/customization unless adopted later.

## Key Decisions

- `docs/API_CONTRACT.md` remains the human-readable source for route groups, envelopes, DTO examples, errors, pagination, and filter semantics.
- Controller tests should lock representative success and failure shapes for each endpoint group.
- TypeScript DTO mirrors are updated in the same feature work that changes backend response/request DTOs.
- Contract tests must include role-sensitive shapes, especially preview modes that hide/show answer keys.
- Generated clients are out of scope for MVP; hand-written services remain acceptable if contract examples and builds stay current.
- Error assertions should check `error.code`, field paths, and HTTP status.

## What To Implement

- Example payloads for every endpoint group.
- Tests for validation errors and forbidden responses.
- Frontend service functions typed to DTOs.
- Contract tests that student preview hides answer keys while teacher/admin preview exposes permitted metadata.
- Contract tests for live speaking booking state transitions, provider pending/failure states, and payment confirmation shape.
- Shared test helpers for envelope assertions, page metadata assertions, and error envelope assertions.
- Frontend type coverage for route params/query params, command DTOs, response DTOs, and feature-specific error codes.
- Checklist item in every implementation plan: API contract updated before frontend consumes new fields.

## Verification

- Backend targeted controller tests.
- Frontend `npm run build`.
- Mock-provider contract tests for Google Calendar/Meet routes after `0067`.
- Focused frontend TypeScript build after DTO/type changes.

## Documentation Sync

- Keep `docs/API_CONTRACT.md` current with every route change.
- Keep `frontend/src/types/backend.ts` synchronized for rich-text and live speaking DTOs.
- Update `docs/frontend-dev.md` if API service/type conventions change.
- Update `docs/backend-dev.md` if controller test commands/conventions change.

## Acceptance Criteria

- Frontend implementation does not rely on undocumented backend shapes.
- Preview and schedule flows cannot ship with undocumented or role-leaky response shapes.
- New endpoints have documented DTO examples, controller tests, and frontend type mirrors before feature work is called complete.
