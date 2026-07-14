# E2E Critical Journey Manifest

This manifest keeps the critical-flow plan visible without turning unimplemented product behavior into test fiction. Status values are `runnable`, `pending`, or `gated`.

| Journey | Status | Current harness target | Blocker or next condition |
| --- | --- | --- | --- |
| Public catalog boots with deterministic API data | runnable | `public-catalog.smoke.spec.ts` mocks `GET /api/v1/public/tests` and proves the Vite app renders a public route. | None. Keep this smoke stable while broader journeys are added. |
| Student registration and login | pending | Add browser journey through `/register` and `/login` with seeded or resettable users. | Backend reset/seed command and stable auth fixture accounts. |
| Teacher registration and approval | pending | Exercise teacher signup, pending profile guidance, and admin approval once seed/reset supports role handoff. | Deterministic admin account plus teacher profile moderation seed/reset support. |
| Teacher creates Reading or Listening test | pending | Cover create/edit, section save, question save, and preview for one test type first. | Stable selectors for builder controls and backend reset data for teacher-owned drafts. |
| Admin approves test | pending | Verify moderation queue, approve action, and public catalog visibility. | Teacher builder journey plus admin moderation seed/reset support. |
| Student solves test and sees result | pending | Exercise Reading or Listening attempt start, answer save, submit, and result page. | Published test fixture, attempt reset, and deterministic scoring outcome. |
| Writing AI feedback with mocked provider | pending | Submit `/writing`, poll feedback states, and assert mocked feedback renders. | Backend mock AI provider mode and stable usage-limit fixtures. |
| Speaking AI feedback with mocked speech-to-text and AI | pending | Upload/record placeholder audio, complete transcription, and render feedback. | Backend speech-to-text and AI mock providers plus practical test audio fixture. |
| Paid marketplace purchase with payment mock | pending | Purchase a paid listing and assert access grant after mock payment confirmation. | Payment mock/sandbox mode, idempotent reset data, and no real payment capture. |
| Live speaking slot announcement and booking | pending | Add a browser journey for `/teacher/schedule` and `/speaking` once seed/reset data can hand off teacher, student, and mock payment state. | Backend schedule/payment/Meet mock exists; needs deterministic full-stack seed/reset and selectors. |

## Release-Readiness Frontend Checks

- [x] Committed `npm run e2e` entry point with failure screenshots/traces configured.
- [x] Added one deterministic smoke that does not require backend seed data.
- [x] Documented local e2e commands and diagnostics in `docs/frontend-dev.md`.
- [ ] Add resettable full-stack seed data before automating auth, builder, attempt, AI, and payment journeys.
- [ ] Add provider-mode assertions before any AI, speech-to-text, payment, file-storage, or Google mock journeys run in CI.
- [ ] Add live speaking browser journey after resettable teacher/student/payment seed data exists.
