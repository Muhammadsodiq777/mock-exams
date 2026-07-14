# Mock Exams Product Overview

This document is the product-level source of truth for the Mock Exams platform. It captures the intended product, roles, modules, MVP boundary, and implementation order before detailed architecture, backend, frontend, and testing plans are written. Living backend and frontend ownership boundaries are maintained in `docs/architecture-boundaries.md`.

## 1. Product Vision

Build an AI-powered IELTS preparation platform with three connected systems:

- IELTS testing for Listening, Reading, Writing, Speaking, and full mock exams
- Teacher marketplace for free and paid tests, packs, courses, and practice materials
- Personalized improvement system that turns test results into feedback, study plans, and recommended teacher content

The core learning loop is:

1. Student takes a test.
2. Platform scores the attempt.
3. Student receives explanations, AI feedback, and weak-area analysis.
4. Platform recommends practice tasks, study plans, and teacher content.
5. Student improves and repeats the loop.

The platform should not be only a test-solving tool. It should help students understand what to fix, how to fix it, and when paid teacher content is useful.

## 2. Main Roles

### Student

Students use the platform to practice IELTS, receive feedback, and track progress.

Core capabilities:

- Register and log in
- Take official IELTS-style tests
- Take teacher-created tests
- Submit Writing Task 1 and Task 2 answers
- Record Speaking Part 1, Part 2, and Part 3 answers
- Receive AI feedback for writing and speaking
- Buy premium tests and teacher-created content
- Track progress by IELTS skill
- View leaderboard and streaks
- Use Telegram bot features later

### Teacher

Teachers create content, sell test packs, review student results, and earn money.

Core capabilities:

- Register and log in with an initial draft or pending-approval teacher profile
- Create tests and test packs
- Create paid and free content
- Upload listening audio
- Add reading passages
- Add questions, answers, scoring, and explanations
- Set prices and visibility
- See student attempts and results
- Earn money from paid content
- Request withdrawals
- Create coupons and promotions

### Admin

Admins control quality, moderation, payments, disputes, AI usage, and platform settings.

Core capabilities:

- Approve or reject teachers
- Moderate submitted tests and teacher content
- Manage users and teachers
- Manage test categories and tags
- Manage payments, withdrawals, and disputes
- Monitor AI costs and usage limits
- Configure platform commission
- Ban users or teachers
- Create official platform tests
- Manage banners, promo codes, complaints, reports, and settings

## 3. Product Modules

### IELTS Test System

Supported test types:

- Listening
- Reading
- Writing Task 1
- Writing Task 2
- Speaking Part 1
- Speaking Part 2
- Speaking Part 3
- Full Mock Test

Supported question types:

- Multiple choice
- True / False / Not Given
- Yes / No / Not Given
- Matching headings
- Matching information
- Sentence completion
- Summary completion
- Table completion
- Diagram labeling
- Short answer
- Gap fill
- Ordering
- Essay answer
- Audio answer

The test engine must support both official platform tests and teacher-created tests.

### Teacher Test Builder

The teacher test builder is a core product differentiator. Teachers must be able to create flexible IELTS-style test structures without developer help.

Test fields:

- Title
- Description
- Test type
- Difficulty
- Estimated IELTS band level
- Duration
- Price
- Free or paid flag
- Language
- Tags
- Visibility status: Draft, Submitted, Published, Rejected, Archived

Example:

- Title: IELTS Reading Mock Test #1
- Type: Reading
- Duration: 60 minutes
- Price: 15,000 UZS
- Visibility: Draft

Section structure:

- Reading: Section 1, Section 2, Section 3
- Listening: Part 1, Part 2, Part 3, Part 4
- Speaking: Part 1 questions, Part 2 cue card, Part 3 discussion questions
- Writing: Task 1 prompt, Task 2 prompt

Question fields:

- Question text
- Question type
- Options when required
- Correct answer or answer patterns
- Explanation
- Score
- Order number

Listening-specific fields:

- Audio file
- Start time
- End time
- Optional transcript

Reading-specific fields:

- Passage text
- Optional paragraph labels
- Related questions

IELTS-style Reading tests require exactly three passage sections before submission.

Publishing flow:

1. Teacher creates draft.
2. Teacher previews the test.
3. Teacher submits the test for moderation.
4. Admin approves or rejects the test.
5. Approved test becomes published.
6. Students buy or solve the test.
7. Teacher earns revenue from paid attempts or purchases.

### Marketplace

Teachers can sell:

- Single tests
- Test packs
- Writing courses
- Speaking question packs
- Vocabulary packs
- Full mock exams
- Subscription plans

Revenue split:

- Teacher earns 70-80%
- Platform keeps 20-30%

Example:

- Test price: 20,000 UZS
- Platform commission: 25%
- Teacher earns: 15,000 UZS
- Platform earns: 5,000 UZS

Marketplace flows must support free content, paid content, coupons, reviews, and later refunds or disputes.
Paid marketplace checkout starts as a provider-backed payment record with a listing amount/currency
snapshot, signature-verified callback, idempotent access grant confirmation, and one idempotent
teacher wallet earning transaction per paid marketplace purchase. Teacher earnings store gross,
platform commission, net, currency, purchase id, and payment id at confirmation time.
Teacher-led live speaking availability can be promoted in marketplace discovery later, but booking
state and session price snapshots belong to the live speaking schedule domain rather than listing
records.

### AI Writing Evaluation

Students submit Writing Task 1 or Task 2 answers.

AI feedback should include:

- Overall band
- Task Achievement or Task Response
- Coherence and Cohesion
- Lexical Resource
- Grammatical Range and Accuracy
- Mistakes and corrections
- Improved version
- Action plan

The system must track AI usage per user and allow admins to configure free usage limits and cost controls.

### AI Speaking Evaluation

Students record audio for speaking practice.

Processing flow:

1. Student uploads or records audio.
2. System stores the audio file.
3. Speech-to-text service transcribes the answer.
4. AI evaluates the transcript and speaking criteria.
5. Student receives band estimate and feedback.

AI speaking feedback should include:

- Fluency and coherence
- Pronunciation
- Vocabulary
- Grammar
- Repeated mistakes
- Better answer example

### AI Study Plan

After several test attempts, the system should generate personalized study recommendations.

Example:

- Current band: 5.5
- Target band: 7.0
- Weakest skill: Writing Task 2
- Plan duration: 30 days
- Daily task: practice opinion essay introductions
- Recommended content: teacher writing pack or official Writing Task 2 test

## 4. Student Experience

### Dashboard

Student dashboard should show:

- Current estimated IELTS band
- Recent tests
- Weak areas
- Daily task
- Progress chart
- Recommended teacher tests
- Purchased content
- AI usage remaining

The backend student progress dashboard is exposed at `GET /api/v1/student/progress`. It builds the
current estimate, Reading/Listening histories, weak question types, recent completed attempts, and
rule-based next-practice recommendations from stored attempt/result data without rescoring attempts.
Missing skills are labelled instead of counted as zero, and Writing/Speaking AI feedback can plug into
the same dashboard model later.

AI study plans are exposed as stored student resources under `/api/v1/student/study-plans`. They are
generated from recent scored attempts, weak areas, Writing/Speaking AI feedback, missing skills,
target band, available study time, and accessible content. Daily tasks are stored as structured rows
with skill, date, estimated minutes, recommendation, and rationale. If evidence is sparse, the plan
must label uncertainty and recommend diagnostics instead of inventing skill scores. Refreshing a plan
replaces incomplete future work only, preserving completed task history.

### Test Page

The test page must feel close to real IELTS test conditions.

Required features:

- Timer
- Question navigation
- Passage or audio area
- Answer sheet
- Save progress
- Submit button
- Clear state for unanswered questions
- Responsive layout for desktop and mobile

### Result Page

Result page should show:

- Raw score
- Estimated IELTS band
- Correct and incorrect answers
- Explanations
- Time spent
- Weak question types
- Suggested practice
- Recommended teacher content when relevant

Reading and Listening objective attempts are auto-scored by the backend after submission/result
request using normalized answer keys. Result review may show correct answers and explanations only
after the attempt is submitted or expired and scored. Current band estimates use a configurable
Reading/Listening raw-score conversion table and are platform estimates, not IELTS-certified scores.

### Progress Tracking

The system should track:

- Listening score history
- Reading score history
- Writing score history
- Speaking score history
- Overall band estimate
- Weak question types
- Completion streaks later

## 5. Teacher Experience

Teacher dashboard should show:

- Published tests
- Draft tests
- Tests waiting for moderation
- Sales
- Earnings
- Student attempts
- Reviews
- Coupons
- Withdrawal status

Teacher test creation should support:

- Reading and Listening test creation in MVP
- Writing and Speaking content later
- Audio uploads for Listening
- Rich text passages and prompts
- Question ordering
- Answer keys
- Explanations
- Preview before submission

## 6. Admin Experience

Admin panel modules:

- Users
- Teachers
- Tests
- Moderation
- Payments
- Withdrawals
- Reports
- AI usage
- Complaints
- Banners
- Promo codes
- Settings

Admin settings:

- Platform commission
- AI free usage limit
- Premium price
- Teacher withdrawal minimum
- Moderation rules
- Payment settings

Admin reports backend:

- `/api/v1/admin/reports/ai-usage` aggregates stored AI feedback usage, statuses, tokens, and cost by date, user, type, provider, and model.
- `/api/v1/admin/reports/sales` reconciles payment totals, marketplace gross, refund markers, and dispute counts from persisted payments and disputes.
- `/api/v1/admin/reports/revenue` reports teacher/listing gross, commission, and net revenue from teacher wallet transactions.
- `/api/v1/admin/reports/test-activity` reports persisted test attempt activity by teacher and test type.
- `/api/v1/admin/reports/provider-failures` combines failed payment and failed AI feedback provider signals.
- Live-speaking payments persist booking/payment/provider state; detailed booking and Meet-link support views live under admin schedule endpoints.

## 7. Suggested Frontend Scope

Current repository direction is React + Vite. Next.js can be reconsidered in architecture planning, but the first plan should decide whether to keep Vite or migrate.

Student pages:

- `/`
- `/login`
- `/register`
- `/dashboard`
- `/tests`
- `/tests/:id`
- `/results/:id`
- `/writing`
- `/speaking`
- `/marketplace`
- `/teachers/:id`
- `/pricing`
- `/profile`

Teacher pages:

- `/teacher/dashboard`
- `/teacher/tests`
- `/teacher/tests/create`
- `/teacher/tests/:id/edit`
- `/teacher/schedule`
- `/teacher/earnings`
- `/teacher/students`
- `/teacher/profile`

Admin pages:

- `/admin/dashboard`
- `/admin/users`
- `/admin/teachers`
- `/admin/tests`
- `/admin/moderation`
- `/admin/payments`
- `/admin/withdrawals`
- `/admin/reports`
- `/admin/settings`

Frontend requirements:

- Tailwind CSS
- PWA support later
- Component library decision in architecture plan, with Ant Design as one candidate
- Responsive layouts
- Explicit loading, empty, error, and success states
- Clear separation between services, types, pages, and components

## 8. Suggested Backend Scope

Current repository direction is Java 21, Spring Boot 3, PostgreSQL, and Docker.

Backend modules:

- Auth module
- User module
- Teacher module
- Test module
- Question module
- Attempt module
- Result module
- AI Evaluation module
- Payment module
- Wallet module
- Marketplace module
- Admin module
- Notification module
- Telegram bot module
- File storage module

Infrastructure:

- PostgreSQL for primary data
- Redis for sessions, rate limits, queues, or caching where needed
- MinIO for files, audio, and uploaded assets
- JWT auth
- OpenAI API for AI evaluation
- Docker Compose for local services
- ClickHouse later for analytics, not MVP

## 9. Main Data Areas

Core entities:

- users
- roles
- teachers
- teacher_profiles
- tests
- test_sections
- questions
- question_options
- question_answers
- test_attempts
- student_answers
- test_results
- writing_submissions
- speaking_submissions
- ai_feedbacks
- payments
- teacher_wallets
- teacher_transactions
- withdrawal_requests
- promo_codes
- reviews
- notifications
- files

Detailed entity relationships, indexes, constraints, and lifecycle rules should be created in a separate ERD document during architecture planning.

## 10. Development Phases

### Phase 1: MVP

Build first:

- Auth
- Student dashboard
- Teacher dashboard
- Teacher creates Reading and Listening tests
- Student solves Reading and Listening tests
- Auto-check answers
- Result page
- Admin moderation
- Basic payment

### Phase 2: AI

Add:

- Writing AI checker
- Speaking audio upload
- Speech-to-text
- Speaking AI feedback
- AI usage limits
- AI cost tracking

### Phase 3: Marketplace

Add:

- Teacher paid tests
- Teacher wallet
- Platform commission
- Withdrawal requests
- Reviews
- Coupons
- Purchase history

### Phase 4: Growth

Add:

- Telegram bot
- Leaderboard
- Daily streak
- AI study plan
- Referral system
- School or learning-center accounts

## 11. Best MVP Boundary

The MVP should not start with every feature. The first competitive version should include:

1. Student login
2. Teacher login
3. Teacher creates Reading and Listening tests
4. Student solves tests
5. Platform auto-checks objective answers
6. Writing AI checker
7. Speaking AI checker
8. Teacher earns from paid tests
9. Admin approves tests before publishing

This MVP is large enough to validate the product loop while avoiding unnecessary early complexity.

## 12. Implementation Order

The implementation should be planned in small numbered documents after this overview. Each plan should be narrow enough to implement, review, and test without mixing unrelated domains.

### Step 1: Architecture and Contracts

Create:

- `docs/plans/0001-architecture-boundaries.md`
- `docs/plans/0002-database-erd-and-migrations.md`
- `docs/plans/0003-api-contract-conventions.md`
- `docs/plans/0004-local-dev-and-infrastructure.md`

These plans should decide:

- Monolith module boundaries
- Package structure
- Auth and permission model
- Database ownership and migration order
- API route, DTO, pagination, and error conventions
- Storage strategy for files and audio
- Redis usage
- AI integration boundaries
- Payment and wallet boundaries
- Moderation lifecycle
- Frontend app structure
- Docker and local development shape

### Step 2: Backend Foundation

Create:

- `docs/plans/0005-backend-auth-registration-login.md`
- `docs/plans/0006-backend-users-roles-permissions.md`
- `docs/plans/0007-backend-teacher-profiles-approval.md`
- `docs/plans/0008-backend-file-storage-minio.md`
- `docs/plans/0009-backend-notifications-basics.md`

These plans should define:

- User registration and login
- JWT session behavior
- Student, teacher, and admin roles
- Teacher profile lifecycle
- Admin teacher approval
- File metadata and upload flow
- Basic notification model for teacher moderation decisions first, with the same persistence service
  intended for later test moderation, payment, withdrawal, and AI feedback events

### Step 3: Backend Test Engine

Create:

- `docs/plans/0010-backend-test-catalog.md`
- `docs/plans/0011-backend-test-builder-core.md`
- `docs/plans/0012-backend-reading-test-builder.md`
- `docs/plans/0013-backend-listening-test-builder.md`
- `docs/plans/0014-backend-question-types-answer-keys.md`
- `docs/plans/0015-backend-test-attempt-lifecycle.md`
- `docs/plans/0016-backend-auto-scoring-results.md`
- `docs/plans/0017-backend-progress-analytics.md`

These plans should define:

- Tests, sections, questions, options, and answers
- Reading passages, rich-text passage/question content, sanitized rendered previews, and optional paragraph labels
- Listening audio segments and transcripts
- Draft, preview, submitted, approved, published, rejected, and archived states
- Attempt start, save-progress, submit, and expire behavior
- Auto-checking for objective questions
- Result summaries, weak question types, and progress history

### Step 4: Backend AI

Create:

- `docs/plans/0018-backend-ai-usage-limits-costs.md`
- `docs/plans/0019-backend-writing-submissions-ai-feedback.md`
- `docs/plans/0020-backend-speaking-audio-transcription.md`
- `docs/plans/0021-backend-speaking-ai-feedback.md`
- `docs/plans/0022-backend-ai-study-plan.md`

These plans should define:

- AI request accounting
- Free usage limits
- Admin cost controls
- Writing evaluation schema and feedback fields
- Speaking audio submission and transcription
- Speaking feedback schema
- Personalized study plan inputs and outputs

### Step 5: Backend Marketplace and Payments

Create:

- `docs/plans/0023-backend-marketplace-catalog.md`
- `docs/plans/0024-backend-purchases-and-access-control.md`
- `docs/plans/0025-backend-payments-provider-integration.md`
- `docs/plans/0026-backend-teacher-wallet-commission.md`
- `docs/plans/0027-backend-withdrawals.md`
- `docs/plans/0028-backend-coupons-promos.md`
- `docs/plans/0029-backend-reviews-ratings.md`

These plans should define:

- Paid and free marketplace listings
- Single test, pack, course, and question-pack products
- Student purchases and access checks
- Payment lifecycle
- Platform commission
- Teacher wallet transactions
- Withdrawal requests
- Coupons, promo codes, reviews, and ratings

### Step 6: Backend Admin and Moderation

Create:

- `docs/plans/0030-backend-admin-dashboard.md`
- `docs/plans/0031-backend-test-moderation.md`
- `docs/plans/0032-backend-user-teacher-management.md`
- `docs/plans/0033-backend-payment-disputes.md`
- `docs/plans/0034-backend-platform-settings.md`
- `docs/plans/0035-backend-reports-ai-usage.md`

These plans should define:

- Admin dashboard metrics
- Test moderation queues
- User and teacher management
- Payment dispute workflows
- Platform settings
- AI usage reports
- Sales and learning reports
- Provider failure, teacher revenue, and test activity reports from persisted records

### Step 7: Frontend Foundation

Create:

- `docs/plans/0036-frontend-app-shell-routing.md`
- `docs/plans/0037-frontend-auth-session.md`
- `docs/plans/0038-frontend-api-client-types.md`
- `docs/plans/0039-frontend-design-system.md`

These plans should define:

- Route tree
- Protected route behavior
- Session store
- API service structure
- Shared DTO types
- Layouts, forms, buttons, tables, empty states, and error states

### Step 8: Frontend Student Experience

Create:

- `docs/plans/0040-frontend-student-dashboard.md`
- `docs/plans/0041-frontend-test-list-marketplace.md`
- `docs/plans/0042-frontend-reading-test-player.md`
- `docs/plans/0043-frontend-listening-test-player.md`
- `docs/plans/0044-frontend-results-progress.md`
- `docs/plans/0045-frontend-writing-practice-ai.md`
- `docs/plans/0046-frontend-speaking-practice-ai.md`
- `docs/plans/0047-frontend-profile-pricing.md`

These plans should define:

- Dashboard cards and charts
- Test discovery and marketplace browsing
- IELTS-style Reading UI
- IELTS-style Listening UI
- Result review and weak-area display
- Writing and Speaking AI flows
- Profile and pricing pages

### Step 9: Frontend Teacher Experience

Create:

- `docs/plans/0048-frontend-teacher-dashboard.md`
- `docs/plans/0049-frontend-teacher-test-list.md`
- `docs/plans/0050-frontend-test-builder-shell.md`
- `docs/plans/0051-frontend-reading-builder.md`
- `docs/plans/0052-frontend-listening-builder.md`
- `docs/plans/0053-frontend-builder-preview-submit.md`
- `docs/plans/0054-frontend-teacher-earnings-students.md`

These plans should define:

- Teacher dashboard
- Draft and published test management
- Builder navigation and autosave
- Reading passage and question editing
- Listening audio and segment editing
- Preview and submit for moderation
- Earnings and student results

### Step 10: Frontend Admin Experience

Create:

- `docs/plans/0055-frontend-admin-dashboard.md`
- `docs/plans/0056-frontend-admin-teacher-approval.md`
- `docs/plans/0057-frontend-admin-test-moderation.md`
- `docs/plans/0058-frontend-admin-payments-withdrawals.md`
- `docs/plans/0059-frontend-admin-settings-reports.md`

These plans should define:

- Admin metrics
- Teacher approval UI
- Test moderation review UI
- Payment and withdrawal screens
- Platform settings and reports

### Step 11: Testing and Release Quality

Create:

- `docs/plans/0060-testing-backend-unit-integration.md`
- `docs/plans/0061-testing-api-contract.md`
- `docs/plans/0062-testing-frontend-components.md`
- `docs/plans/0063-testing-e2e-critical-flows.md`
- `docs/plans/0064-testing-ai-payment-file-storage.md`
- `docs/plans/0065-release-readiness-checklist.md`

These plans should define:

- Backend service and controller coverage
- Database migration verification
- API contract coverage
- Frontend component tests
- End-to-end flows for student, teacher, and admin
- AI mock strategy
- Payment sandbox strategy
- File upload verification
- Manual release checklist

## 13. Open Questions

- Should the frontend stay on React + Vite, or should the project migrate to Next.js before major feature work?
- Which payment provider will be used first for Uzbekistan payments?
- Which speech-to-text provider should be used for Speaking AI?
- Should AI feedback be generated immediately or through an async job queue?
- Should teacher content purchases be per test, per pack, subscription-based, or all three?
- What exact IELTS band conversion tables should be used for Reading and Listening?
- What moderation rules should automatically reject low-quality teacher tests?
- What is the first supported language besides English, if any?

## 14. Success Criteria

The first version is successful when:

- A student can register, buy or open a test, solve it, and see a meaningful result.
- A teacher can create a Reading or Listening test, submit it for moderation, and earn from paid usage.
- An admin can approve or reject teacher content.
- Writing and Speaking AI feedback give students actionable improvement steps.
- The platform records progress and recommends the next useful practice.
