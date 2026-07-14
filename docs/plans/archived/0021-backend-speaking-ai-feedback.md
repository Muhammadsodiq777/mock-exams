# Backend Speaking AI Feedback

> Evaluate transcribed speaking submissions with IELTS-style AI feedback.

## Depends On

- `0020-backend-speaking-audio-transcription.md`

## Context

AI speaking evaluation should complement, not replace, the teacher-led live speaking schedule added later. The AI path evaluates recorded practice submissions and must clearly document pronunciation limitations when it relies only on transcripts.

## Scope

### In scope

- Evaluate Speaking Part 1, 2, and 3 transcripts.
- Store fluency, pronunciation proxy, vocabulary, grammar, coherence, repeated mistakes, and better answer example.
- Link feedback to speaking submission.
- Preserve rubric version, provider/model metadata, token/cost usage, and validation errors for reporting.

### Out of scope

- Acoustic pronunciation scoring unless provider supports it clearly.
- Google Meet live-session scoring; that belongs to the live speaking schedule plan.

## Key Decisions

- Use the same AI job/provider orchestration as writing feedback so usage limits, retries, cost tracking, and failure states are consistent.
- If only transcript text is available, label pronunciation as a proxy or omit numeric pronunciation scoring; do not overstate acoustic analysis.
- Accept optional audio-derived features later through a provider extension, but keep the MVP response schema stable for transcript-only feedback.
- Store feedback as structured criteria scores plus narrative sections, not one opaque prose blob.

## What To Implement

- Speaking feedback prompt builder.
- Criteria score schema in `ai_feedbacks.criteria_scores`.
- Endpoint to retrieve speaking feedback.
- Guard against evaluating missing transcript.
- Rubric templates for Speaking Parts 1, 2, and 3 with part-specific expectations and answer-length guidance.
- Status polling/read endpoints aligned with writing feedback.
- Admin diagnostics fields for cost, model, provider status, and schema validation.

## Tests

- Missing transcript cannot be evaluated.
- Mock AI response stores all expected criteria.
- Usage limits apply.
- Feedback is linked to the correct student and submission.
- Transcript-only feedback documents pronunciation limitations in the returned feedback metadata or copy contract.
- Malformed AI response fails safely without storing partial scores.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Document pronunciation limitation if based only on transcript.
- Update `docs/backend-dev.md` with speaking AI mock/provider configuration.

## Acceptance Criteria

- Student receives speaking band estimate and actionable feedback after transcription.
- Feedback is structured, auditable, retry-safe, and honest about whether pronunciation was actually measured from audio.
