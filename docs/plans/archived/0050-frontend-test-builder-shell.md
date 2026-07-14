# Frontend Test Builder Shell

> Build the shared teacher test-builder workflow.

## Depends On

- `0011-backend-test-builder-core.md`
- `0049-frontend-teacher-test-list.md`

## Context

The builder shell must host both metadata editing and content editors that can render rich passages/questions and preview the actual student experience while the teacher or admin is still authoring.

## Scope

### In scope

- `/teacher/tests/create`.
- `/teacher/tests/:id/edit`.
- Metadata form.
- Section navigation.
- Autosave or explicit save pattern.
- Builder validation summary.
- Shared rich-text editor integration contract for test descriptions, section instructions, passages, questions, and explanations.
- Live preview frame/panel that updates from draft state and server validation state.

### Out of scope

- Reading-specific editor.
- Listening-specific editor.

## Key Decisions

- The builder shell owns metadata, section navigation, save state, validation summary, and preview placement; skill-specific editors plug into it.
- Use typed services/hooks from `services` and `hooks`, with DTO mirrors in `types/backend.ts`.
- Rich-text editing should serialize to the backend contract and preview through the same renderer used by student/admin views.
- Server state remains authoritative for test status and saved content; local draft state is only for unsaved edits and live preview.
- Submitted, published, and archived tests are read-only in the builder unless a future versioning plan changes lifecycle rules.
- Validation errors from backend should be mappable to stable form/block paths.

## What To Implement

- Builder page shell.
- Metadata form.
- Section list and reorder controls.
- Save state indicators.
- Rich-text editor wrapper component with toolbar, keyboard support, serialization, dirty-state tracking, and safe render preview.
- Preview data adapter that can render unsaved local draft state when safe and server-backed preview after save.
- Validation sidebar that maps backend field errors to metadata, sections, rich-text blocks, and questions.
- Test type aware section slots for Reading, Listening, Writing, Speaking, and Full Mock as backend support lands.
- Builder query/mutation hooks for create, update metadata, add/update/reorder/delete sections, and preview.
- Save strategy decision documented in UI behavior: explicit save or autosave with conflict/retry handling.
- Read-only mode for submitted/published/archived tests and rejected edit mode with moderation note.

## Tests

- Required metadata errors display.
- Section reorder updates UI.
- Submitted test opens read-only preview state.
- Rich-text dirty state blocks accidental navigation when unsaved.
- Live preview updates after metadata/content edits and does not expose answer keys in student mode.
- Read-only status prevents edits while still allowing preview.
- Backend validation errors focus or highlight the matching field/block.
- Unsaved changes are preserved or warned on route changes.

## Documentation Sync

- Mirror builder DTOs in frontend types.
- Document the chosen rich-text editor library and serialization contract in `docs/frontend-dev.md`.

## Acceptance Criteria

- Teacher can create a draft and navigate builder sections.
- Teacher can edit rich content and preview the student-facing test without leaving the builder.
- The shell can host specialized builders without each rebuilding metadata, section nav, save state, and preview plumbing.
