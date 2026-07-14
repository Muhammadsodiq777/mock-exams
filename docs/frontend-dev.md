# Frontend Development

## Stack

- React 18.3.1
- TypeScript
- Vite
- TanStack React Query v5
- Axios
- React Router, Zustand, and i18n dependencies available for upcoming app features
- Playwright for committed browser smoke/e2e checks

## Local Run

Use a Node version accepted by Vite, for example:

```sh
nvm use
```

Start the backend first, then run:

```sh
cd frontend
npm run dev
```

Open `http://localhost:5173`.

The Vite dev server proxies `/api` requests to `http://localhost:8080`. With the default frontend API base URL, `backendService.listExams()` calls `/api/v1/exams` through that proxy.

## Environment Variables

Vite only exposes browser env variables prefixed with `VITE_`. Copy `frontend/.env.example` to a local env file when you need overrides:

```sh
cd frontend
cp .env.example .env.local
```

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api/v1` when unset | Versioned backend API base URL. Leave unset for the local Vite proxy, or set `http://localhost:8080/api/v1` for direct browser calls. |
| `VITE_FEATURE_AUTH` | `false` | Enables future auth UI when wired. |
| `VITE_FEATURE_AI` | `false` | Enables future AI feedback/study-plan UI when wired. |
| `VITE_FEATURE_PAYMENTS` | `false` | Enables future payment and purchase UI when wired. |
| `VITE_FEATURE_MARKETPLACE` | `false` | Enables future marketplace UI when wired. |

Feature flags accept `true`, `false`, `1`, `0`, `yes`, `no`, `on`, and `off`. Invalid values fail during app startup with a clear error so local misconfiguration is visible.

Use these common API base URL values:

| Scenario | `VITE_API_BASE_URL` |
| --- | --- |
| Normal local development through Vite proxy | Leave unset |
| Direct local backend calls | `http://localhost:8080/api/v1` |
| Deployed environment | The deployed backend origin plus `/api/v1` |

If `VITE_API_BASE_URL` is set to a direct backend origin, ensure the backend `CORS_ALLOWED_ORIGINS` includes `http://localhost:5173`.

## Full-Stack Local Dependencies

Start local backend dependencies from the repository root before running the app:

```sh
docker compose up -d
```

Expected local service ports:

- PostgreSQL: `localhost:5433`
- Redis: `localhost:6379`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9001`

The frontend does not talk to PostgreSQL, Redis, or MinIO directly. It reaches those capabilities through the backend API.

## Local Data Notes

Auth, AI, payments, and marketplace flags are present now so upcoming UI work can be guarded consistently. Do not wire visible UI behavior to a flag until the related feature is implemented.

When auth lands, local seed data should provide:

- `admin@mock-exams.local`
- `teacher@mock-exams.local`
- `student@mock-exams.local`

The shared development password should be documented in `docs/backend-dev.md` when the backend seed is implemented.

For file-related UI testing, MinIO should contain the `mock-exams-local` bucket. The default `docker compose up -d` command runs the `minio-init` job that creates the bucket; if it is missing, create it in the MinIO console before testing upload flows.

## App Shell Routing

The frontend app shell lives in `frontend/src/app/router.tsx` and uses React Router 6 nested route
groups:

| Area | Frontend routes | Backend ownership |
| --- | --- | --- |
| Public | `/`, `/tests`, `/tests/:id`, `/marketplace`, `/marketplace/:id`, `/teachers/:id`, `/pricing` | `/api/v1/public/*` plus student marketplace access helpers when authenticated |
| Auth | `/login`, `/register` | `/api/v1/auth/*` |
| Student | `/dashboard`, `/results/:id`, `/writing`, `/speaking`, `/profile` | `/api/v1/student/*` |
| Teacher | `/teacher/dashboard`, `/teacher/tests`, `/teacher/tests/create`, `/teacher/tests/:id/edit`, `/teacher/schedule`, `/teacher/earnings`, `/teacher/students`, `/teacher/notifications`, `/teacher/profile` | `/api/v1/teacher/*` |
| Admin | `/admin/dashboard`, `/admin/users`, `/admin/teachers`, `/admin/tests`, `/admin/moderation`, `/admin/payments`, `/admin/withdrawals`, `/admin/reports`, `/admin/settings` | `/api/v1/admin/*` |

Route constants are exported from `frontend/src/constants/routes.ts`, and navigation visibility is
configured in `frontend/src/constants/navigation.ts`. Protected routes read the Zustand session in
`frontend/src/store/sessionStore.ts`; login and registration responses persist the bearer access
token, and reload bootstrap uses `GET /api/v1/auth/me` before guarded routes decide whether to render,
redirect, or show forbidden state.

Guard behavior:

- Unauthenticated student, teacher, and admin URLs redirect to `/login`.
- Authenticated users without the required role see the shared forbidden state.
- Teacher dashboard and profile routes are available to all authenticated teachers so pending,
  draft, rejected, or suspended profiles can see approval guidance. Builder, schedule, earnings,
  student, and notification routes require an approved teacher profile and redirect to
  `/teacher/profile` otherwise.
- The teacher dashboard prefers `GET /teacher/dashboard`; while that backend endpoint is absent, the
  frontend composes a limited fallback from existing profile, wallet, review, notification, and
  listing APIs and marks unsupported sections as unavailable.
- Login and registration redirect students to `/dashboard`, admins to `/admin/dashboard`, approved
  teachers to `/teacher/dashboard`, and non-approved teachers to `/teacher/profile`.
- Auth API calls use `/auth/register/student`, `/auth/register/teacher`, `/auth/login`, and
  `/auth/me` through the shared `/api/v1` client. `401` token/session failures clear local session
  state; `403 ACCOUNT_INACTIVE` from `/auth/me` also clears the persisted session so reload
  bootstrap cannot stay loading forever. `403 FORBIDDEN` keeps the session and renders route-level
  forbidden UI.
- Unknown public and authenticated URLs render the shared not-found state.

## API Client Contract Layer

Frontend HTTP calls are centralized under `frontend/src/services/`. Components and pages should not
import Axios directly. Use typed service methods from `backend.service.ts` or `auth.service.ts`, then
place React Query orchestration in hooks.

The shared contract pieces are:

- `services/api.ts`: Axios instance, `VITE_API_BASE_URL` validation, bearer token injection, session
  expiry handling, and envelope helpers for resource, unpaged list, page, dashboard, and command
  responses.
- `services/apiRoutes.ts`: route constants grouped by backend area: `public`, `student`, `teacher`,
  `admin`, and `auth`.
- `services/backend.service.ts`: grouped domain service methods for catalog, attempts, results,
  AI practice, marketplace, teacher, admin, settings, live speaking schedules, and files.
- `types/backend.ts`: backend-facing DTO mirrors plus shared pagination, sort/filter, dashboard,
  command, and business error code types.
- `utils/apiErrors.ts`: normalized backend error helpers that safely fall back for non-standard
  proxy, HTML, null, or legacy error bodies. Forms and pages should branch on `error.code` and
  `fieldErrors`, not human-readable prose.
- `hooks/queryKeys.ts`: stable TanStack Query keys. Include page/filter/sort inputs in keys for any
  list query.

New feature pages should add API calls to the service layer first, export or reuse a query key from
`hooks/queryKeys.ts`, and only then consume the service from a hook or page. If a backend DTO changes,
update `types/backend.ts` in the same plan.

## Frontend Verification and Test Support

Current minimum verification for frontend implementation is:

```sh
cd frontend
npm run lint
npm run build
npm run e2e
```

There is no committed component/unit test harness yet. Do not add Vitest, React Testing Library,
Playwright component tests, or a new `npm test` script as incidental work. The committed Playwright
harness is browser e2e/smoke coverage only; grow it when a feature plan deliberately adopts a journey,
or when a reusable form, state machine, hook, player control, builder control, or admin action modal
has enough logic that lint/build plus browser checks are no longer credible protection.

## E2E Browser Smoke Tests

Run the current Playwright baseline from the frontend folder:

```sh
cd frontend
npm run e2e
```

The committed smoke starts the Vite dev server and mocks `GET /api/v1/public/tests` inside Playwright,
so it does not require a running backend or reset seed data yet. It proves that the Vite app boots,
the public `/tests` route renders, and deterministic API-shaped catalog data reaches the UI.

| Setting | Default | Purpose |
| --- | --- | --- |
| `E2E_FRONTEND_URL` | `http://127.0.0.1:5173` | Frontend URL Playwright opens. Leave unset for the local Vite server. |
| `E2E_START_FRONTEND` | `true` | Set to `false` when an external frontend server is already running at `E2E_FRONTEND_URL`. |
| Backend URL | `http://localhost:8080` via the Vite `/api` proxy | Not required by the current mocked smoke. Future unmocked full-stack specs should use the local backend or an explicit `VITE_API_BASE_URL`. |

If Chromium is missing, install the browser binary explicitly:

```sh
cd frontend
npx playwright install chromium
```

Diagnostics are written under `frontend/test-results/` on failure. The config keeps screenshots and
traces only when a test fails; inspect a trace with:

```sh
cd frontend
npx playwright show-trace test-results/<test-run>/trace.zip
```

The larger critical-flow checklist lives in `frontend/e2e/critical-journeys.manifest.md`. AI,
speech-to-text, payment, file-storage, and Google Calendar/Meet journeys must run only against
mock/sandbox providers. Real OpenAI calls, real payment capture, and real Google Calendar/Meet calls
are not part of automated local e2e. Live speaking schedule UI is now implemented for teacher slot
management, student reservation/payment, and admin support, but the full browser journey still needs
seed/reset data before it should become a runnable e2e spec.

Use this staged approach for frontend verification:

| Change type | Expected verification |
| --- | --- |
| DTO/type/service/query-key changes | `npm run build`; use typed fixtures from `src/test-support/apiFixtures.ts` when writing examples or future tests. |
| Reusable components and UI states | `npm run lint`, `npm run build`, and manual/browser checks for loading, empty, error, success, disabled, and validation states. |
| Stateful pages, forms, players, builder flows, payments, AI status, and admin mutations | `npm run lint`, `npm run build`, plus targeted browser verification for the changed flow. |
| Full journeys across multiple roles or backend state transitions | Add or update Playwright specs, run `npm run e2e`, and keep blocked/gated journeys current in `frontend/e2e/critical-journeys.manifest.md`. |

`src/test-support/apiFixtures.ts` contains contract-shaped helpers for success envelopes, paginated
lists, validation errors, rich-text content, command responses, AI usage, and AI feedback statuses.
Keep those helpers aligned with `types/backend.ts` and the human-readable examples in
`docs/API_CONTRACT.md`. Prefer these helpers over ad hoc object literals in future component, hook,
browser, or documentation examples so role-sensitive and envelope-shaped responses stay consistent.

Frontend verification checklists should cover:

- Loading, empty, error, success, disabled, and validation states for every user-facing form or async panel.
- Auth guard behavior for unauthenticated, forbidden, inactive, approved teacher, and non-approved teacher states when route access changes.
- Query loading/error states, mutation success cache updates, and invalidation of affected query keys.
- Contract-sensitive responses such as student previews hiding answer keys while teacher/admin previews expose permitted authoring metadata.
- AI queued, processing, completed, failed, usage-limit, and provider-failure states.
- Browser storage reconciliation for local drafts and player attempts until backend resume/read endpoints replace it.
- Accessible labels, stable user-facing selectors, and keyboard reachability for interactive controls.

Student discovery and player pages currently use these shared hooks:

- `hooks/useCatalog.ts` for `/public/tests`, `/public/tests/{id}`, categories, and tags.
- `hooks/useMarketplace.ts` for public marketplace browse/detail, student listing access, protected
  items, and idempotent free-access grants.
- `hooks/useStudentProgress.ts` for `/student/progress`, active study plans, and AI usage.
- `hooks/useAttemptLifecycle.ts` for start, single-answer save, submit, and attempt-result reads.
- `hooks/useUrlFilters.ts` for URL-backed test and marketplace filters. Keep backend-supported filter
  names in the URL: tests use `type`, `difficulty`, `bandMin`, `bandMax`, `tag`/`tags`,
  `category`/`categories`, `language`, `price`, and `teacherId`; marketplace uses `type`,
  `teacherId`, `price`, `minPrice`, `maxPrice`, `tag`/`tags`, `category`/`categories`, `language`,
  and `q`.

Teacher test builder create/edit pages live at `/teacher/tests/create` and `/teacher/tests/:id/edit`.
They use `hooks/useTeacherTests.ts` over the shared `backendService.teacher` methods and treat the
backend preview response as the authoritative saved state. The builder uses an explicit save pattern:
metadata changes are local until "Save metadata", section shell changes are local until "Save
section", and create first posts a draft then redirects to the edit route. Browser unload is guarded
while metadata or section shell changes are dirty; in-app navigation should keep using route-level
buttons or links that can add an explicit confirmation before leaving once React Router blockers are
standardized in this app.

The builder preview is shared under `components/test-builder/BuilderPreviewPanel.tsx`. Student mode
must never render answer keys, accepted variants, scores, explanations, moderation notes, or
readiness diagnostics. Answer-key mode may render teacher/admin authoring details from the saved
teacher preview response. Readiness mode is client guidance only: it checks saved metadata, sections,
rich text/passage content, listening audio when relevant, questions, objective answer keys, price,
status, unsaved local edits, and backend field errors, but `POST /teacher/tests/{id}/submit` remains
the final authority. Submit stays disabled while local metadata or section shell edits are dirty;
local draft overlays in preview must be marked as unsaved and must not be presented as server-saved
content.

The shared rich-text wrapper for the builder is intentionally dependency-free. It accepts plain text,
splits non-empty paragraphs into the backend JSON document contract, and sends that document through
the relevant `*Document` field:

```json
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [{ "type": "text", "text": "Paragraph text." }]
    }
  ]
}
```

Clients must not submit browser HTML for builder rich content. Saved previews should render
backend-sanitized `renderedHtml` through `RichContent`; unsaved local preview may render plain-text
blocks only. Student preview mode must hide teacher-only `answerKeys`, accepted variants, scores, and
explanations even when the teacher preview endpoint returns them for authoring or moderation.

Reading builder internals mount inside the shared section shell instead of replacing it. The shell
continues to own section title, instructions, passage text, duration, and explicit section saves;
`components/test-builder/ReadingSectionEditor.tsx` owns reading paragraph rows and question rows only.
Paragraph links in question forms must be selected from already-saved, nonblank paragraph labels in
the active section, and paragraph/question mutations should keep using `useTeacherTests.ts` so the
teacher preview cache remains the authoritative saved state. Objective question types show answer-key
controls, while essay/audio-like response types omit answer-key payloads. Student preview mode must
continue to hide answer keys and explanations; author mode can render them from the teacher preview.
File embeds in reading content must remain backend-file-id based placeholders/notes unless the
backend rich-text contract and upload flow are both wired into a richer toolbar.

Listening builder internals also mount inside the shared section shell. The shell still saves section
title, instructions, passage text, duration, and `audioFileId`; `components/test-builder/ListeningSectionEditor.tsx`
owns teacher audio upload, signed preview playback, segment rows, transcripts, and question rows. Audio
uploads must use `backendService.files.uploadTeacherFile` with `purpose=listening_audio`, then place the
returned file id into the section draft so the existing section save persists it. Segment links in question
forms must be selected only from saved listening segments in the active section; upload failures should stay
local to the audio panel and must not reset unsaved segment or question drafts.

The Reading player stores a local draft under `mock-exams.reading-attempt.{testId}` so refreshes keep
the attempt id and browser draft answers. The Listening player follows the same attempt lifecycle
under `mock-exams.listening-attempt.{testId}` and additionally stores the active section plus local
audio positions. Listening audio always loads through `backendService.files.getDownloadUrl(fileId)`
and refreshes the signed URL when playback fails or the URL approaches expiry; frontend code must not
derive public object-storage URLs from file ids.

Reading and Listening both use the backend as the source of truth for start, save-one-answer, submit,
and result generation. Local drafts exist only because the backend currently has no attempt-detail
endpoint for rehydrating saved answers from the server. Cleared locally persisted answers are blocked
before submit because the backend exposes answer upsert, not answer delete. Submitted, expired, and
status-conflict responses should move the player into a final state and poll/fetch the result where
available. Do not add client calls to invented resume endpoints; replace the local draft
reconciliation only when the backend exposes an authenticated attempt detail/read model.

Result review lives at `/results/:id` and uses `backendService.results.getResult(resultId)` through a
hook. The page renders raw score, max score, estimated band, timing, sections, answer review rows,
correct answers, explanations, and weak-area tags from `ResultDetailRDO` only. It must not recalculate
scoring or infer answer keys from public test detail. Progress context on the result page is read from
the existing `/student/progress` dashboard API.

Writing practice lives at `/writing` and keeps a browser-local plain-text draft under
`mock-exams.writing-practice`. The submitted payload is `WritingSubmissionCDO`; the UI intentionally
does not add rich text because the backend persists `answerText`. The latest local submission id is
stored so refreshes can load `GET /student/writing-submissions/{id}` and
`GET /student/writing-submissions/{id}/feedback`. Queued and processing feedback is refreshed through
the feedback read endpoint, failed feedback retries through `/feedback/retry`, and retry/refresh must
not create duplicate writing submissions. AI usage-limit and provider-failure errors should be shown
distinctly when AI features are enabled.

Profile and pricing surfaces use student purchase/payment APIs for account history. Public pricing
still reads backend marketplace listing prices, while authenticated student access state is derived
from purchase history so active listings show as active even when public listing summaries omit
`accessStatus`.

Speaking practice lives at `/speaking` and keeps recoverable browser-local state under
`mock-exams.speaking-practice`. The page uploads `speaking_audio` through `POST /student/files`,
creates `SpeakingSubmissionCDO`, starts retry-safe transcription through `/transcription`, and starts
feedback only after a completed nonblank transcript is available. Queued transcription and feedback
states poll their read endpoints. Failed transcription retries through `/transcription`; failed
feedback retries through `/feedback/retry`. The UI must keep AI speaking upload practice separate from
live teacher speaking schedules, which are shown as a separate slot list and booking panel on the
same route.

## Design System Primitives

Reusable UI primitives live in `frontend/src/components/ui` and are exported from
`frontend/src/components/ui/index.ts`. New feature pages should import from that barrel rather than
creating local one-off controls.

Use the shared primitives for:

- `Button`, `TextField`, `SelectField`, `TextareaField`, and `Tabs` for interactive controls.
- `Panel`, `PageContainer`, and `PageHeader` for page structure.
- `DataTable`, `AdminTableShell`, `FilterBar`, and `Pagination` for admin/list surfaces.
- `RepeatedItemCard`, `Badge`, `Alert`, `EmptyState`, `Loader`, and `Skeleton` for repeated items and
  loading, empty, success, warning, and error states.
- `Dialog` for modal flows; it manages focus, Escape close, backdrop close, and focus restoration.
- `SplitPane` and `PlayerLayout` for IELTS reading/listening/writing/speaking player surfaces.
- `RichContent` for backend-sanitized HTML via `trustedHtml`, or structured rich content blocks when
  a feature can avoid raw HTML.

Keep generic primitives free of role or domain language. Role-specific copy belongs in pages or
feature components, not in `components/ui`. Prefer dense, scannable layouts with 6-8px radii and the
existing green/neutral palette. Cards are for repeated items and framed tools; avoid nesting cards
inside cards.

Every input should pass label, disabled/loading, help, and error state through the form-field
primitive so `htmlFor`, `aria-describedby`, and `aria-invalid` stay consistent. Tables and list pages
should expose loading, empty, error, and paginated states explicitly before adding success content.

## Packaging Rules

Frontend source folders are role first. Pages belong under `src/pages/`, reusable and feature UI under `src/components/`, API clients under `src/services/`, React Query and stateful helpers under `src/hooks/`, Zustand stores under `src/store/`, backend-facing and domain types under `src/types/`, and support code under `src/utils/` or `src/constants/`.

When any folder becomes too large to scan comfortably, split by purpose inside that folder, such as `pages/student/`, `components/test-builder/`, `services/admin/`, `hooks/attempts/`, or `types/payments/`. Do not create new feature-first trees that mix pages, services, hooks, stores, and types under one feature folder unless a dedicated architecture plan approves that migration.

The current folder inventory and backend package migration map live in
[`docs/package-inventory.md`](package-inventory.md). New frontend work should keep the role-first
shape immediately, even when a backend legacy package near the same feature has not moved yet.

## Reset Local State

Reset Compose-managed backend data from the repository root:

```sh
docker compose down -v --remove-orphans
docker compose up -d
```

Regenerate frontend build artifacts:

```sh
cd frontend
npm run build
```

## Verification

```sh
cd frontend
npm run lint
npm run build
npm run e2e
npm audit
```

Frontend API calls should go through `src/services/backend.service.ts` or `src/services/auth.service.ts`,
with backend-facing DTOs in `src/types/backend.ts`. A quick direct-Axios scan should only find Axios
in the shared API/error utility layer:

```sh
rg -n "from 'axios'|from \"axios\"" frontend/src
```
