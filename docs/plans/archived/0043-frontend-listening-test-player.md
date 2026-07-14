# Frontend Listening Test Player

> Build IELTS-style Listening attempt UI.

## Depends On

- `0013-backend-listening-test-builder.md`
- `0015-backend-test-attempt-lifecycle.md`

## Context

The Listening player combines timed answers with audio playback and segment metadata. It must handle signed audio URLs, missing/failed audio, save/submit lifecycle, and the same answer-state rules as Reading.

## Scope

### In scope

- Audio player.
- Section/part navigation.
- Segment metadata support.
- Answer sheet.
- Timer.
- Save and submit.
- Signed audio URL loading through backend file access.
- Refresh-safe answers and player state.

### Out of scope

- Waveform editing.
- Live proctoring.
- Speech-to-text for listening content.

## Key Decisions

- Reuse shared attempt hooks and answer components from the Reading player where possible.
- Audio source access must go through backend file download-url contracts; never assume public object storage URLs.
- Missing or expired signed URLs should be recoverable by refetching metadata/download URL.
- Segment metadata is navigational/contextual; it should not force waveform editing in the MVP.
- Backend remains source of truth for attempt status, timer, answers, and submission.

## What To Implement

- Listening variant of `/tests/:id`.
- Audio loading/error states.
- Keyboard-friendly answer inputs.
- Prevent accidental page loss if unsaved answers exist.
- Audio player with play/pause, duration/current time display, disabled state, and clear failure recovery.
- Section/part and segment navigation that can highlight the relevant questions.
- Answer sheet, timer, save/submit controls, and submitted/expired handling.
- Signed download URL refresh behavior when audio link expires.

## Tests

- Missing audio shows a usable error state.
- Answers save while audio is available.
- Submit routes to result page.
- Expired signed URL can be refreshed without losing answers.
- Unsaved answers warn before navigation.
- Missing segment metadata does not prevent answering if the section remains valid.

## Documentation Sync

- Keep audio file URL contract aligned with backend.
- Mirror listening segment and attempt DTOs in frontend types.
- Keep shared player behavior aligned with Reading.

## Acceptance Criteria

- Student can complete a Listening test with audio and answer navigation.
- Audio/file failures are recoverable and do not corrupt attempt answers.
