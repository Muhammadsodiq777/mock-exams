# Backend Test Attempt Lifecycle

> Implement starting, saving, submitting, and expiring student test attempts.

## Depends On

- `0010-backend-test-catalog.md`
- `0014-backend-question-types-answer-keys.md`

## Scope

### In scope

- Start attempt for accessible published test.
- Save answers during attempt.
- Submit attempt.
- Expire attempt when duration is exceeded.
- Prevent answer changes after submit or expiry.

### Out of scope

- Scoring implementation.
- Browser anti-cheat features.

## What To Implement

- Attempt service with status transitions.
- Student answer upsert endpoints.
- Timer calculation from `started_at`, `expires_at`, and duration.
- Access check hook with free/published support first; paid-content checks are attached in `0024-backend-purchases-and-access-control.md`.

## Tests

- Cannot start draft, rejected, archived, or unpublished test.
- Cannot save answer after submit.
- Expired attempt cannot be submitted as normal.
- Saving the same question updates one answer row.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Note where marketplace access rules will plug in later.

## Acceptance Criteria

- Student can save progress and submit once.
- Attempt state is consistent across refreshes.
