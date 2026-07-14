# Frontend Test List Marketplace

> Build test discovery and marketplace browsing.

## Depends On

- `0010-backend-test-catalog.md`
- `0023-backend-marketplace-catalog.md`
- `0038-frontend-api-client-types.md`

## Context

Students need one coherent discovery experience for official tests, teacher tests, marketplace listings, and later live speaking availability. The UI must keep filters in the URL, preserve API pagination metadata, and make paid/free/access states obvious before checkout.

## Scope

### In scope

- `/tests` list.
- `/marketplace` list.
- Live speaking availability discovery entry point after `0067`.
- Filters by type, price, teacher, difficulty, tags, categories, and language.
- Test and listing detail pages.
- Purchase CTA state for paid content.

### Out of scope

- Payment checkout UI.
- Teacher profile page details beyond link target.
- Live speaking booking calendar internals before `0067` frontend work is implemented.

## Key Decisions

- Use separate routes/views for `/tests` and `/marketplace`, but share filter, card, pagination, and empty-state components where useful.
- Store filters in URL search params so results are shareable and back/forward navigation works.
- Use typed catalog/marketplace services and query keys from `0038`.
- Paid content cards show access state and CTA; they do not create payments directly until checkout/profile/pricing flows are implemented.
- Live speaking availability is visually distinct from self-paced content once exposed.

## What To Implement

- Catalog services and hooks.
- Filter state synced with URL search params.
- Reusable listing/test cards.
- Card treatment that distinguishes self-paced tests/content from scheduled live teacher speaking sessions.
- Test detail and listing detail pages with loading/error/not-found states.
- Filter controls for type, price, teacher, difficulty, tags, categories, language, band range, and search text where API supports them.
- Pagination/sort UI using backend metadata.
- CTA state handling for free start, purchased/open, paid purchase required, unavailable, and teacher-owned content if relevant.

## Tests

- Filters update the query.
- Empty results show an empty state.
- Paid listing shows purchase-required CTA.
- Live speaking cards show date/time, teacher, price, and booking availability only for announced slots.
- URL search params round-trip filters without losing pagination/sort.
- Hidden/draft/rejected teacher content never appears if the API omits it.
- Detail not-found errors render useful recovery actions.

## Documentation Sync

- Keep filter params aligned with API contract.
- Add schedule route/DTO references when `0067` adds live speaking discovery.
- Mirror catalog/listing DTOs in frontend types.
- Update `docs/frontend-dev.md` if shared filter/query conventions are introduced.

## Acceptance Criteria

- Student can find free and paid IELTS content.
- Student can discover live speaking sessions as a separate scheduled offering when available.
- Discovery remains usable with loading, empty, error, and paginated result states.
