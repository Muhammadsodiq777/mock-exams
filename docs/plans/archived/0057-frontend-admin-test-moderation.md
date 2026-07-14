# Frontend Admin Test Moderation

> Build admin UI for reviewing submitted tests.

## Depends On

- `0031-backend-test-moderation.md`

## Context

Moderation must review the exact rich content that students will see, plus answer keys and teacher metadata. Admin preview should reuse the builder/player renderer to avoid approving content that later renders differently.

## Scope

### In scope

- `/admin/moderation`.
- Submitted test queue.
- Reading, Listening, Writing, and Speaking preview as builder support expands.
- Answer key visibility.
- Approve and reject with reason.
- Rich-text rendering, file/embed checks, and validation warnings for submitted tests.

### Out of scope

- Automated moderation scoring.

## Key Decisions

- Admin moderation preview reuses the shared preview renderer so approved content matches teacher preview and student player rendering.
- Admin mode can show answer keys, explanations, validation warnings, teacher metadata, and moderation notes; student/public mode cannot.
- Approve/reject actions call backend mutations and invalidate moderation queue/dashboard queries.
- Reject requires a reason and should preserve the admin's review context after validation failure.
- Missing/unsafe embedded files or failed readiness validation should block approval when the backend reports it.
- The moderation UI should remain read-only for content; teachers fix rejected content in the builder.

## What To Implement

- Moderation services and hooks.
- Test preview components reused from player/builder where reasonable.
- Approval and rejection mutations.
- Rich-text/file safety indicators surfaced from backend preview validation.
- Side-by-side student preview and answer-key/moderation metadata mode.

## Tests

- Reject requires reason.
- Approve removes item from pending queue.
- Answer keys are visible only in admin preview.
- Rich-text passages, tables, diagrams, and explanations render consistently with teacher preview.
- Unsafe or missing embedded file warnings are visible before approval.

## Documentation Sync

- Keep moderation API types aligned.
- Keep preview renderer contract aligned with `0053-frontend-builder-preview-submit.md`.

## Acceptance Criteria

- Admin can review and publish teacher tests safely.
- Admin can verify the real student experience before approving content.
