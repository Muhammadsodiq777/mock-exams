# Backend Platform Settings

> Centralize admin-configurable platform settings.

## Depends On

- `0006-backend-users-roles-permissions.md`

## Context

Settings already cover AI, payment, commission, and moderation. The expanded backlog also needs centralized configuration for rich-text sanitization limits and Google Calendar/Meet integration used by live teacher speaking schedules.

## Scope

### In scope

- Settings for platform commission, AI free usage limit, premium price, withdrawal minimum, moderation rules, and payment provider options.
- Settings for AI provider/model/rubric defaults, rich-text content limits, Google Calendar/Meet integration mode, schedule cancellation windows, and live-speaking commission rules.
- Admin read/update endpoints.
- Typed service accessors for other modules.

### Out of scope

- Feature flag service.
- Multi-environment settings UI.
- Secret storage for provider credentials.

## Key Decisions

- Settings are keyed, typed JSON values with backend validation per supported key.
- Services read settings through typed accessors instead of parsing raw JSON throughout the codebase.
- Safe defaults are seeded for local development and tests.
- Secrets stay in environment variables or a future secret store; platform settings may reference non-secret ids, modes, limits, and percentages.
- Settings updates are admin-only and audit `updatedBy`/`updatedAt`.
- Invalid settings fail fast with `VALIDATION_FAILED`; missing required external secret/config returns a clear integration configuration error.

## What To Implement

- Platform settings service.
- JSON schema validation per setting key.
- Seed default values.
- Audit updated_by and updated_at.
- Secret handling boundary for provider secrets: settings may store non-secret toggles and ids, but OAuth client secrets/API secrets must remain environment-managed unless a secure secret store plan is added.
- Typed accessors for rich-text max document size, allowed embed purposes, live-speaking cancellation window, Google integration enabled flag, and live-speaking platform commission.
- Supported setting schemas for platform commission, live-speaking commission, AI limits/model/rubric defaults, payment provider mode, withdrawal minimum, moderation behavior, rich-text limits, and Google Calendar/Meet mode.
- Admin endpoints for list/read/update with optimistic update behavior if needed.
- Seed migration data or startup initializer for default setting values.
- Business errors for unknown setting key, invalid value shape, forbidden update, and missing external configuration when enabling integrations.

## Tests

- Non-admin cannot update settings.
- Invalid setting value is rejected.
- Commission setting is used by wallet service.
- AI free limit setting is used by AI usage service.
- Google integration cannot be enabled unless required environment secrets are configured.
- Rich-text limits are enforced by builder validation.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Document all supported setting keys.
- Update `docs/backend-dev.md` with Google Calendar/Meet local mock configuration when implemented.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if setting persistence changes.
- Keep admin settings frontend and dependent domain plans aligned with supported keys.

## Acceptance Criteria

- Business-critical values can change without code edits.
- Integrations and rich-text behavior have typed, validated configuration instead of scattered constants.
- Secret values are not exposed through settings APIs.
