# API Contract Conventions

> Define backend/frontend HTTP contract rules before endpoints multiply.

## Depends On

- `0001-architecture-boundaries.md`

## Scope

### In scope

- Route prefixes for public, student, teacher, and admin APIs.
- DTO suffix conventions: `CDO`, `UDO`, `RDO`.
- Error envelope with stable business error codes.
- Pagination, sorting, filtering, and validation error shapes.
- Auth header and refresh behavior.

### Out of scope

- Every endpoint in detail.
- OpenAPI UI customization.

## What To Implement Later

- `docs/API_CONTRACT.md`.
- Shared response shapes for list pages, moderation queues, dashboards, and result details.
- Frontend `types/backend.ts` mirrors for all response DTOs.

## Documentation Sync

- Create `docs/API_CONTRACT.md`.
- Update it in every backend plan that exposes endpoints.

## Acceptance Criteria

- Frontend plans can reference one route and error convention.
- Backend plans can name concrete DTOs without inventing response envelopes.
- Validation failures are predictable for forms.
