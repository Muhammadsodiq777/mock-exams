# Backend Teacher Profiles Approval

> Implement teacher profile lifecycle and admin approval.

## Depends On

- `0006-backend-users-roles-permissions.md`

## Scope

### In scope

- Teacher profile view and update.
- Submit profile for approval.
- Admin approve and reject profile.
- Public teacher profile read endpoint.
- Avatar file reference as nullable metadata; actual upload handling is added in `0008-backend-file-storage-minio.md`.

### Out of scope

- Teacher ratings aggregation.
- Teacher earnings.

## What To Implement

- DTOs for teacher profile create/update/read/moderation.
- Service rules for status transitions: `draft -> pending_approval -> approved/rejected`.
- Reject reason storage.
- Public listing only for approved teachers.
- Nullable avatar field that can be populated after file storage exists.

## Tests

- Teacher cannot publish content until approved.
- Rejected profile can be edited and resubmitted.
- Public endpoint hides non-approved teachers.
- Admin approval records reviewer and timestamp.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update `docs/ERD/platform-architecture.dbml` if profile fields change.

## Acceptance Criteria

- Teacher onboarding is enforceable by backend.
- Admin can approve or reject with reason.
- Teacher dashboard can show profile status.
