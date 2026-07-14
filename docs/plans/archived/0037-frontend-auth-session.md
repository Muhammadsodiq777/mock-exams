# Frontend Auth Session

> Implement login, registration, and current-user session state.

## Depends On

- `0005-backend-auth-registration-login.md`
- `0036-frontend-app-shell-routing.md`

## Context

Auth drives every role-specific route and API call. The frontend needs token storage, current-user bootstrap, role redirects, and backend error handling that match the API envelope before teacher, student, and admin pages are implemented.

## Scope

### In scope

- Student registration form.
- Teacher registration form.
- Login form.
- Current user bootstrap from `/api/auth/me`.
- Token storage and logout.
- Role-based redirect after login.
- Pending/rejected teacher profile routing.
- Session expiry handling for `401` and role failure handling for `403`.

### Out of scope

- Password reset.
- OAuth.
- Refresh token rotation unless the backend auth plan exposes it.

## Key Decisions

- Store auth API calls in `services`, backend DTOs in `types/backend.ts`, and session state in a small Zustand store or equivalent app state layer.
- Axios auth header injection belongs in the shared API client, not in page components.
- On auth/token errors, clear session and send the user to login; on forbidden, keep session and render forbidden/status UI.
- Role redirects after login should prefer the user's primary action: student dashboard, approved teacher dashboard, pending teacher status/profile, admin dashboard.
- Use backend `fieldErrors` and business `error.code` for form messages rather than matching prose.

## What To Implement

- Auth service methods.
- Auth store with token and current user.
- React Query current-user hook.
- Form validation and backend error display.
- Login, student registration, teacher registration, logout, and current-user service functions.
- Route guards connected to session bootstrap/loading state.
- Token persistence with clear behavior for reload and logout.
- Teacher status page or routing branch for pending/rejected profiles.
- Error normalization for auth forms using shared API client conventions.

## Tests

- Invalid login shows backend error.
- Teacher login routes to teacher dashboard when approved or status page when pending.
- Logout clears token and cached user.
- Expired/invalid token clears session and returns to login.
- Forbidden role does not clear a valid session.
- Duplicate email and validation field errors render next to the correct form fields.

## Documentation Sync

- Update frontend auth notes.
- Mirror auth DTOs in `types/backend.ts`.
- Keep `docs/API_CONTRACT.md` auth/session examples aligned if frontend expectations expose contract gaps.

## Acceptance Criteria

- Student, teacher, and admin sessions can be represented in the UI.
- Protected routes and API calls behave consistently across reload, logout, expired token, and forbidden role states.
