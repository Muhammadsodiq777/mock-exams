# Local Dev and Infrastructure

> Define local services and environment variables for full-stack development.

## Depends On

- `0001-architecture-boundaries.md`
- `0002-database-erd-and-migrations.md`

## Scope

### In scope

- Docker Compose services for PostgreSQL, Redis, and MinIO.
- Backend environment variables for database, JWT, MinIO, Redis, OpenAI, payment provider, and CORS.
- Frontend environment variables for API base URL and feature flags.
- Local seed strategy for admin, teacher, and student accounts.

### Out of scope

- Production deployment.
- Kubernetes or cloud infrastructure.

## What To Implement Later

- Update `docker-compose.yml`.
- Add backend config properties.
- Add MinIO bucket initialization notes.
- Add local run and reset instructions.

## Documentation Sync

- Create `docs/backend-dev.md`.
- Create `docs/frontend-dev.md`.
- Update README local setup.

## Acceptance Criteria

- A developer can start all local dependencies with one Docker Compose command.
- Backend and frontend can run against local services.
- Missing environment variables fail with clear messages.
