# Release Readiness Checklist

This checklist is the MVP launch gate. Each row must be marked `pass`, `fail`, `exception`, or
`blocked`. Exceptions require a named owner, risk, and follow-up date before the MVP can be
considered ready.

## Status Legend

| Status | Meaning |
| --- | --- |
| `pass` | Check completed successfully for the release candidate. |
| `fail` | Check failed and blocks release. |
| `exception` | Owner accepted the risk, documented follow-up, and confirmed the release can proceed. |
| `blocked` | Required product surface or environment is not available yet. |

## Automated Gates

| Gate | Command or evidence | Status | Owner | Notes |
| --- | --- | --- | --- | --- |
| Backend unit/integration suite | `cd backend && ./mvnw test` |  |  | Must include migration verification from an empty H2 database. |
| Frontend lint | `cd frontend && npm run lint` |  |  | No warnings are allowed unless documented as exceptions. |
| Frontend production build | `cd frontend && npm run build` |  |  | Existing Vite chunk-size warning may be tracked as an exception until code-splitting work lands. |
| E2E critical-flow suite | `cd frontend && npm run e2e` |  |  | Requires local frontend/backend URLs and mock provider modes. |
| Fresh infrastructure reset | `docker compose down -v --remove-orphans` then `docker compose up -d` |  |  | Verify PostgreSQL, Redis, MinIO, and the `mock-exams-local` bucket. |
| Backend environment validation | Backend starts with release env values |  |  | Unsafe provider-enable combinations are release blockers. |

## Environment Checklist

| Area | Required values | Status | Owner | Notes |
| --- | --- | --- | --- | --- |
| Database | `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` point to the intended database. |  |  | Do not use local Compose credentials outside local development. |
| JWT | `JWT_ISSUER`, strong `JWT_SECRET`, access/refresh TTLs. |  |  | `JWT_SECRET` must not be the dev-only default. |
| CORS | `CORS_ALLOWED_ORIGINS` includes the deployed frontend origin only. |  |  | Keep local `localhost` values out of production. |
| Frontend API | `VITE_API_BASE_URL` points at the deployed `/api/v1` backend. |  |  | Leave unset only for local Vite proxy development. |
| Feature flags | `VITE_FEATURE_AUTH`, `VITE_FEATURE_AI`, `VITE_FEATURE_PAYMENTS`, `VITE_FEATURE_MARKETPLACE`. |  |  | Flags must match enabled backend capabilities. |
| MinIO/object storage | Endpoint, bucket, credentials, signed URL access. |  |  | Bucket existence and private-file access are release blockers. |
| AI providers | Mock/disabled locally; sandbox or production keys only when intentionally enabled. |  |  | Confirm usage hard limits and failure accounting. |
| Payments | Mock locally; sandbox/real provider only with valid webhook secret and cost controls. |  |  | Never launch with `PAYMENT_ENABLED=true` and `PAYMENT_PROVIDER=mock`. |
| Google Calendar/Meet | Mock local mode for live speaking; real Google remains disabled until OAuth/provider adapter work. | `partial` |  | Verify no real Google calls happen in local/CI. |

## Seed Accounts

| Account | Purpose | Status | Owner | Notes |
| --- | --- | --- | --- | --- |
| Admin | Teacher approval, moderation, settings, reports, finance. |  |  | Verify login and role guards. |
| Teacher | Builder, listings, earnings, withdrawals. |  |  | Verify approved and non-approved teacher states. |
| Student | Attempts, results, AI practice, marketplace purchase. |  |  | Verify paid/free access behavior. |

Seed credentials must be development-only unless a deployment-specific admin bootstrap process is used.
Do not publish reusable production passwords in committed documentation.

## Manual QA Script

Run these checks after automated gates pass.

| Flow | Script | Status | Owner | Notes |
| --- | --- | --- | --- | --- |
| Student auth | Register student, log out, log in, reload, verify `/dashboard`. |  |  | Inactive/suspended account handling should clear or block the session correctly. |
| Teacher approval | Register teacher, verify pending profile guidance, approve as admin, reload teacher routes. |  |  | Builder routes require approved teacher status. |
| Builder and moderation | Create reading/listening test, add content/questions, preview, submit, approve as admin. |  |  | Student preview must not expose answer keys. |
| Student attempt/result | Start approved test, save answers, submit, view result and progress context. |  |  | Local draft recovery must not invent backend resume data. |
| Writing AI mock | Submit writing answer, request feedback, verify queued/processing/completed or failed states. |  |  | AI usage-limit errors must be visible when applicable. |
| Speaking AI mock | Upload/record audio, transcribe with mock provider, request speaking feedback. |  |  | Failed transcription must keep the audio/submission recoverable. |
| Marketplace payment mock | Create paid listing, start checkout, post valid mock webhook, verify access and wallet earning. |  |  | Duplicate webhook must not duplicate purchase or wallet transaction. |
| Admin finance/settings | Inspect payments, disputes, withdrawals, reports, and platform settings. |  |  | Secrets must not appear in API responses or UI. |
| File access | Upload teacher/student/admin files, verify signed URL and private access boundaries. |  |  | Another user must not access private speaking audio. |
| Live speaking schedule | Teacher slot -> student booking -> payment -> Meet link. |  |  | Covered by backend mock flow; add browser seed/reset before marking e2e-ready. |

## Provider Sandbox Checklist

| Provider | Automated local coverage | Manual sandbox requirement | Status | Owner |
| --- | --- | --- | --- | --- |
| AI writing/speaking/study plan | Mock provider and failure tests. | Verify sandbox/prod key, model, cost limits, malformed response handling. |  |  |
| Speech-to-text | Mock transcription and failure tests. | Verify sandbox audio upload, empty transcript, timeout, and retry behavior before enabling OpenAI transcription. |  |  |
| Payments | Mock checkout/webhook/idempotency tests. | Verify provider dashboard, signed webhook, paid/failed/amount-mismatch callbacks. |  |  |
| File storage | Mocked storage and MinIO local bucket checks. | Verify bucket policy, signed URL expiry, private-file denial, and storage failure alerts. |  |  |
| Google Calendar/Meet | Mock sync row and Meet link in backend tests. | Verify real create/update/cancel/retry/idempotency in sandbox before enabling a non-mock adapter. | `partial` |  |

## Rollback Notes

- Migrations: Flyway migrations are forward-only. A release rollback must restore the previous
  application artifact and database backup or run a separately reviewed corrective migration.
- Provider toggles: Disable OpenAI with `OPENAI_ENABLED=false`; set AI providers back to `mock`.
  Disable payments with `PAYMENT_ENABLED=false` and keep provider callbacks idempotent.
- Frontend release: Roll back to the previous built artifact and matching `VITE_API_BASE_URL`/feature
  flag set.
- Object storage: Do not delete uploaded objects during rollback unless the affected release created
  test-only data and the owner has approved cleanup.

## Exception Template

| Field | Value |
| --- | --- |
| Check |  |
| Status | `exception` |
| Risk |  |
| Owner |  |
| Follow-up |  |
| Approval |  |
