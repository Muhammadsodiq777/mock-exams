# Frontend App Shell Routing

> Establish the React/Vite app shell and route tree.

## Depends On

- `0001-architecture-boundaries.md`
- `0003-api-contract-conventions.md`

## Context

The frontend is currently a small Vite app with shared API setup and starter exam display. Before feature pages multiply, the app needs stable route groups, layouts, navigation rules, and protected-route behavior that match `/api/v1/public`, `/student`, `/teacher`, `/admin`, and `/auth` backend boundaries.

## Scope

### In scope

- React Router route groups for public, student, teacher, and admin pages.
- Shared layout shells.
- Protected route wrappers.
- Not found and forbidden pages.
- Route placeholders for major MVP flows so later page plans can land without reshaping URLs.
- Navigation visibility by auth state, role, and teacher approval status.

### Out of scope

- Final page UI for every route.
- Auth form implementation.
- Data-heavy feature pages beyond placeholders/loading shells.

## Key Decisions

- Keep route modules under role-first frontend folders such as `pages/public`, `pages/student`, `pages/teacher`, and `pages/admin`.
- Use React Router 6 with nested layouts for public, authenticated app, teacher, and admin areas.
- Protected routes depend on the auth/session state from `0037`; placeholders can exist before backend data is ready.
- Forbidden and not-found pages should be reusable across route groups and should not leak role-specific private data.
- Navigation items are configuration-driven where practical and hidden/disabled based on session role and feature flags.

## What To Implement

- `app/router` or equivalent route definition.
- Public layout, app layout, teacher layout, and admin layout.
- Route placeholders for all MVP pages.
- Navigation structure that can hide items by role.
- Route constants for `/tests`, `/marketplace`, `/dashboard`, `/results/:id`, `/writing`, `/speaking`, `/profile`, `/pricing`, `/teacher/*`, `/admin/*`, and future `/teacher/schedule`.
- Auth-required wrapper, role-required wrapper, teacher-approval-aware wrapper, and redirect logic.
- Shared shell states for loading session, forbidden, unauthenticated redirect, and unknown route.
- Layout-level notification/nav slots that later plans can populate.

## Tests

- Build verifies all route modules compile.
- Protected route redirects unauthenticated user to login.
- Forbidden role sees a clear access-denied state.
- Teacher pending approval routes to status/profile action rather than builder pages.
- Unknown public and authenticated URLs render not-found without crashing.

## Documentation Sync

- Update `docs/frontend-dev.md`.
- Keep routes aligned with `docs/specs/project-overview.md`.
- Keep route constants aligned with `docs/API_CONTRACT.md` route groups and frontend service ownership.

## Acceptance Criteria

- The app has stable route locations before feature pages are built.
- Later feature plans can add pages under existing route groups without changing the shell contract.
