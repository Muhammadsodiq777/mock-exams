# Frontend Writing Practice AI

> Build Writing Task 1 and Task 2 AI feedback flow.

## Depends On

- `0019-backend-writing-submissions-ai-feedback.md`

## Context

Writing practice needs a comfortable essay entry surface, but it should not use the same full rich-text feature set as teacher-authored passages unless the backend contract explicitly allows formatted student answers. The AI result display also needs structured sections so students can act on feedback rather than read one long response.

## Scope

### In scope

- `/writing` page.
- Task type selector.
- Prompt input or selected prompt display.
- Essay editor with word count, autosave-friendly local state, and optional limited rich-text mode only if the backend writing contract supports formatted answers.
- Submit for AI feedback.
- Feedback display with criteria, mistakes, improved version, and action plan.
- AI usage remaining state.
- Pending, failed, retry, and polling states for async feedback jobs.

### Out of scope

- Teacher review purchase.
- Collaborative editing.

## Key Decisions

- Default to plain text for IELTS essay scoring unless the backend decides to persist structured rich text for writing answers; keep the editor abstraction ready for either contract.
- Render AI feedback from structured DTO sections, not parsed prose.
- Keep prompt display compatible with rich teacher/admin-authored prompts from the builder.
- Use React Query mutations and polling/query invalidation for async feedback statuses; do not resubmit answers to refresh status.
- Surface usage-limit and provider-failure errors distinctly so students know whether to wait, edit, retry, or upgrade.
- Keep local draft state private to the browser until explicit submit or autosave is introduced by a separate backend contract.

## What To Implement

- Writing service and hooks.
- Form validation.
- Feedback result components.
- Pending/failed feedback states.
- Editor component with word count, min/max guidance, local unsent-change guard, and API-ready serialization.
- Status polling or refresh flow for queued/processing feedback.
- Criteria cards for Task Achievement/Task Response, Coherence and Cohesion, Lexical Resource, and Grammatical Range and Accuracy.
- Feedback history/list or recent submission hook if backend exposes it, with clear empty state.
- Retry control that calls the backend retry/status endpoint rather than creating duplicate submissions.
- Prompt selection/display components that can show official prompts or teacher-authored rich prompts safely.

## Tests

- Empty essay is blocked.
- Usage-limit error is shown clearly.
- Feedback sections render from API data.
- Async queued/processing/failed states render without duplicate submissions.
- Rich prompt content from the backend renders safely without breaking the essay editor layout.
- Retrying a failed feedback job does not duplicate the visible submission.
- Word count updates accurately for pasted and edited text.
- Completed feedback remains visible after page refresh.

## Documentation Sync

- Mirror writing DTOs in frontend types.
- Keep editor serialization aligned with `docs/API_CONTRACT.md`.

## Acceptance Criteria

- Student can submit writing and receive useful AI feedback in the UI.
- Students can understand status, retry safely after failures, and review structured feedback without layout overflow.
- The page handles empty, editing, submitting, queued, processing, completed, failed, and usage-limited states.
