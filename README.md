# Mock Exams

Spring Boot + PostgreSQL backend with a React + TypeScript frontend.

## Run locally

Start local dependencies:

```sh
docker compose up -d
```

Expected local service ports:

- PostgreSQL: `localhost:5433`
- Redis: `localhost:6379`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9001`

Run the backend:

```sh
cd backend
./mvnw spring-boot:run
```

Run the frontend:

```sh
cd frontend
npm run dev
```

Open `http://localhost:5173`.

The frontend defaults to `VITE_API_BASE_URL=/api/v1`, which uses the Vite `/api` proxy to reach the backend on `http://localhost:8080`. Set `VITE_API_BASE_URL=http://localhost:8080/api/v1` only when you want the browser to call the backend directly.

## Useful endpoints

- `GET http://localhost:8080/api/v1/exams`
- `GET http://localhost:8080/api/exams` compatibility route
- `GET http://localhost:8080/actuator/health`

## Environment

Backend local defaults:

- `DATABASE_URL=jdbc:postgresql://localhost:5433/mock_exams`
- `DATABASE_USERNAME=mock_exams`
- `DATABASE_PASSWORD=mock_exams`
- `REDIS_URL=redis://localhost:6379`
- `MINIO_ENDPOINT=http://localhost:9000`
- `MINIO_BUCKET=mock-exams-local`
- `MINIO_ACCESS_KEY=minioadmin`
- `MINIO_SECRET_KEY=minioadmin`
- `JWT_ISSUER=mock-exams-local`
- `JWT_SECRET=dev-only-change-me-local-jwt-secret-32-chars`
- `JWT_ACCESS_TOKEN_TTL=PT1H`
- `JWT_REFRESH_TOKEN_TTL=P7D`
- `CORS_ALLOWED_ORIGINS=http://localhost:5173`
- `OPENAI_ENABLED=false`
- `OPENAI_API_KEY=dev-only-openai-api-key`
- `OPENAI_MODEL=gpt-4.1-mini`
- `OPENAI_BASE_URL=https://api.openai.com/v1`
- `PAYMENT_ENABLED=false`
- `PAYMENT_PROVIDER=mock`
- `PAYMENT_API_KEY=dev-only-payment-api-key`
- `PAYMENT_WEBHOOK_SECRET=dev-only-payment-webhook-secret`

Local development intentionally has safe defaults for disabled integrations. Backend startup validation still fails for invalid URLs, invalid durations, short JWT secrets, and unsafe provider-enable combinations such as `OPENAI_ENABLED=true` with a dev-only key or `PAYMENT_ENABLED=true` with `PAYMENT_PROVIDER=mock`.

Frontend local env examples live in `frontend/.env.example`:

- `VITE_API_BASE_URL` for the versioned backend API base URL
- `VITE_FEATURE_AUTH`
- `VITE_FEATURE_AI`
- `VITE_FEATURE_PAYMENTS`
- `VITE_FEATURE_MARKETPLACE`

## Local data

MinIO should have a `mock-exams-local` bucket for uploaded files. The default `docker compose up -d` command runs the `minio-init` job to create it automatically; if it does not, create it from the MinIO console at `http://localhost:9001` before testing file flows.

Local seed data should include one admin, one teacher, and one student account once auth lands. Keep credentials development-only and document them in `docs/backend-dev.md` when the backend seed is implemented.

Reset local infrastructure data:

```sh
docker compose down -v --remove-orphans
docker compose up -d
```

See `docs/backend-dev.md` and `docs/frontend-dev.md` for fuller local setup notes.

## Quality gates

Run backend verification:

```sh
cd backend
./mvnw test
```

Run frontend verification:

```sh
cd frontend
npm run lint
npm run build
```

Run E2E checks after installing/configuring the selected browser tooling:

```sh
cd frontend
npm run e2e
```

Use `docs/release-readiness-checklist.md` as the MVP release gate. Every checklist item must be
marked `pass`, `fail`, or `exception`; live speaking booking remains blocked until the schedule
domain lands.
