# Frontend Results Progress

> Build result review and progress views.

## Depends On

- `0016-backend-auto-scoring-results.md`
- `0017-backend-progress-analytics.md`

## Context

Result review closes the practice loop. Students need to see score, estimated band, what they answered, correct answers, explanations, weak areas, and next recommendations without leaking result data across accounts.

## Scope

### In scope

- `/results/:id`.
- Raw score and estimated band.
- Correct and incorrect answers.
- Explanations.
- Time spent.
- Weak question types.
- Suggested practice.
- Progress history charts.
- Rich explanations and answer review for objective question types.
- Links back to dashboard, test list, marketplace, writing/speaking practice, and study plan where relevant.

### Out of scope

- AI feedback pages.
- Teacher/admin grading review.

## Key Decisions

- Result detail uses backend `ResultDetailRDO` and should not recalculate scoring client-side.
- Correct answers and explanations are rendered only from result APIs after submission.
- Progress charts use progress/dashboard APIs, not result detail rows, so they can evolve independently.
- Empty/pending/not-found states should guide students to refresh, dashboard, or support paths.
- Rich explanations render through the same safe rich-content renderer as builder/player.

## What To Implement

- Result detail service and hook.
- Answer review components.
- Skill history components.
- Recommendation block.
- Result route `/results/:id` and optional attempt-result route integration.
- Summary cards for raw score, max score, estimated band, time spent, and completed date.
- Sectioned answer review with student answer, correct answer, awarded score, explanation, and weak-area tagging.
- Progress chart components and recommendation CTA components.
- Loading, pending, failed/not-found, and unauthorized states.

## Tests

- Result page handles pending/missing result.
- Correct and incorrect answers are visually distinct.
- Explanation text does not overflow.
- Student cannot see another student's result when API returns forbidden/not found.
- Rich explanations render safely.
- Result summary and per-answer scores match backend DTO values without client recalculation.

## Documentation Sync

- Mirror result DTOs in frontend types.
- Keep progress DTOs aligned with `0040`.
- Update `docs/frontend-dev.md` if shared result/review components become conventions.

## Acceptance Criteria

- Student can understand what they missed and what to practice next.
- Result review is clear, accurate, and connected to the next practice action.
