# Backend AI Usage Limits Costs

> Track AI usage before adding writing and speaking evaluation.

## Depends On

- `0006-backend-users-roles-permissions.md`

## Context

AI features introduce direct provider cost and abuse risk. Usage limits and cost records must exist before Writing, Speaking, and study-plan generation can call external providers. The current schema has `ai_usage_limits` and `ai_feedbacks` cost/token fields; this plan defines the guard and accounting services that every AI plan must use.

## Scope

### In scope

- Per-user AI usage periods.
- Free request limit and hard limit.
- Token and estimated cost recording.
- Configured default limits from application properties or seeded settings.
- Admin editing of these limits is added later in `0034-backend-platform-settings.md`.
- Provider/model metadata and cost estimates recorded with every AI feedback/job.
- Guard checks for writing feedback, speaking transcription/feedback where configured, and study-plan generation.
- Admin-report friendly records for success, provider failure, validation failure, and blocked-by-limit outcomes.

### Out of scope

- Exact provider pricing automation.
- Full async queue implementation unless required by provider latency.
- Billing students directly for extra AI credits.
- Model-quality evaluation.

## Key Decisions

- AI request paths must call a single `AiUsageService` before provider calls and a single accounting method after provider responses or failures.
- Limits are per user and time period. The initial period can be monthly, but the service should not hardcode period math in controllers.
- Free limits, hard limits, model defaults, and pricing assumptions should come from typed defaults first and platform settings later.
- Do not increment successful usage for requests blocked before provider call. Decide and document whether failed provider calls count toward limits; store failures either way for audit.
- Store cost estimates as estimates, not invoices. Admin reports should show provider/model/tokens/status so numbers can be reconciled.
- Use `AI_USAGE_LIMIT_EXCEEDED` for blocked requests and avoid leaking provider pricing or secret config to students.

## What To Implement

- AI usage service.
- Guard method checked before AI requests.
- Record usage after provider response.
- Admin reporting hooks.
- Typed accessor that can read defaults now and be backed by editable platform settings later.
- Repository/service methods to find or create the active `ai_usage_limits` row with transaction safety.
- DTOs for user-facing remaining usage and admin-facing usage report rows.
- Integration points documented for `0019`, `0020`, `0021`, and `0022`.
- Startup/environment validation for provider enabled flags and safe dev defaults.
- Business errors for blocked requests, missing configuration, and temporarily unavailable provider dependencies.

## Tests

- User within free limit can request AI.
- User over hard limit is blocked.
- Failed provider call records failure without incrementing successful completions unless configured.
- Cost values are persisted with feedback.
- Concurrent AI requests cannot exceed hard limit through a race.
- New period creates a new usage row without mutating prior-period history.
- Disabled provider returns a clear service/configuration error before attempting network calls.
- Admin report totals match stored feedback token/cost records.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update platform settings documentation when admin editing is implemented later.
- Update `docs/backend-dev.md` with AI provider env vars, mock mode, enabled flags, and local failure behavior.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if usage periods, failure records, or pricing fields change.
- Keep admin report plan `0035` aligned with stored cost dimensions.

## Acceptance Criteria

- AI requests cannot bypass usage limits.
- Admin can audit token usage and estimated cost.
- Concurrent requests respect limits.
- AI feature plans can plug into one guard/accounting boundary instead of inventing separate limit logic.
