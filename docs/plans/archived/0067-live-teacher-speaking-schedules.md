# Live Teacher Speaking Schedules

> Let teachers sell, announce, and conduct scheduled live IELTS speaking tests with students using Google Calendar and Google Meet.

## Context

The platform currently plans AI speaking practice through uploaded recordings and transcription. A separate high-value path is teacher-led live speaking tests: teachers publish available time slots, students book and pay, the system creates calendar/meeting artifacts, and both sides join a Google Meet session at the scheduled time.

This feature crosses backend persistence, payments, teacher wallet commission, notifications, public marketplace discovery, student booking UX, teacher schedule management, and external Google Calendar/Meet integration. It should reuse existing user, teacher profile, payment, purchase/access, wallet, notification, and settings conventions where possible.

## Scope

### In scope

- Teacher availability blocks and bookable live speaking slots.
- Slot pricing, currency, duration, capacity of one student per IELTS speaking test session, and visibility/announcement controls.
- Student browse, filter, reserve, pay, book, cancel, and view upcoming/past live speaking sessions.
- Google Calendar event creation and Google Meet conference link generation for confirmed bookings.
- Teacher and student notifications for booking, payment confirmation, cancellation, reschedule, reminder, and meeting-link availability.
- Teacher dashboard schedule view with upcoming bookings, earnings references, and join links.
- Admin visibility into live speaking bookings for support, disputes, refunds, and provider diagnostics.
- Backend integration boundary for Google Calendar/Meet with local mock implementation.
- Frontend teacher schedule manager, public/student booking flow, and student/teacher session detail pages.

### Out of scope

- In-app video calling.
- Real-time transcription or AI scoring during the live teacher session.
- Multi-teacher group classes.
- Automated refund provider integration beyond the existing payment/dispute hooks.
- Full Google Workspace domain administration.

## Key Decisions

- Own the backend under existing class-type packages with purpose subpackages as needed, for example `controller/teacher`, `controller/student`, `service/schedules`, `dto/schedules`, `entity/schedules`, and `repository/schedules`.
- Model live speaking as its own schedule/booking domain, not as `speaking_submissions` and not as an AI feedback variant.
- Use the existing payment flow from `0025` and purchase/access concepts from `0024` only after deciding whether a booking creates a purchase, a dedicated booking payment, or both. The sensible MVP path is a dedicated `speaking_session_bookings` row linked to `payments`, then wallet transactions reuse `0026` on paid confirmation.
- Store money on the slot/booking at booking time so later teacher price changes do not alter existing student bookings.
- Use Google OAuth per teacher when teachers need events on their own calendar. For MVP, support a platform-owned Google Calendar as a fallback if per-teacher OAuth is not ready, but clearly store which calendar owner mode created each event.
- Keep Google provider calls out of transactions. Persist booking/payment state first, then create/update external calendar events through an idempotent integration job keyed by booking id.
- Use UTC timestamps in the API and database; frontend renders teacher/student local time zones.
- Prevent double booking with a database uniqueness constraint and service-level transaction/locking around slot reservation and payment confirmation.
- Announced slots are public only for approved teachers. Draft/private slots are visible only to the teacher owner.
- Cancellation policy is settings-driven: teacher cancellation, student cancellation window, no-show marking, and refund eligibility are explicit statuses rather than implicit deletes.

## What To Implement

1. Add persistence model and migrations:
   - `teacher_availability_blocks` for recurring or one-off availability drafts.
   - `speaking_session_slots` for concrete bookable slots with teacher profile, start/end, duration, timezone, price, currency, status, announcement/public visibility, and capacity.
   - `speaking_session_bookings` for student booking state, payment link, price snapshot, Google event id, Meet URL, cancellation/no-show fields, and timestamps.
   - `google_calendar_connections` or equivalent for teacher/platform OAuth metadata without storing raw secrets in ordinary settings tables.
   - Integration job/audit table if provider calls are async or retryable.
2. Add enums for slot status and booking status, such as `draft`, `announced`, `reserved`, `booked`, `completed`, `cancelled`, `expired`, `no_show`, and provider sync statuses.
3. Add teacher endpoints:
   - Create/update/delete availability blocks.
   - Generate slots from availability.
   - Create one-off slot.
   - Set price and announce/unannounce slots.
   - List calendar view and booking detail.
   - Cancel or mark complete/no-show with rules.
   - Connect/disconnect Google Calendar when per-teacher OAuth is enabled.
4. Add public/student endpoints:
   - Browse announced speaking slots by teacher, date range, price, language, rating, and timezone.
   - Reserve a slot for a short hold window.
   - Create payment for a reserved slot.
   - Confirm booking after payment success.
   - View student bookings and meeting details.
   - Cancel according to policy.
5. Add admin endpoints:
   - List bookings and provider sync failures.
   - View booking/payment/provider detail for support.
   - Override cancellation or refund eligibility only through explicit audited commands.
6. Integrate payments and wallets:
   - Payment confirmation moves booking to booked if the slot is still available or gracefully refunds/flags support if the hold expired.
   - Teacher wallet transaction uses the price snapshot and live-speaking commission setting.
   - Existing payment disputes can reference speaking bookings in metadata or through a dedicated relation when implemented.
7. Integrate Google Calendar/Meet:
   - Provider client creates one calendar event per confirmed booking with teacher and student details, event time, and Meet conference data.
   - Store provider event id, Meet URL, sync status, last error, and retry count.
   - Idempotently update/cancel the event when booking status changes.
   - Local development uses a mock provider and never calls Google unless explicitly enabled.
8. Add notifications and reminders:
   - Booking reserved, payment confirmed, Meet link ready, cancellation, reschedule, reminder before start, and no-show/complete notifications.
   - Payloads include booking id and target path, never hidden provider secrets.
9. Add frontend surfaces:
   - `/teacher/schedule` calendar/list manager with slot creation, price controls, announce actions, booking detail, and Google connection state.
   - Public/student live speaking slot list and booking checkout flow.
   - Student upcoming/past live speaking sessions with join link visibility.
   - Teacher upcoming sessions with join link and completion/no-show actions.
   - Admin support table for bookings/provider sync failures.
10. Add validation and concurrency rules:
    - Slot end must be after start; duration must match configured allowed durations.
    - Price must be non-negative and positive when marked paid.
    - Teacher cannot announce overlapping slots.
    - Student cannot book overlapping sessions.
    - One slot cannot be booked by two students.
    - Expired holds cannot be paid as normal bookings.
    - Google failure does not lose a paid booking; it leaves booking booked with `meet_link_pending` or provider failure status visible to support.

## Documentation Sync

- Update `docs/specs/project-overview.md` with teacher-led live speaking as a core speaking marketplace feature.
- Update `docs/API_CONTRACT.md` with teacher, public/student, admin, payment, notification, and Google connection DTOs and endpoints.
- Update `docs/ERD/platform-architecture.dbml` with availability, slot, booking, Google connection, and provider sync tables.
- Add ordered Flyway schema SQL after current migrations for schedule/booking tables and constraints.
- Update `docs/backend-dev.md` with Google Calendar/Meet environment variables, local mock mode, and OAuth setup notes.
- Update `docs/frontend-dev.md` with schedule feature routes, Google enabled flag, and date/time rendering guidance.
- Update testing plans `0060`, `0061`, `0063`, and `0064` with live speaking schedule coverage.

## Acceptance Criteria

- Approved teacher can create slots, set a price, announce availability, and see bookings.
- Student can find announced speaking slots, reserve one, pay, and receive a confirmed booking with a Google Meet link or clear pending provider status.
- Double booking is impossible under concurrent reservation/payment attempts.
- Teacher price changes affect only future slots/bookings, not existing booking price snapshots.
- Calendar/Meet provider failures are retryable and visible to teacher/admin without corrupting booking/payment state.
- Teacher wallet earnings are created exactly once for a paid completed or policy-eligible booking.
- Student and teacher receive notifications for booking lifecycle changes and reminders.
- Admin can inspect booking, payment, and Google sync state for support.
