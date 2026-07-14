# Backend Marketplace Catalog

> Expose paid and free teacher content listings.

## Depends On

- `0010-backend-test-catalog.md`
- `0011-backend-test-builder-core.md`

## Context

The marketplace is the public storefront for paid/free teacher content beyond the basic test catalog. It must distinguish sellable listings from raw tests, support packs/course materials, and provide a stable discovery contract that payments, purchases, reviews, dashboard revenue, and future live speaking schedules can build on.

## Scope

### In scope

- Marketplace listing CRUD for teachers.
- Listing types: single test, test pack, writing course, speaking pack, vocabulary pack, full mock, subscription plan.
- Listing items for packs and course materials.
- Public marketplace browse and detail endpoints.
- Discovery hooks for live teacher speaking slots once `0067-live-teacher-speaking-schedules.md` is implemented.

### Out of scope

- Payments.
- Reviews.
- Booking lifecycle for live teacher speaking sessions; that belongs to `0067`.

## Key Decisions

- Keep marketplace listing persistence separate from `tests`; a listing may reference one test, many listing items, or file/course material entries.
- Public browse returns only active listings from approved teachers and valid referenced content.
- Teacher-owned listing commands require an approved teacher profile and ownership of referenced tests/files.
- Price is stored on listings for marketplace purchase flows. Live speaking booking prices are snapshots in the schedule domain, not reused listing prices.
- Listing activation is a state/flag transition with readiness validation, not just a free-form update.
- Use standard pageable envelopes and filter metadata from `docs/API_CONTRACT.md`.

## What To Implement

- `MarketplaceListing` and `MarketplaceListingItem` services.
- Ownership checks.
- Active/inactive listing status.
- Validation that paid listings have positive price.
- A clear contract boundary that lets `/marketplace` promote live speaking availability without storing bookings in marketplace listing tables.
- Teacher endpoints for listing CRUD, item management, activation/deactivation, and listing preview.
- Public endpoints for listing search/detail with filters by listing type, teacher, price, tag/category where applicable, language, and search text.
- DTOs for listing summary, listing detail, listing item, teacher-owned listing command, and activation validation summary.
- Repository specifications for active public discovery and teacher-owned management lists.
- Business errors such as `LISTING_NOT_FOUND`, `LISTING_STATUS_CONFLICT`, `LISTING_ITEM_INVALID`, and `TEACHER_APPROVAL_REQUIRED`.

## Tests

- Teacher cannot list another teacher's test.
- Pack requires at least one item before activation.
- Public catalog returns active listings only.
- Free listing can have zero price.
- Live speaking slot cards, if included in marketplace discovery later, only show announced slots from approved teachers.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Keep DBML listing types aligned.
- Cross-reference `0067` when live speaking slots are exposed in marketplace discovery.
- Update `docs/specs/project-overview.md` if sellable content types change.
- Update schema SQL if listing fields, constraints, or indexes change.
- Keep frontend marketplace DTOs aligned in `frontend/src/types/backend.ts`.

## Acceptance Criteria

- Teacher can prepare sellable content listings.
- Student can browse marketplace listings.
- Marketplace discovery can later include live speaking availability without confusing listings and bookings.
- Public discovery never exposes inactive listings, unpublished tests, deleted files, or non-approved teacher content.
