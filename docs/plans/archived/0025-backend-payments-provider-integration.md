# Backend Payments Provider Integration

> Add a provider boundary for creating and confirming payments.

## Depends On

- `0023-backend-marketplace-catalog.md`
- `0024-backend-purchases-and-access-control.md`

## Context

Payments connect student checkout to access grants, teacher earnings, and support workflows. The first implementation should introduce a provider boundary and durable payment lifecycle without baking the platform into one vendor's callback shape.

## Scope

### In scope

- Payment creation for marketplace listing.
- Payment creation for live teacher speaking bookings after `0067` adds booking records.
- Provider abstraction for first Uzbekistan-compatible provider.
- Webhook or callback confirmation.
- Payment status transitions.
- Metadata storage.
- Provider configuration from application properties first; admin-editable settings are added in `0034-backend-platform-settings.md`.

### Out of scope

- Multiple provider routing.
- Refund automation.
- Google Calendar or Meet integration.
- Storing provider secrets in database settings.

## Key Decisions

- Use `PaymentService` as the domain boundary and `PaymentProviderClient` as the external adapter boundary.
- Store provider ids, status, amount, currency, and safe metadata in `payments`; do not store card/bank secrets.
- Payment creation validates target availability and price at creation time and stores the charged amount snapshot.
- Webhooks/callbacks must be idempotent and signature-verified when the provider supports it.
- Confirmed paid status dispatches to the target domain exactly once: marketplace purchase grant first, live speaking booking later.
- Payment failures do not grant access or earnings; they remain visible in student/admin payment history.
- Local development uses a mock provider unless `PAYMENT_ENABLED=true` with non-dev credentials.

## What To Implement

- `PaymentService`.
- `PaymentProviderClient` interface.
- Create-payment endpoint.
- Webhook endpoint with signature verification if provider supports it.
- Call purchase grant from `0024` after confirmed paid status.
- Extensible payment target metadata so a payment can be for a marketplace listing, subscription, or live speaking booking without ambiguous JSON-only coupling.
- Idempotent confirmation dispatch that calls the correct domain handler exactly once.
- Student create-payment endpoint and payment status/detail endpoints.
- Provider webhook/callback controller under an appropriate public/provider route with safe validation.
- Payment target resolver for listing purchase, subscription, and future speaking booking targets.
- Status transition rules for `pending`, `paid`, `failed`, `cancelled`, and `refunded` markers.
- Admin-safe payment read models for support screens.

## Tests

- Pending payment is created with provider ID.
- Paid callback updates status once.
- Failed callback does not grant access.
- Invalid webhook signature is rejected.
- Paid callback for a speaking booking confirms the booking only once and handles expired holds through the booking policy.
- Duplicate paid webhook does not create duplicate purchase, wallet, or booking side effects.
- Payment amount/currency mismatch is rejected or flagged according to provider semantics.
- Disabled provider cannot create real payments.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update backend dev docs with sandbox setup.
- Keep payment target examples aligned with `0067` when live speaking bookings are added.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if payment fields, indexes, or target relations change.
- Keep purchase/access plan `0024`, wallet plan `0026`, and frontend checkout/profile plans aligned.

## Acceptance Criteria

- Payment flow can create pending payment and mark it paid through provider callback.
- Payment confirmation can safely fan out to marketplace access or live speaking booking confirmation.
- Provider failures and duplicate callbacks leave business state consistent and support-visible.
