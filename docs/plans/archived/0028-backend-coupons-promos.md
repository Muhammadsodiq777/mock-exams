# Backend Coupons Promos

> Support teacher and platform discount codes.

## Depends On

- `0025-backend-payments-provider-integration.md`

## Context

Coupons and promos affect payment calculation before a provider payment is created. They must be validated atomically so expired, overused, or wrong-teacher codes cannot reduce the amount incorrectly or be redeemed twice.

## Scope

### In scope

- Promo code CRUD for teachers and admins.
- Percent and fixed discounts.
- Expiration, usage limit, and active flag.
- Redemption tracking.
- Apply code to payment calculation.

### Out of scope

- Complex campaign segmentation.
- Referral rewards.
- Stacking multiple promo codes unless explicitly added later.
- Provider-native coupon systems.

## Key Decisions

- Promo validation belongs in a service called by payment creation before provider checkout is created.
- Store redemption rows for successful application and make redemption idempotent per student/payment or purchase.
- Teacher-owned codes apply only to that teacher's listings unless an admin/platform code explicitly allows broader use.
- Fixed-amount discounts cannot reduce a payable amount below zero; zero-amount outcomes should either become free purchase grants or a zero payment according to the payment plan contract.
- Usage-limit checks and increment/redemption creation must be transactional.
- Promo code matching should be case-insensitive after normalization while preserving display code.

## What To Implement

- Promo validation service.
- Redemption persistence.
- Discount calculation in payment creation.
- Admin and teacher list endpoints.
- Teacher CRUD endpoints for own codes and admin CRUD endpoints for platform-wide or teacher-scoped codes.
- Validation rules for percent/fixed shape, date windows, usage limits, teacher ownership, active flag, currency, and listing compatibility.
- Payment quote/application DTO that shows original amount, discount amount, final amount, and reason when invalid.
- Repository indexes for active code lookup and redemption history.
- Business errors for invalid, expired, inactive, exhausted, wrong-owner, and currency-mismatched codes.

## Tests

- Expired code is rejected.
- Usage limit is enforced.
- Teacher code cannot discount another teacher's listing unless configured.
- Redemption is recorded once.
- Fixed discount cannot create negative payable amount.
- Concurrent redemptions cannot exceed usage limit.
- Inactive listing/payment target cannot redeem a promo.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Keep promo DBML synced.
- Update schema SQL if promo constraints, indexes, or redemption shape changes.
- Keep payment creation examples aligned with promo quote/application behavior.

## Acceptance Criteria

- Student can apply valid promo code at checkout.
- Teachers can manage their own promo codes.
- Promo use is auditable and cannot exceed configured usage or ownership rules.
