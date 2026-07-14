# Release Readiness Checklist

> Define the final quality gate before launching MVP.

## Depends On

- `0060-testing-backend-unit-integration.md`
- `0061-testing-api-contract.md`
- `0062-testing-frontend-components.md`
- `0063-testing-e2e-critical-flows.md`
- `0064-testing-ai-payment-file-storage.md`

## Context

Release readiness is the final gate that ties together tests, migrations, local/deployment configuration, provider controls, seed data, documentation, and manual QA. It should prevent "it builds" from being mistaken for "the MVP is launchable."

## Scope

### In scope

- Backend tests.
- Frontend lint and build.
- E2E critical flows.
- Migration verification.
- Environment variable checklist.
- Admin seed account.
- AI and payment cost controls.
- File storage bucket verification.
- Provider mock/sandbox checklist for AI, speech-to-text, payments, MinIO, and Google Calendar/Meet when live speaking is in scope.
- Manual QA script for student, teacher, and admin roles.
- Documentation and rollback readiness.

### Out of scope

- Production incident process.
- Marketing launch plan.
- Long-term observability/on-call process beyond minimum launch checks.

## Key Decisions

- Release readiness is a checklist with explicit pass/fail/exception status for each item.
- Exceptions must name the risk, owner, and follow-up; silent skipped checks are not acceptable.
- Migration verification and environment validation are release blockers.
- Provider integrations must either be disabled safely or verified in sandbox with cost controls.
- Manual QA covers role-based critical paths that automated tests do not fully cover.
- Docs must match actual run/test/deploy behavior before release.

## What To Implement

- Release checklist document or CI job list.
- Manual QA script for student, teacher, and admin.
- Rollback notes for migrations and provider toggles.
- Environment variable checklist for backend/frontend, including enabled flags and secret placeholders.
- Seed account verification for admin, teacher, and student.
- Migration rollback/forward notes for schema changes that cannot be trivially reverted.
- Cost-control checklist for AI limits, payment mock/sandbox mode, and live speaking Google mode.
- File storage bucket/object access checklist.

## Required Verification

- `cd backend && mvn test`.
- `cd frontend && npm run lint`.
- `cd frontend && npm run build`.
- E2E suite for MVP flows.
- Migration verification from empty database.
- Manual QA for auth, teacher approval, builder/moderation, student attempt/result, AI feedback with mock provider, payment sandbox/mock, admin finance/settings, and live speaking schedule when included.

## Documentation Sync

- Update README with final local run steps.
- Update docs when release commands change.
- Update `docs/backend-dev.md` and `docs/frontend-dev.md` with final env/test/provider instructions.
- Update `docs/API_CONTRACT.md`, `docs/specs/project-overview.md`, and `docs/ERD/platform-architecture.dbml` if launch behavior differs from documented plans.

## Acceptance Criteria

- MVP cannot be considered ready until all checklist items pass or have explicit owner-approved exceptions.
- A fresh developer can follow docs to run, test, and smoke-check the MVP without hidden setup.
