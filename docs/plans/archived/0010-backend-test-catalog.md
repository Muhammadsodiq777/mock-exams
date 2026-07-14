# Backend Test Catalog

> Implement public and student-facing test discovery.

## Depends On

- `0007-backend-teacher-profiles-approval.md`
- `0003-api-contract-conventions.md`

## Scope

### In scope

- Published test listing.
- Filters by type, difficulty, band range, tags, category, language, price, and teacher.
- Test detail read endpoint.
- Hide drafts, rejected tests, archived tests, and unapproved teacher tests.

### Out of scope

- Marketplace purchase logic.
- Attempt creation.

## What To Implement

- Test catalog query service.
- DTOs for test card and test detail.
- Category and tag read endpoints.
- Pagination and sorting.

## Tests

- Draft tests are not returned publicly.
- Teacher filter returns only approved published tests.
- Price and type filters combine correctly.
- Archived tests are hidden.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Keep catalog filters aligned with frontend plan `0041`.

## Acceptance Criteria

- Student can browse available tests.
- Teacher profile page can list that teacher's public tests.
