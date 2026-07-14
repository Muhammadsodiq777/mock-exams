# Frontend Teacher Earnings Students

> Build teacher earnings and student result pages.

## Depends On

- `0026-backend-teacher-wallet-commission.md`
- `0027-backend-withdrawals.md`
- `0017-backend-progress-analytics.md`

## Context

Teachers need detailed pages beyond the dashboard to understand earnings, wallet transactions, withdrawals, and student outcomes for their content. These pages must respect ownership and provide clear finance state without pretending to automate payouts.

## Scope

### In scope

- `/teacher/earnings`.
- Wallet balance and transactions.
- Withdrawal request form.
- `/teacher/students`.
- Student attempt/result list for teacher content.
- Live speaking booking earnings and student session history after `0067`.

### Out of scope

- Manual payout provider integration.
- Admin-only finance overrides.

## Key Decisions

- Earnings data comes from wallet/transaction APIs, not frontend calculations from payments.
- Withdrawal form uses backend minimums, validation, and payout detail schema.
- Student results list is teacher-owned and should show attempts/results for teacher content only.
- Live speaking earnings/session history is integrated after `0067` without changing marketplace transaction rows.
- Use tables with filters, pagination, empty, loading, and error states.

## What To Implement

- Earnings service and hooks.
- Transaction table.
- Withdrawal form.
- Student results table.
- Filters that distinguish marketplace purchase earnings from live speaking session earnings.
- Wallet summary cards for available, pending/reserved, lifetime gross/net, and currency.
- Withdrawal request history with statuses and rejection reasons.
- Student result detail links where teacher is allowed to inspect their content's attempts.
- Date/source/status filters and pagination.
- Mutation handling for withdrawal request create/cancel if backend supports cancellation.

## Tests

- Withdrawal below minimum shows validation.
- Empty transactions render clearly.
- Teacher sees only own content results.
- Live speaking booking transactions link to booking detail and use price snapshots.
- Withdrawal validation errors appear on the correct fields.
- Transaction filters preserve selected date/source/status.
- Rejected withdrawal shows reason and leaves balance state consistent with backend response.

## Documentation Sync

- Mirror wallet and withdrawal DTOs.
- Mirror booking-linked transaction DTOs when `0067` lands.
- Keep teacher student/result DTOs aligned with backend access rules.

## Acceptance Criteria

- Teacher can understand earnings and request withdrawals.
- Teacher can trace earnings back to tests, marketplace content, or live speaking sessions.
- Teacher can review student activity for owned content without seeing unrelated student data.
