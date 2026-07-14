# Frontend Reading Test Player

> Build IELTS-style Reading attempt UI.

## Depends On

- `0015-backend-test-attempt-lifecycle.md`
- `0016-backend-auto-scoring-results.md`

## Context

The Reading player is where students spend the most focused test time. It must render rich passages safely, keep answer state stable across refreshes, respect the attempt timer, and submit through the backend lifecycle without exposing answer keys before results.

## Scope

### In scope

- Reading passage display.
- Question navigation.
- Answer sheet.
- Timer.
- Save progress.
- Submit attempt.
- Unanswered question indicators.
- Rich passage, paragraph, image/diagram, and question rendering from backend preview/player DTOs.
- Refresh-safe attempt state and clear submitted/expired behavior.

### Out of scope

- Listening audio player.
- Writing and speaking input.
- Anti-cheat/proctoring.

## Key Decisions

- Use a Reading variant of a shared test player route/component, selected by test type.
- Backend remains the source of truth for attempt state, saved answers, expiration, and submission.
- Local state may buffer edits for responsiveness but must reconcile with backend save success/failure.
- Student player DTOs must never include answer keys or explanations intended only for result review.
- Layout should prioritize dense reading/answer navigation without overlapping content on mobile or desktop.

## What To Implement

- `/tests/:id` player route for Reading.
- Attempt hooks for start, save answer, submit.
- Local draft state with backend save.
- Confirm-submit modal.
- Split-pane passage/question layout, section navigation, answer sheet, timer, and unanswered indicators.
- Answer input components for supported objective types.
- Autosave or explicit-save UX with pending/error states per answer.
- Expired/submitted state handling and route to result page.
- Safe rich-content renderer for passage, paragraphs, question text, options, and instructions.

## Tests

- Saving answer updates UI and calls API.
- Submit button handles unanswered questions.
- Timer displays remaining time.
- Submitted attempt routes to result page.
- Refresh reloads saved answers and timer state from backend.
- Submit warns on unanswered questions but allows final confirmation according to backend rules.
- Answer keys/explanations are not visible before result page.
- Long passages and rich tables/images do not break the layout.

## Documentation Sync

- Keep answer payload types aligned with API contract.
- Mirror attempt/player/result DTOs in frontend types.
- Keep shared player primitives aligned with Listening and result-review plans.

## Acceptance Criteria

- Student can complete and submit a Reading test cleanly.
- Reading attempts survive refresh, network save errors, and timer expiry with clear UI states.
