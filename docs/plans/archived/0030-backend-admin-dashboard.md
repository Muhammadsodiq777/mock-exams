# Backend Admin Dashboard

> Provide top-level operational metrics for admins.

## Depends On

- `0017-backend-progress-analytics.md`
- `0026-backend-teacher-wallet-commission.md`
- `0018-backend-ai-usage-limits-costs.md`

## Context

Admins need a concise operational dashboard before drilling into moderation, users, payments, withdrawals, AI usage, and reports. This plan should aggregate existing domain data without becoming the owner of each domain's business rules.

## Scope

### In scope

- User, teacher, test, payment, withdrawal, AI usage, and moderation counts.
- Recent activity summaries.
- Date range filter.
- Links/counts for provider failures, pending moderation, disputed payments, and pending withdrawals.
- Optional live speaking booking counters after `0067`.

### Out of scope

- Deep analytics warehouse.
- Export files.
- Mutating admin actions; detail screens own actions.

## Key Decisions

- Dashboard data is read-only and aggregated through query services, not denormalized into a dashboard table for MVP.
- Date filters use UTC inclusive bounds and echo effective filters in response metadata.
- Counts should be cheap enough for dashboard load; use indexed aggregate queries and avoid loading full entity graphs.
- Dashboard cards should link to existing admin queues/reports rather than duplicating their full data.
- Non-admin users receive `FORBIDDEN`; inactive admin accounts are blocked by auth/session rules.

## What To Implement

- Admin dashboard query service.
- Summary DTOs.
- Role-protected endpoint.
- Efficient aggregate queries.
- Endpoint such as `GET /api/v1/admin/dashboard?from=&to=` returning cards, queue counts, revenue/AI totals, and recent events.
- Query methods for users by status, teacher approvals, test moderation, payments by status, withdrawals by status, AI usage totals, and recent notifications/activity.
- Freshness metadata (`asOf`) and date filter echo.
- Business-safe fallback values for empty systems.

## Tests

- Non-admin is forbidden.
- Empty system returns zero counts.
- Date range filters payment and AI totals.
- Dashboard query does not expose student private answer/feedback text.
- Counts match the underlying domain repository totals for the same date window.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update `docs/specs/project-overview.md` if admin dashboard capabilities change.
- Update schema SQL only if new dashboard-specific persistence is introduced.
- Keep frontend admin dashboard DTOs aligned.

## Acceptance Criteria

- Admin dashboard can render key platform metrics from one endpoint or a small endpoint set.
- Dashboard remains fast and read-only while pointing admins to the correct domain screens for action.
