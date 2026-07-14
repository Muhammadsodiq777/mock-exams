# Backend Reviews Ratings

> Let students review purchased or attempted teacher content.

## Depends On

- `0024-backend-purchases-and-access-control.md`

## Context

Reviews help students evaluate teacher content and help teacher profiles show trust signals. Review eligibility must be tied to real access or completed attempts so public ratings cannot be manipulated by accounts that never used the content.

## Scope

### In scope

- Create review for test or teacher after access.
- One review per student per test.
- Update teacher rating average and count.
- Public reviews list.
- Student update/delete-own-review behavior if selected for MVP.
- Teacher dashboard read-only review list.
- Admin hide/delete hook for moderation or support.

### Out of scope

- Review replies.
- Abuse detection automation.
- Weighted or ML-based rating ranking.
- Anonymous public posting.

## Key Decisions

- A student can review a test after purchase/access or after submitting an attempt for that test.
- A student can review a teacher after purchasing/attempting that teacher's content or completing a live speaking session once `0067` lands.
- Store individual reviews and update aggregate rating fields through one service method.
- Public review lists should expose only non-hidden reviews and safe user display information.
- Duplicate reviews should be handled by an explicit update endpoint or return a clear conflict; do not silently overwrite.
- Admin moderation should hide/delete reviews without breaking aggregate consistency.

## What To Implement

- Review service.
- Rating aggregation update.
- Public and teacher dashboard review endpoints.
- Admin delete/hide hook for moderation later.
- DTOs for review create/update, public review item, teacher review summary, and admin review moderation item.
- Eligibility checks using `AccessControlService`, submitted attempts, and future speaking bookings.
- Aggregate recalculation or transactional incremental update that remains correct after update/hide/delete.
- Pagination and filters by teacher, test, rating, date, and visibility where relevant.
- Business errors for not eligible, duplicate review, invalid rating, review not found, and forbidden access.

## Tests

- Student cannot review content they never accessed.
- Rating must be within allowed range.
- Duplicate review is rejected or updated by explicit endpoint.
- Teacher rating aggregate updates.
- Hidden/deleted review is excluded from public list and aggregate.
- Student cannot review as another student or review a draft/unpublished test.
- Teacher can read reviews for own content but cannot edit student comments.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update `docs/ERD/platform-architecture.dbml` and schema SQL if review visibility/moderation fields are added.
- Update `docs/specs/project-overview.md` if eligibility or public rating behavior changes.
- Keep frontend marketplace/profile plans aligned.

## Acceptance Criteria

- Marketplace can display ratings and review comments.
- Rating aggregates remain accurate after create, update, hide, or delete.
- Reviews are limited to students with real eligible interaction.
