# Backend Test Moderation

> Implement admin review queue for submitted teacher tests.

## Depends On

- `0011-backend-test-builder-core.md`
- `0014-backend-question-types-answer-keys.md`

## Context

Teacher-created tests should not reach students until an admin verifies metadata, rich content, audio, questions, answer keys, price, and readiness. Moderation owns final approve/reject transitions and teacher notifications, while builder services own authoring rules.

## Scope

### In scope

- List submitted tests.
- Admin preview with answer keys, rich content, audio metadata, validation warnings, and metadata.
- Approve test.
- Reject test with reason.
- Notify teacher.
- Queue metadata for pending count and oldest submitted item.

### Out of scope

- Automated quality scoring.
- Version comparison.
- Editing teacher content directly from moderation.

## Key Decisions

- Admin moderation routes live under `/api/v1/admin/moderation/tests`.
- Only tests in `submitted` status can be approved or rejected.
- Approval sets visibility to `published`, records reviewer/timestamp, clears rejection note as appropriate, and makes the test visible to public catalog.
- Rejection stores a reason/moderation note, records reviewer/timestamp, notifies teacher, and returns the test to an editable rejected state.
- Admin preview may include answer keys and explanations; student/public preview must not reuse this DTO directly.
- Moderation should run readiness validation again at decision time to catch stale or incomplete content.

## What To Implement

- Moderation query service.
- Admin routes under `/api/admin/moderation/tests`.
- Status transition rules.
- Published timestamp on approval.
- Queue endpoint with filters by type, teacher, submitted date, difficulty, and text search where useful.
- Detail/preview endpoint that returns full submitted content, answer keys, file references, rich-text render fields, and validation findings.
- Approve/reject command DTOs, including required rejection reason and optional internal note if supported.
- Notification producer events for approval and rejection with target paths.
- Business errors for not found, invalid status transition, missing rejection reason, and failed readiness validation.

## Tests

- Only submitted test can be approved.
- Reject requires reason.
- Approval makes test visible in public catalog.
- Teacher receives notification.
- Admin preview includes answer keys while public/student catalog detail does not.
- Re-approving or re-rejecting the same test is rejected as a status conflict.
- Approval fails if referenced files are missing or content readiness no longer passes.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Keep lifecycle docs aligned.
- Update `docs/specs/project-overview.md` if moderation flow changes.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if moderation fields/statuses change.
- Keep frontend admin moderation and builder preview plans aligned.

## Acceptance Criteria

- Admin can safely approve or reject teacher tests before students see them.
- Moderation decisions are auditable, status-safe, and notify the teacher.
