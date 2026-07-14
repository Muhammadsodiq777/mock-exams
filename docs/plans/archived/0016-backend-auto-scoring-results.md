# Backend Auto Scoring Results

> Score objective Reading and Listening attempts and generate result details.

## Depends On

- `0015-backend-test-attempt-lifecycle.md`
- `0014-backend-question-types-answer-keys.md`

## Context

Objective Reading and Listening attempts become valuable only after the platform can turn saved answers into an immutable result. The current target schema already has `test_attempts`, `student_answers`, `test_results`, question answers, normalized values, explanations, and rich-text answer explanations. This plan should implement the scoring boundary that consumes submitted attempts and produces the result read models used by progress analytics, result review, dashboards, and recommendations.

## Scope

### In scope

- Score objective answers.
- Store per-answer correctness and awarded score.
- Create one result per submitted attempt.
- Estimate IELTS band for Reading and Listening with configurable conversion tables.
- Include explanations and weak question types in result detail.
- Support answer types from the MVP question model: selected option, short answer, gap fill, sentence/summary/table completion, ordering where represented as normalized values, and matching-style objective answers.
- Return result details with student answer, correct answer, score awarded, explanation, question type, section, and timing context.
- Keep result generation idempotent so repeated submit/result requests do not create duplicate `test_results`.

### Out of scope

- Writing and Speaking AI scoring.
- Official IELTS-certified scoring claims.
- Manual teacher grading and partial-credit rubrics for essays/audio.
- Anti-cheat scoring adjustments.

## Key Decisions

- Own scoring in backend `service` classes with DTOs under `dto` and mappers under `mapper`; controllers should only authorize, call services, and wrap responses.
- Treat `test_results` as immutable for a submitted attempt unless an explicit future rescore/audit plan is created.
- Use the normalized answer values produced by the question/answer-key plan; do not reimplement ad hoc answer cleaning inside controllers.
- Band conversion tables are configuration or seeded platform data, not hardcoded inside a controller. The result stores the band that was calculated at submission time.
- Correct answers and explanations are visible only after the attempt is submitted or expired and scored. Attempt/player endpoints must still hide answer keys.
- Result creation should run in one transaction and lock/read the submitted attempt consistently so concurrent result requests return the same result.

## What To Implement

- `ScoringService`.
- Band conversion configuration.
- Result summary DTO.
- Result detail endpoint with correct answer visibility only after submission.
- Student endpoints such as `GET /api/v1/student/results/{resultId}` and `GET /api/v1/student/attempts/{attemptId}/result`, plus whatever command is needed if result generation is not triggered directly by submit.
- Repository queries that load an attempt with its test, sections, questions, options, answers, and saved student answers without N+1 behavior for normal result pages.
- Per-question scoring rules for exact normalized answer matches, selected option matches, accepted-answer variants, unanswered questions, and unsupported/non-objective question types.
- Weak-area aggregation by question type, section, and optionally tag/category so `0017` can build progress insights.
- Mapper read models for result summary, result detail sections, answer review rows, and criterion metadata needed by the frontend.
- Business errors including `ATTEMPT_NOT_FOUND`, `ATTEMPT_NOT_SUBMITTED`, `RESULT_NOT_READY`, and `RESULT_ALREADY_EXISTS` only where a true conflict needs to be reported.

## Tests

- Exact and normalized answer matches score correctly.
- Wrong answer earns zero for objective question.
- Result creation is idempotent for one attempt.
- Band conversion uses configured table.
- Unanswered objective questions score zero and appear in result detail as unanswered.
- Student cannot read another student's result.
- Draft/unpublished test attempts cannot be scored outside the allowed attempt lifecycle.
- Result detail includes rich-text explanations only after submission.
- Concurrent result generation requests create one `test_results` row.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Document band conversion source and assumptions.
- Update `docs/specs/project-overview.md` if scoring or result review behavior changes.
- Update `docs/ERD/platform-architecture.dbml` and Flyway schema SQL if result, answer, or band conversion persistence changes.
- Keep frontend result DTO mirrors aligned in `frontend/src/types/backend.ts` when the result API is implemented.

## Acceptance Criteria

- Submitted Reading/Listening attempt returns score, band, answers, explanations, and weak areas.
- Result creation is repeatable and safe under retries.
- Student-facing APIs never expose answer keys before submission.
- Progress analytics can consume stored results without recalculating raw answers.
