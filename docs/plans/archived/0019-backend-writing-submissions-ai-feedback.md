# Backend Writing Submissions AI Feedback

> Evaluate Writing Task 1 and Task 2 submissions with AI feedback.

## Depends On

- `0018-backend-ai-usage-limits-costs.md`

## Context

Writing feedback is one of the highest-cost and highest-trust AI surfaces. It must produce useful IELTS-aligned feedback while preserving provider auditability, cost controls, retry safety, and clear failure states. The implementation should avoid coupling the domain directly to one model response shape.

## Scope

### In scope

- Submit writing prompt and answer.
- Store word count.
- Call AI provider for IELTS-style feedback.
- Persist overall band, criteria scores, mistakes, improved version, and action plan.
- Return feedback status and result.
- Support async job state when provider latency exceeds normal request/response expectations.
- Preserve the prompt, rubric version, model/provider metadata, usage counts, and schema-validation errors needed for audit and admin reports.

### Out of scope

- Human teacher marking.
- Plagiarism detection.
- Claiming official IELTS certification.

## Key Decisions

- Use a provider boundary with a mock implementation for local/test runs and an OpenAI implementation behind `OPENAI_ENABLED`.
- Ask the model for strict structured JSON and validate it against a backend-owned schema before persisting `completed`.
- Separate public feedback fields from internal prompt/provider diagnostics; students should see feedback and retry state, not raw provider errors or hidden rubrics.
- Idempotency should be based on submission id plus feedback type so retries do not create duplicate completed feedback for the same writing submission.
- Cost and token accounting should be recorded even when feedback validation fails, while usage-limit counters should follow the explicit policy from `0018`.

## What To Implement

- `WritingSubmission` service and controller.
- AI prompt builder with structured JSON response contract.
- Feedback parser and validation.
- Retry-safe persistence.
- `AiFeedbackJobService` or equivalent orchestration that owns `queued`, `processing`, `completed`, and `failed` transitions.
- Rubric templates for Task 1 and Task 2 with criterion-specific scoring constraints, mistake categorization, improved answer requirements, and action-plan shape.
- Poll/read endpoints that let the frontend refresh status without resubmitting the same answer.
- Admin-safe diagnostics hooks for `0035-backend-reports-ai-usage.md`, including provider, model, token counts, cost estimate, status, and failure category.

## Tests

- Empty answer is rejected.
- Usage limit blocks request before provider call.
- Mock AI response stores all criteria fields.
- Provider failure leaves feedback as failed with error message.
- Malformed AI JSON leaves the feedback failed with an internal validation reason and no partial student-visible band.
- Retrying a failed job updates the existing feedback lifecycle or creates a clearly linked retry according to the chosen contract.
- Task 1 and Task 2 prompts use the correct rubric and reject unsupported task types.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Document AI response schema.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if prompt audit, retry, or rubric-version fields are added.
- Update `docs/backend-dev.md` with local mock/OpenAI provider configuration.

## Acceptance Criteria

- Student can submit Writing Task 1 or 2 and receive stored actionable feedback.
- AI feedback is traceable, cost-auditable, schema-validated, retry-safe, and blocked by usage limits before provider calls.
