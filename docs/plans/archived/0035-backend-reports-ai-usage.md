# Backend Reports AI Usage

> Add admin reports for sales, usage, learning, and AI costs.

## Depends On

- `0018-backend-ai-usage-limits-costs.md`
- `0026-backend-teacher-wallet-commission.md`
- `0030-backend-admin-dashboard.md`

## Context

Reports are deeper admin views for trends and reconciliation after the dashboard shows top-level metrics. They should aggregate existing domain records for AI cost, sales, teacher earnings, test activity, and later live speaking bookings without introducing a separate analytics warehouse in the MVP.

## Scope

### In scope

- AI usage report by date, model, user, and feedback type.
- Sales report by teacher, listing, and date.
- Live speaking booking revenue and provider-sync report after `0067`.
- Test activity report by test type and teacher.
- Basic export-ready JSON responses.

### Out of scope

- ClickHouse.
- CSV/PDF export in first pass.
- Real-time streaming analytics.
- Financial statements or tax documents.

## Key Decisions

- Reports are read-only query services with date filters and aggregate DTOs.
- Use the same UTC date boundary semantics as the API contract and dashboard.
- AI cost reports read stored feedback/job token/cost/status fields; they do not call providers for pricing at report time.
- Sales reports reconcile payments, purchases/bookings, wallet transactions, refunds/disputes where available, and teacher net amounts.
- Export-ready JSON means stable table/chart DTOs; CSV/PDF export can be added later without changing the data semantics.
- Non-admin access is forbidden.

## What To Implement

- Admin report query services.
- Date range filters.
- Aggregated DTOs suitable for tables and charts.
- Aggregations that distinguish marketplace revenue, live speaking revenue, AI costs, refunds/disputes, and teacher net earnings.
- Endpoints for AI usage report, sales report, teacher/listing revenue report, test activity report, and provider failure report.
- Filters by date range, teacher, listing, test type, feedback type, model, provider, payment status, and booking status where relevant.
- Response metadata with effective filters, totals, and generated-at timestamp.
- Repository projections/queries backed by indexes from the relevant domain tables.
- Business errors for invalid date range, unsupported filter/sort, and forbidden access.

## Tests

- AI totals match stored feedback usage.
- Sales totals match teacher transactions.
- Date filters are inclusive and documented.
- Non-admin is forbidden.
- Live speaking report totals match booking/payment/wallet records and expose Google sync failure counts.
- AI report separates completed, failed, validation-failed, and blocked usage where stored.
- Sales report totals reconcile gross, commission, net, refunds/disputes markers, and currency.
- Empty reports return zero totals and empty series, not errors.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Keep report dimensions aligned with live speaking booking statuses from `0067`.
- Update `docs/specs/project-overview.md` if admin report capabilities change.
- Update schema SQL only if new report indexes/materialized views are introduced.
- Keep frontend admin reports plan aligned.

## Acceptance Criteria

- Admin can monitor AI costs, sales, and content activity.
- Admin can monitor AI costs, marketplace sales, live speaking bookings, and integration health.
- Report totals are reproducible from persisted domain records for the same filters.
