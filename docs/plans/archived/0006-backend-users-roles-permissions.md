# Backend Users Roles Permissions

> Centralize role checks and user lifecycle rules.

## Depends On

- `0005-backend-auth-registration-login.md`

## Scope

### In scope

- Role constants for student, teacher, admin, and future school manager.
- Method or route-level authorization for student, teacher, and admin endpoints.
- User status rules for active, suspended, banned, and deleted users.
- Admin user list and status update endpoints.

### Out of scope

- Fine-grained permission UI.
- Organization-level permissions.

## What To Implement

- Authorization helpers used by services.
- Admin routes for listing and changing user status.
- Business errors for forbidden role and inactive account.
- Audit fields where useful.

## Tests

- Student cannot call teacher-only endpoints.
- Teacher cannot call admin-only endpoints.
- Banned user token is rejected.
- Admin can suspend and reactivate a user.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update architecture notes if new roles are added.

## Acceptance Criteria

- Every protected route has a role rule.
- User status is enforced consistently.
- Permission failures return one stable error shape.
