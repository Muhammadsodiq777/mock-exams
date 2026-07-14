# Backend Auth Registration Login

> Implement student and teacher-capable registration plus JWT login.

## Depends On

- `0002-database-erd-and-migrations.md`
- `0003-api-contract-conventions.md`

## Scope

### In scope

- Register student users.
- Register teacher users with an initial teacher profile in `draft` or `pending_approval`.
- Login with email and password.
- Password hashing.
- JWT access token issuance.
- `/api/auth/me` current-user response.

### Out of scope

- OAuth.
- Password reset.
- Refresh token rotation unless required by architecture.

## What To Implement

- Entities: `User`, `Role`, `UserRole`, `TeacherProfile`.
- DTOs: `RegisterStudentCDO`, `RegisterTeacherCDO`, `LoginCDO`, `AuthSessionRDO`, `CurrentUserRDO`.
- Services for registration, credential validation, and token creation.
- Controller routes under `/api/auth`.

## Tests

- Register student creates `ROLE_STUDENT`.
- Register teacher creates `ROLE_TEACHER` and teacher profile.
- Duplicate email returns a business error.
- Wrong password does not issue a token.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update auth section in `docs/specs/project-overview.md` if behavior changes.

## Acceptance Criteria

- Student and teacher can register and log in.
- Suspended or banned users cannot log in.
- `/api/auth/me` returns roles and teacher approval status.
