---
name: react-typescript-guidelines
description: >
  Opinionated React + TypeScript guidelines for the Mock Exams frontend. Covers the current Vite,
  React Query, React Router, Zustand, i18n, Tailwind, API service, and page/component structure.
  Trigger whenever the user writes, reviews, scaffolds, or plans frontend code in `frontend/`
  for this repository. Do not trigger for backend-only work.
---

# Mock Exams React + TypeScript Guidelines

Use this skill as the frontend convention source for Mock Exams.

## Read First

Before coding or reviewing frontend changes, read:

1. `docs/overview.md` if it exists
2. Relevant `docs/specs/*.md`
3. `docs/API_CONTRACT.md` when the task affects API usage, auth, pagination, or DTO shape
4. `docs/frontend-dev.md`
5. The nearby page, component, hook, service, store, and type files you are changing

If `docs/overview.md` is missing, note that briefly and continue.

## Stack

- React 18.3.1 in this project
- TypeScript
- Vite 5.4.x in this project
- Tailwind CSS
- TanStack React Query v5
- React Router 6
- Zustand
- i18next
- Axios

## Verification

Prefer `just` recipes if the repo adds them later. Today the working defaults are:

- `npm run lint`
- `npm run build`
- `npm run e2e` only when the changed flow truly needs browser coverage

## Codebase Shape

The frontend folder rule is role first, with purpose subfolders only inside those role folders.
Pages stay under `pages/`, reusable and feature UI under `components/`, API calls under `services/`,
React Query/stateful helpers under `hooks/`, Zustand stores under `store/`, backend-facing and
domain types under `types/`, and support code under `utils/` or `constants/`.
When a folder becomes crowded, split it by purpose inside that folder, for example
`pages/student/`, `components/test-builder/`, `services/admin/`, `hooks/attempts/`, or `types/payments/`.

Do not add feature-first trees that mix pages, services, hooks, and types together under one feature
folder unless a dedicated architecture plan approves that migration.

- `app/`: providers and router
- `pages/`: page containers
- `components/ui/`: reusable presentation building blocks
- `components/auth/`: auth-related UI
- `services/`: axios client and API access
- `hooks/`: reusable React Query hooks and stateful helpers
- `store/`: Zustand session state
- `types/`: API and domain types
- `utils/` and `constants/`: support code

Read these references as needed:

- App structure: `references/app-patterns.md`
- Data and state flow: `references/data-patterns.md`
- Verification and UX states: `references/verification.md`

## Non-Negotiable Rules

- Keep API calls in `services/`, not scattered through random components
- Reuse `backendService` and `apiRoutes` before adding new ad hoc clients
- Keep query keys stable and invalidate affected queries after mutations
- In TanStack Query v5 code, prefer `isLoading` over deprecated `isInitialLoading`
- Handle loading, error, empty, and success states explicitly
- Preserve i18n patterns with `useTranslation`
- Match existing Tailwind and responsive layout patterns instead of redesigning the app during feature work
- Mirror backend DTOs in `types/backend.ts` when API shapes change

## Component Guidance

- Pages should own page-level orchestration
- Shared UI belongs in reusable components when it is actually reused
- Prefer small local helpers over premature abstraction
- Keep forms and mutation handlers easy to trace

## Documentation

When frontend behavior changes, update the relevant `docs/specs/*.md` so the documented flow still
matches the UI and backend contract.
Update `docs/API_CONTRACT.md` when the client-visible contract changes.
