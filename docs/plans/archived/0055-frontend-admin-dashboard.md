# Frontend Admin Dashboard

> Build top-level admin operational dashboard.

## Depends On

- `0030-backend-admin-dashboard.md`

## Context

The admin dashboard is the first screen for platform operations. It should summarize health and queues across users, teachers, tests, payments, withdrawals, AI, moderation, reports, and later live speaking without duplicating every detail page.

## Scope

### In scope

- `/admin/dashboard`.
- User, teacher, test, payment, withdrawal, AI, and moderation metric cards.
- Recent activity sections.
- Date range selector.

### Out of scope

- Detailed reports pages.
- Mutating admin actions beyond navigation to detail queues.

## Key Decisions

- Use the backend dashboard DTO as source of truth; do not aggregate counts from many frontend pages.
- Date range changes update query params or local state and refetch through a stable query key.
- Metric cards link to detailed admin pages/filters.
- Dashboard must render zero/empty states for early development seed data.
- Live speaking metrics stay hidden or feature-gated until `0067` backend DTOs exist.

## What To Implement

- Admin dashboard service and hook.
- Metric cards.
- Small trend/chart components.
- Loading and error states.
- Date range selector with default range and validation.
- Recent activity/queue sections for teacher approvals, test moderation, payment disputes, withdrawals, AI failures, and provider warnings.
- Navigation links to admin users/teachers/moderation/payments/withdrawals/settings/reports.
- Skeleton/empty/error states and retry.

## Tests

- Non-admin route is forbidden.
- Empty metrics render zero states.
- Date filter reloads metrics.
- Metric card links route to expected admin detail page/filter.
- Dashboard does not crash when optional live speaking metrics are absent.
- Non-admin forbidden state preserves app shell.

## Documentation Sync

- Mirror admin dashboard DTOs.
- Keep admin route and report links aligned with API contract and frontend route constants.

## Acceptance Criteria

- Admin can quickly see platform health.
- Admin can identify queues needing action and navigate to the owning screen.
