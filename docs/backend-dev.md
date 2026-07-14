# Backend Development

## Stack

- Java 21 target
- Spring Boot 3.5.x
- Maven wrapper
- PostgreSQL for local runtime
- Redis for cache/session/work-queue style local dependencies as features land
- MinIO for local object storage
- H2 for tests
- Flyway migrations under `backend/src/main/resources/db/migration`

## Local Run

Use Java 21 for development. The Maven compiler is configured through Spring Boot's parent and the project `java.version`.

Start all local dependencies:

```sh
docker compose up -d
```

Run the backend:

```sh
cd backend
./mvnw spring-boot:run
```

Useful checks:

```sh
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/exams
```

## Backend Tests

Run focused backend tests from `backend/` while iterating:

```sh
./mvnw -Dtest=ClassName test
```

Run the full backend suite before calling a backend feature complete:

```sh
./mvnw test
```

Prefer service tests for business rules, controller tests for HTTP status/auth/envelope/validation
contracts, and migration tests for schema constraints. Versioned controller tests should assert
stable `error.code` values rather than prose messages. For reusable API contract assertions, use
`com.mockexams.backend.support.ApiContractAssertions` in `backend/src/test/java`.

Provider-facing automated coverage and manual sandbox gates are tracked in
[`docs/testing-provider-readiness.md`](testing-provider-readiness.md). Keep normal backend tests on
mock or disabled providers; real provider checks are opt-in manual checks only.

## Local Services

| Service | Local address | Notes |
| --- | --- | --- |
| PostgreSQL | `localhost:5433` | Database `mock_exams`, user `mock_exams`. |
| Redis | `localhost:6379` | Reserved for local cache/session/work-queue features. |
| MinIO API | `http://localhost:9000` | S3-compatible object storage for file flows. |
| MinIO console | `http://localhost:9001` | Use to inspect buckets and uploaded objects. |

## Environment Variables

Current database variables are already wired by `backend/src/main/resources/application.properties`.
The remaining variables define the local contract for upcoming auth, file, AI, payment, and Redis-backed features. Local development intentionally has safe defaults for disabled integrations, while backend startup validation fails for invalid values or unsafe provider-enable combinations.

| Variable | Local value | Required for |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5433/mock_exams` | PostgreSQL runtime. |
| `DATABASE_USERNAME` | `mock_exams` | PostgreSQL runtime. |
| `DATABASE_PASSWORD` | `mock_exams` | PostgreSQL runtime. |
| `REDIS_URL` | `redis://localhost:6379` | Redis-backed features. |
| `MINIO_ENDPOINT` | `http://localhost:9000` | File storage. |
| `MINIO_BUCKET` | `mock-exams-local` | File storage. |
| `MINIO_ACCESS_KEY` | `minioadmin` | File storage. |
| `MINIO_SECRET_KEY` | `minioadmin` | File storage. |
| `MULTIPART_MAX_FILE_SIZE` | `101MB` | Servlet parser ceiling for file uploads. |
| `MULTIPART_MAX_REQUEST_SIZE` | `105MB` | Servlet parser ceiling for multipart upload requests. |
| `JWT_ISSUER` | `mock-exams-local` | Auth token issuer. |
| `JWT_SECRET` | Local-only secret with at least 32 characters | Auth token signing. |
| `JWT_ACCESS_TOKEN_TTL` | `PT1H` | Auth token lifetime. |
| `JWT_REFRESH_TOKEN_TTL` | `P7D` | Auth refresh lifetime. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Browser access from Vite dev server. |
| `OPENAI_ENABLED` | `false` | AI integration guard. |
| `OPENAI_API_KEY` | `dev-only-openai-api-key` | AI feedback and study-plan flows. |
| `OPENAI_MODEL` | `gpt-4.1-mini` | AI model selection. |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | AI API base URL. |
| `AI_WRITING_PROVIDER` | `mock` | Writing feedback provider; use `mock` locally or `openai` with `OPENAI_ENABLED=true`. |
| `AI_SPEAKING_PROVIDER` | `mock` | Speaking feedback provider; use `mock` locally or `openai` with `OPENAI_ENABLED=true`. |
| `AI_STUDY_PLAN_PROVIDER` | `mock` | Study-plan provider; use `mock` locally or `openai` with `OPENAI_ENABLED=true`. |
| `TRANSCRIPTION_PROVIDER` | `mock` | Speaking transcription provider; use `mock` locally or `openai` with `OPENAI_ENABLED=true`. |
| `TRANSCRIPTION_MODEL` | `gpt-4o-mini-transcribe` | Speech-to-text model when using the OpenAI transcription provider. |
| `TRANSCRIPTION_MAX_AUDIO_DURATION` | `PT5M` | Maximum accepted speaking recording duration. |
| `TRANSCRIPTION_COST_PER_MINUTE` | `0.000000` | Optional local cost-estimate assumption for transcription minutes. |
| `AI_USAGE_PERIOD` | `monthly` | AI usage limit period. |
| `AI_USAGE_FREE_REQUESTS_LIMIT` | `20` | Free AI requests per user period. |
| `AI_USAGE_HARD_LIMIT_REQUESTS` | `50` | Hard AI request cap per user period. |
| `AI_USAGE_COUNT_PROVIDER_FAILURES` | `false` | Whether provider failures consume reserved AI usage. |
| `AI_USAGE_PROMPT_TOKEN_COST_PER_1K` | `0.000000` | Local cost-estimate assumption for prompt tokens. |
| `AI_USAGE_COMPLETION_TOKEN_COST_PER_1K` | `0.000000` | Local cost-estimate assumption for completion tokens. |
| `PAYMENT_ENABLED` | `false` | Payment integration guard. |
| `PAYMENT_PROVIDER` | `mock` | Payment provider selection. |
| `PAYMENT_API_KEY` | `dev-only-payment-api-key` | Payment provider calls. |
| `PAYMENT_WEBHOOK_SECRET` | `dev-only-payment-webhook-secret` | Payment webhook validation. |
| `GOOGLE_CLIENT_ID` | `dev-only-google-client-id` | Google Calendar/Meet OAuth client id. |
| `GOOGLE_CLIENT_SECRET` | `dev-only-google-client-secret` | Google Calendar/Meet OAuth client secret. |
| `GOOGLE_PLATFORM_CALENDAR_ID` | `dev-only-google-calendar-id` | Platform-owned Google Calendar id. |

Keep local secrets out of committed files. Prefer shell exports, IDE run configuration, or a local-only env file ignored by Git.

Validation allows the dev-only OpenAI, payment, and Google secrets while their integrations are disabled. It fails startup if URLs are malformed, JWT durations are not positive, `JWT_SECRET` is shorter than 32 characters, `OPENAI_ENABLED=true` still uses a dev-only key, `TRANSCRIPTION_PROVIDER` is not `mock` or `openai`, OpenAI transcription is selected while OpenAI is disabled, AI usage period or hard-limit defaults are invalid, or `PAYMENT_ENABLED=true` still uses dev-only/mock payment settings.

## Provider Mocks And Sandbox Checks

Local development and automated tests should use provider mocks by default:

| Integration | Local/test mode | Manual sandbox gate |
| --- | --- | --- |
| Writing AI feedback | `AI_WRITING_PROVIDER=mock`, `OPENAI_ENABLED=false` | Switch to `openai` only with a real `OPENAI_API_KEY` and low AI usage limits. |
| Speaking AI feedback | `AI_SPEAKING_PROVIDER=mock`, `OPENAI_ENABLED=false` | Requires a completed transcript before calling OpenAI feedback. |
| Study-plan AI | `AI_STUDY_PLAN_PROVIDER=mock`, `OPENAI_ENABLED=false` | Verify no `study_plans` or tasks are saved when provider output is invalid or unavailable. |
| Speech-to-text | `TRANSCRIPTION_PROVIDER=mock`, `OPENAI_ENABLED=false` | Switch to `openai` only for short sandbox audio under `TRANSCRIPTION_MAX_AUDIO_DURATION`. |
| Payments | `PAYMENT_PROVIDER=mock`, `PAYMENT_ENABLED=false` | Use signed mock webhooks locally; real payment sandbox is gated until a non-mock provider client exists. |
| Files | Local MinIO bucket `mock-exams-local` | Verify bucket existence, signed URL access control, and storage-down failure behavior before release. |
| Google Calendar/Meet | Mock event/Meet link creation for live speaking bookings | Local mode creates deterministic `https://meet.google.com/mock-{bookingId}` links and `google_calendar_sync_jobs` audit rows without network calls. |

Provider failures should leave an auditable failed row and preserve domain state: AI failures stay in
`ai_feedbacks`, transcription failures keep the uploaded speaking audio and submission, payment
signature/mismatch failures do not grant marketplace access, and file-storage failures do not save
metadata. Before enabling any real provider in a shared environment, run the relevant automated tests
and the manual checklist in `docs/testing-provider-readiness.md`.

Admin-managed platform settings live under `/api/v1/admin/settings` and are persisted in
`platform_settings`. The backend validates each supported key as typed JSON and exposes typed
accessors to domain services, so wallet commission, AI usage limits, withdrawal minimums, rich-text
limits, Google Calendar/Meet mode, and live-speaking defaults can change without code edits. Settings
may store non-secret modes, ids, limits, prices, and percentages only; OAuth client secrets, payment
API keys, webhook secrets, and OpenAI keys stay in environment variables. Local Google Calendar/Meet
work should keep `google.calendar_meet.enabled=false` unless `GOOGLE_CLIENT_ID`,
`GOOGLE_CLIENT_SECRET`, and the needed calendar id are real non-dev values.

Payment checkout is local-safe with `PAYMENT_PROVIDER=mock` and `PAYMENT_ENABLED=false`. Student
listing checkout uses `POST /api/v1/student/payments` with `targetType=marketplace_listing`; live
speaking checkout uses `targetType=live_speaking_booking` with a reserved booking id or
`POST /api/v1/student/schedule/bookings/{bookingId}/payment`. The mock provider returns a
deterministic local-style checkout URL and never performs network calls. Mock
provider callbacks post to `POST /api/v1/provider/payments/webhook` and must include
`X-MockExams-Payment-Signature`, an HMAC-SHA256 hex signature of the raw JSON body using
`PAYMENT_WEBHOOK_SECRET`. Paid callbacks grant marketplace access through `AccessControlService`
exactly once, then create one idempotent teacher wallet earning transaction for paid marketplace
purchases. The earning uses `platform_settings.marketplace.default_commission_percent`, which is
seeded at 25% and defaults to 25% if absent. Amount/currency mismatches are flagged as failed
payments and do not grant access.
Selecting a non-mock provider while `PAYMENT_ENABLED=false` returns `PAYMENT_PROVIDER_DISABLED`.

Live speaking paid callbacks confirm the booking while its hold is active, mark the slot booked,
create one mock Google Calendar/Meet sync audit row, and record one teacher earning transaction using
`live_speaking.platform_commission_percent`. Expired holds are marked `expired` and released without
creating a normal booking earning.

Promo codes discount listing checkout. Teachers manage their own codes under
`/api/v1/teacher/promo-codes`; admins manage platform-wide or teacher-scoped codes under
`/api/v1/admin/promo-codes`. Students preview a code with
`GET /api/v1/student/payments/promo-quote` and apply it by passing `promoCode` to
`POST /api/v1/student/payments`. Validation and the usage-limit check run before the provider
checkout is created; the usage increment is an atomic conditional update, and one redemption row is
recorded per payment. Fixed discounts never push the payable amount below zero — a fully discounted
checkout becomes a zero-amount payment that is marked `paid` immediately without a provider call,
grants access once, and records no teacher earning. Purchases snapshot the amount actually paid, so
teacher wallet earnings for discounted payments use the discounted gross.

Teacher withdrawals reserve available wallet balance on request, release it on rejection or teacher
cancellation, and settle it into one idempotent `withdrawal` ledger transaction on admin approval.
The minimum withdrawal amount comes from the `wallet.minimum_withdrawal_amount` platform setting,
seeded at `10000.00` (UZS-denominated for the MVP) and defaulting to `10000.00` when the setting row
is absent. Payout details are backend-validated JSON (`card` or `bank_transfer`) and must never be
written to logs or notification payloads. The MVP does not automate transfers: admins approve, pay
outside the platform, then mark the request paid.

Payment disputes are student-opened support records against their own `paid`/`failed` payments,
reviewed by admins under `/api/v1/admin/payment-disputes`. A dispute never mutates the underlying
payment's own status, marketplace access, or the teacher wallet ledger: `refunded` is a support-facing
status marker only, and `TeacherWalletService.recordMarketplacePurchaseRefund` is deliberately not
wired to this flow pending a future refund plan. At most one non-terminal dispute (`open` or
`under_review`) may exist per payment at a time; `refunded`, `rejected`, and `resolved` are terminal
and cannot be decided again. The student who opened the dispute is notified on every status change,
including the initial `open`.

Admin reports live under `/api/v1/admin/reports/*` and are read-only query services over persisted
domain records. The AI usage report reads `ai_feedbacks`; sales reads `payments` and
`payment_disputes`; teacher/listing revenue reads `teacher_transactions` through purchases/listings;
test activity reads `test_attempts` and `tests`; provider failures combine failed payment and failed
AI feedback rows. Date filters are inclusive UTC date-time bounds, unknown filters or sort parameters
return `VALIDATION_FAILED`, and reports never call AI or payment providers for pricing or health.
Live speaking schedule support lives under `/api/v1/*/schedule/*`. Admin reports may include
persisted `payments.targetType=live_speaking_booking` rows; booking/provider diagnostics are exposed
through `/api/v1/admin/schedule/bookings` and `/api/v1/admin/schedule/provider-failures`.

AI request flows must call `AiUsageService.guardAndReserve(...)` before any provider call and then call the matching accounting method after success or failure. The guard creates per-user monthly `ai_usage_limits` rows from typed `app.ai.usage.*` defaults, reserves capacity transactionally, and returns `AI_USAGE_LIMIT_EXCEEDED` without consuming usage when the hard limit is reached. Writing feedback uses `AI_WRITING_PROVIDER=mock` by default for local and test runs; speaking feedback uses `AI_SPEAKING_PROVIDER=mock`; study plans use `AI_STUDY_PLAN_PROVIDER=mock`. Set the relevant provider to `openai`, `OPENAI_ENABLED=true`, and real OpenAI credentials to call OpenAI. Other disabled-provider attempts are recorded in `ai_feedbacks` with `failure_code=AI_PROVIDER_DISABLED` and return `SERVICE_UNAVAILABLE` before any network call. Provider failures are audited and do not consume the reserved request by default unless `AI_USAGE_COUNT_PROVIDER_FAILURES=true`.

Writing feedback stores each student prompt/answer in `writing_submissions`, then stores the feedback job/result in `ai_feedbacks` with `feedback_type=writing`. Completed feedback includes IELTS-style overall band, criterion scores, mistakes, improved answer, action plan, provider/model metadata, token and cost estimates, rubric version, and prompt audit metadata. Provider JSON that fails backend schema validation is marked `failed` with validation diagnostics and no student-visible band. Retrying a failed writing feedback creates a linked retry row via `retry_of_feedback_id`; completed or in-progress feedback for the same submission is returned idempotently.

Speaking feedback evaluates completed recorded practice speaking submissions after transcription. `POST /api/v1/student/speaking-submissions/{submissionId}/feedback` starts or returns the feedback job, `GET .../feedback` reads status/result, and `POST .../feedback/retry` retries a failed job. The backend refuses evaluation until `speaking_submissions.transcription_status=completed` and a nonblank transcript exists. Feedback is stored in `ai_feedbacks` with `feedback_type=speaking`, criteria in `criteria_scores`, repeated mistakes in `mistakes`, the better answer example in `improved_answer`, action steps in `action_plan`, and provider/model/token/cost/rubric/prompt audit metadata in the shared AI feedback fields. Because the current MVP evaluates transcript text only, returned speaking feedback labels pronunciation as a transcript-only proxy and does not claim acoustic pronunciation scoring.

AI study plans are generated through `POST /api/v1/student/study-plans` and persisted in `study_plans` plus structured `study_plan_tasks`. The service assembles recent Reading/Listening results, weak question types, Writing/Speaking AI feedback, missing skills, target band, available time, and accessible free or purchased content before calling the provider. Provider output must validate to the backend schema for summary and daily tasks; invalid JSON or inaccessible recommended content is recorded in `ai_feedbacks` with `feedback_type=study_plan` and no tasks are saved. Regeneration through `POST /api/v1/student/study-plans/{planId}/regenerate` validates the new provider output first, then replaces only incomplete future tasks while preserving completed task history.

## MinIO Bucket

Local file flows expect a bucket named `mock-exams-local`. The default `docker compose up -d` command starts the MinIO service and runs the `minio-init` job that creates this bucket automatically.

If the bucket is missing, create it from the MinIO console at `http://localhost:9001`, or with a local `mc` client using the same credentials configured for Compose:

```sh
mc alias set mock-exams-local http://localhost:9000 <access-key> <secret-key>
mc mb --ignore-existing mock-exams-local/mock-exams-local
```

Uploaded test assets, teacher avatars, speaking audio, and writing attachments should use this bucket in local development.

The backend uses the MinIO Java SDK (`io.minio:minio`) with an explicit OkHttp JVM artifact because
the current SDK release publishes OkHttp 5 as split JVM metadata. File uploads are available at:

| Endpoint | Local use |
| --- | --- |
| `POST /api/v1/teacher/files` | Teacher listening audio, avatars, passage images, diagrams, documents, and course materials. |
| `POST /api/v1/student/files` | Student speaking audio. |
| `POST /api/v1/admin/files` | Admin banner images. |
| `GET /api/v1/files/{fileId}/download-url` | Authenticated short-lived signed URL access. |

Uploads are multipart requests with a `purpose` field and a `file` part. The servlet multipart
parser defaults to `101MB` per file and `105MB` per request so requests reach the service layer,
where the backend validates purpose-specific content types and sizes before calling MinIO. The
`MULTIPART_MAX_FILE_SIZE` and `MULTIPART_MAX_REQUEST_SIZE` environment variables can raise or lower
that parser ceiling per deployment. Metadata is written to the `files` table only after object
storage succeeds. Speaking audio is private to the owner. Course materials attached to marketplace
listing items are gated by `AccessControlService`, so paid listing files require a purchase or active
subscription before signed URL creation. Signed download URLs currently expire after 900 seconds.

## Marketplace Catalog

Marketplace listings are separate from raw `tests`. Teachers manage sellable listings under
`/api/v1/teacher/marketplace/listings`, while students and anonymous users browse active public
listings through `/api/v1/public/marketplace`. A listing can reference one owned published test
directly for `single_test` and `full_mock_exam`, or contain ordered `marketplace_listing_items` for
packs, courses, vocabulary content, and subscription plans.

Teacher listing commands require the `teacher` role and an approved teacher profile. The service
validates test/file ownership before saving references, rejects another teacher's content with
`LISTING_ITEM_INVALID`, and activates listings only after readiness checks pass. Public discovery
returns active listings from approved teachers only, and filters out inactive listings, unpublished or
deleted tests, and invalid item references.

Purchase/access control lives behind `AccessControlService`. Students can idempotently claim active
free listings through `/api/v1/student/marketplace/listings/{listingId}/free-access`; paid grants are
created through the internal grant method used by payment confirmation. Student purchase
history is available under `/api/v1/student/purchases`. Starting paid tests, reading protected listing
items, and downloading listed course materials all call the same access boundary. Access requires the
listing to remain active; inactive listings and revoked grants deny already-purchased content.
Subscriptions grant access only inside their active UTC window. Live speaking availability may be
promoted in marketplace discovery later, but booking state and price snapshots stay in the schedule
domain rather than `marketplace_listings`.

## Speaking Transcription

Speaking transcription is local-safe by default. Students upload recordings through
`POST /api/v1/student/files` with purpose `speaking_audio`, then submit a prompt and `audioFileId` to
`POST /api/v1/student/speaking-submissions`. The backend reads the stored object through the file
storage boundary, validates ownership, purpose, and configured duration limit, and stores transcript
status on `speaking_submissions`.

The default `TRANSCRIPTION_PROVIDER=mock` returns deterministic local transcripts without network
access. To use OpenAI speech-to-text, set `OPENAI_ENABLED=true`, provide a real `OPENAI_API_KEY`, and
set `TRANSCRIPTION_PROVIDER=openai`. The OpenAI adapter posts multipart audio to
`/audio/transcriptions` using `TRANSCRIPTION_MODEL`; local tests should continue to use the mock
provider. Transcription provider/model and optional estimated minute cost are stored on the speaking
submission and are separate from `ai_usage_limits`, which still governs later AI feedback requests.

## Seed Data

Flyway currently creates the core schema and role rows. When auth is implemented, local seed data should add deterministic development accounts for each primary role:

| Role | Email | Purpose |
| --- | --- | --- |
| Admin | `admin@mock-exams.local` | Moderation, settings, reports, and support workflows. |
| Teacher | `teacher@mock-exams.local` | Teacher profile, test builder, marketplace publishing, and earnings workflows. |
| Student | `student@mock-exams.local` | Student dashboard, attempts, results, purchases, and AI feedback workflows. |

Use a shared development-only password and a password hash generated by the same encoder as production auth. Keep the seed idempotent so resetting and re-running migrations produces the same usable accounts.

## Packaging Rules

Backend Java packages are class-type first. Controllers belong under `controller/`, services under `service/`, service implementations under `service/impl/`, repositories under `repository/`, entities under `entity/`, DTOs under `dto/`, and mappers under `mapper/`.

When any package becomes too large to scan comfortably, split by purpose inside that package, such as `controller/auth/`, `controller/admin/`, `dto/tests/`, `service/payments/`, or `mapper/attempts/`. Do not create new feature-first package trees like `auth/controller/` for new work. Move existing mixed package shapes only through an explicit repackaging plan so imports, tests, and API behavior can be verified together.

The current package inventory and remaining legacy feature-first migration map live in
[`docs/package-inventory.md`](package-inventory.md). New feature work should follow that target
shape immediately, even when nearby legacy code has not moved yet.

## Reset Local Data

Reset all Compose-managed local state:

```sh
docker compose down -v --remove-orphans
docker compose up -d
```

Then restart the backend so Flyway can rebuild the database schema.

## Verification

Run all backend tests:

```sh
cd backend
./mvnw test
```

Run a focused test:

```sh
cd backend
./mvnw -Dtest=MockExamControllerTest test
```
