# Backend AI Study Plan

> Generate personalized study plans from progress and AI feedback.

## Depends On

- `0017-backend-progress-analytics.md`
- `0019-backend-writing-submissions-ai-feedback.md`
- `0021-backend-speaking-ai-feedback.md`

## Context

Study plans should be generated from actual attempt results, AI feedback, weak skills, target band, and marketplace availability. The plan must avoid generic advice by grounding each task in observed evidence or a clearly labeled recommendation.

## Scope

### In scope

- Create study plan with target band and duration.
- Use recent scores and weak skills as inputs.
- Store plan summary and daily tasks.
- Recommend tests or marketplace listings.
- Include evidence references from recent results/feedback so students can understand why tasks were recommended.
- Regenerate or revise active plans without losing completed task history.

### Out of scope

- Calendar integrations.
- Teacher-managed study plans.
- Guaranteeing official IELTS improvement outcomes.

## Key Decisions

- Study-plan generation uses the same AI provider/job/usage boundary as Writing and Speaking, so cost, status, retries, and provider failures behave consistently.
- Plans must be persisted as structured data. The frontend should not parse a single prose blob into tasks.
- Generated tasks should be grounded in available evidence: recent scores, weak question types, AI feedback, target band, time window, and accessible content.
- If data is sparse, the plan should state uncertainty and recommend diagnostic practice instead of inventing missing skill scores.
- Regeneration changes future tasks only; completed tasks are retained for continuity.
- Marketplace recommendations must respect active status and access/price context.

## What To Implement

- `StudyPlanService`.
- AI prompt for structured 30-day plan.
- Daily task persistence.
- Complete-task endpoint.
- Input assembler that summarizes recent Reading/Listening scores, Writing/Speaking AI feedback, weak question types, target band, available time, and purchased/free content.
- Strict schema validation for generated tasks: date, skill, title, description, estimated minutes, recommended resource, and rationale.
- Revision flow that can replace future tasks while preserving completed past tasks.
- Student endpoints to create, read, list active/history plans, complete/uncomplete tasks, and regenerate future tasks.
- Provider prompt templates with versioning so plan quality can be audited over time.
- Recommendation resolver that validates referenced tests/listings before saving them into tasks.
- Business errors for invalid target band/duration, active-plan conflicts if only one active plan is allowed, and inaccessible recommended content.

## Tests

- Plan generation requires target band and duration.
- Tasks are ordered per day.
- Completed task remains completed after refresh.
- Recommended listing must be active if present.
- Plan generation works with partial data and labels missing skills instead of fabricating scores.
- Regeneration preserves completed tasks and does not recommend inactive or inaccessible paid content as if it were free.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update DBML if task fields change.
- Update AI usage/report docs so study-plan cost appears alongside writing/speaking feedback.
- Mirror study-plan DTOs in `frontend/src/types/backend.ts`.
- Update `docs/specs/project-overview.md` if study-plan behavior or student dashboard expectations change.

## Acceptance Criteria

- Student can generate and follow a stored study plan.
- Generated tasks are evidence-backed, schema-valid, and safe to revise.
- Students can complete tasks and refresh without losing progress.
