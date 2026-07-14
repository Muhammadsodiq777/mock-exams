---
name: backend-developer
description: >
  Backend implementation skill for Mock Exams. Use when implementing or changing Java/Spring code
  under `backend/`, especially controllers, services, DTOs, mappers, repositories, entities,
  validation, API behavior, or backend tests. Requires backend-review before work is considered done.
---

# Backend Developer

Use this skill to implement backend changes cleanly and safely.

## Required Skills

- Use `java-spring-guidelines` for package layout, DTOs, controllers, services, persistence, errors, and verification.
- Use `red-green-tdd` for logic-bearing changes and bug fixes.
- Use `dependency-refresh` when changing or auditing backend dependency versions.

## Workflow

1. Read the relevant docs and nearby backend code before editing.
2. Add or update the narrowest useful failing test for logic-bearing changes.
3. Implement the smallest clean change that satisfies the behavior.
4. Keep controllers thin, business rules in services, mapping in mappers, and persistence behind repositories.
5. Follow the class-type package rule: controllers in `controller/`, services in `service/`, repositories in `repository/`, entities in `entity/`, DTOs in `dto/`, and mappers in `mapper/`; use purpose subpackages inside those folders when they grow.
6. Run targeted verification while iterating and final backend verification when appropriate.
7. Run or self-apply `backend-review` before finishing; fix clean-code, correctness, performance, packaging, architecture, and test issues it finds.

## Output

Report the implementation summary, changed files, verification results, and whether backend review passed or what remains.
