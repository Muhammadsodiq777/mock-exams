# Frontend Builder Preview Submit

> Let teachers preview and submit tests for moderation.

## Depends On

- `0031-backend-test-moderation.md`
- `0051-frontend-reading-builder.md`
- `0052-frontend-listening-builder.md`

## Context

Preview is a creation-time safety feature, not only a final submit screen. Teachers need to see what students will see while building, and admins need the same rendering fidelity while moderating submitted content.

## Scope

### In scope

- Real-time preview mode for Reading, Listening, Writing, and Speaking test content as each builder surface lands.
- Validation summary before submit.
- Submit for moderation.
- Submitted read-only state.
- Rejected edit-and-resubmit state.
- Teacher preview modes for student view, answer-key view, and validation view where permitted.
- Admin moderation preview integration that reuses the same renderer but includes answer keys and moderation metadata.

### Out of scope

- Admin moderation UI.

## Key Decisions

- Preview rendering should be shared across teacher builder, admin moderation, student player, and result review where possible, with mode/role flags controlling answer-key visibility.
- Server-backed preview is authoritative for saved content and validation; local draft overlay can improve responsiveness but must clearly show unsaved state.
- Submit flow depends on backend readiness validation and status transitions; frontend should not duplicate all business rules as final authority.
- Student preview mode must never expose answer keys, accepted answers, internal moderation notes, or teacher-only diagnostics.
- Rejected tests reopen in editable mode with rejection reason visible.
- Submitted tests become read-only until admin moderation changes status.

## What To Implement

- Preview route or panel.
- Submit mutation.
- Validation summary component.
- Status-driven builder state.
- Live preview panel fed by saved server preview plus local draft overlay when the editor has unsaved changes.
- Preview renderer shared by teacher builder, admin moderation, and student player with role/mode flags rather than divergent layouts.
- Submit-readiness checklist for required metadata, sections, rich-text content, audio, questions, answer keys, price/visibility, and moderation notes.

## Tests

- Submit disabled when backend readiness fails.
- Submitted test becomes read-only.
- Rejected test displays reason and edit action.
- Real-time preview updates after rich-text edits and section/question reorder.
- Student preview mode never shows answer keys; admin/teacher answer-key mode does.
- Preview remains usable when backend validation returns field errors.

## Documentation Sync

- Keep moderation statuses aligned.
- Keep preview DTOs aligned in `docs/API_CONTRACT.md` and `frontend/src/types/backend.ts`.

## Acceptance Criteria

- Teacher can confidently preview and submit content for admin approval.
- Admin and teacher see the same high-fidelity rendered test before publication decisions.
