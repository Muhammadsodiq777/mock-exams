# Provider Testing Readiness

This checklist covers backend-facing external providers for AI feedback, speech-to-text, payments,
file storage, and live-speaking calendar links. Normal local and CI backend tests must run with mock
or disabled providers and must not make real OpenAI, payment, Google, or object-store calls unless a
developer intentionally runs a manual sandbox check.

## Automated Coverage Map

| Area | Coverage | Current tests |
| --- | --- | --- |
| Provider environment validation | Safe local defaults, invalid OpenAI/payment combinations, malformed URLs, JWT and limit validation. | `BackendEnvironmentPropertiesTest`, `AiUsageDisabledProviderServiceTest` |
| AI usage guard | Reservation before provider calls, hard-limit rejection, provider-disabled audit rows, provider-failure release behavior. | `AiUsageServiceTest`, `WritingSubmissionServiceTest`, `SpeakingFeedbackServiceTest`, `StudyPlanServiceTest` |
| Writing AI feedback | Mock success, usage-limit blocking before provider call, malformed provider JSON, provider exception, retry linkage, rubric selection. | `WritingSubmissionServiceTest` |
| Speaking transcription | Speaking-audio purpose checks, max-duration checks, mock transcript persistence, provider failure visibility, duplicate submit idempotency. | `SpeakingSubmissionControllerTest` |
| Speaking AI feedback | Completed-transcript gate, mock success, usage-limit blocking, malformed provider JSON, provider exception, transcript-only pronunciation marker. | `SpeakingFeedbackServiceTest` |
| Study-plan AI | Mock success, partial-data labeling, inaccessible recommendation rejection, unsupported provider skill validation, provider exception, regeneration preservation. | `StudyPlanServiceTest` |
| Payments | Mock checkout, HMAC webhook validation, duplicate paid callback idempotency, failed/cancelled/refunded status handling, amount/currency mismatch, already-owned checks. | `PaymentControllerTest`, `PromoRedemptionControllerTest`, `MockPaymentProviderClientTest` |
| File storage | Purpose/content-size validation before storage, no metadata save when storage fails, signed download URL access control and expiry. | `FileMetadataServiceTest`, `FileControllerTest`, `MultipartUploadPropertiesTest` |
| Provider failure reporting | Failed AI feedback and payment rows appear in admin provider-health reports without calling providers for live pricing or health. | `AdminReportControllerTest` |
| Google Calendar/Meet | Live speaking paid booking confirmation creates a deterministic mock Meet URL and one provider sync audit row without network calls. | `SpeakingScheduleControllerTest` |

Focused backend provider checks can be run from `backend/`:

```sh
./mvnw -Dtest=WritingSubmissionServiceTest,SpeakingSubmissionControllerTest,SpeakingFeedbackServiceTest,StudyPlanServiceTest,AiUsageDisabledProviderServiceTest test
./mvnw -Dtest=PaymentControllerTest,PromoRedemptionControllerTest,MockPaymentProviderClientTest test
./mvnw -Dtest=FileMetadataServiceTest,FileControllerTest,BackendEnvironmentPropertiesTest test
```

## Manual Sandbox Checklist

Record each manual check as pass, fail, or exception. Exceptions must name the risk, owner, and
follow-up plan.

| Provider | Checklist |
| --- | --- |
| OpenAI writing/speaking/study-plan | Set `OPENAI_ENABLED=true`, use real non-dev `OPENAI_API_KEY`, switch only the target provider to `openai`, keep low `AI_USAGE_*` limits, submit one happy-path writing request, one speaking feedback request after transcription, and one study-plan request. Confirm `ai_feedbacks` stores provider/model/tokens/cost and that failures remain visible without corrupting submissions or plans. |
| OpenAI transcription | Set `TRANSCRIPTION_PROVIDER=openai`, upload one short `speaking_audio` file under `TRANSCRIPTION_MAX_AUDIO_DURATION`, confirm transcript metadata is stored, then verify too-long or wrong-purpose audio is rejected before the provider call. |
| Payment mock | With `PAYMENT_PROVIDER=mock` and `PAYMENT_ENABLED=false`, create a marketplace payment, post a signed `paid` webhook, repeat the same webhook, and confirm one purchase plus one teacher earning transaction. Post invalid-signature and amount/currency mismatch webhooks and confirm no access grant. |
| Real payment sandbox | Gated until a non-mock `PaymentProviderClient` is implemented. The current mock client returns `SERVICE_UNAVAILABLE` for configured real providers and `PAYMENT_PROVIDER_DISABLED` when a non-mock provider is selected while payments are disabled. |
| MinIO | Start Compose, confirm the `mock-exams-local` bucket exists, upload teacher listening audio, student speaking audio, and admin banner image, create signed URLs as owner/admin, verify another student cannot read private speaking audio, and briefly stop MinIO to confirm storage failures do not create file metadata. |
| Google Calendar/Meet | With mock mode, reserve and pay a live speaking booking, repeat the paid webhook, and confirm one `google_calendar_sync_jobs` row plus a stable mock Meet URL. Before enabling real Google, verify create, update, cancel, retry, and duplicate webhook/idempotency paths with fixed UTC fixtures and a sandbox calendar. |

## Release Gates

- Local and CI backend tests must pass with provider mocks or disabled providers.
- Manual sandbox checks are required only for providers enabled in the target environment.
- Real provider secrets must be supplied through environment variables or deployment secret storage,
  never committed files.
- Real Google Calendar/Meet and real payment sandbox launch checks remain explicit gated gaps until
  their non-mock adapters exist.
