# Frontend Listening Builder

> Build Listening audio, segment, and question authoring UI.

## Depends On

- `0013-backend-listening-test-builder.md`
- `0014-backend-question-types-answer-keys.md`
- `0050-frontend-test-builder-shell.md`

## Context

Listening authoring requires safe audio upload, segment timing, optional transcript, question linkage, and answer-key editing. It should reuse the builder shell and shared question editor while adding audio-specific validation and preview behavior.

## Scope

### In scope

- Audio upload.
- Segment editor.
- Transcript field.
- Question list.
- Link questions to segments.
- Answer keys and explanations.

### Out of scope

- Waveform visualization.
- Speech recognition for teacher-uploaded listening materials.

## Key Decisions

- Audio upload uses the teacher file service with `listening_audio` purpose and backend validation.
- Segment start/end times are stored as seconds and must be validated on both client and backend.
- Questions can optionally link to a segment, but segment linkage must be constrained to the current section.
- Transcript is teacher-authored helper content and should not be required unless backend readiness rules require it.
- Use shared question/answer editor components from Reading where possible.

## What To Implement

- Audio upload component using file service.
- Segment table/form.
- Listening section editor.
- Backend validation display.
- Audio metadata panel with upload progress, selected file, duration when available, signed preview URL, and error/retry state.
- Segment create/update/reorder/delete controls with time inputs and validation.
- Question editor with segment selector and answer-key/explanation fields.
- Listening preview handoff to shared preview renderer.
- Save/query invalidation behavior consistent with builder shell.

## Tests

- Invalid audio upload shows error.
- Segment end before start is blocked or shows backend validation.
- Question cannot link to missing segment.
- Segment selector only offers current-section segments.
- Audio upload failure does not lose unsaved segment/question edits.
- Preview shows audio metadata and questions in order.

## Documentation Sync

- Keep file upload and segment DTOs aligned.
- Mirror listening builder DTOs in frontend types.
- Keep shared question editor behavior aligned with Reading builder.

## Acceptance Criteria

- Teacher can author a complete Listening part with audio and questions.
- Teacher can validate audio, segments, questions, and preview before submitting.
