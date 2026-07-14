# Backend Purchases and Access Control

> Grant students access to paid content after purchase.

## Depends On

- `0023-backend-marketplace-catalog.md`

## Context

Paid content must be protected consistently across starting attempts, reading listing items, downloading course materials, and viewing purchase history. This plan creates the access-control boundary before provider-specific payment confirmation exists, so later payment and subscription flows can grant access through one service.

## Scope

### In scope

- Purchase records and access-control model for listings.
- Access checks for tests and listing items.
- Free listing access.
- Manual/internal paid purchase grant hook for later payment integration.
- Subscription access window support.

### Out of scope

- Provider-specific payment creation and callbacks.
- Refunds and disputes.
- Live speaking booking access; the schedule plan owns bookings while payments may share confirmation infrastructure.

## Key Decisions

- Purchases grant access to marketplace listings and their items; attempts and file downloads should call `AccessControlService` instead of duplicating purchase checks.
- Free active listings can grant access without a payment, but the behavior must be idempotent and auditable.
- Paid access is granted by an internal service method that payment confirmation calls later; public student endpoints must not self-grant paid access.
- Subscription access is time-window based and checked against UTC instants.
- Purchase history is student-owned and must not expose provider metadata beyond safe payment/status fields.
- Duplicate purchase attempts for the same listing/student return the existing access record or a documented conflict.

## What To Implement

- `AccessControlService` used by attempt, file, and listing flows.
- Purchase creation API/service that can be called manually now and by payment success flow in `0025`.
- Student purchase history endpoint.
- Idempotent purchase handling.
- Access check integration points for starting paid tests, viewing listing items, and retrieving protected file download URLs.
- Internal grant method with source metadata: free listing, manual/admin grant if allowed, payment id, subscription id.
- DTOs for purchase summary/detail and access status used by marketplace/test detail CTAs.
- Business errors such as `PAYMENT_REQUIRED`, `PURCHASE_NOT_FOUND`, `ACCESS_EXPIRED`, and `LISTING_NOT_ACCESSIBLE`.
- Repository indexes for student purchase history and listing access lookups.

## Tests

- Student cannot start paid test without access.
- Internal paid purchase grant gives access and is idempotent.
- Duplicate purchase is idempotent.
- Expired subscription denies access.
- Student cannot read another student's purchase history.
- Paid listing items remain inaccessible until payment confirmation calls the internal grant.
- Revoked/inactive listing behavior is documented and enforced for already purchased content.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Reference access checks from attempt lifecycle plan.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if purchase/subscription fields or indexes change.
- Keep payment plan `0025` and frontend pricing/profile plans aligned.

## Acceptance Criteria

- Paid content is protected consistently.
- Student can see purchased content.
- All paid-content flows call the same access boundary and return consistent errors.
