# Implementation Plans

Use these plans in numeric order. Each file is intentionally small and focused so it can be implemented, reviewed, and tested independently.

## Architecture and Contracts

- `0001-architecture-boundaries.md`
- `0002-database-erd-and-migrations.md`
- `0003-api-contract-conventions.md`
- `0004-local-dev-and-infrastructure.md`

## Backend Foundation

- `0005-backend-auth-registration-login.md`
- `0006-backend-users-roles-permissions.md`
- `0007-backend-teacher-profiles-approval.md`
- `0008-backend-file-storage-minio.md`
- `0009-backend-notifications-basics.md`

## Backend Test Engine

- `0010-backend-test-catalog.md`
- `0011-backend-test-builder-core.md`
- `0012-backend-reading-test-builder.md`
- `0013-backend-listening-test-builder.md`
- `0014-backend-question-types-answer-keys.md`
- `0015-backend-test-attempt-lifecycle.md`
- `0016-backend-auto-scoring-results.md`
- `0017-backend-progress-analytics.md`

## Backend AI

- `0018-backend-ai-usage-limits-costs.md`
- `0019-backend-writing-submissions-ai-feedback.md`
- `0020-backend-speaking-audio-transcription.md`
- `0021-backend-speaking-ai-feedback.md`
- `0022-backend-ai-study-plan.md`

## Backend Marketplace and Admin

- `0023-backend-marketplace-catalog.md`
- `0024-backend-purchases-and-access-control.md`
- `0025-backend-payments-provider-integration.md` (implemented, see `archived/`)
- `0026-backend-teacher-wallet-commission.md` (implemented, see `archived/`)
- `0027-backend-withdrawals.md` (implemented, see `archived/`)
- `0028-backend-coupons-promos.md` (implemented, see `archived/`)
- `0029-backend-reviews-ratings.md` (implemented, see `archived/`)
- `0030-backend-admin-dashboard.md` (implemented, see `archived/`)
- `0031-backend-test-moderation.md` (implemented, see `archived/`)
- `0032-backend-user-teacher-management.md` (implemented, see `archived/`)
- `0033-backend-payment-disputes.md`
- `0034-backend-platform-settings.md`
- `0035-backend-reports-ai-usage.md`

## Frontend Foundation and Student Experience

- `0036-frontend-app-shell-routing.md`
- `0037-frontend-auth-session.md`
- `0038-frontend-api-client-types.md`
- `0039-frontend-design-system.md`
- `0040-frontend-student-dashboard.md`
- `0041-frontend-test-list-marketplace.md`
- `0042-frontend-reading-test-player.md`
- `0043-frontend-listening-test-player.md`
- `0044-frontend-results-progress.md`
- `0045-frontend-writing-practice-ai.md`
- `0046-frontend-speaking-practice-ai.md`
- `0047-frontend-profile-pricing.md`

## Frontend Teacher and Admin Experience

- `0048-frontend-teacher-dashboard.md`
- `0049-frontend-teacher-test-list.md`
- `0050-frontend-test-builder-shell.md`
- `0051-frontend-reading-builder.md`
- `0052-frontend-listening-builder.md`
- `0053-frontend-builder-preview-submit.md`
- `0054-frontend-teacher-earnings-students.md`
- `0055-frontend-admin-dashboard.md`
- `0056-frontend-admin-teacher-approval.md`
- `0057-frontend-admin-test-moderation.md`
- `0058-frontend-admin-payments-withdrawals.md`
- `0059-frontend-admin-settings-reports.md`

## Live Teacher Speaking

- `0067-live-teacher-speaking-schedules.md`

## Testing and Release

- `0060-testing-backend-unit-integration.md`
- `0061-testing-api-contract.md`
- `0062-testing-frontend-components.md`
- `0063-testing-e2e-critical-flows.md`
- `0064-testing-ai-payment-file-storage.md`
- `0065-release-readiness-checklist.md`
- `0066-packaging-rules-and-repackaging.md`

Keep `docs/specs/project-overview.md`, `docs/ERD/platform-architecture.dbml`, and `docs/API_CONTRACT.md` synchronized as each plan is implemented.
