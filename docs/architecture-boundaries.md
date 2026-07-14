# Architecture Boundaries

This document is the living source of truth for Mock Exams architecture ownership. Feature plans should reference it instead of inventing new backend packages, frontend folder ownership, or cross-cutting integration rules.

## Platform Shape

- Backend stays a Spring Boot modular monolith under `backend/`.
- Frontend stays React + Vite under `frontend/` unless a later migration plan explicitly changes that decision.
- The product source of truth remains `docs/specs/project-overview.md`.
- The target persistence model remains `docs/ERD/platform-architecture.dbml`.
- API route and DTO conventions belong in `docs/API_CONTRACT.md`; this document only defines ownership boundaries.

## Backend Package Model

Backend source lives under `backend/src/main/java/com/mockexams/backend`. New product work should use module packages under that root:

```text
com.mockexams.backend.<module>
```

Each module may contain the same internal package areas when needed:

```text
controller/
service/
service/impl/
repository/
entity/
dto/
mapper/
config/
```

Shared framework plumbing belongs in a shared package only when it is genuinely cross-cutting. A module should own its domain rules, persistence access, DTO mapping, and module-specific configuration. Other modules call it through services, not repositories or entities.

### Module Ownership

| Module | Package area | Owns |
| --- | --- | --- |
| Auth | `auth` | Registration, login, JWT/session behavior, password handling, refresh/logout flows, current-user resolution, authentication filters, and role/permission checks that protect routes. |
| Users | `users` | User profile data, user status, preferences, role assignment records, account lifecycle, soft-delete behavior, and user-facing account management outside login itself. |
| Teachers | `teachers` | Teacher profiles, approval status, public teacher pages, teacher rating summaries, teacher profile moderation inputs, and teacher-owned content relationships. |
| Tests, Questions, Results | `tests` | Test catalog, IELTS test types, sections, reading passages, listening segments, questions, options, answer keys, explanations, categories, tags, publishing state, auto-scoring rules, and result summaries. |
| Attempts | `attempts` | Attempt lifecycle, start/save/submit/expire behavior, student answers, time tracking, answer validation at submission time, and handoff to scoring/result generation. |
| AI | `ai` | Writing submissions, speaking submissions, transcription boundary, AI feedback jobs, AI usage limits, cost accounting, study plan generation, and admin-visible AI usage summaries. |
| Marketplace | `marketplace` | Listings, listing items, purchases, access checks for paid/free content, subscriptions, coupons, promo code redemption, reviews, and marketplace discovery metadata. |
| Payments, Wallets, Withdrawals | `payments` | Payment intents/records, provider callback handling, payment status transitions, disputes, teacher wallets, wallet transactions, platform commission, withdrawal requests, and payout review state. |
| Admin, Moderation, Settings, Reports | `admin` | Admin dashboards, moderation queues, platform settings, complaints, banners, reports, user/teacher/test moderation commands, and administrative read models across modules. |
| Notifications | `notifications` | Notification records, notification preferences when added, delivery status, notification payloads, and domain events converted into user-visible messages. |
| Files | `files` | File metadata, upload/download authorization, storage object keys, file purpose validation, audio/image/document constraints, checksums, and storage-provider adapters. |
| Growth | `growth` | Streaks, leaderboards, referral codes, referral rewards, school accounts, school members, Telegram bot integration boundary, and other acquisition or retention features. |

## Backend Boundary Rules

- Controllers expose HTTP behavior for their module and delegate business decisions to services.
- Services own business invariants and may call other module services for explicit workflows.
- Repositories are private to their module. Do not inject another module's repository directly.
- Entities are private persistence models for their owning module. Cross-module responses use DTOs or service return types.
- Mappers stay close to the owning module and should not become shared dumping grounds.
- Module-specific enums follow the module that owns the lifecycle. For example, `attempt_status` belongs to attempts, while `payment_status` belongs to payments.
- Database migrations may create relationships across modules, but the application code owner remains the module listed above.

## Frontend Folder Model

Frontend source lives under `frontend/src`. New product work should keep route ownership separate from shared infrastructure:

```text
app/
pages/
components/
services/
hooks/
store/
types/
utils/
constants/
```

Route groups own screens and page-specific composition. Shared folders own reusable infrastructure only after at least one product area needs it outside a single route group.

### Route And Folder Ownership

| Area | Route group or folder area | Owns |
| --- | --- | --- |
| App shell | `app`, root routes, layout components | Providers, router setup, protected route wrappers, top-level layout, navigation shell, global loading/error boundaries, and app-wide bootstrap. |
| Student | `/`, `/login`, `/register`, `/dashboard`, `/tests`, `/tests/:id`, `/results/:id`, `/writing`, `/speaking`, `/marketplace`, `/teachers/:id`, `/pricing`, `/profile`; `pages/student` or equivalent | Student dashboard, test discovery, test player, result review, writing/speaking practice, marketplace browsing, pricing, and profile screens. |
| Teacher | `/teacher/*`; `pages/teacher` or equivalent | Teacher dashboard, test list, test builder, reading/listening builder flows, preview/submit flow, earnings, students, coupons, and teacher profile screens. |
| Admin | `/admin/*`; `pages/admin` or equivalent | Admin dashboard, user/teacher/test management, moderation, payments, withdrawals, reports, AI usage, complaints, banners, promo codes, and settings screens. |
| Services and API | `services`, `types` | API client setup, backend service functions, backend-facing DTO types, response normalization, and API error conversion for UI consumption. |
| Hooks and Store | `hooks`, `store` | React Query hooks, local UI hooks, session store, persisted preferences, feature-specific client state, and query key helpers when added. |
| Shared UI | `components` | Reusable layouts, forms, buttons, tables, empty states, dialogs, upload controls, feedback states, and other UI primitives shared by multiple route groups. |
| Utilities and Constants | `utils`, `constants` | Pure formatting helpers, validators, route constants, status labels, option sets, and product constants that are not tied to a single screen. |

## Frontend Boundary Rules

- Pages compose features for a route group; reusable UI moves to `components` only when shared.
- API calls go through `services`, with backend-facing DTOs in `types`.
- React Query hooks belong in `hooks` and should hide service call details from pages.
- Global session state belongs in `store`; server data belongs in React Query.
- Admin, teacher, and student route groups should not import each other's page-level components.
- Shared UI must stay product-neutral. If it knows about payments, tests, AI, or moderation, it belongs to that feature area first.

## Cross-Cutting Boundaries

### Errors

- Backend error shape and HTTP status conventions belong in `docs/API_CONTRACT.md`.
- Backend modules throw or return module-specific failures, then a central error layer converts them to API responses.
- Frontend services normalize backend errors before pages render loading, empty, error, and success states.
- Feature plans should name new error cases, but should not create one-off response shapes.

### Auth And Session

- Backend `auth` owns authentication, token/session lifecycle, and current-user resolution.
- Backend `users` owns account state and profile data used after authentication.
- Authorization checks should be expressed at module service boundaries and HTTP entry points.
- Frontend `app` owns protected route behavior; frontend `store` owns session state; frontend `services` owns attaching auth credentials to requests.

### Files

- Backend `files` owns storage metadata, object keys, file purpose, upload/download authorization, and provider adapters.
- Domain modules reference files by file id and purpose; they do not construct storage keys or call storage providers directly.
- Listening audio, speaking audio, avatars, diagrams, documents, course materials, and banners all pass through the file boundary.
- Exact storage providers are chosen by dedicated implementation plans, not by feature plans that only consume files.

### Payments

- Backend `payments` owns provider integration boundaries, payment records, status transitions, disputes, wallets, commission, and withdrawals.
- Backend `marketplace` owns listings, purchases, subscriptions, coupons, reviews, and access checks, then calls payments for money movement.
- Teacher earnings are recorded through wallet transactions; feature modules must not mutate wallet balances directly.
- Exact payment providers are chosen by dedicated implementation plans.

### AI Integrations

- Backend `ai` owns provider adapters, prompt/job orchestration, usage limits, cost tracking, feedback records, transcription boundary, and study plans.
- Tests and attempts provide submission context; AI owns evaluation and feedback lifecycle.
- Files owns audio storage; AI consumes approved file references for speaking transcription and feedback.
- Exact AI, speech-to-text, and queue providers are chosen by dedicated implementation plans.

## Planning Checklist

Before a future feature plan introduces a package, folder, route group, integration, or shared abstraction, it should answer:

- Which backend module owns the business rule and persistence model?
- Which frontend route group owns the screen?
- Does the work require an API contract update in `docs/API_CONTRACT.md`?
- Does the work require an ERD or migration update?
- Is the feature using the existing files, payments, auth, errors, or AI boundary instead of creating a new one?
