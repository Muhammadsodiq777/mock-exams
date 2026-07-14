# Backend Withdrawals

> Let teachers request payouts and admins review them.

## Depends On

- `0026-backend-teacher-wallet-commission.md`

## Context

Withdrawals turn wallet balance into a teacher payout request that admins can approve or reject. The MVP does not automate bank transfers, so correctness comes from reservation rules, auditable admin decisions, and clear wallet ledger entries.

## Scope

### In scope

- Teacher withdrawal request.
- Minimum withdrawal amount setting.
- Admin approve and reject.
- Wallet transaction for approved withdrawal.
- Payout details JSON.

### Out of scope

- Automated bank transfer.
- Multi-currency conversion.
- Tax reporting and KYC automation.
- Teacher-to-teacher transfers.

## Key Decisions

- Withdrawal requests reserve available wallet balance when created so teachers cannot spend the same balance twice.
- Admin approval creates the withdrawal ledger transaction and marks the request approved or paid according to the MVP payout semantics.
- Rejection or teacher cancellation releases reserved balance.
- Minimum withdrawal amount and payout details validation come from platform settings or seeded defaults.
- Payout details are JSON for MVP, but must be validated by a backend schema and never logged with sensitive details.
- Admin actions must record reviewer, timestamp, and reason where applicable.

## What To Implement

- Withdrawal service.
- Teacher withdrawal endpoints.
- Admin review endpoints.
- Balance reservation rules.
- DTOs for withdrawal request create, teacher list/detail, admin queue item, and admin decision commands.
- Status transitions for `requested`, `approved`, `rejected`, `paid`, and `cancelled`.
- Wallet integration for reserve, release, approved withdrawal transaction, and available/pending balance updates.
- Notification events for teacher request created, approved, rejected, paid, or cancelled.
- Business errors for below minimum, insufficient balance, invalid status transition, forbidden teacher/admin access, and invalid payout details.

## Tests

- Withdrawal below minimum is rejected.
- Teacher cannot withdraw more than available balance.
- Rejected withdrawal releases reserved amount.
- Approved withdrawal creates transaction.
- Teacher cannot cancel an already approved/paid withdrawal.
- Admin rejection requires a reason.
- Non-owner teacher cannot view or mutate another teacher's withdrawal.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update settings docs.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if withdrawal fields or statuses change.
- Update `docs/backend-dev.md` if local seed/settings for payout details are added.
- Keep frontend teacher earnings and admin finance plans aligned.

## Acceptance Criteria

- Teacher can request withdrawal and track status.
- Admin can approve or reject with reason.
- Wallet reserved/available balances remain consistent through approval, rejection, cancellation, and duplicate admin submissions.
