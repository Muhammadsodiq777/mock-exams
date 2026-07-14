# Frontend Student Dashboard

> Build the student home page for progress and next actions.

## Depends On

- `0017-backend-progress-analytics.md`
- `0038-frontend-api-client-types.md`
- `0039-frontend-design-system.md`

## Context

The student dashboard is the home screen for the learning loop: current level, recent work, weak areas, AI usage, recommendations, and next action. It should consume one dashboard/progress read model instead of making pages assemble analytics ad hoc.

## Scope

### In scope

- Current estimated band.
- Recent tests.
- Weak areas.
- Daily study task.
- Progress chart.
- Recommended tests/listings.
- AI usage remaining.
- Loading, error, and empty states for a new student.
- Links into test list, result detail, writing, speaking, pricing, and study-plan task flows.

### Out of scope

- Leaderboard.
- Study plan generation UI beyond showing current daily task.
- Admin/teacher analytics.

## Key Decisions

- Keep dashboard orchestration in a page component and reusable presentation pieces in `components`.
- Fetch dashboard/progress through a typed hook and stable query key.
- The UI must handle missing skill data gracefully; missing skills are prompts to practice, not zero scores.
- Recommendations should show access/price state clearly and route to catalog/marketplace/detail pages.
- AI usage remaining should be hidden or disabled when the AI feature flag/API is unavailable.

## What To Implement

- `/dashboard` page.
- Progress query hook.
- Dashboard cards and chart components.
- Empty state for new students.
- Skill summary cards, recent attempts list, weak areas panel, progress chart, recommendation list, daily task card, and AI usage card.
- URL/navigation links to result detail, tests, marketplace, writing, speaking, and study plan when available.
- Empty dashboard action set for first Reading/Listening test and first AI practice.
- Error retry behavior and skeleton/loading state.

## Tests

- Empty dashboard renders without crashing.
- Loading and error states are explicit.
- Weak areas and recommendations render from API data.
- Missing Writing/Speaking data does not break overall band display.
- Recommendation CTA respects paid/free/access state.
- Date/filter metadata renders consistently with backend response.

## Documentation Sync

- Mirror dashboard DTOs in frontend types.
- Keep dashboard route and query key documented in `docs/frontend-dev.md` if shared conventions are added.
- Keep `docs/API_CONTRACT.md` aligned if the dashboard DTO changes during implementation.

## Acceptance Criteria

- Student can see current status and next useful action after login.
- New and returning students both get a clear, actionable home screen.
