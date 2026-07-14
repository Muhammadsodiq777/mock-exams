# Migration Verification Notes

`platform-architecture.dbml` is the target schema source for the platform migrations. The migration verification test intentionally checks a focused set of ERD anchors instead of repeating every column from the DBML.

Current verification anchors:

- Core identity and access tables: `users`, `roles`, `user_roles`
- Teacher and file tables: `teacher_profiles`, `files`
- Test engine tables: `tests`, `test_sections`, `questions`
- Attempt and result tables: `test_attempts`, `test_results`
- Marketplace, payment, AI, and settings tables: `marketplace_listings`, `purchases`, `payments`, `ai_feedbacks`, `platform_settings`
- Relationship/uniqueness checks: user-role foreign keys, test section ordering, selected-answer option/question integrity, attempt-to-result uniqueness, review target uniqueness, and representative payment/listing links

`DbmlSyntaxTest` provides repo-local DBML syntax coverage without network-installed tooling. It validates the top-level DBML declarations, field/index blocks, and relationship statements used by `platform-architecture.dbml`. If a full DBML CLI/parser is later vendored into the repository, wire it into the same migration verification path.

The Docker-backed PostgreSQL migration test is intentionally gated behind `-Dmockexams.migration.postgres.enabled=true`. The default local path still replays migrations against H2 in PostgreSQL mode; when the PostgreSQL property is enabled, Docker/Testcontainers failures are real test failures rather than silent skips.
