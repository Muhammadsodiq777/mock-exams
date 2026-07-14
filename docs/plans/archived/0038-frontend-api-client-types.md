# Frontend API Client Types

> Centralize API calls, route constants, DTOs, and error handling.

## Depends On

- `0003-api-contract-conventions.md`
- `0037-frontend-auth-session.md`

## Context

The frontend should not scatter Axios calls, route strings, envelope parsing, or DTO definitions across pages. The current `frontend/src/services/api.ts` and `frontend/src/types/backend.ts` are starter shapes; this plan turns them into the shared contract layer for all later pages.

## Scope

### In scope

- Axios client configuration.
- Auth header injection.
- Backend error normalization.
- API route constants.
- Shared DTO types in `types/backend.ts`.
- Response envelope helpers for single-resource, list/page, dashboard, and command responses.
- Query key constants for TanStack React Query feature hooks.

### Out of scope

- Feature-specific UI.
- Generated OpenAPI client.
- Runtime schema validation library unless adopted deliberately.

## Key Decisions

- Keep raw HTTP calls in `services/` and feature data hooks in `hooks/`; components should call hooks or service helpers, not Axios directly.
- Mirror backend DTO names and enum strings exactly in `types/backend.ts`.
- Normalize API errors once so forms/pages can branch on `error.code` and `fieldErrors`.
- Keep route constants grouped by backend API area: public, student, teacher, admin, auth.
- Query keys should be stable arrays that include filters/pagination values.
- Feature flags in `services/api.ts` should guard visible incomplete flows, not replace authorization.

## What To Implement

- `services/backendService`.
- `services/apiRoutes`.
- Typed service functions per feature domain.
- Query key constants.
- API envelope unwrap helpers and error normalizer.
- Auth header injection and optional unauthorized handler hook.
- Types for pagination metadata, sort/filter metadata, dashboard metadata, and common business error codes.
- Service modules or grouped methods for auth, catalog, attempts, results, AI practice, marketplace, teacher, admin, settings, and schedule when implemented.
- Test/build checks that detect DTO import drift.

## Tests

- Build catches DTO type drift.
- Error normalization handles validation and business errors.
- Auth token is attached when present.
- Unknown or unsupported API base URL values fail at startup with a clear message.
- List/page helpers preserve metadata needed by filters and pagination UI.
- Components do not import `axios` directly.

## Documentation Sync

- Update `docs/frontend-dev.md`.
- Keep `docs/API_CONTRACT.md` mirrored.
- Update `frontend/src/types/backend.ts` in the same implementation plan whenever backend DTOs change.

## Acceptance Criteria

- Components never call Axios directly.
- API errors display consistently across forms and pages.
- Later feature pages can use shared typed services, query keys, and error handling without reinventing API glue.
