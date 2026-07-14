# Backend Question Types Answer Keys

> Implement question, option, and answer-key modeling for IELTS objective questions.

## Depends On

- `0011-backend-test-builder-core.md`
- `0012-backend-reading-test-builder.md`
- `0013-backend-listening-test-builder.md`

## Context

Question authoring is used by Reading and Listening builders, but the planned rich-text editor means question prompts, instructions, options, answer explanations, and matching/table content need the same safe structured content contract as passages.

## Scope

### In scope

- CRUD for questions.
- Options for multiple choice and matching-like types.
- Answer keys with normalized values and rich-text explanations.
- Per-question score.
- Question ordering.
- Structured rich text for question prompts, instructions, option text, table/diagram labels, and explanations where the type needs informative formatting.

### Out of scope

- Manual teacher grading.
- AI grading for essay and audio answers.

## What To Implement

- Question DTOs grouped by type.
- Validation rules per question type.
- Answer normalization utility for case, whitespace, punctuation, and accepted variants.
- Preview shape that hides correct answers from students.
- Shared rich-text validation/mapping rules from `0012`, so question payloads cannot bypass sanitization.
- Student preview DTOs that expose rendered prompts/options but hide correct answers, accepted variants, and scoring explanations until result review.
- Teacher/admin preview DTOs that expose answer keys, explanations, and validation warnings in the builder/moderation context.

## Tests

- Student-facing test detail never exposes answer keys.
- Multiple choice question requires options.
- Objective question requires at least one answer.
- Unsupported question type payload is rejected.
- Unsafe rich text in prompts, options, or explanations is rejected or sanitized consistently with the reading builder.
- Student-facing preview omits answer keys even when explanations exist.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Add examples for each MVP question type.
- Keep rich-text examples aligned with `0012-backend-reading-test-builder.md` and frontend builder plans.

## Acceptance Criteria

- Teacher can configure answer keys and explanations.
- Auto-scoring has reliable normalized answers to use later.
- The same question content can be rendered in authoring, moderation, student attempt, and result review without leaking answer keys.
