# Backend Speaking Audio Transcription

> Store speaking recordings and produce transcripts.

## Depends On

- `0008-backend-file-storage-minio.md`
- `0018-backend-ai-usage-limits-costs.md`

## Context

Speaking AI feedback depends on a reliable transcript, but live teacher speaking tests introduced later in the backlog are a separate human-led flow. This plan remains responsible for student-uploaded/recorded AI practice audio and its transcript pipeline.

## Scope

### In scope

- Submit speaking prompt and audio file.
- Validate file purpose and duration.
- Call speech-to-text provider.
- Store transcript on speaking submission.
- Track transcription status, provider metadata, duration, and failure category for user recovery and admin diagnostics.

### Out of scope

- Pronunciation scoring from raw audio.
- Live recording transport.
- Live teacher session scheduling, Google Meet, and teacher grading.

## Key Decisions

- Store the original audio through the existing file service before transcription starts; never send untracked binary blobs directly to a provider.
- Use a `TranscriptionProviderClient` boundary with a mock implementation for tests/local development and a real provider implementation behind an enabled flag.
- Keep speaking submission ownership strict: only the student who owns the audio/submission can start or view transcription status.
- Prefer async-compatible status fields even if the first implementation executes synchronously, because audio provider latency can exceed normal request time.
- Transcription failure must preserve the uploaded audio and submission so the student can retry or support can inspect the issue.
- Usage-limit accounting must be consistent with `0018`; if transcription cost is tracked separately from feedback, document that dimension in reports.

## What To Implement

- Speaking submission service.
- Transcription client boundary.
- Provider mock for tests.
- Status handling for queued, processing, completed, and failed flows if async is chosen.
- Idempotent transcription job creation keyed by speaking submission.
- File ownership and purpose checks that prevent a student from transcribing another user's audio.
- Poll/read endpoint for transcription status and transcript review before AI feedback starts when the flow is split.

## Tests

- Non-speaking audio file is rejected.
- Missing prompt is rejected.
- Mock transcript is saved.
- Provider failure is visible to student without losing uploaded audio.
- Duplicate submit does not create duplicate active transcription jobs.
- Audio longer than the configured maximum duration is rejected before provider call.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update local dev docs with provider configuration.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if transcription-specific metadata is added.
- Mirror transcription/speaking submission DTOs in `frontend/src/types/backend.ts`.

## Acceptance Criteria

- Student audio becomes a speaking submission with a transcript or a clear failed status.
- Transcription is auditable, retry-safe, and recoverable without losing the original uploaded audio.
