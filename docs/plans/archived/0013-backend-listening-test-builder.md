# Backend Listening Test Builder

> Add IELTS Listening audio and segment support to the builder.

## Depends On

- `0011-backend-test-builder-core.md`
- `0008-backend-file-storage-minio.md`

## Scope

### In scope

- Attach listening audio to a section.
- Create audio segments with start/end timestamps.
- Optional transcript storage.
- Link questions to listening segments.
- Validate segment timing.

### Out of scope

- Audio waveform generation.
- Speech recognition for uploaded listening materials.

## What To Implement

- Listening segment DTOs.
- Segment CRUD and ordering.
- Validation that segment end is after start.
- Validation that audio file purpose is `listening_audio`.

## Tests

- Segment with invalid time range is rejected.
- Question cannot link to segment from another section.
- Listening test submission fails without audio where required.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Keep file purpose rules aligned with `0008`.

## Acceptance Criteria

- Teacher can upload audio and define listening parts.
- Student test player can request audio metadata and segments.
