# Backend Teacher Wallet Commission

> Split paid revenue between teacher and platform.

## Depends On

- `0024-backend-purchases-and-access-control.md`
- `0025-backend-payments-provider-integration.md`

## Context

Teacher earnings must be traceable from a paid student action through platform commission and wallet balance. This plan defines the ledger-style wallet boundary used by marketplace purchases first and live speaking bookings later, without mixing payout review or payment provider callbacks into the wallet service itself.

## Scope

### In scope

- Teacher wallet creation.
- Platform commission from application properties or seeded default setting first.
- Wallet transaction creation after paid purchase.
- Wallet transaction creation after paid live speaking bookings once `0067` lands.
- Gross, commission, and net amount tracking.
- Admin-editable commission is added later in `0034-backend-platform-settings.md`.

### Out of scope

- Withdrawal payout.
- Tax handling.
- Provider payment confirmation; wallet listens to confirmed business events rather than webhooks directly.
- Manual wallet adjustments except as a later audited admin feature.

## Key Decisions

- Treat `teacher_transactions` as an append-only ledger for earnings, commission, withdrawal, refund, and adjustment records.
- Wallet balances are derived/updated through one wallet service transaction, never directly mutated by payment controllers.
- Commission uses a typed setting with a seeded default. If live speaking has a separate commission, it must use a separate key and be recorded on the transaction.
- Store gross, commission, net, currency, source id, and description at transaction time so later setting changes do not rewrite history.
- The wallet service must be idempotent per source event, such as one purchase or one speaking booking confirmation.
- Teacher can read only their own wallet; admins can read wallet summaries through admin/report endpoints.

## What To Implement

- `TeacherWalletService`.
- Transaction creation in same business flow as purchase grant.
- Commission calculation using a typed settings accessor with defaults.
- Balance and pending balance rules.
- Source-type handling so earnings can reference marketplace purchases and live speaking bookings without double counting.
- Wallet creation on first earning or teacher approval, with deterministic handling if two requests race.
- Teacher endpoints for wallet summary and transaction history.
- Internal methods for earning creation, withdrawal reservation/release, approved withdrawal ledger entries, refunds, and future adjustments.
- Repository queries for teacher wallet by profile, paged transaction history, and report aggregates by date/source.
- Business errors for missing teacher profile, currency mismatch, duplicate source transaction, and insufficient balance.

## Tests

- 25% commission on 20,000 UZS yields 15,000 teacher net and 5,000 platform commission.
- Wallet transaction is created once per purchase.
- Free listing does not create earning transaction.
- Teacher can read own wallet only.
- Paid live speaking booking creates exactly one earning transaction using the booking price snapshot and live-speaking commission setting.
- Wallet balance cannot go negative through withdrawal reservation or refund.
- Currency mismatch between wallet and source transaction is rejected.
- Duplicate payment/purchase confirmation does not duplicate earnings.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Document commission setting.
- Document separate commission keys if marketplace and live speaking sessions use different rates.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if transaction source fields or constraints change.
- Keep withdrawal plan `0027`, reports plan `0035`, and teacher earnings frontend plan aligned.

## Acceptance Criteria

- Teacher earnings are auditable and derived from purchases.
- Teacher earnings remain auditable when the source is a marketplace purchase or a live speaking booking.
- Wallet APIs expose enough transaction metadata for teacher/admin screens without exposing student private details unnecessarily.
