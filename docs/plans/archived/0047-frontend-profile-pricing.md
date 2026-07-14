# Frontend Profile Pricing

> Build student profile, purchase history, and pricing surfaces.

## Depends On

- `0024-backend-purchases-and-access-control.md`
- `0025-backend-payments-provider-integration.md`

## Context

Profile and pricing surfaces let students understand account basics, purchase history, active access, subscriptions, and how to initiate payment for premium content. These pages must align with access-control and payment states rather than inventing local purchase logic.

## Scope

### In scope

- `/profile`.
- Purchase history.
- Basic account info.
- `/pricing`.
- Premium or subscription plan display.
- Payment CTA state.
- Active access/subscription state and payment status feedback.
- Safe handling for payment disabled/mock/provider redirect states.

### Out of scope

- Payment provider embedded widget until provider is chosen.
- Account deletion flow.
- Refund/dispute detail UI beyond linking to support/admin-owned flows.

## Key Decisions

- Payment initiation is a typed service call; components should not build provider URLs manually.
- Purchase history comes from student APIs and includes safe payment/access status, not raw provider metadata.
- Pricing displays backend/configured prices and currencies, not hardcoded UI values.
- Payment feature flag controls visible payment UI when backend/provider is not enabled.
- Profile account editing beyond basic display should be a later explicit plan unless backend endpoints exist.

## What To Implement

- Profile services and hooks.
- Pricing card components.
- Purchase history table.
- Payment redirect or checkout initiation hook.
- Profile route with account summary, active purchases/subscriptions, and recent payments.
- Pricing route with plans/listings/subscriptions from backend or platform settings.
- Payment creation mutation with pending/loading/error/success redirect state.
- Access status badges and empty purchase-history state.
- Error handling for `PAYMENT_REQUIRED`, provider disabled, failed payment creation, and validation errors.

## Tests

- Empty purchase history renders cleanly.
- Pricing displays configured currency.
- Payment creation error is shown.
- Successful payment creation uses provider redirect/checkout URL when returned.
- Disabled payment feature hides or disables payment CTA with clear state.
- Purchase history pagination/filtering works when backend supports it.

## Documentation Sync

- Keep payment DTOs aligned.
- Mirror purchase, payment, pricing, and subscription DTOs in frontend types.
- Keep `docs/API_CONTRACT.md` and `docs/frontend-dev.md` aligned if checkout behavior changes.

## Acceptance Criteria

- Student can understand current purchases and available paid plans.
- Student can initiate checkout or see why checkout is unavailable without losing context.
