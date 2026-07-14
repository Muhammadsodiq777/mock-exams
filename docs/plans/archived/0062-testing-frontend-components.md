# Testing Frontend Components

> Define focused frontend verification for reusable UI and stateful pages.

## Depends On

- Frontend plans `0036` through `0059`

## Context

Frontend complexity will grow around auth/session state, IELTS players, rich-text builder/preview, payment/admin tables, AI async status, and settings forms. This plan defines minimum frontend verification without requiring a large test harness before it is justified.

## Scope

### In scope

- Component tests if a test harness exists or is added.
- Hook tests for data/state helpers where practical.
- Build and lint as minimum verification.
- Loading, empty, error, and success states.

### Out of scope

- Pixel-perfect screenshot testing for every page.
- Cross-browser matrix for every component.
- Testing backend business rules in frontend tests.

## Key Decisions

- `npm run lint` and `npm run build` are the minimum checks for every frontend implementation.
- Add component/hook tests when a form, state machine, or reusable UI component has enough logic to regress.
- Use browser verification for complex interactive flows until a formal component test harness is adopted.
- Verify loading, empty, error, success, disabled, and validation states for user-facing pages.
- Prefer stable accessible selectors/labels over brittle DOM structure assumptions.
- Keep API calls mocked at the service/hook boundary for frontend-only tests.

## What To Implement

- Add tests for forms, player controls, builder controls, and admin action modals.
- Use browser verification for complex interactive flows when unit harness is missing.
- Decide and document whether the repo uses Vitest/React Testing Library, Playwright component tests, or browser-only verification for each stage.
- Add reusable fixtures for API envelopes, validation errors, paginated lists, rich-text content, and async AI statuses.
- Add coverage for auth guards, form validation, query loading/error states, route navigation, and mutation invalidation where practical.
- Add manual verification checklist for flows not yet covered by automated tests.

## Verification

- `npm run lint`.
- `npm run build`.
- Targeted browser checks for test player and builder.
- `npm run e2e` only for full journey checks from `0063`.

## Documentation Sync

- Update `docs/frontend-dev.md` with chosen frontend test approach.
- Update feature plans when a reusable component's expected states change.
- Keep `docs/API_CONTRACT.md` examples available as frontend fixtures where useful.

## Acceptance Criteria

- Critical UI states are verified before release.
- Frontend features do not ship with only the happy path manually checked when they include forms, payments, AI status, builder state, or admin mutations.
