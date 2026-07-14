# Frontend Admin Payments Withdrawals

> Build admin screens for payments, disputes, and withdrawals.

## Depends On

- `0027-backend-withdrawals.md`
- `0033-backend-payment-disputes.md`

## Context

Admins need finance support screens for payment status, disputes, and withdrawal review. The UI should expose enough context for decisions while keeping provider secrets and raw sensitive payout details out of the browser.

## Scope

### In scope

- `/admin/payments`.
- `/admin/withdrawals`.
- Payment status list.
- Dispute detail and resolution.
- Withdrawal approval and rejection.
- Live speaking booking payment/dispute context after `0067`.

### Out of scope

- Direct provider refund automation UI.
- Editing provider payment records directly.

## Key Decisions

- Use separate tabs/routes for payments, disputes, and withdrawals while sharing table/filter/review modal primitives.
- Admin decisions use backend command endpoints; frontend never mutates local status as source of truth.
- Resolution/rejection forms require backend-required reasons and show field errors.
- Booking context from `0067` is displayed only when backend DTOs include it.
- Provider metadata shown in UI must be safe and support-oriented, not secret.

## What To Implement

- Payment and withdrawal services.
- Tables with filters by status/date.
- Review modals.
- Resolution note forms.
- Links from a payment/dispute row to related booking, teacher, student, and Google sync state when present.
- Payment detail drawer/page with status, amount, currency, provider id, safe metadata, purchase/booking target, and timeline.
- Dispute review flow with status transitions and resolution note.
- Withdrawal review flow with payout detail summary, approve/reject actions, and reason capture.
- Filters by status, date, teacher/student, provider, and amount range where API supports them.
- Loading, empty, error, success toast, and retry states.

## Tests

- Withdrawal rejection requires reason.
- Dispute resolution updates status.
- Non-admin cannot load pages.
- Live speaking dispute detail shows slot time, booking status, Meet-link status, and cancellation/no-show markers.
- Invalid resolution/rejection reason displays backend validation.
- Duplicate admin submissions do not create duplicate visible state.
- Provider metadata never shows secrets.

## Documentation Sync

- Mirror payment, dispute, and withdrawal DTOs.
- Mirror booking support DTOs from `0067` when implemented.
- Keep admin finance routes aligned with dashboard/report links.

## Acceptance Criteria

- Admin can process finance support workflows.
- Admin can process finance issues that involve live speaking sessions without leaving the admin area.
- Admin decisions are validated, traceable, and reflected in list/detail state after mutation.
