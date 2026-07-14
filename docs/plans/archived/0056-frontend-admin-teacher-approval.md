# Frontend Admin Teacher Approval

> Build admin UI for teacher review and approval.

## Depends On

- `0007-backend-teacher-profiles-approval.md`
- `0032-backend-user-teacher-management.md`

## Context

Teacher approval is the admin gate before teachers can publish content and earn money. The UI must make pending profiles easy to review, show enough profile detail to decide, and preserve rejection reasons/status updates from the backend.

## Scope

### In scope

- `/admin/teachers`.
- Pending teacher queue.
- Teacher profile detail.
- Approve and reject actions.
- Reject reason form.

### Out of scope

- Teacher content moderation.
- User banning/suspension beyond links to user management.

## Key Decisions

- Approval queue uses admin teacher profile APIs and pageable table patterns from the design system.
- Reject requires a reason and should keep the admin in context after submission.
- Approve/reject mutations invalidate teacher queues, dashboard metrics, and relevant teacher detail queries.
- Teacher profile detail must not expose sensitive account data beyond the admin contract.
- Approved/rejected status changes should be reflected immediately without optimistic lies if backend rejects the transition.

## What To Implement

- Teacher admin service and hooks.
- Approval table.
- Review modal/detail page.
- Mutation success and error states.
- Filters by status, search text, submitted date, and possibly language/subject if backend exposes them.
- Profile detail view with bio, headline, avatar, approval history/rejection reason, and account status.
- Approve/reject action dialogs with validation and disabled/loading states.
- Empty/loading/error states and route protection.

## Tests

- Reject requires reason.
- Approve updates list status.
- Backend validation appears in form.
- Non-admin receives forbidden state.
- Reject reason persists and displays after mutation.
- Mutations invalidate dashboard/teacher queue queries.

## Documentation Sync

- Keep teacher status DTOs aligned.
- Mirror admin teacher profile DTOs in frontend types.
- Keep admin routes aligned with app shell.

## Acceptance Criteria

- Admin can approve or reject teachers without backend console access.
- Approval decisions are clear, validated, and reflected across admin queues.
