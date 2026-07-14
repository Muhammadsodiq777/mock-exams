# Testing E2E Critical Flows

> Cover the user journeys that prove the product loop works.

## Depends On

- Backend MVP plans.
- Frontend MVP plans.

## Context

E2E coverage proves that the product loop works across backend, frontend, auth, files, AI mocks, payments, and eventually Google schedule mocks. It should focus on a small number of critical journeys rather than trying to cover every edge case already handled by unit/integration tests.

## Scope

### In scope

- Student registration and login.
- Teacher registration and approval.
- Teacher creates Reading or Listening test.
- Teacher creates rich Reading content and previews it in real time.
- Admin approves test.
- Student solves test and sees result.
- Student submits writing and speaking AI requests with mocked providers.
- Student purchases paid content with payment sandbox/mock.
- Teacher announces a paid live speaking slot; student books it; payment mock confirms; Google mock returns a Meet link.

### Out of scope

- Full growth features.
- Cross-browser matrix in MVP.
- Load/performance testing.
- Real money, real OpenAI, or real Google calls in automated E2E.

## Key Decisions

- E2E tests use deterministic seed data and reset scripts so journeys can run repeatedly.
- External providers are mocked: AI, speech-to-text, payment, MinIO-compatible file flows where needed, and Google Calendar/Meet.
- Tests should assert user-visible behavior and critical persisted outcomes, not internal implementation details.
- Stable selectors/accessible names are required for key controls.
- Keep the automated journey set small and high-value; deeper negative cases stay in backend/frontend focused tests.
- Use fixed timezone-safe dates for schedule/booking tests.

## What To Implement

- E2E seed data.
- Stable selectors for critical controls.
- Mock external providers.
- Reset script for local test data.
- Calendar/timezone-safe fixtures for live speaking schedules.
- Journey specs for student auth, teacher approval, teacher builder/moderation, student attempt/result, AI writing/speaking mocked feedback, paid marketplace purchase, and live speaking booking when available.
- Environment config for local backend/frontend URLs and mock provider modes.
- Test data cleanup/reset that does not depend on manual database edits.
- Failure diagnostics: screenshots/traces/log pointers where the selected E2E tool supports them.

## Verification

- `npm run e2e` when configured.
- Backend test services must run with isolated test data.
- Provider mock assertions for payment confirmation, AI response, file access, and Google Meet link creation.

## Documentation Sync

- Add E2E setup to `docs/frontend-dev.md`.
- Document Google mock mode and deterministic test times for live speaking E2E.
- Update README/local dev docs if E2E setup changes required services or commands.

## Acceptance Criteria

- The core student -> score -> feedback -> buy/improve loop is verified end to end.
- The teacher schedule -> student booking -> payment -> Meet link loop is verified without real Google or real money calls.
- E2E suite can be run locally from a clean reset with documented commands and deterministic results.
