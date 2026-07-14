# Backend File Storage MinIO

> Store metadata in PostgreSQL and binary files in MinIO.

## Depends On

- `0004-local-dev-and-infrastructure.md`
- `0006-backend-users-roles-permissions.md`

## Scope

### In scope

- File metadata table support.
- Upload flow for listening audio, speaking audio, avatars, diagrams, banners, and course materials.
- Content-type and size validation by purpose.
- Download or signed URL generation.

### Out of scope

- Video uploads.
- Virus scanning in MVP.

## What To Implement

- `FileStorageService`.
- `FileMetadataService`.
- Controller endpoints for upload and file access.
- MinIO client configuration.
- Validation rules per `file_purpose`.

## Tests

- Invalid content type is rejected.
- Oversized file is rejected.
- File metadata is created only after storage succeeds.
- User cannot access another user's private speaking audio.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update `docs/backend-dev.md` with MinIO setup.

## Acceptance Criteria

- Teacher can upload listening audio.
- Student can upload speaking audio.
- Admin can upload banner images.
