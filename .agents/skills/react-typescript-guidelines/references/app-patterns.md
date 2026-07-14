# App Patterns

## Routing

- Router setup lives in `frontend/src/app/router.tsx`
- Route-level UI should plug into the existing app shell and route error handling patterns

## State

- Auth state lives in Zustand stores under `frontend/src/store/`
- Do not duplicate session state in page-local component state without a reason

## Types

- Backend-facing DTO types live in `frontend/src/types/backend.ts`
- General API wrappers live in `frontend/src/types/api.ts`
- Keep types aligned with backend responses

## Shared UI

- Reuse existing UI primitives before adding near-duplicates
