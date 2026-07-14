# Backend Payment Disputes

> Track payment complaints and admin dispute resolution.

## Depends On

- `0025-backend-payments-provider-integration.md`

## Context

Disputes give students and admins a structured support workflow for payment problems without changing payment provider state automatically. The dispute model must link to payments and later live speaking bookings while preserving a clear audit trail.

## Scope

### In scope

- Student opens dispute for payment.
- Admin reviews dispute.
- Store reason, description, status, resolution note.
- Optional refund status marker.
- Dispute context for live teacher speaking bookings after `0067`, including cancelled, no-show, and missing Meet-link cases.

### Out of scope

- Automated refund provider integration.
- Chargeback webhooks.
- Reversing teacher wallet transactions automatically unless a later refund plan defines it.

## Key Decisions

- Disputes are support records linked to a payment; provider refunds and chargebacks remain separate future work.
- Student-created disputes are limited to their own payments and allowed payment statuses.
- Admin decisions record reviewer, timestamp, status, and resolution note.
- Notifications are sent when dispute status changes.
- Booking-related disputes after `0067` should include booking context without moving booking ownership into the payment module.
- Resolution statuses must be explicit: `open`, `under_review`, `refunded`, `rejected`, `resolved`.

## What To Implement

- `PaymentDisputeService`.
- Student and admin endpoints.
- Link disputes to payments.
- Notifications for status changes.
- Booking-aware dispute metadata or relation so support can inspect the teacher, student, slot time, payment, and Google sync state.
- DTOs for student dispute create/list/detail and admin dispute queue/detail/decision.
- Status transition rules and reason taxonomy for common support cases.
- Query filters by status, payment status, date range, teacher/student, and reason.
- Business errors for dispute not found, forbidden payment, invalid status transition, duplicate open dispute if that rule is chosen, and missing resolution note.

## Tests

- Student cannot dispute another student's payment.
- Only paid or failed payment can be disputed according to chosen rule.
- Admin resolution records reviewer and timestamp.
- Booking dispute can be opened only by the booked student or an admin support action.
- Student cannot open disputes for another student's payment.
- Resolved/rejected disputes cannot be resolved again.
- Dispute status-change notifications target the correct student and relevant teacher/admin where applicable.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update DBML if dispute statuses change.
- Update booking/support docs from `0067` if disputes gain a direct booking relation.
- Update schema SQL if dispute uniqueness, relation, or status fields change.
- Keep admin finance frontend plan aligned.

## Acceptance Criteria

- Payment disputes are visible to student and admin with clear statuses.
- Admin can resolve live speaking payment disputes with enough booking context to make a decision.
- Dispute records are auditable and never mutate payment/provider state implicitly.
