# Data Patterns

## API Layer

- `frontend/src/services/api.ts` owns axios setup, auth headers, refresh flow, and error normalization
- `frontend/src/services/backend.service.ts` is the main business API surface for app features
- Small domain-specific services can exist, but do not bypass the established axios client

## React Query

- Use React Query for server state
- Query keys should include relevant filter context
- Mutations should invalidate only the keys affected by the change
- Prefer existing hooks when they already model the fetch lifecycle cleanly
- Prefer `isLoading` over deprecated `isInitialLoading` in TanStack Query v5

## Page Data Flow

- Pages often orchestrate queries and mutations directly when the logic is page-specific
- Extract a custom hook only when the state or fetch flow is reused or getting hard to follow
- Keep optimistic updates conservative unless the backend contract clearly supports them
