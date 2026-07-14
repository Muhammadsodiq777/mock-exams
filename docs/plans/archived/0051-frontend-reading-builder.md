# Frontend Reading Builder

> Build Reading passage, paragraph, and question authoring UI.

## Depends On

- `0012-backend-reading-test-builder.md`
- `0014-backend-question-types-answer-keys.md`
- `0050-frontend-test-builder-shell.md`

## Context

Reading authoring depends on informative passages. Teachers and admins need a real rich-text passage editor, paragraph labeling, and a preview that looks like the eventual student reading player.

## Scope

### In scope

- Rich-text passage editor.
- Paragraph labels.
- Question list.
- Question type forms.
- Answer keys and explanations.
- Related paragraph selection.
- Safe embeds for backend-owned passage images, diagrams, and documents where the backend permits them.

### Out of scope

- Rich collaborative editing.

## Key Decisions

- Reading-specific UI plugs into the shared builder shell and does not own metadata, submit, or global preview flow.
- Use the backend rich-text document/rendered HTML contract for passages, paragraphs, questions, options, and explanations.
- Paragraph labels are optional where backend allows them, but related-question selectors must only point to existing labels.
- Question editor components should be shared with Listening where the question type behavior is identical.
- Student-preview mode hides answer keys and teacher-only notes; answer-key mode is limited to teacher/admin contexts.
- File embeds must use backend-owned file ids and allowed purposes, not arbitrary external URLs.

## What To Implement

- Reading section editor components.
- Question editor components shared where possible.
- Validation messages from backend.
- Preview handoff.
- Rich-text toolbar/actions for headings, lists, tables, emphasis, links, and file embeds supported by the API contract.
- Passage outline/paragraph-label controls that stay synchronized with rich content.
- Student-preview rendering mode that hides answer keys and teacher-only notes.

## Tests

- Duplicate paragraph label error is shown.
- Question form changes by question type.
- Answer key is not required for essay/audio types.
- Unsafe pasted content is sanitized or rejected according to backend validation and surfaced clearly.
- Preview renders long passages, tables, and diagrams without overlapping the question pane.

## Documentation Sync

- Keep question payload types aligned.
- Keep rich-text editor payloads aligned with `0012` and `0014`.

## Acceptance Criteria

- Teacher can author a complete Reading section with answer keys.
- Teacher can create informative rich passages and verify the student view in real time.
