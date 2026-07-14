# Backend Notifications Basics

> Add simple in-app notifications for moderation, payment, withdrawal, and AI events.

## Depends On

- `0006-backend-users-roles-permissions.md`

## Scope

### In scope

- Create notifications from backend services.
- List current user's notifications.
- Mark notification as read.
- Store payload JSON for navigation targets.

### Out of scope

- Email, SMS, Telegram, and push notifications.

## What To Implement

- `Notification` entity and repository.
- `NotificationService` used by teacher approval, moderation, payments, withdrawals, and AI.
- Student, teacher, and admin read endpoints.

## Tests

- Notification is created for teacher approval decision.
- User sees only their notifications.
- Mark-read is idempotent.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update notification event list when new producers are added.

## Acceptance Criteria

- Backend modules can emit notifications without duplicating persistence code.
- Frontend can render unread count and notification list.
