# Backend User Teacher Management

> Give admins management screens for users and teachers.

## Depends On

- `0006-backend-users-roles-permissions.md`
- `0007-backend-teacher-profiles-approval.md`

## Context

Admins need user and teacher management beyond the approval queue: search users, inspect roles/statuses, suspend or ban unsafe accounts, and understand a teacher's content/earnings references before taking action. These tools must enforce last-admin and account-status rules centrally.

## Scope

### In scope

- Admin user list/detail.
- Admin teacher list/detail.
- Suspend, ban, reactivate users or teachers.
- View teacher content and earnings summary references.

### Out of scope

- Manual wallet adjustment.
- Impersonation.
- Editing another user's password or private content.
- Full audit-log UI unless an audit-log plan is added.

## Key Decisions

- User status changes affect authentication and all protected route access through the existing auth/authorization layer.
- Teacher suspension should hide or block teacher marketplace/public content according to catalog/access rules, while preserving historical purchases/results.
- Prevent banning, deleting, or suspending the last active admin.
- Keep user management DTOs safe: no password hashes, tokens, provider secrets, or private answer bodies.
- Admin filters follow API contract pagination/filter conventions.
- Management services should call domain services for teacher status/content side effects rather than editing unrelated tables directly.

## What To Implement

- Admin management controllers.
- Query filters by status, role, email, teacher status.
- Service rules that prevent banning the last admin.
- Endpoints for user list/detail/status update and teacher list/detail/status moderation/management.
- DTOs for admin user summary/detail, teacher management summary/detail, status update commands, and status-change result.
- Query joins or projections for roles, teacher profile status, content counts, wallet summary references, and recent activity references.
- Business errors for user not found, teacher not found, invalid status transition, last-admin protection, and forbidden admin action.
- Notification hooks for teacher/user status changes where user-visible action is needed.

## Tests

- Admin can suspend teacher and hide teacher listings.
- Non-admin is forbidden.
- Last active admin cannot be banned.
- Status changes are reflected in login/access checks.
- Suspended teacher content is hidden or blocked according to documented catalog rules.
- Admin detail pages do not expose secrets or private submitted answer content.
- Filters and pagination metadata match API contract.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update `docs/specs/project-overview.md` if admin management capabilities change.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL only if status/audit fields change.
- Keep frontend admin teacher/user management screens aligned.

## Acceptance Criteria

- Admin can manage users and teachers without direct database access.
- Management actions are permission-safe, audit-friendly, and cannot remove the last admin.
