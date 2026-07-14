---
name: orchestrate
description: >
  Task orchestration skill for approved implementation plans in Mock Exams. Use when the user
  explicitly asks to orchestrate, delegate, parallelize, or execute a plan through multiple
  agents or workstreams. Decomposes a plan into implementation, review, and optional fix tasks
  without owning git workflow. Do not trigger for planning itself, direct single-task coding,
  or when the user has not asked for delegation.
---

# Mock Exams Plan Orchestration

You are an orchestrator. Your job is to break an approved plan into executable tasks, dispatch
specialized agents only when the user explicitly asked for delegation, and keep the work aligned.

## Inputs

- An approved inline plan
- Or a plan file such as `docs/plans/*.md`

If the user gives a shorthand plan reference such as `orchestrate: 0004`, resolve it by finding
the markdown file in `docs/plans/` whose filename starts with that prefix, then use that file as
the approved plan input.

Read the plan first. Read only the minimum additional docs needed to split work accurately.

## Decomposition Rules

- Create all implementation tasks first
- Add one review task after implementation
- Add one fix task only if review finds real issues
- Keep tasks as meaningful deliverables, not micro-steps
- Separate backend and frontend tasks when they touch different file sets
- Keep ownership clear so parallel workers do not collide

Use labels like:

- `[impl] Backend exam service changes`
- `[impl] Frontend exam UI updates`
- `[review] Relevant changes`
- `[fix] Review findings`

## Delegation Rules

- Only spawn sub-agents when the user explicitly asked for orchestration or delegation
- Fresh agent per task
- Give the agent the task objective, affected files or scope, and dependency context
- Do not make agents run git commands
- Do not duplicate work locally while an assigned agent is handling it

## Review Cycle

- Run one review pass after all implementation tasks complete
- Use the `review` skill for the review task; it routes to `backend-review` and `frontend-review` by changed area and adds cross-layer contract checks
- If review finds issues, create one fix task
- Do not loop through repeated review/fix cycles unless the user asks

## Verification

Run the relevant final verification once after implementation and fixes:

- Backend-only changes: `mvn test` in `backend/`
- Frontend-only changes: `npm run lint` and `npm run build` in `frontend/`
- Cross-layer changes: verify both areas

During inner implementation loops, prefer narrower checks where available.

If the repo later adds a `justfile`, prefer its equivalent recipes.

## Plan Archiving

After implementation, review, any fix task, and final verification are complete, move the approved
plan file into `docs/plans/archived/` while preserving its filename.

- Create `docs/plans/archived/` if it does not exist.
- Archive only plan files that were used as the orchestration input.
- Do not archive inline plans, blocked plans, incomplete plans, `README.md`, or `implementation-plan-template.md`.
- Mention the archived path in the final response.

## Hard Rules

1. Do not manage git workflow unless the user explicitly asks for git operations.
2. Do not widen scope beyond the approved plan.
3. Review only the touched changes, not the whole repository.
4. Keep task state explicit and easy to audit.
5. Call out blocked dependencies instead of guessing through them.
