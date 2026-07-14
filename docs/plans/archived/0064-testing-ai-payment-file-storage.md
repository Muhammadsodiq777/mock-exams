# Testing AI Payment File Storage

> Define test strategy for external integrations.

## Depends On

- `0008-backend-file-storage-minio.md`
- `0018-backend-ai-usage-limits-costs.md`
- `0025-backend-payments-provider-integration.md`

## Context

AI, speech-to-text, payments, file storage, and Google Calendar/Meet are external integrations where failures are normal. The platform needs adapter boundaries, fake implementations, sandbox checklists, and tests proving external failures do not corrupt domain state.

## Scope

### In scope

- Mock AI providers for writing, speaking, and study plan.
- Mock speech-to-text provider.
- Payment sandbox or mock provider.
- Google Calendar/Meet mock provider for live teacher speaking schedules.
- MinIO test bucket verification.
- Failure and retry cases.

### Out of scope

- Real money test in automated suite.
- Real Google Calendar/Meet calls in automated suite.
- Unbounded provider snapshot tests.
- Testing provider billing/pricing accuracy beyond stored estimates.

## Key Decisions

- Every external provider has an interface/adapter and a fake/mock implementation used by automated tests and local development.
- Automated tests should cover adapter request construction, response parsing, failure mapping, retry/idempotency behavior, and business-state preservation.
- Real provider sandbox checks are manual or explicitly gated; they should never be required for normal local test runs.
- Provider secrets stay outside committed files and are validated through environment configuration.
- Store enough provider metadata for support and reporting, but do not expose secrets in API responses.
- External failure tests must assert the domain status left behind, not only that an exception was thrown.

## What To Implement

- Provider interfaces with fake implementations.
- Contract tests around provider adapters.
- File upload/download integration tests.
- Webhook signature tests.
- Google event/Meet-link create, update, cancel, retry, and idempotency tests around the provider adapter.
- AI provider mock responses for valid JSON, malformed JSON, provider timeout, rate limit, and disabled provider.
- Speech-to-text mock responses for completed transcript, empty transcript, unsupported audio, timeout, and retry.
- Payment mock responses for pending, paid, failed, duplicate webhook, invalid signature, amount mismatch.
- File storage tests for object-store failure before metadata save, metadata save failure handling, signed URL expiration, and access control.

## Verification

- Targeted backend integration tests.
- Manual sandbox checklist for real providers.
- Manual Google sandbox checklist only after local mock coverage passes.
- Local automated suite runs with all providers disabled or mocked.

## Documentation Sync

- Update `docs/backend-dev.md` with provider mock configuration.
- Add Google Calendar/Meet local mock and optional sandbox variables when `0067` lands.
- Update `docs/API_CONTRACT.md` when provider status/error fields are client-visible.
- Update release checklist `0065` with required sandbox/manual checks.

## Acceptance Criteria

- External failures do not corrupt business state.
- Local development can run without paid external calls.
- Calendar/Meet failures leave bookings recoverable and visible to support instead of breaking payment or booking state.
- Provider integrations are testable without network access and have documented manual sandbox checks before launch.
