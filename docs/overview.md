# Mock Exams Overview

Mock Exams is a Spring Boot and React platform for IELTS-style practice exams, teacher-created content, marketplace flows, and AI-assisted feedback.

The detailed product source of truth is `docs/specs/project-overview.md`. Current implementation is an early development starter with:

- PostgreSQL-backed Spring Boot API
- Flyway-managed `mock_exam` seed table
- React frontend that reads available exams from the backend
- Docker Compose PostgreSQL for local development

Use the plans under `docs/plans/` for implementation sequencing.
