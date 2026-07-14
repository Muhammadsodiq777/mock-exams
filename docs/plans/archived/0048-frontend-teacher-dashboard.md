# Frontend Teacher Dashboard

> Build teacher home page for content, sales, and moderation status.

## Depends On

- `0007-backend-teacher-profiles-approval.md`
- `0026-backend-teacher-wallet-commission.md`

## Context

The teacher dashboard is the teacher's operating home: approval state, content status, sales/earnings, student activity, notifications, and later live speaking obligations. It should show what needs action without replacing detailed test builder, earnings, or schedule pages.

## Scope

### In scope

- `/teacher/dashboard`.
- Profile status.
- Draft, submitted, published, rejected test counts.
- Sales and wallet summary.
- Upcoming live speaking bookings after `0067`.
- Recent student attempts.
- Notifications.

### Out of scope

- Detailed earnings page.
- Test builder.
- Admin-only teacher management.

## Key Decisions

- Dashboard fetches a teacher-owned summary DTO through typed services/hooks; it should not assemble counts from many unrelated page calls unless the backend contract chooses that.
- Pending/rejected teacher profiles should see approval guidance and limited navigation instead of empty sales widgets.
- Cards link to the detailed owner pages: tests, builder, earnings, students, notifications, and schedule.
- Schedule summary remains hidden or feature-gated until `0067` APIs exist.
- Do not show fake earnings or attempts when the backend returns empty states.

## What To Implement

- Teacher dashboard service and hook.
- Status cards.
- Moderation warning banners.
- Links to next teacher actions.
- Schedule summary card with next live speaking session, pending provider/Meet-link warnings, and link to `/teacher/schedule`.
- Summary cards for draft/submitted/published/rejected tests, wallet balance, recent sales, recent attempts, reviews, and unread notifications.
- Approval status banner with profile completion/resubmission CTA.
- Loading, error, empty, and partial-data states.
- Stable query key and invalidation hooks for actions that affect counts.

## Tests

- Pending teacher sees approval state.
- Rejected teacher sees reason.
- Empty dashboard renders without fake data.
- Upcoming live speaking booking renders with local time and join-link availability when the schedule feature exists.
- Approved teacher sees correct content/sales summaries from API data.
- Dashboard links route to the expected teacher pages.
- API errors render retry state without losing shell navigation.

## Documentation Sync

- Mirror teacher dashboard DTOs.
- Keep live speaking summary fields aligned with `0067`.
- Update `docs/frontend-dev.md` if teacher dashboard query keys/components become conventions.

## Acceptance Criteria

- Teacher can see what needs action after login.
- Teacher can see both content and scheduled session obligations from the dashboard.
- Dashboard remains useful for pending, rejected, new, and active teachers.
