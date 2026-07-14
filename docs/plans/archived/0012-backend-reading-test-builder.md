# Backend Reading Test Builder

> Add IELTS Reading passage and paragraph support to the builder.

## Depends On

- `0011-backend-test-builder-core.md`

## Context

The current builder stores plain section `instructions`, `passage_text`, paragraph `content`, question text, and explanations. Reading passages need richer formatting for IELTS-style informative passages: headings, emphasis, lists, tables, inline images/diagrams by file reference, paragraph labels, and safe links. The backend should treat rich text as structured content, not arbitrary HTML.

## Scope

### In scope

- Reading section passage text.
- Structured rich text for passage bodies, instructions, paragraph content, question prompts, answer explanations, and optional informative teacher/admin notes.
- Optional paragraph labels.
- Link questions to paragraph labels.
- Validate Reading tests have 3 sections for IELTS-style mocks.
- Sanitized student, teacher, and admin preview read models that render the same content safely.

### Out of scope

- Auto-generated passages.
- Collaborative editing and revision history.

## Key Decisions

- Store rich text as a versioned JSON document model with a server-sanitized rendered HTML projection if the frontend editor needs it; do not persist unsanitized HTML from the browser.
- Keep plain-text fallback fields or generated excerpts for search, list cards, and accessibility labels.
- File references inside rich text must point to existing `files` rows with an allowed purpose such as `passage_image`, `diagram`, or `document`.
- Student-facing preview must omit answer keys while teacher/admin preview may include authoring metadata and answer explanations according to route permissions.
- Submission readiness must validate rich-text minimum content after stripping formatting, so an empty paragraph with only tags does not pass.

## What To Implement

- Reading section DTOs.
- Paragraph CRUD and ordering.
- Validation service for Reading test readiness.
- Preview response with passage and related questions.
- Rich-text DTO fields such as `contentDocument`, `contentText`, and `renderedHtml` where the API contract chooses them, applied consistently to passages, instructions, paragraph content, question prompts, and explanations.
- Rich-text sanitizer service with an allowlist for headings, paragraphs, lists, tables, emphasis, block quotes, links, and backend-owned image/file embeds.
- Migration updates for rich-text JSONB columns or a dedicated reusable content table, plus indexes for search fallback text where needed.
- Mapper rules that return role-appropriate preview data for teacher owner preview, admin moderation preview, and student test player preview.

## Tests

- Duplicate paragraph label in a section is rejected.
- Question cannot link to a missing paragraph label.
- Reading test submission fails without sections.
- Rich text with unsafe tags, inline scripts, external image URLs, or unknown file ids is rejected or sanitized according to the chosen contract.
- Rich text that renders visually but has no meaningful text does not satisfy required passage/question fields.
- Teacher/admin preview renders rich passages consistently, while student preview never includes answer keys.

## Documentation Sync

- Update `docs/API_CONTRACT.md`.
- Update `docs/specs/project-overview.md` if Reading rules change.
- Update `docs/ERD/platform-architecture.dbml` and both schema SQL files when rich-text persistence is added.
- Update `docs/frontend-dev.md` with the selected rich-text editor contract when the frontend implementation lands.

## Acceptance Criteria

- Teacher can create Reading sections with passages.
- Student preview data can render passage and questions together.
- Rich-text passages, diagrams, and explanations render safely in teacher preview, admin moderation preview, and student test player views.
- Backend rejects unsafe or empty rich-text payloads with `VALIDATION_FAILED`.
