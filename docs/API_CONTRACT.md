# API Contract

This document is the source of truth for backend/frontend HTTP contract conventions in Mock Exams.
Feature plans should reference these conventions instead of inventing route prefixes, DTO names,
response wrappers, error shapes, or pagination behavior.

The current implementation is still an early development starter. Existing exam routes are documented
at the end of this file as transitional compatibility shape; new work should follow the conventions
below.

## Versioning And Route Prefixes

Current API version: `/api/v1`.

All new backend endpoints must live under a versioned route. Compatibility routes under `/api/*`
may remain while the starter app is being migrated, but they are not the target for new frontend work.

| API area | Prefix | Intended users |
| --- | --- | --- |
| Public | `/api/v1/public/*` | Anonymous or authenticated users reading public catalog, public teacher profiles, pricing, landing data, and other public read models. |
| Student | `/api/v1/student/*` | Authenticated student workflows: dashboards, attempts, results, practice, marketplace access, profile self-service. |
| Teacher | `/api/v1/teacher/*` | Authenticated teacher workflows: teacher dashboard, test builder, earnings, coupons, profile, owned content. |
| Admin | `/api/v1/admin/*` | Authenticated admin workflows: moderation, reports, settings, user and teacher management, payments, AI usage. |
| Auth and session | `/api/v1/auth/*` | Registration, login, current user/session, and auth-owned account entry points. |
| Compatibility | `/api/*` | Temporary starter routes only. Do not add new product routes here unless a migration plan explicitly requires it. |

Route names should be resource oriented and plural where the route represents a collection:

```http
GET /api/v1/public/tests
GET /api/v1/student/results/{resultId}
POST /api/v1/teacher/tests
GET /api/v1/admin/moderation/tests
POST /api/v1/auth/refresh
```

Use path parameters for required resource identity and query parameters for optional narrowing,
pagination, sorting, and filtering. Do not encode user roles in DTO names when the route prefix already
defines the role boundary.

## Admin Platform Settings

Admin platform settings endpoints require the `admin` role:

```http
GET /api/v1/admin/settings
GET /api/v1/admin/settings/{key}
PATCH /api/v1/admin/settings/{key}
```

Settings are keyed JSON objects stored in `platform_settings.setting_value`. `PATCH` accepts
`PlatformSettingUDO`:

```json
{
  "value": { "value": 18.50 }
}
```

Responses return `PlatformSettingRDO`:

```json
{
  "key": "marketplace.default_commission_percent",
  "value": { "value": 18.50 },
  "updatedByUserId": 1,
  "updatedAt": "2026-07-10T03:20:00Z"
}
```

Supported setting keys and schemas:

| Key | JSON value shape | Used by |
| --- | --- | --- |
| `marketplace.default_commission_percent` | `{ "value": 25.00 }`, 0-100 | Marketplace wallet earnings. |
| `platform.default_commission_percent` | `{ "value": 25.00 }`, 0-100 | General platform commission default. |
| `live_speaking.platform_commission_percent` | `{ "value": 25.00 }`, 0-100 | Live-speaking commission default. |
| `ai.free_monthly_requests` | `{ "value": 20, "period": "monthly" }` | AI usage free allowance for new period rows. |
| `ai.hard_monthly_requests` | `{ "value": 50, "period": "monthly" }` | AI usage hard cap for new period rows. |
| `ai.default_model` | `{ "provider": "openai", "model": "gpt-4.1-mini" }` | AI provider/model default accessor. |
| `ai.rubric_defaults` | `{ "writingRubricVersion": "...", "speakingRubricVersion": "..." }` | AI rubric default accessors. |
| `ai.premium_monthly_price` | `{ "amount": 0.00, "currency": "UZS" }` | Reserved premium pricing setting. |
| `payments.provider_mode` | `{ "enabled": false, "provider": "mock", "mode": "mock" }` | Payment provider mode metadata. |
| `payments.provider_options` | `{ "defaultProvider": "mock", "allowedProviders": ["mock"] }` | Payment provider options metadata. |
| `payments.default_currency` | `{ "value": "UZS" }` | Default payment currency metadata. |
| `wallet.minimum_withdrawal_amount` | `{ "value": 10000.00, "currency": "UZS" }` | Teacher withdrawal minimum. |
| `moderation.behavior` | `{ "autoHideThreshold": 3, "manualReviewRequired": true }` | Moderation behavior metadata. |
| `rich_text.limits` | `{ "maxDocumentBytes": 65536, "allowedEmbedPurposes": ["passage_image", "diagram", "document"] }` | Rich-text validation. |
| `google.calendar_meet` | `{ "enabled": false, "mode": "disabled", "calendarOwner": "platform" }` | Google Calendar/Meet integration mode. |
| `live_speaking.cancellation_policy` | `{ "studentCancellationWindowHours": 24, "teacherCancellationWindowHours": 2 }` | Live-speaking cancellation windows. |
| `tests.default_language` | `{ "value": "en" }` | Default test language metadata. |

Settings APIs never accept or return provider secrets. OAuth client secrets, payment API keys,
webhook secrets, and OpenAI keys remain environment-managed. Enabling `google.calendar_meet` in
`platform_calendar` or `teacher_oauth` mode requires real Google client configuration in the backend
environment; otherwise the update returns `INTEGRATION_CONFIGURATION_MISSING` (409). Unknown keys
return `UNKNOWN_SETTING` (404), and invalid JSON shapes return `VALIDATION_FAILED` (400).

## DTO Naming

Backend DTOs use suffixes that describe request intent and response shape.

| Suffix | Meaning | Used for |
| --- | --- | --- |
| `CDO` | Create Data Object | Request bodies that create a new resource, for example `TeacherTestCDO`. |
| `UDO` | Update Data Object | Request bodies that mutate an existing resource, for example `TeacherTestUDO`. |
| `RDO` | Response Data Object | Backend responses, including detail, summary, list item, dashboard, queue item, and result view models. |

Rules:

- `CDO` and `UDO` types are request-only. They should not be returned from controllers.
- `RDO` types are response-only. They should be shaped for the frontend and should not expose entities.
- Prefer explicit names when views differ: `TestSummaryRDO`, `TestDetailRDO`, `ResultDetailRDO`,
  `ModerationQueueItemRDO`, `AdminDashboardRDO`.
- IDs are serialized as numbers when the backend id type is numeric. Use strings only for naturally
  string ids, external provider ids, or opaque public identifiers.
- Timestamps are ISO-8601 strings in UTC, for example `2026-06-21T17:00:17.29398Z`.
- Enums are stable uppercase string values, for example `PENDING_REVIEW`, `APPROVED`, `REJECTED`.

Frontend TypeScript mirrors backend-facing DTOs in `frontend/src/types/backend.ts`.

- Keep the same DTO names in TypeScript: `TeacherTestCDO`, `TeacherTestUDO`, `TeacherTestRDO`.
- Frontend service functions may map backend DTOs into UI-specific view models, but pages should not
  invent backend field names.
- When a backend response changes, update the corresponding frontend DTO mirror in the same feature plan.
- Frontend route constants live in `frontend/src/services/apiRoutes.ts` and are grouped by backend
  area: `public`, `student`, `teacher`, `admin`, and `auth`.
- Frontend service methods unwrap standard envelopes through `frontend/src/services/api.ts` helpers:
  single-resource, list/page, dashboard, and command responses.
- Frontend query keys live in `frontend/src/hooks/queryKeys.ts`; list keys include the effective
  filter, pagination, and sort inputs that affect the backend response.

## Success Response Envelopes

New versioned endpoints return a stable JSON envelope instead of bare arrays or ad hoc objects.

Single-resource success:

```json
{
  "data": {
    "id": 42,
    "title": "IELTS Academic Reading Practice 1"
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Command success that has no useful response body:

```json
{
  "data": null,
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Use standard HTTP status codes alongside the envelope:

| Status | Use |
| --- | --- |
| `200 OK` | Successful reads and updates with response data. |
| `201 Created` | Successful creates. Include the created `RDO` in `data`. |
| `202 Accepted` | Async work accepted, such as AI feedback jobs or processing queues. Include a job/status `RDO` in `data`. |
| `204 No Content` | Avoid for new JSON APIs unless a route is intentionally bodyless. Prefer the command envelope for frontend consistency. |

## List And Page Envelopes

Collection endpoints return `data` as an array and put collection metadata under `meta`.

```json
{
  "data": [
    {
      "id": 42,
      "title": "IELTS Academic Reading Practice 1"
    }
  ],
  "meta": {
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 143,
      "totalPages": 8,
      "first": true,
      "last": false
    },
    "sort": [
      {
        "field": "createdAt",
        "direction": "desc"
      }
    ],
    "filters": {
      "status": "PUBLISHED",
      "q": "reading"
    },
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Use this envelope for regular list pages, marketplace discovery, teacher content lists, admin tables,
and student history views. Use zero-based page numbers in requests and responses.

Moderation queues add queue metadata:

```json
{
  "data": [
    {
      "id": 77,
      "type": "TEST",
      "status": "PENDING_REVIEW",
      "submittedAt": "2026-06-21T17:00:17.29398Z"
    }
  ],
  "meta": {
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 14,
      "totalPages": 1,
      "first": true,
      "last": true
    },
    "queue": {
      "pendingCount": 14,
      "oldestSubmittedAt": "2026-06-19T09:12:00Z"
    },
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Dashboard endpoints return a dashboard `RDO` as `data`, with freshness metadata in `meta` when useful:

```json
{
  "data": {
    "summary": {
      "activeStudents": 120,
      "pendingReviews": 14
    },
    "cards": [],
    "series": []
  },
  "meta": {
    "asOf": "2026-06-21T17:00:17.29398Z",
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Result detail endpoints return a detail `RDO` as `data`. Large nested sections should be explicitly
named so the frontend can render tabs without guessing:

```json
{
  "data": {
    "id": 501,
    "attemptId": 401,
    "overallBand": 7.0,
    "sections": [],
    "answers": [],
    "feedback": []
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

## Pagination, Sorting, And Filtering

Use these query parameter names for pageable collection routes:

| Parameter | Meaning | Example |
| --- | --- | --- |
| `page` | Zero-based page number. Default: `0`. | `page=0` |
| `size` | Page size. Default: route-specific, usually `20`. Backend should enforce a maximum. | `size=20` |
| `sort` | Sort field and optional direction. May repeat for multi-sort. | `sort=createdAt,desc&sort=title,asc` |
| `q` | Free-text search term. | `q=reading` |
| `status` | Stable enum status filter when the resource has a status lifecycle. | `status=PUBLISHED` |
| `from` | Inclusive lower datetime or date bound. | `from=2026-06-01T00:00:00Z` |
| `to` | Inclusive upper datetime or date bound. | `to=2026-06-30T23:59:59Z` |

Filtering rules:

- Prefer explicit query parameters for common filters, for example `teacherId`, `testType`, `section`,
  `difficulty`, `paymentStatus`, `approvalStatus`.
- Use UTC ISO-8601 values for datetime filters.
- Repeated parameters represent a logical OR for the same field, for example
  `status=PENDING_REVIEW&status=CHANGES_REQUESTED`.
- Different filter fields combine as logical AND.
- Unknown filter or sort fields should fail with `VALIDATION_FAILED`, not be silently ignored.

Response metadata must echo the effective page, sort, and filter values after defaults are applied so
the frontend can keep table controls and URLs in sync.

## Error Envelope

Errors use a stable envelope with a machine-readable business code. The frontend should branch on
`error.code`, not on prose messages.

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Validation failed.",
    "fieldErrors": [
      {
        "field": "email",
        "code": "NotBlank",
        "message": "Email is required."
      }
    ],
    "details": {
      "form": "registration"
    },
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Error fields:

| Field | Required | Meaning |
| --- | --- | --- |
| `error.code` | Yes | Stable business error code. |
| `error.message` | Yes | Human-readable fallback message. The frontend may replace it with localized copy. |
| `error.fieldErrors` | No | Form validation errors. Present for request-body or query validation failures. |
| `error.details` | No | Structured extra context safe for clients. Do not include secrets or stack traces. |
| `error.requestId` | Yes | Request correlation id for support and logs. |

Validation field error shape:

| Field | Required | Meaning |
| --- | --- | --- |
| `field` | Yes | Dot/bracket path matching frontend form names, for example `email`, `sections[0].title`. |
| `code` | Yes | Stable validation rule code, for example `NotBlank`, `Email`, `Min`, `Max`, `Unique`. |
| `message` | Yes | Human-readable fallback message. |

Baseline business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `VALIDATION_FAILED` | `400` | Request body, query parameter, path parameter, or form validation failed. |
| `AUTHENTICATION_REQUIRED` | `401` | No valid access token or session is available. |
| `TOKEN_INVALID` | `401` | Access token is malformed, unsupported, or cannot be verified. |
| `TOKEN_EXPIRED` | `401` | Access token is valid but expired. |
| `REFRESH_TOKEN_INVALID` | `401` | Refresh failed; frontend should clear session and send the user to login. |
| `ACCOUNT_INACTIVE` | `403` | Authenticated account is suspended, banned, deleted, or otherwise not allowed to access protected routes. |
| `FORBIDDEN` | `403` | Authenticated user lacks the required role or permission. |
| `NOT_FOUND` | `404` | Resource does not exist or is not visible to the caller. |
| `CONFLICT` | `409` | Request conflicts with current state, for example duplicate unique value or stale status. |
| `RATE_LIMITED` | `429` | Request exceeded a configured limit. |
| `INTERNAL_ERROR` | `500` | Unexpected server error. |
| `SERVICE_UNAVAILABLE` | `503` | Dependency, queue, storage, payment, or AI provider is temporarily unavailable. |

Feature plans may add module-specific codes, but they must remain stable uppercase strings and should
use a specific prefix when helpful, for example `ATTEMPT_ALREADY_SUBMITTED`, `PAYMENT_REQUIRED`,
`TEACHER_APPROVAL_REQUIRED`, `AI_USAGE_LIMIT_EXCEEDED`.

## Backend Contract Tests

Controller tests for versioned endpoints should lock a representative success envelope and at least
one failure envelope for each endpoint group. Use backend test support helpers such as
`ApiContractAssertions` to assert `data`, `meta.requestId`, `meta.page` for paginated routes,
`error.code`, `error.requestId`, and validation `fieldErrors` with stable field paths and rule codes.

## Auth And Session

Authenticated requests use the standard bearer token header:

```http
Authorization: Bearer <accessToken>
```

Auth and session conventions:

- Frontend services own attaching the `Authorization` header to authenticated requests.
- Registration and login issue JWT bearer access tokens in `AuthSessionRDO`.
- Access tokens are short-lived. Expiration is reported with `TOKEN_EXPIRED`; malformed or unverifiable
  bearer tokens are reported with `TOKEN_INVALID`.
- Protected student, teacher, and admin routes reject suspended, banned, and deleted users with
  `ACCOUNT_INACTIVE`, even when the bearer token itself is valid.
- On `401` with `AUTHENTICATION_REQUIRED`, `TOKEN_INVALID`, `TOKEN_EXPIRED`, or
  `REFRESH_TOKEN_INVALID`, the frontend should clear session state and route the user to login.
- On `403` with `FORBIDDEN`, the frontend should keep the session but show an authorization failure or
  redirect to the appropriate dashboard.
- Public routes must not require an `Authorization` header, but may use it to personalize optional fields
  if the user is logged in.

Registration, login, and current-user endpoints for the auth registration/login plan:

```http
POST /api/v1/auth/register/student
POST /api/v1/auth/register/teacher
POST /api/v1/auth/login
GET /api/v1/auth/me
```

### Register Student

```http
POST /api/v1/auth/register/student
```

Request body: `RegisterStudentCDO`.

```json
{
  "email": "student@example.com",
  "password": "correct-horse-battery-staple",
  "fullName": "Student Example"
}
```

Success: `201 Created` with `AuthSessionRDO` in the standard `{ data, meta }` envelope. The created
user has the seeded `student` role code.

```json
{
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresAt": "2026-06-21T17:15:17.29398Z",
    "user": {
      "id": 101,
      "email": "teacher@example.com",
      "fullName": "Teacher Example",
      "phone": null,
      "status": "active",
      "preferredLanguage": "en",
      "roles": ["student"]
    }
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

### Register Teacher

```http
POST /api/v1/auth/register/teacher
```

Request body: `RegisterTeacherCDO`.

```json
{
  "email": "teacher@example.com",
  "password": "correct-horse-battery-staple",
  "fullName": "Teacher Example",
  "displayName": "Teacher Example",
  "bio": "IELTS reading and listening teacher."
}
```

Success: `201 Created` with `AuthSessionRDO` in the standard `{ data, meta }` envelope. The created
user has the seeded `teacher` role code and an initial teacher profile. The profile starts in
`pending_approval`.

```json
{
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresAt": "2026-06-21T17:15:17.29398Z",
    "user": {
      "id": 202,
      "email": "teacher@example.com",
      "fullName": "Teacher Example",
      "phone": null,
      "status": "active",
      "preferredLanguage": "en",
      "roles": ["teacher"],
      "teacherProfile": {
        "id": 302,
        "displayName": "Teacher Example",
        "bio": "IELTS reading and listening teacher.",
        "status": "pending_approval"
      }
    }
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

### Login

```http
POST /api/v1/auth/login
```

Request body: `LoginCDO`.

```json
{
  "email": "student@example.com",
  "password": "correct-horse-battery-staple"
}
```

Success: `200 OK` with `AuthSessionRDO` in the standard `{ data, meta }` envelope.

```json
{
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresAt": "2026-06-21T17:15:17.29398Z",
    "user": {
      "id": 101,
      "email": "student@example.com",
      "fullName": "Student Example",
      "phone": null,
      "status": "active",
      "preferredLanguage": "en",
      "roles": ["student"]
    }
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Suspended, banned, or otherwise disabled accounts must not receive an access token.

### Current User

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

Success: `200 OK` with `CurrentUserRDO` in the standard `{ data, meta }` envelope.

```json
{
  "data": {
    "id": 202,
    "email": "teacher@example.com",
    "fullName": "Teacher Example",
    "phone": null,
    "status": "active",
    "preferredLanguage": "en",
    "roles": ["teacher"],
    "teacherProfile": {
      "id": 302,
      "displayName": "Teacher Example",
      "bio": "IELTS reading and listening teacher.",
      "status": "pending_approval"
    }
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Auth-specific business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `EMAIL_ALREADY_REGISTERED` | `409` | Registration email is already used by another account. |
| `AUTHENTICATION_REQUIRED` | `401` | Login email or password is incorrect, or a protected route was called without a bearer token. |
| `ACCOUNT_INACTIVE` | `403` | Account exists but is suspended, banned, deleted, or otherwise not allowed to log in. |
| `TOKEN_INVALID` | `401` | Bearer token is malformed, unsupported, or cannot be verified. |
| `TOKEN_EXPIRED` | `401` | Bearer token is valid but expired. |

Future auth-owned endpoints, such as token refresh and logout, must stay under `/api/v1/auth/*` and
use the same envelope and error conventions when their plans are implemented.

## Role-Protected User Routes

All routes in this section require `Authorization: Bearer <accessToken>`. The bearer token must
resolve to an active user. Role checks use lowercase role codes seeded by the identity migration:
`student`, `teacher`, `admin`, and `school_manager`.

### Student Session

```http
GET /api/v1/student/session
Authorization: Bearer <accessToken>
```

Requires the `student` role. Success: `200 OK` with `CurrentUserRDO` in the standard
`{ data, meta }` envelope. This route is the minimal student-owned session surface for route-level
authorization and should be extended or replaced by richer student dashboard/profile routes in later
plans.

### Teacher Session

```http
GET /api/v1/teacher/session
Authorization: Bearer <accessToken>
```

Requires the `teacher` role. Success: `200 OK` with `CurrentUserRDO` in the standard `{ data, meta }`
envelope. This route is the minimal teacher-owned session surface for route-level authorization and
should be extended or replaced by richer teacher dashboard/profile routes in later plans.

### Teacher Profile

Teacher profile endpoints require the `teacher` role and return `TeacherProfileRDO` in the standard
`{ data, meta }` envelope.

```http
GET /api/v1/teacher/profile
POST /api/v1/teacher/profile
PATCH /api/v1/teacher/profile
POST /api/v1/teacher/profile/submit
```

`POST` uses `TeacherProfileCDO`; `PATCH` uses `TeacherProfileUDO`.

```json
{
  "displayName": "Teacher Example",
  "headline": "IELTS reading coach",
  "bio": "I help students improve reading scores.",
  "avatarFileId": null
}
```

Teacher registration still creates an initial profile in `pending_approval`. A standalone created
profile starts as `draft`. Editable statuses are `draft` and `rejected`; editing a rejected profile
moves it back to `draft` and clears moderation fields. Submit moves `draft` or `rejected` profiles to
`pending_approval`.

`TeacherProfileRDO`:

```json
{
  "id": 302,
  "userId": 202,
  "displayName": "Teacher Example",
  "headline": "IELTS reading coach",
  "bio": "I help students improve reading scores.",
  "avatarFileId": null,
  "status": "pending_approval",
  "ratingAverage": 0.0,
  "ratingCount": 0,
  "createdAt": "2026-06-21T17:00:17.29398Z",
  "updatedAt": "2026-06-21T17:00:17.29398Z"
}
```

`approvedByUserId`, `approvedAt`, and `rejectionReason` are included when set.

Teacher profile business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `TEACHER_PROFILE_NOT_FOUND` | `404` | The teacher profile does not exist or is not publicly visible. |
| `TEACHER_PROFILE_STATUS_CONFLICT` | `409` | The requested transition is not valid for the current profile status. |
| `TEACHER_APPROVAL_REQUIRED` | `403` | Teacher content publishing is blocked until the teacher profile is approved. |

### Teacher Dashboard

The preferred teacher dashboard endpoint requires the `teacher` role and returns dashboard data in the
standard `{ data, meta }` dashboard envelope.

```http
GET /api/v1/teacher/dashboard
```

Until the dedicated backend endpoint is implemented in all environments, the frontend may compose an
interim dashboard from existing teacher profile, wallet, review, notification, and listing APIs.
Interim clients must show unavailable sections as unavailable instead of fabricating counts.

`TeacherDashboardRDO` includes the teacher profile approval summary, owned test status counts,
wallet balance, recent sales, recent attempts, reviews, unread notifications, and optional live
speaking schedule data. Pending, draft, rejected, and suspended teacher profiles may receive limited
dashboard data focused on approval guidance. Empty systems return zero counts and `null` or omitted
optional sections instead of fabricated earnings, attempts, or schedule items.

### Public Teacher Profiles

Public teacher profile endpoints do not require authentication and expose approved profiles only.
They return `TeacherProfilePublicRDO` values and never expose rejection or reviewer metadata.

```http
GET /api/v1/public/teacher-profiles?page=0&size=20
GET /api/v1/public/teacher-profiles/{profileId}
```

The list is sorted by `approvedAt,desc`, caps `size` at `100`, and echoes `status=approved` in
response filters. Non-approved profile detail requests return `TEACHER_PROFILE_NOT_FOUND`.

### Public Test Catalog

Public test catalog endpoints do not require authentication and expose tests available for anonymous
or student browsing.

```http
GET /api/v1/public/tests?page=0&size=20&type=reading&price=free&sort=publishedAt,desc
GET /api/v1/public/tests/{testId}
GET /api/v1/public/test-categories
GET /api/v1/public/test-tags
```

Visibility rules:

- Only `visibility=published` and `deletedAt=null` tests are visible.
- Official/platform tests with `teacherProfileId=null` are visible when published only if the creating
  user has the `admin` role. This keeps teacher-owned tests from becoming public if their teacher
  profile is physically removed and `teacherProfileId` is set to `null`.
- Teacher-owned tests are visible only when the linked teacher profile has `status=approved`.
- Draft, submitted, rejected, archived, deleted, and unapproved-teacher tests are hidden from list and detail responses.

List filters use query parameters and combine different fields with logical AND. Repeated `tag`,
`tags`, `category`, or `categories` values are OR filters within that field. Category and tag filters
use slugs. Category filters match active categories only.

| Parameter | Meaning | Example |
| --- | --- | --- |
| `type` | Test type. Supported values match `test_type`, for example `reading`, `listening`, `full_mock`. | `type=reading` |
| `difficulty` | Exact difficulty label. | `difficulty=intermediate` |
| `bandMin` | Inclusive lower requested IELTS band. Tests match when their estimated range overlaps it. | `bandMin=5.5` |
| `bandMax` | Inclusive upper requested IELTS band. Tests match when their estimated range overlaps it. | `bandMax=7.0` |
| `tag` / `tags` | Tag slug, repeatable or comma-separated. | `tag=academic` |
| `category` / `categories` | Category slug, repeatable or comma-separated. | `category=ielts-reading` |
| `language` | Exact language code. | `language=en` |
| `price` | `free` or `paid`. | `price=paid` |
| `teacherId` | Approved teacher profile id. | `teacherId=302` |

Sort accepts `publishedAt`, `title`, `priceAmount`, `durationMinutes`, and `difficulty` with optional
`asc` or `desc` direction. Default sort is `publishedAt,desc`. Unknown query parameters, unsupported
`type`, unsupported `price`, invalid band ranges, and unknown sort fields or directions return
`VALIDATION_FAILED`.

`TestCardRDO` uses this catalog card shape. `TestDetailRDO` includes the same metadata plus a
student-safe preview tree in `sections`:

```json
{
  "id": 42,
  "title": "IELTS Academic Reading Practice 1",
  "description": "Practice test fixture.",
  "type": "reading",
  "difficulty": "intermediate",
  "estimatedBandMin": 5.5,
  "estimatedBandMax": 7.0,
  "durationMinutes": 60,
  "priceAmount": 0.00,
  "currency": "UZS",
  "free": true,
  "language": "en",
  "publishedAt": "2026-06-21T17:00:17.29398Z",
  "teacher": {
    "id": 302,
    "displayName": "Teacher Example",
    "headline": "IELTS reading coach",
    "ratingAverage": 4.80,
    "ratingCount": 25
  },
  "categories": [
    { "id": 11, "parentId": null, "name": "IELTS Reading", "slug": "ielts-reading", "sortOrder": 10 }
  ],
  "tags": [
    { "id": 21, "name": "Academic", "slug": "academic" }
  ],
  "sections": [
    {
      "id": 44,
      "title": "Reading Passage 1",
      "instructions": "Read and answer the questions.",
      "instructionsContent": {
        "contentDocument": { "version": 1, "type": "doc", "content": [] },
        "contentText": "Read and answer the questions.",
        "renderedHtml": "<p>Read and answer the questions.</p>"
      },
      "passageText": "A plain-text IELTS Reading passage.",
      "passageTextContent": {
        "contentDocument": { "version": 1, "type": "doc", "content": [] },
        "contentText": "A plain-text IELTS Reading passage.",
        "renderedHtml": "<p>A plain-text IELTS Reading passage.</p>"
      },
      "audioFileId": null,
      "sectionOrder": 1,
      "durationMinutes": 20,
      "paragraphs": [],
      "listeningSegments": [],
      "questions": [
        {
          "id": 501,
          "questionType": "multiple_choice",
          "questionText": "Which answer is correct?",
          "questionTextContent": {
            "contentDocument": { "version": 1, "type": "doc", "content": [] },
            "contentText": "Which answer is correct?",
            "renderedHtml": "<p>Which answer is correct?</p>"
          },
          "instructions": "Choose one answer.",
          "instructionsContent": {
            "contentDocument": { "version": 1, "type": "doc", "content": [] },
            "contentText": "Choose one answer.",
            "renderedHtml": "<p>Choose one answer.</p>"
          },
          "questionOrder": 1,
          "relatedParagraphLabel": null,
          "listeningSegmentId": null,
          "options": [
            {
              "id": 601,
              "optionLabel": "A",
              "optionText": "First option",
              "optionTextContent": {
                "contentDocument": { "version": 1, "type": "doc", "content": [] },
                "contentText": "First option",
                "renderedHtml": "<p>First option</p>"
              },
              "optionOrder": 1
            }
          ]
        }
      ]
    }
  ]
}
```

`teacher` is `null` for official/platform tests. Category arrays include active categories only, even
when a test still has inactive category links. Hidden or missing detail requests return
`TEST_NOT_FOUND`. Public/student-facing detail never returns question scores, answer keys, normalized
answers, accepted variants, explanations, or listening transcripts; those are reserved for
teacher/admin builder, moderation, or later result-review contexts.

### Marketplace Catalog

Marketplace catalog endpoints expose sellable teacher content listings separately from raw tests.
Public discovery returns only `active` listings owned by approved teachers and backed by valid
published tests or existing course-material/document files. Paid public listing detail does not expose
protected item test/file identifiers; authenticated student item reads and protected course-material
downloads go through the purchase/access boundary. Live speaking availability can be promoted by
marketplace discovery later, but live bookings remain in the schedule domain rather than marketplace
listing tables.

Public endpoints:

```http
GET /api/v1/public/marketplace
GET /api/v1/public/marketplace/{listingId}
```

Teacher endpoints require the `teacher` role and an approved teacher profile:

```http
GET /api/v1/teacher/marketplace/listings
POST /api/v1/teacher/marketplace/listings
GET /api/v1/teacher/marketplace/listings/{listingId}
PATCH /api/v1/teacher/marketplace/listings/{listingId}
DELETE /api/v1/teacher/marketplace/listings/{listingId}
POST /api/v1/teacher/marketplace/listings/{listingId}/items
PATCH /api/v1/teacher/marketplace/listings/{listingId}/items/{itemId}
DELETE /api/v1/teacher/marketplace/listings/{listingId}/items/{itemId}
GET /api/v1/teacher/marketplace/listings/{listingId}/activation
POST /api/v1/teacher/marketplace/listings/{listingId}/activate
PATCH /api/v1/teacher/marketplace/listings/{listingId}/deactivate
GET /api/v1/teacher/marketplace/listings/{listingId}/preview
```

Student marketplace access endpoints require the `student` role:

```http
GET /api/v1/student/purchases
GET /api/v1/student/purchases/{purchaseId}
POST /api/v1/student/payments
GET /api/v1/student/payments
GET /api/v1/student/payments/promo-quote?listingId=501&promoCode=SUMMER10
GET /api/v1/student/payments/{paymentId}
POST /api/v1/student/marketplace/listings/{listingId}/free-access
GET /api/v1/student/marketplace/listings/{listingId}/access
GET /api/v1/student/marketplace/listings/{listingId}/items
```

`POST /api/v1/student/marketplace/listings/{listingId}/free-access` idempotently grants access only
for active zero-price listings and returns `PurchaseRDO`. Paid listings cannot be self-granted by
students; later payment confirmation calls the internal access grant boundary with source metadata.
Duplicate grants for the same student/listing return the existing purchase row. `GET
/api/v1/student/marketplace/listings/{listingId}/items` returns protected `MarketplaceListingItemRDO`
rows only when the listing is free, purchased, or covered by an active subscription window.

Teacher wallet endpoints require the `teacher` role and use the current teacher profile only:

```http
GET /api/v1/teacher/wallet
GET /api/v1/teacher/wallet/transactions?page=0&size=20
```

Paid marketplace payment confirmation records one teacher earning transaction after the purchase grant
is created or found. Earnings use the `marketplace.default_commission_percent` platform setting, which
defaults to `25.00` when the setting is absent. Each earning row stores gross, platform commission, net,
currency, source metadata, purchase id, and payment id so later setting changes do not rewrite history.
Duplicate purchase/payment confirmations return the existing earning row and do not change the wallet
balance twice. Free listing purchases do not create wallet transactions.

`TeacherWalletRDO`:

```json
{
  "id": 9101,
  "teacherProfileId": 22,
  "balanceAmount": 15000.00,
  "pendingAmount": 0.00,
  "availableAmount": 15000.00,
  "currency": "UZS",
  "createdAt": "2026-07-02T10:20:00Z",
  "updatedAt": "2026-07-02T10:20:00Z"
}
```

`TeacherWalletTransactionRDO`:

```json
{
  "id": 9201,
  "walletId": 9101,
  "teacherProfileId": 22,
  "purchaseId": 7001,
  "paymentId": 8101,
  "withdrawalRequestId": null,
  "transactionType": "earning",
  "sourceType": "marketplace_purchase",
  "sourceReference": "payment:8101",
  "grossAmount": 20000.00,
  "platformCommissionAmount": 5000.00,
  "netAmount": 15000.00,
  "currency": "UZS",
  "description": "Marketplace purchase earning",
  "createdAt": "2026-07-02T10:20:00Z"
}
```

`transactionType` is one of `earning`, `commission`, `withdrawal`, `refund`, or `adjustment`.
`sourceType` currently uses `marketplace_purchase`, `live_speaking_booking`, and
`withdrawal_request`, with supported values for `refund` and `adjustment`. Wallet business errors include
`WALLET_CURRENCY_MISMATCH`, `WALLET_INSUFFICIENT_BALANCE`, and `PLATFORM_SETTING_INVALID`.

### Reviews And Ratings

Students review tests they actually interacted with. A review may be created only when the student
has a `granted` marketplace purchase for a listing referencing the test (directly or through a
listing item) or a `submitted` attempt for that test. In-progress or expired attempts do not grant
eligibility. Completed live speaking sessions are tracked in the schedule domain but are not yet a
review eligibility source. Teacher rating aggregates
(`teacher_profiles.rating_average`, `rating_count`) are derived from reviews of the teacher's tests
and are recalculated inside the same transaction as every review create, update, hide, unhide, and
delete, over "countable" reviews: not hidden by moderation and not attached to a soft-deleted test.

Student endpoints (require the `student` role):

```http
POST /api/v1/student/tests/{testId}/reviews
PATCH /api/v1/student/reviews/{reviewId}
```

`ReviewCDO` and `ReviewUDO` share the same shape: required integer `rating` between `1` and `5`
and an optional `comment` up to 2000 characters. Create returns `201` with a `StudentReviewRDO`;
one review per student per test is enforced, and a repeat create returns `REVIEW_ALREADY_EXISTS`
instead of silently overwriting. Changing a review goes through the explicit `PATCH` endpoint,
which only resolves the caller's own reviews. Reviews target published, visible tests; drafts,
deleted tests, and unapproved-teacher tests return `TEST_NOT_FOUND`.

```json
{
  "id": 9301,
  "testId": 42,
  "rating": 4,
  "comment": "Solid practice test.",
  "createdAt": "2026-07-05T10:00:00Z",
  "updatedAt": "2026-07-05T10:00:00Z"
}
```

Public endpoints (no authentication) return paginated `PublicReviewRDO` values containing only safe
display information: no emails and no student ids, only the student display name.

```http
GET /api/v1/public/tests/{testId}/reviews?rating=5&from=2026-07-01T00:00:00Z&page=0&size=20
GET /api/v1/public/teacher-profiles/{profileId}/reviews?testId=42&rating=5&page=0&size=20
```

```json
{
  "id": 9301,
  "testId": 42,
  "testTitle": "IELTS Academic Reading Practice 1",
  "teacherProfileId": 302,
  "rating": 4,
  "comment": "Solid practice test.",
  "studentDisplayName": "Student Example",
  "createdAt": "2026-07-05T10:00:00Z"
}
```

Public lists exclude hidden reviews and reviews of deleted tests, sort by `createdAt,desc`, cap
`size` at `100`, and support `rating`, `from`, and `to` filters (`testId` additionally on the
teacher route). Hidden or missing tests return `TEST_NOT_FOUND`; non-approved teacher profiles
return `TEACHER_PROFILE_NOT_FOUND`.

Teacher endpoints (require the `teacher` role and an existing teacher profile) are read-only views
over reviews of the teacher's own content; teachers cannot edit or moderate student comments:

```http
GET /api/v1/teacher/reviews?testId=42&rating=4&page=0&size=20
GET /api/v1/teacher/reviews/summary
```

The list returns `TeacherReviewRDO` values (review id, test id/title, rating, comment, student
display name, created at) filtered by the same countable-review rules as the public aggregate. The
summary returns `TeacherReviewSummaryRDO` with the current `ratingAverage` (2 decimal places) and
`ratingCount`.

Admin moderation endpoints (require the `admin` role):

```http
GET /api/v1/admin/reviews?teacherProfileId=302&testId=42&rating=1&hidden=true&page=0&size=20
POST /api/v1/admin/reviews/{reviewId}/hide
POST /api/v1/admin/reviews/{reviewId}/unhide
DELETE /api/v1/admin/reviews/{reviewId}
```

The moderation list returns `AdminReviewRDO` values that include `studentUserId`,
`studentDisplayName`, `hidden`, `hiddenAt`, and `hiddenByUserId`. Hide and unhide are idempotent
and return the updated moderation item; delete removes the review permanently and returns the
command envelope with `data: null`. All three recalculate the affected teacher aggregate in the
same transaction.

Review business errors:

| Code | Status | Meaning |
| --- | --- | --- |
| `REVIEW_NOT_ELIGIBLE` | `403` | The student has neither a granted purchase nor a submitted attempt for the test. |
| `REVIEW_ALREADY_EXISTS` | `409` | The student already reviewed this test; use the update endpoint. |
| `REVIEW_NOT_FOUND` | `404` | Review does not exist or does not belong to the caller. |
| `TEST_NOT_FOUND` | `404` | Test is missing, draft, deleted, or otherwise not publicly visible. |
| `TEACHER_PROFILE_NOT_FOUND` | `404` | Teacher profile is missing or not approved (public route), or the caller has no profile (teacher routes). |
| `VALIDATION_FAILED` | `400` | Rating outside 1-5, comment over 2000 characters, or invalid filters. |

### Withdrawals

Teachers turn available wallet balance into payout requests that admins review. Creating a request
reserves the amount on the wallet (`balanceAmount` decreases, `pendingAmount` increases). Rejection
and teacher cancellation release the reservation. Admin approval settles the reservation and records
one idempotent `withdrawal` ledger transaction with `sourceType=withdrawal_request` and
`sourceReference=withdrawal:{withdrawalRequestId}`; duplicate admin submissions cannot settle the
same request twice. The idempotency lookup for the ledger transaction is re-checked after the
wallet's row lock is acquired, so two concurrent approvals of the same request cannot both settle:
the winner records the transaction, and the loser fails with `WITHDRAWAL_INVALID_STATUS` (409)
instead of a raw constraint-violation error. The MVP does not automate bank transfers: after approval
an admin marks the request paid once the payout is completed outside the platform.

Status transitions are explicit: `requested → approved | rejected | cancelled` and
`approved → paid`. Any other transition returns `WITHDRAWAL_INVALID_STATUS`.

The minimum withdrawal amount comes from the `wallet.minimum_withdrawal_amount` platform setting
(seeded at `10000.00`, defaults to `10000.00` when the setting row is absent).

Teacher endpoints require the `teacher` role and operate on the current teacher profile only.
Another teacher's withdrawal is never visible and returns `WITHDRAWAL_NOT_FOUND`:

```http
POST /api/v1/teacher/wallet/withdrawals
GET /api/v1/teacher/wallet/withdrawals?status=requested&page=0&size=20
GET /api/v1/teacher/wallet/withdrawals/{withdrawalRequestId}
POST /api/v1/teacher/wallet/withdrawals/{withdrawalRequestId}/cancel
```

Admin endpoints require the `admin` role:

```http
GET /api/v1/admin/withdrawals?status=requested&teacherProfileId=22&page=0&size=20
GET /api/v1/admin/withdrawals/{withdrawalRequestId}
POST /api/v1/admin/withdrawals/{withdrawalRequestId}/approve
POST /api/v1/admin/withdrawals/{withdrawalRequestId}/reject
POST /api/v1/admin/withdrawals/{withdrawalRequestId}/mark-paid
```

`WithdrawalRequestCDO` (teacher create):

```json
{
  "amount": 20000.00,
  "currency": "UZS",
  "payoutDetails": {
    "method": "card",
    "cardNumber": "8600123412341234",
    "cardHolder": "Teacher Name"
  }
}
```

`currency` is optional and defaults to the wallet currency. `payoutDetails` is validated by a backend
schema: `method` must be `card` (with `cardNumber` of 12-19 digits and `cardHolder`) or
`bank_transfer` (with `bankName`, `accountNumber` of 6-34 letters/digits, and `accountHolder`).
Unknown fields are rejected. The full `cardNumber`/`accountNumber` is what create-flow validation
checks and what is persisted; every RDO returned afterwards (teacher list/detail and admin
queue/detail) masks it down to the last 4 characters (`"**** **** **** 1234"` for cards, `"…6789"`
for bank accounts) so the full number is never sent back over the API, logged, or included in
notification payloads. `method`, `cardHolder`/`bankName`/`accountHolder` stay readable.

`WithdrawalRejectionCDO` (admin reject, `reason` required):

```json
{
  "reason": "Payout details could not be verified."
}
```

`WithdrawalRequestRDO` (teacher list/detail; `payoutDetails` is masked):

```json
{
  "id": 9301,
  "teacherProfileId": 22,
  "requestedAmount": 20000.00,
  "currency": "UZS",
  "status": "requested",
  "payoutDetails": {
    "method": "card",
    "cardNumber": "**** **** **** 1234",
    "cardHolder": "Teacher Name"
  },
  "reviewedByUserId": null,
  "reviewedAt": null,
  "rejectionReason": null,
  "paidAt": null,
  "createdAt": "2026-07-03T10:20:00Z",
  "updatedAt": "2026-07-03T10:20:00Z"
}
```

`AdminWithdrawalRequestRDO` (admin queue item/detail) adds `teacherUserId` and `teacherDisplayName`
to the same shape. Admin decisions persist `reviewedByUserId`, `reviewedAt`, and, for rejections, the
required `rejectionReason`.

The owning teacher receives one `withdrawal` notification for each status change (`requested`,
`approved`, `rejected`, `paid`, `cancelled`) with the withdrawal id, status, amount, and currency in
the payload.

Withdrawal business errors: `WITHDRAWAL_BELOW_MINIMUM` (400), `WITHDRAWAL_PAYOUT_DETAILS_INVALID`
(400), `WITHDRAWAL_INVALID_STATUS` (409), `WITHDRAWAL_NOT_FOUND` (404), plus wallet errors
`WALLET_INSUFFICIENT_BALANCE` (409) and `WALLET_CURRENCY_MISMATCH` (409). A missing rejection reason
returns `VALIDATION_FAILED` (400).

### Payment Disputes

Students raise a structured support case against one of their own payments; admins review the queue
and record a decision. A dispute is a support record only: opening one, or an admin deciding it, never
changes the underlying payment's own `status`, grants/revokes marketplace access, or touches the
teacher wallet ledger. `TeacherWalletService.recordMarketplacePurchaseRefund` exists for a future
refund plan but is intentionally not called from this flow; a `refunded` dispute decision is a
support-facing status marker, not an automated refund.

Only `paid` or `failed` payments can be disputed: `paid` covers charges the student wants investigated
(wrong amount, duplicate charge, service not delivered), and `failed` covers charge attempts the
student believes were incorrectly declined. `pending` payments have no settled outcome yet and
`cancelled`/`refunded` payments have no student-facing state left to dispute; creating a dispute for
either returns `PAYMENT_DISPUTE_STATUS_NOT_ALLOWED` (409). A payment may have at most one dispute in a
non-terminal status (`open` or `under_review`) at a time; opening another while one is pending returns
`PAYMENT_DISPUTE_ALREADY_OPEN` (409). Once a dispute reaches a terminal status
(`refunded`, `rejected`, `resolved`) a new dispute can be opened for the same payment if the problem
persists.

Student endpoints require the `student` role and are scoped to the caller's own payments/disputes;
another student's payment or dispute is never visible and returns `PAYMENT_NOT_FOUND` or
`PAYMENT_DISPUTE_NOT_FOUND` respectively, the same not-found-for-non-owner pattern used by withdrawals
and payments:

```http
POST /api/v1/student/payment-disputes
GET /api/v1/student/payment-disputes?status=open&page=0&size=20
GET /api/v1/student/payment-disputes/{disputeId}
```

`PaymentDisputeCDO`:

```json
{
  "paymentId": 8101,
  "reason": "Charged twice",
  "description": "Please investigate the duplicate charge on my card statement."
}
```

`reason` is required, up to 160 characters; common values are short freeform labels such as
"Charged twice", "Wrong amount", "Service not delivered", or "Charge failed but money left my
account" — the field is not restricted to a fixed enum. `description` is optional, up to 2000
characters.

`PaymentDisputeRDO` (student create/list/detail):

```json
{
  "id": 9401,
  "paymentId": 8101,
  "paymentStatus": "paid",
  "paymentTargetType": "marketplace_listing",
  "paymentTargetLiveSpeakingBookingId": null,
  "status": "open",
  "reason": "Charged twice",
  "description": "Please investigate the duplicate charge on my card statement.",
  "resolutionNote": null,
  "reviewedByUserId": null,
  "reviewedAt": null,
  "createdAt": "2026-07-06T10:20:00Z",
  "updatedAt": "2026-07-06T10:20:00Z"
}
```

`paymentTargetLiveSpeakingBookingId` surfaces the payment's own target column. It is populated for
live speaking booking payments and remains `null` for marketplace listing payments.

Admin endpoints require the `admin` role:

```http
GET /api/v1/admin/payment-disputes?status=open&paymentStatus=paid&teacherProfileId=22&studentUserId=101&reason=charged&from=2026-07-01T00:00:00Z&to=2026-07-08T00:00:00Z&page=0&size=20
GET /api/v1/admin/payment-disputes/{disputeId}
POST /api/v1/admin/payment-disputes/{disputeId}/decision
```

The queue supports the standard page envelope sorted by `createdAt` descending, with `page`
(default `0`) and `size` (default `20`, maximum `100`). Filters: `status` (dispute status),
`paymentStatus`, `from`/`to` (dispute `createdAt` window), `teacherProfileId` (via the payment's
marketplace listing), `studentUserId`, and `reason` (case-insensitive substring match). All filters
are optional and echoed in `meta.filters`; an unsupported `status` or `paymentStatus` returns
`VALIDATION_FAILED`.

`AdminPaymentDisputeRDO` (admin queue item/detail) adds payment and party context to the student
shape:

```json
{
  "id": 9401,
  "paymentId": 8101,
  "paymentStatus": "paid",
  "paymentAmount": 25000.00,
  "paymentCurrency": "UZS",
  "paymentTargetType": "marketplace_listing",
  "paymentTargetListingId": 501,
  "paymentTargetLiveSpeakingBookingId": null,
  "studentUserId": 101,
  "studentDisplayName": "Student Example",
  "teacherProfileId": 22,
  "teacherDisplayName": "Teacher Example",
  "status": "open",
  "reason": "Charged twice",
  "description": "Please investigate the duplicate charge on my card statement.",
  "resolutionNote": null,
  "reviewedByUserId": null,
  "reviewedAt": null,
  "createdAt": "2026-07-06T10:20:00Z",
  "updatedAt": "2026-07-06T10:20:00Z"
}
```

`teacherProfileId`/`teacherDisplayName` are `null` when the payment has no marketplace listing target
(for example a `legacy` target type).

`PaymentDisputeDecisionCDO`:

```json
{
  "status": "resolved",
  "resolutionNote": "Confirmed single charge with the payment provider; explained to the student."
}
```

`status` must be one of `under_review`, `refunded`, `rejected`, or `resolved`; `open` is never a valid
decision target because it is only the initial state a student creates. `refunded`, `rejected`, and
`resolved` are terminal: a dispute already in one of these statuses cannot be decided again and
returns `PAYMENT_DISPUTE_INVALID_STATUS_TRANSITION` (409). Deciding into a terminal status without a
non-blank `resolutionNote` returns `VALIDATION_FAILED` (400), mirroring the withdrawal rejection-reason
requirement. A `under_review` decision does not require a note; the admin queue can move a dispute
into `under_review` before terminal resolution.

The admin decision records `reviewedByUserId` and `reviewedAt`, and the student who opened the dispute
receives one `payment` notification for every status change (including the initial `open` on create)
with the dispute id, payment id, status, and resolution note (when present) in the payload. Repeating a
non-terminal decision with the dispute's current status (for example `under_review` to
`under_review`) does not send another status-change notification.

Payment dispute business errors: `PAYMENT_DISPUTE_STATUS_NOT_ALLOWED` (409, payment status is not
`paid`/`failed`), `PAYMENT_DISPUTE_ALREADY_OPEN` (409, a non-terminal dispute already exists for the
payment), `PAYMENT_DISPUTE_NOT_FOUND` (404), `PAYMENT_DISPUTE_INVALID_STATUS_TRANSITION` (409,
terminal dispute decided again), `PAYMENT_NOT_FOUND` (404, missing or not-owned payment on create),
and `VALIDATION_FAILED` (400, unsupported status filter/decision status or missing resolution note).

Live speaking booking dispute context uses `paymentTargetLiveSpeakingBookingId` to identify the paid
booking. Disputes remain support records and do not automatically cancel sessions, refund payments,
or mutate teacher wallet ledger entries.

### Promo Codes

Teachers and admins manage discount codes that students apply during payment creation. A promo code
holds exactly one discount shape: `discountPercent` (percent of the listing price) or
`discountAmount` plus `currency` (fixed amount). Codes match case-insensitively after normalization
while the display code keeps its original casing; `SUMMER10` and `summer10` are the same code and
cannot coexist. Teacher-owned codes apply only to that teacher's listings. Admin codes created
without `teacherProfileId` are platform-wide and apply to any payable listing; admin codes created
with `teacherProfileId` behave like that teacher's codes.

Promo validation runs inside payment creation before the provider checkout is created. The usage
limit check and the redemption insert are transactional and guarded by an atomic conditional
increment, so concurrent redemptions cannot exceed `usageLimit`. One redemption row is recorded per
promo code and payment (idempotent per payment); usage is consumed when the checkout payment is
created. If that payment later transitions to `failed` or `cancelled` on a provider webhook, the
consumed usage is released in the same transaction as the status change: `usedCount` is atomically
decremented (never below zero) and the redemption row is deleted, so the same student can redeem the
code again and it no longer blocks other students under `usageLimit`. Release only fires on the
payment's first transition out of `pending`, so a duplicate `failed`/`cancelled` webhook for the same
payment is a no-op and cannot double-release. `paid` payments never release usage. Redemption rows
snapshot the discount amount and currency for auditing. Fixed discounts never reduce the payable
amount below zero: when the discount covers the full price, the backend creates a zero-amount payment
that is marked `paid` immediately without any provider call, grants marketplace access through the
internal purchase boundary, and records no teacher earning. Purchase rows snapshot the amount the
student actually paid, so wallet earnings for discounted payments use the discounted gross.

Teacher endpoints require the `teacher` role and operate on the current teacher profile only.
Another teacher's code is never visible and returns `PROMO_CODE_NOT_FOUND`:

```http
POST /api/v1/teacher/promo-codes
GET /api/v1/teacher/promo-codes?active=true&page=0&size=20
GET /api/v1/teacher/promo-codes/{promoCodeId}
PATCH /api/v1/teacher/promo-codes/{promoCodeId}
```

Admin endpoints require the `admin` role and manage platform-wide or teacher-scoped codes:

```http
POST /api/v1/admin/promo-codes
GET /api/v1/admin/promo-codes?teacherProfileId=22&active=true&page=0&size=20
GET /api/v1/admin/promo-codes/{promoCodeId}
PATCH /api/v1/admin/promo-codes/{promoCodeId}
```

List endpoints return the standard page envelope sorted by `createdAt` descending with `page`
(default `0`) and `size` (default `20`, maximum `100`); effective `active` and `teacherProfileId`
filters are echoed in `meta.filters`. There is no delete endpoint: deactivate a code with
`isActive=false` so redemption history stays auditable.

`PromoCodeCDO` (teacher create; admin create uses `AdminPromoCodeCDO` with an extra optional
`teacherProfileId`):

```json
{
  "code": "SUMMER10",
  "discountPercent": 10.00,
  "discountAmount": null,
  "currency": null,
  "startsAt": null,
  "expiresAt": "2026-09-01T00:00:00Z",
  "usageLimit": 100,
  "isActive": true
}
```

`code` is 3-60 characters of letters, digits, dashes, and underscores. Exactly one of
`discountPercent` (0 < percent <= 100) or `discountAmount` (> 0) is required; `currency` applies to
fixed-amount discounts only and defaults to `UZS`. `expiresAt` must be after `startsAt` when both
are set. `isActive` defaults to `true`.

`PromoCodeUDO` (PATCH; omitted fields stay unchanged, `code` is immutable):

```json
{
  "discountPercent": 15.00,
  "expiresAt": "2026-10-01T00:00:00Z",
  "usageLimit": 200,
  "isActive": false
}
```

Providing a discount field switches the code to that shape. `usageLimit` cannot go below the
current `usedCount`.

`PromoCodeRDO`:

```json
{
  "id": 9401,
  "teacherProfileId": 22,
  "code": "SUMMER10",
  "discountPercent": 10.00,
  "discountAmount": null,
  "currency": null,
  "startsAt": null,
  "expiresAt": "2026-09-01T00:00:00Z",
  "usageLimit": 100,
  "usedCount": 3,
  "isActive": true,
  "createdAt": "2026-07-03T10:20:00Z",
  "updatedAt": "2026-07-03T10:20:00Z"
}
```

Students preview a code before checkout with the quote endpoint (requires the `student` role):

```http
GET /api/v1/student/payments/promo-quote?listingId=501&promoCode=SUMMER10
```

`PromoQuoteRDO` shows the original amount, discount amount, final amount, and the would-be business
error code as `reasonCode` when the code cannot be applied (quotes always return `200` and never
consume usage):

```json
{
  "listingId": 501,
  "code": "SUMMER10",
  "valid": true,
  "reasonCode": null,
  "originalAmount": 25000.00,
  "discountAmount": 2500.00,
  "finalAmount": 22500.00,
  "currency": "UZS"
}
```

Promo business errors (also used as quote `reasonCode` values): `PROMO_CODE_INVALID` (404, unknown
code), `PROMO_CODE_INACTIVE` (409, deactivated or not yet started), `PROMO_CODE_EXPIRED` (409),
`PROMO_CODE_EXHAUSTED` (409, usage limit reached), `PROMO_CODE_WRONG_OWNER` (409, teacher code on
another teacher's listing), `PROMO_CODE_CURRENCY_MISMATCH` (409), `PROMO_CODE_NOT_FOUND` (404, CRUD
reads), and `PROMO_CODE_DUPLICATE` (409, normalized code already exists). Inactive listings fail
before promo validation with `LISTING_NOT_ACCESSIBLE`. A listing the student already has granted
access to fails the same way with `ALREADY_PURCHASED` as the quote `reasonCode`, before promo
validation runs; the quote endpoint still returns `200` and never consumes usage.

Marketplace payment endpoints create a provider payment for active paid marketplace listings and
return a support-safe `PaymentRDO`. Creation checks existing access through the same access-control
boundary used by `listingAccessStatus` before creating anything: a student who already has granted
access (an active purchase or subscription) cannot start another payment for that listing and gets
`ALREADY_PURCHASED` (409); students without access are unaffected. Creation stores the listing
price/currency snapshot and durable target columns (`targetType`, `targetListingId`) before calling
the provider adapter. When `promoCode` is supplied, the discount is validated and applied before the
provider checkout is created, `amount` snapshots the discounted final amount, and the promo fields
record the applied code and discount. Local development uses the mock provider; non-mock providers
cannot create payments while disabled.

`PaymentCDO`:

```json
{
  "targetType": "marketplace_listing",
  "listingId": 501,
  "subscriptionId": null,
  "liveSpeakingBookingId": null,
  "promoCode": "SUMMER10",
  "returnUrl": "http://localhost:5173/checkout/complete"
}
```

`targetType` is one of `marketplace_listing`, `subscription`, or `live_speaking_booking`.
`marketplace_listing` is supported for paid listing checkout. `live_speaking_booking` is supported
for reserved live speaking bookings and requires `liveSpeakingBookingId`. `subscription` remains
represented as an explicit target type but returns `PAYMENT_TARGET_UNSUPPORTED` until subscription
checkout is implemented.

`PaymentRDO`:

```json
{
  "id": 8101,
  "studentUserId": 101,
  "provider": "mock",
  "providerPaymentId": "mock-01J...",
  "providerCheckoutUrl": "mock://payments/mock-01J...",
  "status": "pending",
  "amount": 22500.00,
  "currency": "UZS",
  "targetType": "marketplace_listing",
  "targetListingId": 501,
  "targetListingTitle": "IELTS Reading Pack",
  "targetSubscriptionId": null,
  "targetLiveSpeakingBookingId": null,
  "promoCode": "SUMMER10",
  "promoOriginalAmount": 25000.00,
  "promoDiscountAmount": 2500.00,
  "providerAmount": null,
  "providerCurrency": null,
  "failureCode": null,
  "failureMessage": null,
  "createdAt": "2026-07-01T10:20:00Z",
  "paidAt": null,
  "confirmationDispatchedAt": null,
  "updatedAt": "2026-07-01T10:20:00Z"
}
```

`promoCode`, `promoOriginalAmount`, and `promoDiscountAmount` are `null` when no promo was applied.
Provider callbacks must match the discounted `amount`. Fully discounted payments are returned from
creation already `paid` with `amount` `0.00`, no provider payment id or checkout URL, and
`confirmationDispatchedAt` set; they receive no provider callbacks.

Provider callbacks are public provider routes and must include `X-MockExams-Payment-Signature`,
an HMAC-SHA256 hex signature over the raw JSON body using `PAYMENT_WEBHOOK_SECRET` for the mock
contract:

```http
POST /api/v1/provider/payments/webhook
```

```json
{
  "eventId": "provider-event-1",
  "providerPaymentId": "mock-01J...",
  "status": "paid",
  "amount": 25000.00,
  "currency": "UZS",
  "occurredAt": "2026-07-01T10:22:00Z",
  "failureCode": null,
  "failureMessage": null
}
```

Callback statuses are `pending`, `paid`, `failed`, `cancelled`, and `refunded`. Status callbacks are
idempotent. A valid first `paid` callback dispatches marketplace access once through the internal
purchase/access boundary and sets `confirmationDispatchedAt`; duplicate `paid` callbacks return the
same payment without duplicate purchase rows. `failed`, `cancelled`, and mismatched paid callbacks do
not grant access. Amount/currency mismatch on a paid callback is flagged by moving the payment to
`failed` with `failureCode=PAYMENT_AMOUNT_MISMATCH`.

For `live_speaking_booking`, a paid callback confirms the reserved booking when the hold is still
active, marks the slot `booked`, stores a mock Google Meet URL, creates one completed
`google_calendar_sync_jobs` audit row, and records one idempotent teacher wallet earning with
`sourceType=live_speaking_booking`. If the booking hold has expired before confirmation, the booking
is marked `expired`, the slot is released, and no teacher earning is recorded.

Admin support payment reads require the `admin` role:

```http
GET /api/v1/admin/payments?page=0&size=20
GET /api/v1/admin/payments/{paymentId}
```

`GET /api/v1/admin/payments` returns the standard page envelope sorted by `createdAt` descending,
with `page` (default `0`) and `size` (default `20`, maximum `100`) parameters. It supports optional
`status=pending|paid|failed|cancelled|refunded`; the effective status filter is echoed in
`meta.filters.status`, and an unknown status fails with `VALIDATION_FAILED`.

### Live Speaking Schedules

Approved teachers can create one-off live speaking slots, announce them publicly, and review booking
status under `/teacher/schedule`. Student and admin reads return the same standard page envelope used
elsewhere. All timestamps are UTC ISO-8601 values; `timezone` stores the display timezone chosen by
the teacher.

Teacher endpoints require the `teacher` role and an approved teacher profile:

```http
GET /api/v1/teacher/schedule/slots?status=announced&page=0&size=20
POST /api/v1/teacher/schedule/slots
PATCH /api/v1/teacher/schedule/slots/{slotId}
POST /api/v1/teacher/schedule/slots/{slotId}/announce
POST /api/v1/teacher/schedule/slots/{slotId}/unannounce
GET /api/v1/teacher/schedule/bookings?status=booked&page=0&size=20
GET /api/v1/teacher/schedule/bookings/{bookingId}
POST /api/v1/teacher/schedule/bookings/{bookingId}/complete
POST /api/v1/teacher/schedule/bookings/{bookingId}/no-show
```

Student endpoints require the `student` role:

```http
GET /api/v1/student/schedule/slots?teacherProfileId=22&from=2026-07-20T00:00:00Z&to=2026-07-31T23:59:59Z
POST /api/v1/student/schedule/slots/{slotId}/reserve
GET /api/v1/student/schedule/bookings?status=booked&page=0&size=20
GET /api/v1/student/schedule/bookings/{bookingId}
POST /api/v1/student/schedule/bookings/{bookingId}/payment
POST /api/v1/student/schedule/bookings/{bookingId}/cancel
```

Admin support endpoints require the `admin` role:

```http
GET /api/v1/admin/schedule/bookings?status=booked&teacherProfileId=22&studentUserId=101&page=0&size=20
GET /api/v1/admin/schedule/bookings/{bookingId}
GET /api/v1/admin/schedule/provider-failures?page=0&size=20
```

`ScheduleSlotCDO`:

```json
{
  "startsAt": "2026-07-20T09:00:00Z",
  "endsAt": "2026-07-20T09:30:00Z",
  "timezone": "Asia/Tashkent",
  "priceAmount": 100000.00,
  "currency": "UZS"
}
```

`ScheduleSlotRDO`:

```json
{
  "id": 6601,
  "teacherProfileId": 22,
  "teacherDisplayName": "Teacher Example",
  "teacherHeadline": "IELTS teacher",
  "startsAt": "2026-07-20T09:00:00Z",
  "endsAt": "2026-07-20T09:30:00Z",
  "timezone": "Asia/Tashkent",
  "priceAmount": 100000.00,
  "currency": "UZS",
  "status": "announced",
  "announced": true,
  "holdExpiresAt": null,
  "createdAt": "2026-07-14T10:00:00Z",
  "updatedAt": "2026-07-14T10:05:00Z"
}
```

`ScheduleBookingRDO`:

```json
{
  "id": 7701,
  "slotId": 6601,
  "studentUserId": 101,
  "studentDisplayName": "Student Example",
  "teacherProfileId": 22,
  "teacherDisplayName": "Teacher Example",
  "status": "booked",
  "startsAt": "2026-07-20T09:00:00Z",
  "endsAt": "2026-07-20T09:30:00Z",
  "timezone": "Asia/Tashkent",
  "priceAmount": 100000.00,
  "currency": "UZS",
  "paymentId": 8102,
  "paymentStatus": "paid",
  "meetUrl": "https://meet.google.com/mock-7701",
  "holdExpiresAt": null,
  "providerSyncStatus": "synced",
  "providerFailureCode": null,
  "providerFailureMessage": null,
  "createdAt": "2026-07-14T10:08:00Z",
  "updatedAt": "2026-07-14T10:10:00Z"
}
```

Slot statuses are `draft`, `announced`, `reserved`, `booked`, `completed`, and `cancelled`.
Booking statuses are `reserved`, `booked`, `cancelled`, `expired`, `completed`, and `no_show`.
Reservation holds last 10 minutes. Teachers cannot announce overlapping active slots, students cannot
reserve overlapping active sessions, and a slot can have only one active reservation/booking.

Business errors: `SCHEDULE_SLOT_NOT_FOUND` (404), `SCHEDULE_SLOT_UNAVAILABLE` (409),
`SCHEDULE_BOOKING_NOT_FOUND` (404), `SCHEDULE_BOOKING_STATUS_CONFLICT` (409),
`TEACHER_APPROVAL_REQUIRED` (403), and `VALIDATION_FAILED` (400).

Browse supports the standard page envelope with filters: `type`, `teacherId`, `price=free|paid`,
`minPrice`, `maxPrice`, `tag`, `category`, `language`, and `q`. Sort fields are `createdAt`, `title`,
and `priceAmount`.

`MarketplaceListingCDO`:

```json
{
  "title": "IELTS Reading Pack",
  "description": "Three published teacher reading tests.",
  "listingType": "test_pack",
  "testId": null,
  "priceAmount": 25000.00,
  "currency": "UZS",
  "language": "en"
}
```

`listingType` is one of `single_test`, `test_pack`, `writing_course`, `speaking_question_pack`,
`vocabulary_pack`, `full_mock_exam`, or `subscription_plan`. `single_test` and `full_mock_exam`
listings may reference one owned test directly. Pack/course/subscription listings use listing items.
Teachers cannot reference another teacher's test or file.

`MarketplaceListingDetailRDO` includes `accessStatus` when returned by student access-aware flows; it
is `null` for anonymous public and teacher management reads. Paid anonymous public detail returns an
empty `items` array.

`MarketplaceListingDetailRDO`:

```json
{
  "id": 501,
  "title": "IELTS Reading Pack",
  "description": "Three published teacher reading tests.",
  "listingType": "test_pack",
  "testId": null,
  "priceAmount": 25000.00,
  "currency": "UZS",
  "free": false,
  "active": true,
  "language": "en",
  "createdAt": "2026-07-01T10:00:00Z",
  "updatedAt": "2026-07-01T10:15:00Z",
  "teacher": {
    "id": 22,
    "displayName": "Teacher Example",
    "headline": "IELTS teacher",
    "ratingAverage": 0,
    "ratingCount": 0
  },
  "categories": [],
  "tags": [],
  "items": [
    {
      "id": 9001,
      "testId": 42,
      "fileId": null,
      "title": "Reading Test 1",
      "description": "First pack item.",
      "itemOrder": 1
    }
  ],
  "accessStatus": null
}
```

Activation is a validated state transition. Pack/course/subscription listings require at least one
item before activation; direct test listings require a referenced published test. The activation
summary returns `MarketplaceListingActivationRDO` with `ready` and `errors`. Important business error
codes are `LISTING_NOT_FOUND`, `LISTING_STATUS_CONFLICT`, `LISTING_ITEM_INVALID`, and
`TEACHER_APPROVAL_REQUIRED`. Student access errors include `PAYMENT_REQUIRED` (`402`),
`PURCHASE_NOT_FOUND` (`404`), `ACCESS_EXPIRED` (`403`), and `LISTING_NOT_ACCESSIBLE` (`403`).
Inactive listings are not accessible even to students with an existing purchase.

`PurchaseRDO`:

```json
{
  "id": 7001,
  "studentUserId": 101,
  "listingId": 501,
  "listingTitle": "IELTS Reading Pack",
  "listingType": "test_pack",
  "amount": 25000.00,
  "currency": "UZS",
  "status": "granted",
  "sourceType": "payment",
  "sourceReference": "payment-123",
  "paymentId": null,
  "paymentStatus": null,
  "purchasedAt": "2026-07-01T10:20:00Z",
  "accessExpiresAt": null,
  "accessStatus": {
    "listingId": 501,
    "accessible": true,
    "status": "accessible",
    "reasonCode": null,
    "purchaseId": 7001,
    "accessExpiresAt": null
  }
}
```

### Student Test Attempts

Student attempt endpoints require the `student` role and use the standard `{ data, meta }` envelope.
Paid marketplace entitlement checks run through the shared access boundary before an attempt starts.
Free published tests still start normally, and active free listings are idempotently granted when
encountered. Paid tests require an active purchase or active subscription window through a direct
listing or listing item.

```http
POST /api/v1/student/tests/{testId}/attempts
PATCH /api/v1/student/attempts/{attemptId}/answers
POST /api/v1/student/attempts/{attemptId}/submit
GET /api/v1/student/attempts/{attemptId}/result
GET /api/v1/student/results/{resultId}
```

Attempt start verifies the test is public-catalog visible: `visibility=published`, `deletedAt=null`,
not draft/submitted/rejected/archived, and owned by an approved teacher or admin-created platform
test. Success: `201 Created` with `StudentTestAttemptRDO`.

```json
{
  "id": 401,
  "testId": 42,
  "studentUserId": 101,
  "status": "in_progress",
  "startedAt": "2026-06-21T17:00:17.29398Z",
  "submittedAt": null,
  "expiresAt": "2026-06-21T18:00:17.29398Z",
  "durationMinutes": 60,
  "remainingSeconds": 3600,
  "timeSpentSeconds": 0
}
```

Answer upsert uses `StudentAnswerUDO`. Re-saving the same `questionId` for an attempt updates the
existing answer row. The question must belong to the attempted test, `selectedOptionId`, when
present, must belong to that question, and `audioFileId`, when present, must reference the student's
own `speaking_audio` file. At least one of `answerText`, `selectedOptionId`, or `audioFileId` is
required.

```json
{
  "questionId": 501,
  "answerText": "two hours",
  "selectedOptionId": null,
  "audioFileId": null
}
```

Success: `200 OK` with `StudentAnswerRDO`. Scoring is intentionally not performed by these endpoints,
so correctness and score fields are not returned.

```json
{
  "id": 701,
  "attemptId": 401,
  "questionId": 501,
  "answerText": "two hours",
  "selectedOptionId": null,
  "audioFileId": null,
  "answeredAt": "2026-06-21T17:12:17.29398Z"
}
```

Submit transitions an in-progress attempt to `submitted` and sets `submittedAt` and
`timeSpentSeconds`. When the current time is at or after `expiresAt`, save and submit requests mark
the attempt `expired` and return `ATTEMPT_EXPIRED`; answers cannot be changed after `submitted` or
`expired`.

Result reads are available only to the student who owns the attempt/result. `GET
/api/v1/student/attempts/{attemptId}/result` is idempotent: it locks the submitted or expired Reading
or Listening attempt, returns the existing `test_results` row when present, or scores objective
answers and creates exactly one row when missing. `GET /api/v1/student/results/{resultId}` returns an
existing result detail and does not score another attempt.

Objective scoring compares the student's selected option label or text answer after backend
normalization against `question_answers.normalized_value`. Correct saved answers get their question
score in `student_answers.score_awarded`; wrong saved answers get zero and `is_correct=false`.
Unanswered objective questions are returned in result detail with `unanswered=true`, zero score, and
do not create synthetic `student_answers` rows.

Result detail responses include correct answers and explanations only after the attempt has been
submitted or expired and scored:

```json
{
  "id": 501,
  "attemptId": 401,
  "testId": 42,
  "testTitle": "IELTS Academic Reading Practice 1",
  "testType": "reading",
  "rawScore": 31.00,
  "maxScore": 40.00,
  "estimatedBand": 7.0,
  "correctCount": 31,
  "incorrectCount": 9,
  "startedAt": "2026-06-21T17:00:17.29398Z",
  "submittedAt": "2026-06-21T17:55:17.29398Z",
  "timeSpentSeconds": 3300,
  "sections": [
    {
      "id": 601,
      "title": "Reading Passage 1",
      "sectionOrder": 1,
      "rawScore": 10.00,
      "maxScore": 13.00,
      "correctCount": 10,
      "incorrectCount": 3
    }
  ],
  "answers": [
    {
      "questionId": 701,
      "sectionId": 601,
      "sectionTitle": "Reading Passage 1",
      "questionOrder": 1,
      "questionType": "short_answer",
      "questionText": "Which river is named?",
      "questionRenderedHtml": null,
      "studentAnswer": "The River Nile",
      "selectedOptionId": null,
      "selectedOptionLabel": null,
      "correctAnswers": ["The River Nile"],
      "correct": true,
      "scoreAwarded": 1.00,
      "maxScore": 1.00,
      "unanswered": false,
      "explanation": "The passage names the River Nile.",
      "explanationContent": {
        "contentDocument": null,
        "contentText": "The passage names the River Nile.",
        "renderedHtml": null
      }
    }
  ],
  "weakAreas": [
    {
      "questionType": "gap_fill",
      "sectionId": 601,
      "sectionTitle": "Reading Passage 1",
      "incorrectCount": 2,
      "totalCount": 4
    }
  ]
}
```

Band estimates come from the seeded `band_conversions` table (`test_type`, raw score range,
`estimated_band`). The seed is an internal IELTS-style Reading/Listening approximation for raw scores
0-40, not an official IELTS-certified conversion. If no configured range matches a scored raw value,
`estimatedBand` is `null` while raw/max score and answer detail still return.

Student attempt error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `TEST_NOT_FOUND` | `404` | The test is missing or not startable from the student/public catalog. |
| `PAYMENT_REQUIRED` | `402` | The test/listing/file requires marketplace purchase access. |
| `ACCESS_EXPIRED` | `403` | A purchase or subscription exists but no longer grants access. |
| `LISTING_NOT_ACCESSIBLE` | `403` | Referenced marketplace content is inactive or the access grant is revoked. |
| `PAYMENT_NOT_FOUND` | `404` | The payment does not exist or is not visible to the requester. |
| `PAYMENT_PROVIDER_DISABLED` | `503` | A non-mock payment provider is selected while payments are disabled. |
| `PAYMENT_SIGNATURE_INVALID` | `401` | The provider webhook signature is missing or invalid. |
| `PAYMENT_TARGET_UNSUPPORTED` | `409` | The requested payment target is represented but not implemented yet. |
| `ALREADY_PURCHASED` | `409` | The student already has granted access to the marketplace listing; no new payment is needed. |
| `PROMO_CODE_INVALID` | `404` | The submitted promo code does not exist. |
| `PROMO_CODE_INACTIVE` | `409` | The promo code is deactivated or not yet started. |
| `PROMO_CODE_EXPIRED` | `409` | The promo code expiration date has passed. |
| `PROMO_CODE_EXHAUSTED` | `409` | The promo code usage limit has been reached. |
| `PROMO_CODE_WRONG_OWNER` | `409` | A teacher-owned code was applied to another teacher's listing. |
| `PROMO_CODE_CURRENCY_MISMATCH` | `409` | The promo code currency does not match the listing currency. |
| `PROMO_CODE_NOT_FOUND` | `404` | The promo code is missing or not visible to the requester. |
| `PROMO_CODE_DUPLICATE` | `409` | A promo code with the same normalized value already exists. |
| `ATTEMPT_NOT_FOUND` | `404` | The attempt is missing or belongs to another student. |
| `QUESTION_NOT_FOUND` | `404` | The question does not belong to the attempted test. |
| `QUESTION_OPTION_NOT_FOUND` | `404` | The selected option does not belong to the submitted question. |
| `ATTEMPT_ALREADY_SUBMITTED` | `409` | The attempt has already been submitted and cannot be changed. |
| `ATTEMPT_EXPIRED` | `409` | The attempt exceeded its duration and has been marked expired. |
| `ATTEMPT_STATUS_CONFLICT` | `409` | The attempt is in a non-editable lifecycle state. |
| `ATTEMPT_NOT_SUBMITTED` | `409` | The attempt has not reached a result-visible status. |
| `RESULT_NOT_FOUND` | `404` | The result is missing or belongs to another student. |
| `RESULT_NOT_READY` | `409` | The attempt/test type cannot be auto-scored yet or has no objective answer keys. |

## Student Speaking Submissions

Speaking transcription endpoints require the `student` role and only expose submissions owned by the
authenticated student. Audio must be uploaded first through `POST /api/v1/student/files` with purpose
`speaking_audio`; the speaking submission references that stored file so transcription never uses an
untracked binary blob.

```http
POST /api/v1/student/speaking-submissions
GET /api/v1/student/speaking-submissions/{submissionId}
POST /api/v1/student/speaking-submissions/{submissionId}/transcription
POST /api/v1/student/speaking-submissions/{submissionId}/feedback
GET /api/v1/student/speaking-submissions/{submissionId}/feedback
POST /api/v1/student/speaking-submissions/{submissionId}/feedback/retry
```

Create uses `SpeakingSubmissionCDO`:

```json
{
  "partType": "speaking_part_2",
  "prompt": "Talk about a memorable journey.",
  "audioFileId": 501,
  "testAttemptId": null,
  "durationSeconds": 45
}
```

The backend validates that the prompt is present, `partType` is one of `speaking_part_1`,
`speaking_part_2`, or `speaking_part_3`, the audio file is the student's own `speaking_audio` file,
and duration is positive and no longer than the configured transcription maximum. When the file row
already has `durationSeconds`, that stored value is authoritative over the request value. Exact
duplicate creates for the same student, prompt, part, optional attempt, and audio file are idempotent:
the existing submission is returned and no second provider call is made.

Success returns `201 Created` for a new submission or `200 OK` for an idempotent duplicate with
`SpeakingSubmissionRDO`:

```json
{
  "data": {
    "id": 901,
    "studentUserId": 101,
    "testAttemptId": null,
    "partType": "speaking_part_2",
    "prompt": "Talk about a memorable journey.",
    "audioFileId": 501,
    "transcript": "I would like to talk about...",
    "durationSeconds": 45,
    "transcriptionStatus": "completed",
    "provider": "mock",
    "model": "mock-transcriber",
    "failureCode": null,
    "errorMessage": null,
    "estimatedCost": 0.000000,
    "transcriptionStartedAt": "2026-06-21T17:00:17.29398Z",
    "transcriptionCompletedAt": "2026-06-21T17:00:18.29398Z",
    "createdAt": "2026-06-21T17:00:17.29398Z"
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

`transcriptionStatus` is one of `queued`, `processing`, `completed`, or `failed`. Provider failures
return a persisted submission with `failed`, `failureCode`, and `errorMessage`; the original audio
file remains stored and readable by the owner. `POST .../{submissionId}/transcription` is retry-safe:
it returns the current submission for `queued`, `processing`, or `completed` records, and retries
records that are `failed`.

Transcription provider/model and estimated transcription cost are stored on `speaking_submissions`.
They are not counted in `ai_usage_limits` or `ai_feedbacks`; later speaking AI feedback remains
responsible for feedback request accounting.

Speaking AI feedback can be started only after transcription completes with a nonblank transcript.
`POST .../{submissionId}/feedback` returns `202 Accepted` and creates a shared `ai_feedbacks` job with
`feedbackType=speaking`, or returns the existing completed/active job idempotently. `GET .../feedback`
polls the latest job. `POST .../feedback/retry` creates a linked retry for failed feedback via
`retryOfFeedbackId`; completed or active feedback is returned unchanged. Missing or failed
transcripts fail with `TRANSCRIPT_REQUIRED` (`409`) and do not reserve AI usage or call the provider.
Usage limits, provider failures, validation failures, token/cost metadata, and admin diagnostics use
the same `ai_feedbacks` and `ai_usage_limits` orchestration as writing feedback.

`SpeakingFeedbackRDO` exposes student-safe fields: `status`, `overallBand`, `criteriaScores`,
`repeatedMistakes`, `betterAnswerExample`, `actionPlan`, provider/model metadata, token/cost
estimates, rubric version, retry linkage, and the explicit pronunciation limitation fields
`transcriptOnlyPronunciation` and `pronunciationScoringNote`. Because the MVP evaluates transcript
text only, `pronunciation_proxy` is a transcript-only proxy and must not be presented as acoustic
pronunciation scoring.

Provider responses must validate to the backend-owned speaking JSON schema:

```json
{
  "overallBand": 6.5,
  "criteriaScores": {
    "fluency_and_coherence": {
      "band": 6.5,
      "feedback": "Ideas are easy to follow.",
      "suggestions": ["Extend answers with clearer linking."]
    },
    "lexical_resource": {
      "band": 6.0,
      "feedback": "Vocabulary is suitable but sometimes repeated.",
      "suggestions": ["Use more precise topic phrases."]
    },
    "grammatical_range_and_accuracy": {
      "band": 6.0,
      "feedback": "Meaning is clear despite grammar slips.",
      "suggestions": ["Practice complex sentences accurately."]
    },
    "pronunciation_proxy": {
      "band": 6.5,
      "feedback": "Transcript-only phrasing suggests mostly clear delivery.",
      "suggestions": ["Record and review stress and intonation separately."]
    }
  },
  "repeatedMistakes": [
    {
      "type": "grammar",
      "original": "he go",
      "correction": "he goes",
      "explanation": "Use third-person singular verb agreement."
    }
  ],
  "betterAnswerExample": "A more natural and extended version of the answer.",
  "actionPlan": ["Practice extending answers with reasons."]
}
```

Speaking Parts 1, 2, and 3 all require `fluency_and_coherence`, `lexical_resource`,
`grammatical_range_and_accuracy`, and `pronunciation_proxy`. Part-specific prompts adjust
expectations for short personal answers, sustained cue-card long turns, or abstract discussion.
Malformed or schema-invalid provider JSON marks feedback `failed`, stores internal validation
errors, and leaves student-visible band/result fields empty.

Speaking submission error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `VALIDATION_FAILED` | `400` | Prompt, part type, file purpose, or duration is invalid. |
| `NOT_FOUND` | `404` | The submission or audio file is missing or belongs to another user. |
| `ATTEMPT_NOT_FOUND` | `404` | The optional attempt does not belong to the student. |
| `TRANSCRIPT_REQUIRED` | `409` | Speaking feedback was requested before a completed nonblank transcript exists. |
| `AI_FEEDBACK_NOT_FOUND` | `404` | Feedback has not been started for this submission. |
| `AI_USAGE_LIMIT_EXCEEDED` | `429` | The student has reached the configured AI usage hard limit. |

## Student Progress Dashboard

Student progress endpoints require the `student` role and only read progress for the authenticated
student. They use stored `test_results` and completed attempts as the source of truth; progress reads
do not rescore attempts.

```http
GET /api/v1/student/progress?days=180
```

`days` defaults to `180` and is bounded to `1..365`. The response includes dashboard freshness in
`meta.asOf` and the effective bounded read window in `meta.window`.

```json
{
  "data": {
    "state": "active",
    "summary": {
      "overallBandEstimate": 7.0,
      "skillBands": {
        "listening": 7.0,
        "reading": 6.5,
        "writing": null,
        "speaking": null
      },
      "missingSkills": ["writing", "speaking"],
      "completedAttemptCount": 2,
      "latestResultAt": "2026-06-29T15:31:12Z"
    },
    "skillHistories": {
      "listening": [],
      "reading": [
        {
          "resultId": 501,
          "attemptId": 401,
          "testId": 42,
          "testTitle": "IELTS Academic Reading Practice 1",
          "attemptStatus": "submitted",
          "scoredAt": "2026-06-29T15:31:12Z",
          "rawScore": 31.00,
          "maxScore": 40.00,
          "estimatedBand": 7.0
        }
      ]
    },
    "weakAreas": [
      {
        "skill": "reading",
        "questionType": "gap_fill",
        "incorrectCount": 3,
        "attemptCount": 1,
        "latestSeenAt": "2026-06-29T15:31:12Z"
      }
    ],
    "recentAttempts": [
      {
        "attemptId": 401,
        "resultId": 501,
        "testId": 42,
        "testTitle": "IELTS Academic Reading Practice 1",
        "testType": "reading",
        "attemptStatus": "submitted",
        "startedAt": "2026-06-29T14:31:12Z",
        "submittedAt": "2026-06-29T15:31:12Z",
        "timeSpentSeconds": 3600,
        "rawScore": 31.00,
        "maxScore": 40.00,
        "estimatedBand": 7.0
      }
    ],
    "recommendations": [
      {
        "sourceType": "teacher_test",
        "testId": 77,
        "title": "Listening Multiple Choice Practice",
        "testType": "listening",
        "reason": "Practice listening multiple_choice",
        "directlyAvailable": true,
        "free": true,
        "publishedAt": "2026-06-20T09:00:00Z"
      }
    ],
    "aiUsage": {
      "periodStart": "2026-06-01",
      "periodEnd": "2026-06-30",
      "freeRequestsLimit": 20,
      "usedRequests": 0,
      "hardLimitRequests": 50,
      "freeRequestsRemaining": 20,
      "hardRequestsRemaining": 50,
      "hardLimitReached": false
    },
    "extensionHooks": [
      {
        "skill": "writing",
        "source": "ai_feedback",
        "status": "pending"
      }
    ]
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A",
    "asOf": "2026-06-30T14:46:12Z",
    "window": {
      "days": 180,
      "from": "2026-01-01T14:46:12Z",
      "to": "2026-06-30T14:46:12Z",
      "historyLimit": 100,
      "recentAttemptLimit": 10
    }
  }
}
```

`overallBandEstimate` averages only available skill bands, so missing skills are labelled in
`missingSkills` instead of treated as zero. Reading and Listening histories are separated and each is
bounded to the most recent 100 result rows in the selected window. Recent attempts include submitted and
expired attempts, bounded to the latest 10 rows, and exclude teacher-owned drafts.

Recommendations are rule-based for this MVP. Directly available recommendations include only free,
published, non-deleted tests that are startable by students: rejected, archived, draft, unpublished,
and inaccessible paid content are not returned as directly available recommendations. Writing,
Speaking, and AI study-plan inputs are represented by extension hooks until their AI feedback plans
land.

For a new student with no completed objective results, `state` is `empty`, score/history/weak-area
lists are empty, `overallBandEstimate` is `null`, and recommendations may include first-practice
Reading or Listening tests when startable content exists.

### Teacher Test Builder

Teacher test builder endpoints require the `teacher` role and an approved teacher profile. They use
the standard `{ data, meta }` envelope and create teacher-owned content in the existing `tests` and
`test_sections` tables.

```http
GET  /api/v1/teacher/tests?status=draft&q=academic&page=0&size=20&sort=updatedAt,desc
POST /api/v1/teacher/tests
PATCH /api/v1/teacher/tests/{testId}
GET /api/v1/teacher/tests/{testId}/preview
POST /api/v1/teacher/tests/{testId}/submit
POST /api/v1/teacher/tests/{testId}/archive
POST /api/v1/teacher/tests/{testId}/sections
PATCH /api/v1/teacher/tests/{testId}/sections/{sectionId}
PATCH /api/v1/teacher/tests/{testId}/sections/reorder
DELETE /api/v1/teacher/tests/{testId}/sections/{sectionId}
POST /api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs
PATCH /api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs/{paragraphId}
PATCH /api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs/reorder
DELETE /api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs/{paragraphId}
POST /api/v1/teacher/tests/{testId}/sections/{sectionId}/listening-segments
PATCH /api/v1/teacher/tests/{testId}/sections/{sectionId}/listening-segments/{segmentId}
PATCH /api/v1/teacher/tests/{testId}/sections/{sectionId}/listening-segments/reorder
DELETE /api/v1/teacher/tests/{testId}/sections/{sectionId}/listening-segments/{segmentId}
POST /api/v1/teacher/tests/{testId}/sections/{sectionId}/questions
PATCH /api/v1/teacher/tests/{testId}/sections/{sectionId}/questions/{questionId}
DELETE /api/v1/teacher/tests/{testId}/sections/{sectionId}/questions/{questionId}
```

`GET /api/v1/teacher/tests` returns only tests owned by the authenticated approved teacher profile.
It uses the standard list/page envelope with `TeacherTestSummaryRDO` items. Supported query
parameters:

| Parameter | Meaning |
| --- | --- |
| `status` | Optional test visibility filter: `draft`, `submitted`, `published`, `rejected`, or `archived`. |
| `q` | Optional case-insensitive search over title and description. |
| `page` / `size` | Zero-based pagination. Default size is `20`; maximum size is `100`. |
| `sort` | Optional sort in `field,direction` form. Supported fields are `updatedAt`, `createdAt`, `publishedAt`, `title`, `visibility`, and `type`. Default is `updatedAt,desc`. |

`TeacherTestSummaryRDO` intentionally omits sections, paragraphs, listening segments, questions, and
answer keys for list performance:

```json
{
  "id": 42,
  "teacherProfileId": 302,
  "createdByUserId": 202,
  "title": "IELTS Academic Reading Practice 1",
  "description": "Teacher-owned practice test.",
  "type": "reading",
  "difficulty": "intermediate",
  "estimatedBandMin": 5.5,
  "estimatedBandMax": 7.0,
  "durationMinutes": 60,
  "priceAmount": 0.00,
  "currency": "UZS",
  "free": true,
  "language": "en",
  "visibility": "draft",
  "moderationNote": null,
  "publishedAt": null,
  "createdAt": "2026-06-21T17:00:17.29398Z",
  "updatedAt": "2026-06-21T17:00:17.29398Z"
}
```

Create uses `TeacherTestCDO`; update uses `TeacherTestUDO`. The current builder accepts the same
metadata fields for both commands:

```json
{
  "title": "IELTS Academic Reading Practice 1",
  "description": "Teacher-owned practice test.",
  "type": "reading",
  "difficulty": "intermediate",
  "estimatedBandMin": 5.5,
  "estimatedBandMax": 7.0,
  "durationMinutes": 60,
  "priceAmount": 0.00,
  "currency": "UZS",
  "free": true,
  "language": "en"
}
```

Section create uses `TeacherTestSectionCDO`; section update uses `TeacherTestSectionUDO`:

```json
{
  "title": "Reading Passage 1",
  "instructions": "Read and answer the questions.",
  "instructionsDocument": {
    "version": 1,
    "type": "doc",
    "content": [
      { "type": "paragraph", "content": [{ "type": "text", "text": "Read and answer the questions." }] }
    ]
  },
  "passageText": "A plain-text IELTS Reading passage.",
  "passageDocument": {
    "version": 1,
    "type": "doc",
    "content": [
      { "type": "heading", "attrs": { "level": 2 }, "content": [{ "type": "text", "text": "A passage heading" }] },
      { "type": "paragraph", "content": [{ "type": "text", "text": "A formatted IELTS Reading passage." }] }
    ]
  },
  "audioFileId": null,
  "durationMinutes": 20
}
```

`instructions` and `passageText` remain accepted as legacy plain-text inputs. When a corresponding
`instructionsDocument` or `passageDocument` is provided, the rich-text document wins and the backend
stores the stripped text in the legacy string field for search, accessibility, and readiness
validation.

Rich text is a backend-validated JSON document, not browser HTML. The current version is:

```json
{
  "version": 1,
  "type": "doc",
  "content": []
}
```

Allowed nodes are `heading`, `paragraph`, `bullet_list`, `ordered_list`, `list_item`, `blockquote`,
`table`, `table_row`, `table_cell`, `table_header`, `text`, `hard_break`, `image`, and `file`.
Allowed text marks are `bold`, `italic`, `underline`, `strike`, `code`, and safe `link` marks using
`http`, `https`, or `mailto`. Unsafe link marks are stripped. Unknown nodes, scripts, external image
URLs, and file embeds that do not point at an existing `files` row with purpose `passage_image`,
`diagram`, or `document` fail with `VALIDATION_FAILED`. The backend returns a sanitized HTML
projection in `renderedHtml`; clients must not submit arbitrary HTML. Read and preview responses
derive this projection from the stored rich-text document, or from escaped legacy plain text when no
document exists, rather than trusting any persisted HTML cache.

Reorder uses `TeacherTestSectionReorderUDO` and must include every current section exactly once:

```json
{
  "sectionIds": [44, 42, 43]
}
```

Reading paragraph create uses `TeacherReadingParagraphCDO`; paragraph update uses
`TeacherReadingParagraphUDO`. Labels are optional, limited to 20 characters when present, and
non-blank labels must be unique within a section. Multiple unlabeled paragraphs are valid and remain
ordered by `paragraphOrder`:

```json
{
  "label": "A",
  "content": "Paragraph A text.",
  "contentDocument": {
    "version": 1,
    "type": "doc",
    "content": [
      { "type": "paragraph", "content": [{ "type": "text", "text": "Paragraph A text." }] }
    ]
  }
}
```

Paragraph reorder uses `TeacherReadingParagraphReorderUDO` and must include every current paragraph
in the section exactly once:

```json
{
  "paragraphIds": [102, 101, 103]
}
```

Listening segment create uses `TeacherListeningSegmentCDO`; segment update uses
`TeacherListeningSegmentUDO`. Segments are ordered within a section and may store an optional
teacher-provided transcript:

```json
{
  "title": "Questions 1-4",
  "startSeconds": 0,
  "endSeconds": 95,
  "transcript": "Speaker transcript text."
}
```

`startSeconds` and `endSeconds` are whole-second offsets into the section audio. `startSeconds` must
be zero or greater, and `endSeconds` must be greater than `startSeconds`; invalid ranges fail with
`VALIDATION_FAILED`. Segment reorder uses `TeacherListeningSegmentReorderUDO` and must include every
current segment in the section exactly once:

```json
{
  "segmentIds": [202, 201, 203]
}
```

Section `audioFileId`, when present, must point at an existing `files` row with purpose
`listening_audio`; missing files or other purposes fail with `VALIDATION_FAILED`. This purpose remains
teacher-uploadable through `POST /api/v1/teacher/files` and follows the file storage rules below.

Teacher question create/update uses `TeacherQuestionCDO` and `TeacherQuestionUDO`. Question prompts,
instructions, option text, and answer explanations accept the same structured rich-text document
contract as section and paragraph content. Nested `options` and `answers` are replaced as a set when
provided on update. Omitted nested fields preserve existing values unless the question type changes
to a type that does not use options, in which case options are cleared.

```json
{
  "questionType": "multiple_choice",
  "questionText": "Which paragraph mentions this idea?",
  "questionDocument": {
    "version": 1,
    "type": "doc",
    "content": [
      { "type": "paragraph", "content": [{ "type": "text", "text": "Which paragraph mentions this idea?" }] }
    ]
  },
  "instructions": "Choose the paragraph.",
  "instructionsDocument": {
    "version": 1,
    "type": "doc",
    "content": [
      { "type": "paragraph", "content": [{ "type": "text", "text": "Choose the paragraph." }] }
    ]
  },
  "score": 1,
  "relatedParagraphLabel": "A",
  "listeningSegmentId": null,
  "options": [
    {
      "optionLabel": "A",
      "optionText": "Paragraph A",
      "optionDocument": {
        "version": 1,
        "type": "doc",
        "content": [
          { "type": "paragraph", "content": [{ "type": "text", "text": "Paragraph A" }] }
        ]
      }
    },
    { "optionLabel": "B", "optionText": "Paragraph B" }
  ],
  "answers": [
    {
      "answerValue": "A",
      "primary": true,
      "explanation": "Paragraph A states the idea.",
      "explanationDocument": {
        "version": 1,
        "type": "doc",
        "content": [
          { "type": "paragraph", "content": [{ "type": "text", "text": "Paragraph A states the idea." }] }
        ]
      }
    },
    { "answerValue": "paragraph a", "primary": false }
  ]
}
```

MVP objective question type examples:

```json
{ "questionType": "multiple_choice", "options": [{ "optionLabel": "A", "optionText": "A" }], "answers": [{ "answerValue": "A" }] }
{ "questionType": "matching_information", "options": [{ "optionLabel": "A", "optionText": "Paragraph A" }], "answers": [{ "answerValue": "A" }] }
{ "questionType": "true_false_not_given", "answers": [{ "answerValue": "TRUE" }] }
{ "questionType": "yes_no_not_given", "answers": [{ "answerValue": "NO" }] }
{ "questionType": "sentence_completion", "answers": [{ "answerValue": "the river nile" }, { "answerValue": "Nile", "primary": false }] }
{ "questionType": "summary_completion", "answers": [{ "answerValue": "carbon dioxide" }] }
{ "questionType": "table_completion", "answers": [{ "answerValue": "42" }] }
{ "questionType": "diagram_labeling", "answers": [{ "answerValue": "evaporator" }] }
{ "questionType": "short_answer", "answers": [{ "answerValue": "two hours" }] }
{ "questionType": "gap_fill", "answers": [{ "answerValue": "migration" }] }
{ "questionType": "ordering", "options": [{ "optionLabel": "1", "optionText": "First event" }], "answers": [{ "answerValue": "1,2,3" }] }
```

Answer values are stored with a normalized projection used by later auto-scoring. Normalization folds
case, repeated whitespace, and punctuation; accepted variants are represented by additional answer
rows, usually with `primary=false`. Teacher/admin builder and moderation views may return
`answerKeys`; student attempt/public preview views must not.

When `relatedParagraphLabel` is present, it must match an existing paragraph label in the same
section. Missing non-blank labels fail with `VALIDATION_FAILED`; omitted, null, or blank values are
treated as no paragraph link.

When `listeningSegmentId` is present, it must match an existing listening segment in the same section.
Segment ids from another section fail with `VALIDATION_FAILED`; omitted or null values are treated as
no listening segment link.

Multiple-choice, matching, and ordering questions require at least one option. Objective question
types require at least one answer key. Unsupported question types, empty required prompt/option text,
duplicate option labels, and duplicate normalized answer values fail with `VALIDATION_FAILED`.
Unsafe rich text in prompts, instructions, options, or explanations follows the same sanitizer rules
as sections and paragraphs.

`TeacherTestRDO`:

```json
{
  "id": 42,
  "teacherProfileId": 302,
  "createdByUserId": 202,
  "title": "IELTS Academic Reading Practice 1",
  "description": "Teacher-owned practice test.",
  "type": "reading",
  "difficulty": "intermediate",
  "estimatedBandMin": 5.5,
  "estimatedBandMax": 7.0,
  "durationMinutes": 60,
  "priceAmount": 0.00,
  "currency": "UZS",
  "free": true,
  "language": "en",
  "visibility": "draft",
  "moderationNote": null,
  "publishedAt": null,
  "createdAt": "2026-06-21T17:00:17.29398Z",
  "updatedAt": "2026-06-21T17:00:17.29398Z",
  "sections": [
    {
      "id": 44,
      "title": "Reading Passage 1",
      "instructions": "Read and answer the questions.",
      "instructionsContent": {
        "contentDocument": { "version": 1, "type": "doc", "content": [] },
        "contentText": "Read and answer the questions.",
        "renderedHtml": "<p>Read and answer the questions.</p>"
      },
      "passageText": "A plain-text IELTS Reading passage.",
      "passageTextContent": {
        "contentDocument": { "version": 1, "type": "doc", "content": [] },
        "contentText": "A plain-text IELTS Reading passage.",
        "renderedHtml": "<p>A plain-text IELTS Reading passage.</p>"
      },
      "audioFileId": null,
      "sectionOrder": 1,
      "durationMinutes": 20,
      "paragraphs": [
        {
          "id": 101,
          "label": "A",
          "content": "Paragraph A text.",
          "contentDocument": { "version": 1, "type": "doc", "content": [] },
          "contentText": "Paragraph A text.",
          "renderedHtml": "<p>Paragraph A text.</p>",
          "paragraphOrder": 1
        }
      ],
      "listeningSegments": [
        {
          "id": 202,
          "title": "Questions 1-4",
          "startSeconds": 0,
          "endSeconds": 95,
          "transcript": "Speaker transcript text.",
          "segmentOrder": 1
        }
      ],
      "questions": [
        {
          "id": 501,
          "questionType": "matching_information",
          "questionText": "Which paragraph mentions this idea?",
          "questionTextContent": {
            "contentDocument": { "version": 1, "type": "doc", "content": [] },
            "contentText": "Which paragraph mentions this idea?",
            "renderedHtml": "<p>Which paragraph mentions this idea?</p>"
          },
          "instructions": "Choose the paragraph.",
          "instructionsContent": {
            "contentDocument": { "version": 1, "type": "doc", "content": [] },
            "contentText": "Choose the paragraph.",
            "renderedHtml": "<p>Choose the paragraph.</p>"
          },
          "score": 1.00,
          "questionOrder": 1,
          "relatedParagraphLabel": "A",
          "listeningSegmentId": null,
          "options": [
            {
              "id": 601,
              "optionLabel": "A",
              "optionText": "Paragraph A",
              "optionTextContent": {
                "contentDocument": { "version": 1, "type": "doc", "content": [] },
                "contentText": "Paragraph A",
                "renderedHtml": "<p>Paragraph A</p>"
              },
              "optionOrder": 1
            }
          ],
          "answerKeys": [
            {
              "id": 701,
              "answerValue": "A",
              "normalizedValue": "a",
              "primary": true,
              "answerOrder": 1,
              "explanation": "Paragraph A states the idea.",
              "explanationContent": {
                "contentDocument": { "version": 1, "type": "doc", "content": [] },
                "contentText": "Paragraph A states the idea.",
                "renderedHtml": "<p>Paragraph A states the idea.</p>"
              }
            }
          ]
        }
      ],
      "createdAt": "2026-06-21T17:00:17.29398Z",
      "updatedAt": "2026-06-21T17:00:17.29398Z"
    }
  ]
}
```

Lifecycle and ownership rules:

- Create requires an approved teacher profile and starts the test as `visibility=draft`.
- Only the owner teacher can preview, edit, reorder, delete sections, or submit the test.
- Editable test statuses are `draft` and `rejected`. Editing a rejected test moves it back to `draft`
  and clears moderation fields.
- `submitted`, `published`, and `archived` tests are read-only for the teacher builder.
- Submit moves `draft` or `rejected` tests to `submitted`, clears moderation fields, and keeps the
  test read-only until an admin moderation decision changes its status.
- Archive is a non-destructive owner action. `POST /api/v1/teacher/tests/{testId}/archive` moves
  owned `draft`, `rejected`, or `published` tests to `visibility=archived` and returns the updated
  `TeacherTestRDO`. It does not hard-delete rows. `submitted` and already `archived` tests fail with
  `TEACHER_TEST_STATUS_CONFLICT`.
- Submit validates required metadata and requires at least one section for non-Reading tests.
- Reading tests require exactly three sections, and each Reading section must include meaningful
  passage text after rich-text formatting is stripped.
- Listening tests require every section to include an `audioFileId` that references a `listening_audio`
  file.
- Section reorder preserves unique positive `sectionOrder` values from `1..n`.
- Paragraph reorder preserves unique positive `paragraphOrder` values from `1..n`.
- Listening segment reorder preserves unique positive `segmentOrder` values from `1..n`.
- Question delete preserves unique positive `questionOrder` values from `1..n`.
- Teacher preview returns sanitized rich-text projections for section instructions, passage text,
  paragraph content, listening segments, question text, question instructions, option text, and
  answer explanations. Public/student preview reuses sanitized projections but hides answer keys,
  accepted variants, scores, and explanations.

Teacher test builder business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `TEACHER_APPROVAL_REQUIRED` | `403` | The teacher profile is missing or not approved. |
| `TEST_NOT_FOUND` | `404` | The test does not exist, is deleted, or does not belong to the teacher. |
| `TEST_SECTION_NOT_FOUND` | `404` | The section does not exist for the owned test. |
| `READING_PARAGRAPH_NOT_FOUND` | `404` | The paragraph does not exist for the owned section. |
| `LISTENING_SEGMENT_NOT_FOUND` | `404` | The listening segment does not exist for the owned section. |
| `QUESTION_NOT_FOUND` | `404` | The question does not exist for the owned section. |
| `TEACHER_TEST_STATUS_CONFLICT` | `409` | The requested edit, submit, or archive action is invalid for the current test status. |
| `VALIDATION_FAILED` | `400` | Metadata, section data, list filters/sort, submit readiness, or reorder input is invalid. |

### Admin Test Moderation

Admin moderation routes require the `admin` role and use the standard `{ data, meta }` envelope.

```http
GET  /api/v1/admin/moderation/tests?type=reading&teacherProfileId=302&difficulty=intermediate&submittedFrom=2026-06-01T00:00:00Z&submittedTo=2026-07-01T00:00:00Z&search=academic&page=0&size=20
GET  /api/v1/admin/moderation/tests/meta
GET  /api/v1/admin/moderation/tests/{testId}
POST /api/v1/admin/moderation/tests/{testId}/approve
POST /api/v1/admin/moderation/tests/{testId}/reject
```

The queue lists only `visibility=submitted` tests, sorted `updatedAt,asc` (oldest submissions first),
and supports optional filters for `type`, `teacherProfileId`, `difficulty`, a `submittedFrom`/
`submittedTo` window matched against `updatedAt` (the timestamp `submitForModeration()` last
touched), and a case-insensitive `search` match against the title.

`AdminModerationTestRDO` (queue item):

```json
{
  "id": 42,
  "teacherProfileId": 302,
  "teacherDisplayName": "Teacher Example",
  "title": "IELTS Academic Reading Practice 1",
  "type": "reading",
  "difficulty": "intermediate",
  "durationMinutes": 60,
  "priceAmount": 0.00,
  "currency": "UZS",
  "free": true,
  "visibility": "submitted",
  "submittedAt": "2026-06-21T17:00:17.29398Z",
  "createdAt": "2026-06-20T09:00:00Z"
}
```

`GET /api/v1/admin/moderation/tests/meta` returns `AdminModerationQueueMetaRDO`:

```json
{
  "pendingCount": 5,
  "oldestSubmittedAt": "2026-06-18T08:30:00Z"
}
```

`GET /api/v1/admin/moderation/tests/{testId}` and both decision endpoints return
`AdminModerationTestDetailRDO`, which wraps the same `TeacherTestRDO` shape the teacher builder
preview returns (full rich content, audio metadata, and `answerKeys`) plus a re-run readiness
report:

```json
{
  "test": { "...": "same shape as TeacherTestRDO, including sections[].questions[].answerKeys" },
  "readyForDecision": true,
  "validationFindings": {}
}
```

`readyForDecision=false` and a non-empty `validationFindings` map (field path to message, using the
same field keys `TeacherTestBuilderService.submit` validates) can appear on the preview even though
only fully-ready tests should have been submitted, because content or referenced files can change or
go missing between submission and review. This DTO is admin-only; the public/student catalog detail
(`TestDetailRDO`) never includes answer keys and is never built from this mapper.

Approve uses no request body. It only succeeds from `visibility=submitted`, re-runs the same
readiness validation `submit` enforces, and fails with `TEST_MODERATION_NOT_READY` if the test is no
longer ready (for example a referenced file was deleted after submission). On success it sets
`visibility=published`, stamps `publishedAt`/`reviewedByUserId`/`reviewedAt`, clears any prior
`moderationNote`, makes the test visible through the public catalog, and notifies the teacher.

Reject uses `TestModerationRejectCDO` and requires a non-blank reason:

```json
{
  "reason": "Listening audio is missing for section 2."
}
```

It only succeeds from `visibility=submitted`. On success it sets `visibility=rejected`, stores the
reason in `moderationNote`, stamps `reviewedByUserId`/`reviewedAt`, notifies the teacher, and leaves
the test in the same editable-on-next-edit `rejected` state the teacher builder already returns to
`draft` from (see Teacher Test Builder lifecycle rules above).

Approving or rejecting a test that is not currently `submitted` — including re-approving or
re-rejecting an already-decided test — fails with `TEST_MODERATION_STATUS_CONFLICT`.

Notification producer events for moderation decisions:

| Producer | Type | Recipient | Payload |
| --- | --- | --- | --- |
| Admin test moderation approve | `test_moderation` | Teacher user | `testId`, `status=published`, `targetPath=/teacher/tests/{id}` |
| Admin test moderation reject | `test_moderation` | Teacher user | `testId`, `status=rejected`, `rejectionReason`, `targetPath=/teacher/tests/{id}` |

Admin test moderation business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `TEST_NOT_FOUND` | `404` | The test does not exist. |
| `TEST_MODERATION_STATUS_CONFLICT` | `409` | The test is not `submitted`, so it cannot be approved or rejected. |
| `TEST_MODERATION_NOT_READY` | `400` | Approval was requested but readiness validation failed at decision time. |
| `VALIDATION_FAILED` | `400` | The rejection reason is missing or blank. |

### Admin Teacher Profiles

Admin teacher profile moderation and lifecycle endpoints require the `admin` role.

```http
GET /api/v1/admin/teacher-profiles?page=0&size=20&status=pending_approval
GET /api/v1/admin/teacher-profiles/{profileId}
POST /api/v1/admin/teacher-profiles/{profileId}/approve
POST /api/v1/admin/teacher-profiles/{profileId}/reject
POST /api/v1/admin/teacher-profiles/{profileId}/suspend
POST /api/v1/admin/teacher-profiles/{profileId}/reactivate
```

Approve is valid only from `pending_approval`; it records `approvedByUserId` and `approvedAt`.
Reject is valid only from `pending_approval` and uses `TeacherProfileModerationUDO`.

```json
{
  "rejectionReason": "Please add more teaching background."
}
```

Approve and reject also emit a `test_moderation` notification to the teacher user with
`payload.teacherProfileId`, `payload.status`, and `payload.targetPath`.

Suspend is valid only from `approved` and takes an optional `TeacherProfileSuspensionUDO` body:

```json
{
  "reason": "Repeated policy violations."
}
```

Reactivate is valid only from `suspended` and returns the profile to `approved`, keeping the
original `approvedByUserId`/`approvedAt`. Both actions return the updated `TeacherProfileRDO` and
emit an `account_status` notification to the teacher user with `payload.teacherProfileId`,
`payload.status`, `payload.reason` (when provided), and `payload.targetPath=/teacher/profile`.
Invalid transitions (for example suspending a profile that is not `approved`, or reactivating one
that is not `suspended`) return `409 TEACHER_PROFILE_STATUS_CONFLICT`.

Suspending a teacher profile immediately removes their content from public discovery: the public
marketplace and public test catalog queries only surface content from teacher profiles with
`status = approved`, so suspended-teacher listings and tests stop appearing without any separate
hide step. Historical purchases, results, and access grants are untouched.
This is separate from banning or reactivating the teacher's user account: account access is managed
through `PATCH /api/v1/admin/users/{userId}/status`, while teacher profile suspend/reactivate only
controls whether teacher-owned public content is visible.

The admin teacher profile detail endpoint (`GET /{profileId}`) returns `AdminTeacherProfileDetailRDO`
instead of a bare `TeacherProfileRDO`, adding content and earnings references for management screens:

```json
{
  "data": {
    "profile": { "id": 302, "displayName": "Teacher Example", "status": "approved" },
    "totalTestCount": 6,
    "publishedTestCount": 4,
    "wallet": {
      "id": 12,
      "teacherProfileId": 302,
      "balanceAmount": "150000.00",
      "pendingAmount": "0.00",
      "availableAmount": "150000.00",
      "currency": "UZS",
      "createdAt": "2026-06-21T17:00:17.29398Z",
      "updatedAt": "2026-06-21T17:00:17.29398Z"
    },
    "recentTests": [
      { "id": 55, "title": "IELTS Reading Practice 3", "visibility": "published", "updatedAt": "2026-06-21T17:00:17.29398Z" }
    ]
  }
}
```

`wallet` is omitted (`null`) when the teacher has no wallet yet (no earnings activity). Neither this
endpoint nor any other admin user/teacher view returns password hashes, tokens, provider secrets, or
private submitted answer content.

### Notifications

Notification endpoints require the matching route role. A student token can use only the student
notification route, a teacher token can use only the teacher route, and an admin token can use only
the admin route.

```http
GET /api/v1/student/notifications?page=0&size=20
GET /api/v1/teacher/notifications?page=0&size=20
GET /api/v1/admin/notifications?page=0&size=20
GET /api/v1/student/notifications/unread-count
GET /api/v1/teacher/notifications/unread-count
GET /api/v1/admin/notifications/unread-count
POST /api/v1/student/notifications/{notificationId}/read
POST /api/v1/teacher/notifications/{notificationId}/read
POST /api/v1/admin/notifications/{notificationId}/read
PATCH /api/v1/{student|teacher|admin}/notifications/{notificationId}/read
```

Lists are sorted by `createdAt,desc`, use zero-based pagination, and cap `size` at `100`.
Notification read commands are idempotent and return the notification with the original `readAt`
when it was already read.

`NotificationRDO`:

```json
{
  "id": 901,
  "type": "test_moderation",
  "title": "Teacher profile approved",
  "body": "Your teacher profile has been approved. You can now publish teacher content.",
  "payload": {
    "teacherProfileId": 302,
    "status": "approved",
    "targetPath": "/teacher/profile"
  },
  "readAt": null,
  "createdAt": "2026-06-21T17:00:17.29398Z"
}
```

Unread count returns `NotificationUnreadCountRDO`:

```json
{
  "unreadCount": 3
}
```

Current notification producer events:

| Producer | Type | Recipient | Payload |
| --- | --- | --- | --- |
| Admin teacher profile approve | `test_moderation` | Teacher user | `teacherProfileId`, `status=approved`, `targetPath` |
| Admin teacher profile reject | `test_moderation` | Teacher user | `teacherProfileId`, `status=rejected`, `rejectionReason`, `targetPath` |
| Admin teacher profile suspend | `account_status` | Teacher user | `teacherProfileId`, `status=suspended`, `reason` (optional), `targetPath` |
| Admin teacher profile reactivate | `account_status` | Teacher user | `teacherProfileId`, `status=approved`, `targetPath` |
| Admin user status change | `account_status` | Affected user | `status`, `reason` (optional), `targetPath=/account` |

Notification business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `NOTIFICATION_NOT_FOUND` | `404` | The notification does not exist or does not belong to the current user. |

### Admin Users

```http
GET /api/v1/admin/users?page=0&size=20&status=active&role=teacher&teacherStatus=approved&q=jane
Authorization: Bearer <accessToken>
GET /api/v1/admin/users/{userId}
Authorization: Bearer <accessToken>
```

Requires the `admin` role. Query parameters: `page` defaults to `0`, `size` defaults to `20`, and
`size` is capped at `100`. Sorting is currently fixed to `createdAt,desc`. Optional filters: `status`
(one of `active`, `suspended`, `banned`, `deleted`), `role` (one of `student`, `teacher`, `admin`,
`school_manager`, `moderator`), `teacherStatus` (one of `draft`, `pending_approval`, `approved`,
`rejected`, `suspended`), and `q` (case-insensitive partial match on email). `teacherStatus` matches
users with a teacher profile in that profile status. An unsupported `status`, `role`, or
`teacherStatus` value returns `400 VALIDATION_FAILED`. Effective filters are echoed under
`meta.filters`. Success: `200 OK` with an array of `AdminUserRDO` values in the standard paged
`{ data, meta }` envelope. The detail route returns `200 OK` with a single `AdminUserRDO`, or
`404 USER_NOT_FOUND` when the user does not exist. `AdminUserRDO.teacherStatus` is included for
teacher users that have a teacher profile and omitted otherwise.

```json
{
  "data": [
    {
      "id": 101,
      "email": "student@example.com",
      "fullName": "Student Example",
      "phone": null,
      "status": "active",
      "teacherStatus": "approved",
      "preferredLanguage": "en",
      "roles": ["teacher"],
      "createdAt": "2026-06-21T17:00:17.29398Z",
      "updatedAt": "2026-06-21T17:00:17.29398Z"
    }
  ],
  "meta": {
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 143,
      "totalPages": 8,
      "first": true,
      "last": false
    },
    "sort": [
      {
        "field": "createdAt",
        "direction": "desc"
      }
    ],
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

```http
PATCH /api/v1/admin/users/{userId}/status
Authorization: Bearer <accessToken>
```

Requires the `admin` role. Request body: `AdminUserStatusUDO`.

```json
{
  "status": "suspended",
  "reason": "Reported for abusive behavior."
}
```

Allowed statuses are `active`, `suspended`, `banned`, and `deleted`; `reason` is optional (max 2000
characters) and is included in the account-status notification when provided. Success: `200 OK` with
the updated `AdminUserRDO` in the standard `{ data, meta }` envelope. Setting `deleted` records
`deletedAt`; setting any other status clears `deletedAt`. A user with an active session immediately
loses protected-route access on their next request once their status leaves `active`, because
`AuthorizationService` re-checks the user's persisted status (not just JWT claims) on every
authenticated request.

Use this endpoint to ban, suspend, delete, or reactivate a teacher account itself. Use
`POST /api/v1/admin/teacher-profiles/{profileId}/suspend` and
`POST /api/v1/admin/teacher-profiles/{profileId}/reactivate` only for teacher profile/content
visibility moderation.

The last currently-active user holding the `admin` role cannot be moved to `suspended`, `banned`, or
`deleted` — the request fails with `409 LAST_ADMIN_PROTECTED` and no change is made. This check is
race-safe: when the target could be leaving active-admin status, all locks needed for the check
(including the target row itself) are acquired in a single ascending-id-ordered pass across every
currently-active admin row before counting, so two concurrent requests demoting different admins
always acquire their locks in the same relative order and cannot deadlock or both succeed.

Admin user business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `USER_NOT_FOUND` | `404` | The user does not exist. |
| `VALIDATION_FAILED` | `400` | An unsupported `status`, `role`, or `teacherStatus` filter or status value was supplied. |
| `LAST_ADMIN_PROTECTED` | `409` | The target is the only currently-active admin; the status change was rejected. |

### Admin Dashboard

```http
GET /api/v1/admin/dashboard?from=2026-06-01T00:00:00Z&to=2026-06-30T23:59:59Z
Authorization: Bearer <accessToken>
```

Requires the `admin` role; other roles receive `403 FORBIDDEN`. The endpoint is read-only and
aggregates operational metrics across existing domains through cheap indexed count/aggregate
queries. `from` and `to` are optional ISO-8601 date-times interpreted as UTC inclusive bounds. A
`to` earlier than `from` returns `400 VALIDATION_FAILED`.

Window semantics:

- `users.newInWindow`, all `payments` counts/totals, all `withdrawals` counts/totals, and all
  `aiUsage` totals are filtered by `createdAt` within the requested window.
- `users`, `teachers`, and `tests` status breakdowns, `queues`, and `recentActivity` reflect the
  current platform state so admin queue cards always show actionable counts.
- Effective filters are echoed in `meta.window`; omitted bounds are omitted from the echo.

Success: `200 OK` with `AdminDashboardRDO` and freshness metadata:

```json
{
  "data": {
    "users": {
      "total": 143,
      "active": 140,
      "suspended": 2,
      "banned": 1,
      "deleted": 0,
      "newInWindow": 12
    },
    "teachers": {
      "total": 18,
      "draft": 2,
      "pendingApproval": 3,
      "approved": 12,
      "rejected": 1,
      "suspended": 0
    },
    "tests": {
      "total": 64,
      "draft": 20,
      "submitted": 4,
      "published": 36,
      "rejected": 2,
      "archived": 2
    },
    "payments": {
      "total": 57,
      "pending": 3,
      "paid": 49,
      "failed": 4,
      "cancelled": 1,
      "refunded": 0,
      "amountMismatch": 1,
      "paidAmountTotal": 2450000.00
    },
    "withdrawals": {
      "total": 9,
      "requested": 2,
      "approved": 1,
      "rejected": 1,
      "paid": 5,
      "cancelled": 0,
      "requestedAmountTotal": 480000.00
    },
    "aiUsage": {
      "totalRequests": 310,
      "completedRequests": 290,
      "failedRequests": 14,
      "validationFailedRequests": 6,
      "blockedRequests": 6,
      "promptTokens": 402113,
      "completionTokens": 160441,
      "estimatedCost": 12.404500
    },
    "queues": {
      "pendingTeacherApprovals": 3,
      "pendingTestModeration": 4,
      "pendingWithdrawals": 2,
      "failedPayments": 4,
      "amountMismatchPayments": 1
    },
    "recentActivity": [
      {
        "type": "payment_paid",
        "referenceId": 501,
        "summary": "50000.00 UZS",
        "occurredAt": "2026-06-21T17:00:17.29398Z"
      },
      {
        "type": "test_submitted",
        "referenceId": 77,
        "summary": "IELTS Reading Mock 4",
        "occurredAt": "2026-06-21T16:40:02Z"
      }
    ]
  },
  "meta": {
    "asOf": "2026-06-21T17:00:17.29398Z",
    "window": {
      "from": "2026-06-01T00:00:00Z",
      "to": "2026-06-30T23:59:59Z"
    },
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

`recentActivity` contains up to 10 of the latest `user_registered`, `test_submitted`,
`payment_paid`, and `withdrawal_requested` events with non-sensitive summaries only. The dashboard
never exposes student answers, AI feedback text, or payout details; empty systems return zero
counts and empty lists instead of errors. Dashboard cards should link to the existing admin
queue/report endpoints for drill-down and actions.

### Admin Reports

Admin report endpoints require the `admin` role and return export-ready JSON with `generatedAt`,
effective `filters`, `totals`, and aggregate `rows` under the standard `{ data, meta }` envelope:

```http
GET /api/v1/admin/reports/ai-usage
GET /api/v1/admin/reports/sales
GET /api/v1/admin/reports/revenue
GET /api/v1/admin/reports/test-activity
GET /api/v1/admin/reports/provider-failures
Authorization: Bearer <accessToken>
```

All date filters are ISO-8601 date-times and are applied as inclusive UTC bounds. `to` before
`from`, unsupported enum values, unknown filter names, and ad hoc `sort` parameters return
`400 VALIDATION_FAILED`. Non-admin users receive `403 FORBIDDEN`.

Supported filters:

| Endpoint | Filters |
| --- | --- |
| `/ai-usage` | `from`, `to`, `studentUserId`, `feedbackType`, `status`, `provider`, `model` |
| `/sales` | `from`, `to`, `teacherProfileId`, `listingId`, `provider`, `paymentStatus` |
| `/revenue` | `from`, `to`, `teacherProfileId`, `listingId` |
| `/test-activity` | `from`, `to`, `teacherProfileId`, `testType` |
| `/provider-failures` | `from`, `to`, `provider`, `paymentStatus`, `status`, `model` |

Reports aggregate current persisted records only:

- AI usage reads `ai_feedbacks` and groups by date, user, feedback type, status, provider, model,
  and failure code so provider failures, validation failures, and usage-limit blocks stay separable.
- Sales reads `payments` and persisted `payment_disputes`, including paid/failed/refunded totals and dispute counts.
- Revenue reads `teacher_transactions` joined through purchases/listings for teacher gross, platform commission, and net totals.
- Test activity reads `test_attempts` joined to `tests` for attempt status and test type activity.
- Provider failures combine failed payment rows and failed AI feedback rows; no provider is called at report time.

Example response shape:

```json
{
  "data": {
    "generatedAt": "2026-07-10T04:10:44.695072Z",
    "filters": {
      "from": "2026-06-01T00:00:00Z",
      "to": "2026-06-30T23:59:59Z",
      "provider": "mock"
    },
    "totals": {
      "totalPayments": 57,
      "paidPayments": 49,
      "failedPayments": 4,
      "refundedPayments": 1,
      "grossAmount": 2710000.00,
      "paidGrossAmount": 2450000.00,
      "failedGrossAmount": 120000.00,
      "refundedGrossAmount": 50000.00,
      "marketplaceGrossAmount": 2710000.00,
      "liveSpeakingGrossAmount": 0,
      "disputeCount": 2
    },
    "rows": [
      {
        "activityDate": "2026-06-21",
        "teacherProfileId": 12,
        "listingId": 88,
        "targetType": "marketplace_listing",
        "provider": "mock",
        "status": "paid",
        "currency": "UZS",
        "totalPayments": 3,
        "grossAmount": 150000.00,
        "paidGrossAmount": 150000.00,
        "failedGrossAmount": 0,
        "refundedGrossAmount": 0,
        "disputeCount": 0
      }
    ],
    "liveSpeakingStatus": "Live-speaking booking payments are persisted; dedicated schedule/provider totals are reported through admin schedule support endpoints."
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Sales and provider reports may include persisted `payments.targetType=live_speaking_booking` rows.
Booking status, Meet-link readiness, and Google sync failures are inspected through
`/api/v1/admin/schedule/bookings` and `/api/v1/admin/schedule/provider-failures`.

### AI Usage

AI feature implementations must use the backend `AiUsageService` guard/accounting boundary before
calling any provider. Hard-limit blocks return `AI_USAGE_LIMIT_EXCEEDED` with HTTP `429` and are
audited without consuming successful usage. Disabled provider dependencies return `SERVICE_UNAVAILABLE`
before any network call and are audited with `failureCode=AI_PROVIDER_DISABLED`.

```http
GET /api/v1/student/ai-usage
Authorization: Bearer <accessToken>
```

Requires the `student` role. Success: `200 OK` with `AiUsageRemainingRDO`.

```json
{
  "data": {
    "periodStart": "2026-06-01",
    "periodEnd": "2026-06-30",
    "freeRequestsLimit": 20,
    "usedRequests": 3,
    "hardLimitRequests": 50,
    "freeRequestsRemaining": 17,
    "hardRequestsRemaining": 47,
    "hardLimitReached": false
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

```http
GET /api/v1/admin/ai-usage/report?page=0&size=20&studentUserId=101&status=completed
GET /api/v1/admin/ai-usage/report/totals?studentUserId=101
Authorization: Bearer <accessToken>
```

Requires the `admin` role. Report rows support optional filters: `studentUserId`, `feedbackType`,
`status`, `provider`, `from`, and `to`. `page` defaults to `0`, `size` defaults to `20`, and `size`
is capped at `100`. Sorting is fixed to `createdAt,desc`.

`GET /api/v1/admin/ai-usage/report` returns `AdminAiUsageReportRDO` rows in the standard paged
envelope. Each row includes `id`, `studentUserId`, `feedbackType`, `status`, `provider`, `model`,
`promptTokens`, `completionTokens`, `estimatedCost`, `failureCode`, `errorMessage`, `createdAt`, and
`completedAt`.

`GET /api/v1/admin/ai-usage/report/totals` returns `AdminAiUsageReportTotalsRDO` with
`totalRequests`, `completedRequests`, `failedRequests`, `validationFailedRequests`,
`blockedRequests`, `promptTokens`, `completionTokens`, and `estimatedCost`. `failedRequests`
counts failed AI attempts that are neither backend validation failures nor usage-limit blocks.

### AI Study Plans

Study-plan endpoints require the `student` role and use the standard `{ data, meta }` envelope. The
backend stores generated plans in `study_plans`, daily work in `study_plan_tasks`, and provider
usage/status/cost rows in `ai_feedbacks` with `feedbackType=study_plan`. Local/test generation uses
the mock provider by default; OpenAI generation requires `AI_STUDY_PLAN_PROVIDER=openai` and
`OPENAI_ENABLED=true`.

```http
POST /api/v1/student/study-plans
GET /api/v1/student/study-plans/{planId}
GET /api/v1/student/study-plans/active
GET /api/v1/student/study-plans/history
POST /api/v1/student/study-plans/{planId}/regenerate
POST /api/v1/student/study-plans/{planId}/tasks/{taskId}/complete
POST /api/v1/student/study-plans/{planId}/tasks/{taskId}/uncomplete
Authorization: Bearer <accessToken>
```

Create request body: `StudyPlanCDO`.

```json
{
  "targetBand": 7.0,
  "durationDays": 30,
  "availableMinutesPerDay": 60,
  "startsAt": "2026-07-01"
}
```

`targetBand` and `durationDays` are required. `availableMinutesPerDay` defaults to `60` when omitted,
and `startsAt` defaults to the current date. Only one active plan is allowed for the same student.
Provider output is accepted only when every task includes `date`, `skill`, `title`, `description`,
`estimatedMinutes`, `recommendedResource`, and `rationale`. Referenced tests must be free, published,
and visible; referenced listings must be active and either free or already accessible through purchase
or subscription. Sparse progress data must be labelled in `planSummary.missingSkills` and
`planSummary.uncertaintyNote`.

Success: `201 Created` with `StudyPlanRDO`.

```json
{
  "data": {
    "id": 77,
    "studentUserId": 101,
    "targetBand": 7.0,
    "currentBand": 6.5,
    "durationDays": 30,
    "weakestSkills": ["reading", "writing"],
    "planSummary": {
      "overview": "Focus on reading accuracy and Task 2 planning.",
      "uncertaintyNote": "Speaking has no recent AI feedback.",
      "missingSkills": ["speaking"]
    },
    "startsAt": "2026-07-01",
    "endsAt": "2026-07-30",
    "createdByFeedbackId": 9001,
    "tasks": [
      {
        "id": 501,
        "taskDate": "2026-07-01",
        "taskOrder": 1,
        "skill": "reading",
        "title": "Reading diagnostic review",
        "description": "Review recent reading mistakes and complete a targeted passage.",
        "estimatedMinutes": 45,
        "recommendedResource": {
          "type": "test",
          "id": 42,
          "title": "IELTS Academic Reading Practice 1"
        },
        "rationale": "Gap-fill mistakes appeared in recent results.",
        "completed": false,
        "completedAt": null
      }
    ],
    "createdAt": "2026-07-01T10:00:00Z",
    "updatedAt": "2026-07-01T10:00:00Z"
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

`GET /active` returns plans whose date window includes today. `GET /history` returns plans outside
that active window. Completing or uncompleting a task returns `StudyPlanTaskRDO`. Regeneration returns
`202 Accepted` with the refreshed `StudyPlanRDO`; completed tasks are preserved, and only incomplete
tasks from today forward are replaced after the new provider output has validated.

Study-plan business error codes:

| Code | Typical HTTP status | Meaning |
| --- | --- | --- |
| `VALIDATION_FAILED` | `400` | Target band, duration, available time, or request shape is invalid. |
| `STUDY_PLAN_ACTIVE_CONFLICT` | `409` | The student already has an active plan. |
| `STUDY_PLAN_NOT_FOUND` | `404` | The plan does not exist or belongs to another student. |
| `STUDY_PLAN_TASK_NOT_FOUND` | `404` | The task does not exist for the requested plan. |
| `INACCESSIBLE_RECOMMENDED_CONTENT` | `409` | Provider output referenced inactive, paid, or hidden content that is not accessible to the student. |
| `AI_STUDY_PLAN_VALIDATION_FAILED` | `503` | Provider JSON failed backend schema validation. |

### Writing Submissions And AI Feedback

Writing feedback endpoints require the `student` role and use the standard `{ data, meta }` envelope.
The backend stores the submitted prompt/answer in `writing_submissions` and the feedback lifecycle in
`ai_feedbacks` with `feedbackType=writing`. Local/test feedback uses the mock provider by default;
OpenAI feedback requires `AI_WRITING_PROVIDER=openai` and `OPENAI_ENABLED=true`.

```http
POST /api/v1/student/writing-submissions
GET /api/v1/student/writing-submissions/{submissionId}
GET /api/v1/student/writing-submissions/{submissionId}/feedback
POST /api/v1/student/writing-submissions/{submissionId}/feedback/retry
Authorization: Bearer <accessToken>
```

Create request body: `WritingSubmissionCDO`.

```json
{
  "taskType": "writing_task_2",
  "testAttemptId": 401,
  "prompt": "Some people think ... Discuss both views and give your opinion.",
  "answerText": "In recent years ..."
}
```

`taskType` must be `writing_task_1` or `writing_task_2`. Empty prompts or answers fail with
`VALIDATION_FAILED`. Unsupported task types fail with `UNSUPPORTED_WRITING_TASK_TYPE`. Successful
submits return `202 Accepted` with `WritingSubmissionRDO`; the current implementation processes the
mock provider synchronously but preserves `queued`, `processing`, `completed`, and `failed` job
statuses in the stored feedback record.

`WritingFeedbackRDO` exposes student-safe fields: `status`, `overallBand`, `criteriaScores`,
`mistakes`, `improvedAnswer`, `actionPlan`, `failureCode`, a generic `failureMessage`, provider/model
metadata, token/cost estimates, and retry linkage. Raw provider errors, hidden prompts, validation
diagnostics, and prompt-audit details stay internal for admin reports.

Provider responses must validate to the backend-owned JSON schema:

```json
{
  "overallBand": 6.5,
  "criteriaScores": {
    "task_response": {
      "band": 6.5,
      "feedback": "Addresses the prompt with relevant ideas.",
      "suggestions": ["Develop the second body paragraph with a clearer example."]
    },
    "coherence_and_cohesion": {
      "band": 6.0,
      "feedback": "Paragraphing is clear but linking is sometimes mechanical.",
      "suggestions": ["Use reference words instead of repeating the topic noun."]
    },
    "lexical_resource": {
      "band": 6.5,
      "feedback": "Vocabulary is adequate with occasional repetition.",
      "suggestions": ["Use more precise topic vocabulary where accurate."]
    },
    "grammatical_range_and_accuracy": {
      "band": 6.0,
      "feedback": "Meaning is clear despite recurring sentence errors.",
      "suggestions": ["Check subject-verb agreement and sentence boundaries."]
    }
  },
  "mistakes": [
    {
      "type": "grammar",
      "original": "people is",
      "correction": "people are",
      "explanation": "Use plural verb agreement with people."
    }
  ],
  "improvedAnswer": "A stronger version of the answer.",
  "actionPlan": ["Plan body paragraphs before writing.", "Review grammar in the final two minutes."]
}
```

Task 1 requires `task_achievement`; Task 2 requires `task_response`. Both require
`coherence_and_cohesion`, `lexical_resource`, and `grammatical_range_and_accuracy`. Malformed or
schema-invalid provider JSON marks feedback `failed`, stores internal validation errors, and leaves
student-visible band/result fields empty. Retrying a failed feedback creates a linked retry row with
`retryOfFeedbackId`; completed or active feedback is returned idempotently.

## File Storage

File endpoints require `Authorization: Bearer <accessToken>`. Uploads use `multipart/form-data` with
a `purpose` text field and a `file` part. Binary content is stored in MinIO, while the `files` table
stores metadata after storage succeeds.

```http
POST /api/v1/teacher/files
POST /api/v1/student/files
POST /api/v1/admin/files
GET /api/v1/files/{fileId}/download-url
```

Upload role and purpose rules:

| Route | Role | Allowed purposes |
| --- | --- | --- |
| `/api/v1/teacher/files` | `teacher` | `listening_audio`, `teacher_avatar`, `passage_image`, `diagram`, `document`, `course_material` |
| `/api/v1/student/files` | `student` | `speaking_audio` |
| `/api/v1/admin/files` | `admin` | `banner_image` |

Content-type and size validation:

| Purpose | Allowed content types | Max size |
| --- | --- | --- |
| `listening_audio` | `audio/aac`, `audio/m4a`, `audio/mp3`, `audio/mp4`, `audio/mpeg`, `audio/ogg`, `audio/wav`, `audio/webm`, `audio/x-m4a`, `audio/x-wav` | 100 MiB |
| `speaking_audio` | Same audio types as `listening_audio` | 25 MiB |
| `teacher_avatar` | `image/jpeg`, `image/png`, `image/webp` | 5 MiB |
| `passage_image` | `image/jpeg`, `image/png`, `image/webp` | 10 MiB |
| `diagram` | `image/jpeg`, `image/png`, `image/webp` | 10 MiB |
| `document` | `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | 25 MiB |
| `course_material` | Document types plus the audio types above | 100 MiB |
| `banner_image` | `image/jpeg`, `image/png`, `image/webp` | 8 MiB |

Successful uploads return `201 Created` with `FileRDO` in the standard `{ data, meta }` envelope:

```json
{
  "data": {
    "id": 501,
    "ownerUserId": 101,
    "purpose": "speaking_audio",
    "originalName": "answer.webm",
    "contentType": "audio/webm",
    "sizeBytes": 45123,
    "durationSeconds": null,
    "checksum": null,
    "createdAt": "2026-06-21T17:00:17.29398Z"
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

`GET /api/v1/files/{fileId}/download-url` returns a short-lived MinIO signed URL:

```json
{
  "data": {
    "fileId": 501,
    "url": "http://localhost:9000/mock-exams-local/...",
    "expiresInSeconds": 900
  },
  "meta": {
    "requestId": "01J2Z4M3KJ9Y8Q7P6N5R4T3S2A"
  }
}
```

Privacy rules: `speaking_audio` is private to the owning user and returns `NOT_FOUND` to other
authenticated users. `course_material` files referenced by marketplace listing items require access
to at least one active listing containing that file; otherwise the download URL request returns
`PAYMENT_REQUIRED`, `ACCESS_EXPIRED`, or `LISTING_NOT_ACCESSIBLE` from the shared access boundary.
Other file purposes, and course materials not attached to listings, are shareable authenticated
assets and may return signed URLs to any active authenticated user. Missing files also return
`NOT_FOUND`. Invalid purposes, content types, empty files, and oversized files return
`VALIDATION_FAILED`. Storage dependency failures return `SERVICE_UNAVAILABLE`.

## Starter Compatibility Example

The current starter app exposes an exam list before the full envelope conventions are implemented.
This shape is useful for local development history, but it is not the target shape for new product APIs.

```http
GET /api/v1/exams
```

Compatibility route:

```http
GET /api/exams
```

Current starter response:

```json
[
  {
    "id": 1,
    "title": "Algebra Fundamentals",
    "subject": "Mathematics",
    "durationMinutes": 60,
    "questionCount": 40,
    "createdAt": "2026-06-21T17:00:17.29398Z"
  }
]
```

When this endpoint is migrated into the product API, it should move to the relevant route prefix and
return a `data` plus `meta` envelope like other list endpoints.
