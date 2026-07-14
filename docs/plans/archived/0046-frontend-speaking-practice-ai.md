# Frontend Speaking Practice AI

> Build speaking recording upload and AI feedback UI.

## Depends On

- `0020-backend-speaking-audio-transcription.md`
- `0021-backend-speaking-ai-feedback.md`

## Context

This page is for AI speaking practice. Teacher-led live speaking bookings should appear as a nearby alternative path once `0067` lands, but they should not be mixed into the AI upload workflow.

## Scope

### In scope

- `/speaking` page.
- Speaking part selector.
- Prompt display.
- Record or upload audio.
- Submit for transcription and feedback.
- Transcript display.
- Feedback display.
- Clear distinction between AI practice and scheduled teacher speaking tests.

### Out of scope

- Live streaming transcription.
- Advanced audio waveform editing.
- In-app live teacher meeting UI.

## Key Decisions

- Use the existing file upload service for speaking audio before creating or evaluating a speaking submission.
- Keep recording/upload UI separate from transcription/feedback status polling so failures can be retried without losing the original file.
- Show transcript-only pronunciation limitations exactly as the backend provides them.
- Use feature flags/API availability to expose live teacher speaking entry points without mixing bookings into AI practice state.
- Browser recording support should degrade to file upload when permissions or APIs are unavailable.

## What To Implement

- Audio recorder/upload component.
- Speaking service and hooks.
- Transcription/feedback status polling if async.
- File validation messaging.
- Recovery controls for failed transcription or failed feedback.
- Result components that show pronunciation limitations when feedback is transcript-only.
- Entry point to live teacher speaking schedules after `0067` is implemented, without blocking the AI practice MVP.
- Permission/error states for microphone access denied, unsupported browser recording, upload failure, transcription failure, feedback failure, and usage limit exceeded.
- Status polling hooks for transcription and feedback jobs.
- Transcript review block and criteria cards for fluency/coherence, vocabulary, grammar, and pronunciation/proxy feedback.
- Recent speaking practice history if backend exposes it.

## Tests

- Unsupported audio file is rejected before upload.
- Failed transcription shows recovery state.
- Completed feedback renders criteria and better answer example.
- Transcript-only feedback displays the backend limitation note.
- Link/CTA to live speaking schedules can be hidden until the schedule feature flag/API is available.
- Microphone denial falls back to upload instructions without crashing.
- Page refresh can recover current submission status from backend.
- Failed transcription can be retried without re-recording when backend allows it.

## Documentation Sync

- Mirror speaking DTOs in frontend types.
- Keep AI practice routes separate from live teacher schedule routes.
- Keep file upload purpose and validation messages aligned with `docs/API_CONTRACT.md`.

## Acceptance Criteria

- Student can practice speaking and view transcript plus AI feedback.
- Student understands when feedback is AI-generated and when live teacher speaking is available as a paid alternative.
- Recording/upload, transcription, feedback, retry, and limitation states are explicit and recoverable.
