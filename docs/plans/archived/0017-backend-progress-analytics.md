# Backend Progress Analytics

> Build student progress summaries from completed attempts, with extension points for later AI feedback.

## Depends On

- `0016-backend-auto-scoring-results.md`

## Context

Progress analytics turns completed attempts and AI feedback into the student's home-page summary. The first useful version can be built from Reading and Listening `test_results`, then extended by Writing and Speaking AI feedback when those plans land. This plan should define reusable query/read-model boundaries rather than mixing analytics calculations into dashboard controllers.

## Scope

### In scope

- Student dashboard summary for Reading and Listening results.
- Score history by skill for objective test results.
- Estimated overall band from available completed skills.
- Weak question types.
- Recent attempts and recommended next practice.
- Extension inputs for Writing and Speaking AI feedback, study-plan recommendations, purchased/free content, and AI usage remaining.
- Time-window filters for recent history and dashboard freshness metadata.
- Empty/new-student states that the frontend can render without guessing.

### Out of scope

- ClickHouse analytics.
- Leaderboard and streaks.
- Heavy data warehouse style cohort analytics.
- Teacher-facing student analytics beyond summaries needed later by teacher pages.

## Key Decisions

- Keep analytics in query services that read attempt/result/feedback tables and return DTOs; do not store derived dashboard snapshots for the MVP.
- Use stored `test_results` as the source for Reading/Listening scores. Do not rescore attempts inside analytics.
- Overall band is an estimate from available skills and must label missing skills rather than treating them as zero.
- Recommendations start rule-based from weak areas and active/free or accessible content; AI study-plan generation belongs to `0022`.
- Keep the API response stable enough for frontend cards/charts: summary, skill histories, weak areas, recent attempts, recommendations, and usage status.

## What To Implement

- Progress query service.
- Dashboard DTO for student home.
- Aggregation from `test_results` and attempts first.
- Recommendation hook that can use marketplace later.
- Extension points for Writing and Speaking AI feedback once `0019` and `0021` are implemented.
- Student route such as `GET /api/v1/student/dashboard` or `GET /api/v1/student/progress` following envelope conventions.
- Query methods for latest result per skill, score history by skill, recent attempts, weak question type counts, and current estimate.
- Recommendation read model that can include official tests, teacher tests, marketplace listings, or study-plan tasks without hardcoding one content type.
- Freshness metadata (`asOf`, date window) for dashboard responses.
- Permission checks so users can only read their own progress.

## Tests

- Empty student has empty progress state.
- Reading and Listening history are separated.
- Overall band ignores missing skills and can be recalculated when AI skill data is added later.
- Weak areas use latest relevant attempts.
- Recent attempts include submitted/expired attempts according to documented rules and exclude drafts owned by teachers.
- Recommendations never include archived, rejected, unpublished, or inaccessible paid content as directly available.
- Dashboard query remains bounded for a student with many attempts.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Keep frontend dashboard plan aligned.
- Update `docs/specs/project-overview.md` if the dashboard or progress model changes.
- Update `docs/ERD/platform-architecture.dbml` only if analytics persistence is introduced.
- Mirror dashboard/progress DTOs in `frontend/src/types/backend.ts`.

## Acceptance Criteria

- Dashboard can show current band, recent tests, weak areas, and progress chart data.
- New students get a clear empty state with recommended first actions.
- Analytics can incorporate Writing/Speaking AI data later without changing the entire dashboard contract.
