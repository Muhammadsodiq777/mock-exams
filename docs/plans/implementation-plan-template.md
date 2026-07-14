# Implementation Plan Template

Use this template for feature work, larger fixes, and cross-layer changes. Copy it into `docs/plans/` with a descriptive filename, for example `exam-results-history.md`.

## Summary

- **Feature / change:**
- **Owner:**
- **Status:** Draft
- **Last updated:** YYYY-MM-DD

## Problem

Describe the user or system problem this work solves. Include current behavior, desired behavior, and any known constraints.

## Goals

- Goal 1
- Goal 2
- Goal 3

## Non-Goals

- Explicitly out-of-scope item 1
- Explicitly out-of-scope item 2

## Assumptions

- Assumption 1
- Assumption 2

## Proposed Approach

Describe the implementation approach at a high level. Call out the main modules, components, APIs, data model changes, and integration points.

## Backend Plan

- Controllers / endpoints:
- Services / business rules:
- Persistence / migrations:
- Validation / authorization:
- Error handling:

## Frontend Plan

- Routes / pages:
- Components:
- State / data fetching:
- Forms / validation:
- Loading / empty / error states:

## API Contract

List new or changed API endpoints, request payloads, response payloads, status codes, and error cases.

```http
METHOD /api/path
```

```json
{
  "example": "payload"
}
```

## Data Model

Document entity changes, table changes, indexes, constraints, and migration ordering.

## Test Plan

- Backend unit tests:
- Backend integration tests:
- Frontend component tests:
- Frontend workflow tests:
- Manual verification:

## Rollout Plan

- Migration / deployment order:
- Feature flags or compatibility steps:
- Monitoring or logs to check:
- Rollback considerations:

## Risks and Open Questions

- Risk or question 1
- Risk or question 2

## Implementation Checklist

- [ ] Confirm requirements and edge cases
- [ ] Update backend contract and tests
- [ ] Implement backend changes
- [ ] Update frontend types and API client
- [ ] Implement frontend changes
- [ ] Add or update automated tests
- [ ] Run verification commands
- [ ] Update documentation
