# Backend Test Builder Core

> Implement teacher-owned draft test creation and lifecycle.

## Depends On

- `0007-backend-teacher-profiles-approval.md`
- `0010-backend-test-catalog.md`

## Scope

### In scope

- Create draft test metadata.
- Update test metadata.
- Add, update, reorder, and delete sections.
- Preview draft test for owner.
- Submit for moderation.

### Out of scope

- Reading-specific passages.
- Listening audio segments.
- Question-specific validation.

## What To Implement

- Teacher routes under `/api/teacher/tests`.
- Status transition rules for draft, submitted, rejected, published, and archived.
- Ownership checks.
- Service methods for section ordering.

## Tests

- Only owner teacher can edit draft.
- Published test cannot be edited directly.
- Submit fails when required metadata is missing.
- Reordering sections preserves unique order.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update DBML if test lifecycle fields change.

## Acceptance Criteria

- Approved teacher can create and prepare a test draft.
- Submitted tests become read-only until moderation decision.
