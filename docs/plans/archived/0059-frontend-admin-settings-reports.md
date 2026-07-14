# Frontend Admin Settings Reports

> Build admin settings and reporting pages.

## Depends On

- `0034-backend-platform-settings.md`
- `0035-backend-reports-ai-usage.md`

## Context

Settings and reports are admin control-room pages. Settings must expose typed editable business values without leaking secrets, while reports must let admins inspect AI costs, sales, activity, provider failures, and later live speaking metrics with reliable filters.

## Scope

### In scope

- `/admin/settings`.
- `/admin/reports`.
- Commission, AI limit, premium price, withdrawal minimum, moderation, payment, rich-text, and Google schedule settings.
- AI usage, sales, live speaking booking, Google sync, and test activity reports.

### Out of scope

- CSV/PDF export in first pass.
- Editing provider secrets in-browser.
- Building a full BI dashboard.

## Key Decisions

- Settings forms are generated or grouped from typed backend setting schemas; raw JSON editing is avoided unless explicitly required for admin/debug.
- Secret-dependent settings show configured/missing health but never display secret values.
- Reports use backend aggregate DTOs and date filters; frontend does not recalculate financial totals.
- Date/report filters are reflected in URL or stable query keys so refresh/back navigation works.
- Live speaking/Google report sections remain hidden or empty until `0067` data exists.

## What To Implement

- Settings form with typed fields.
- Reports filters and tables/charts.
- Save confirmation and validation display.
- Provider configuration health display for OpenAI, payment, storage, and Google Calendar/Meet mock/enabled states.
- Settings groups for commission, AI limits/model/rubric defaults, payment mode, withdrawal minimum, moderation, rich-text limits, and Google schedule mode.
- Report pages/tabs for AI usage, sales/revenue, test activity, teacher/listing revenue, provider failures, and live speaking when available.
- Validation mapping from backend field errors to setting controls.
- Save/reload/discard behavior for dirty settings.
- Loading, empty, error, and no-permission states.

## Tests

- Invalid commission value is rejected.
- Settings reload after save.
- Report date filters call API with expected params.
- Google integration setting validates required backend configuration and shows disabled/mock states clearly.
- Live speaking reports render booking status and provider sync failures.
- Settings never render secret values.
- Date filters call API with UTC-safe values and preserve selected filters.
- Failed settings save keeps the user's edits visible for correction.

## Documentation Sync

- Keep setting keys and report DTOs aligned.
- Keep rich-text and Google setting keys aligned with `0034` and `0067`.
- Update `docs/frontend-dev.md` if settings/report form conventions are shared.

## Acceptance Criteria

- Admin can configure core platform values and monitor costs/activity.
- Admin can monitor AI costs, marketplace revenue, live speaking revenue, and integration health.
- Admin can safely adjust supported settings and inspect reports without direct database access.
