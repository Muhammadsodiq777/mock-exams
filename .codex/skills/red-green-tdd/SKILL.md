---
name: red-green-tdd
description: >
  Test-driven development workflow for Mock Exams implementation work. Use when adding or changing
  logic-bearing backend or frontend behavior, service rules, API contracts, data mapping, validation,
  stateful UI behavior, or bug fixes that can be protected by tests. Do not trigger for docs-only,
  purely visual, formatting-only, or exploratory tasks.
---

# Red-Green TDD

Use this as the default workflow for logic-bearing changes.

## Loop

1. RED: add or update the narrowest useful failing test for the intended behavior.
2. GREEN: make the smallest production change that passes that test.
3. REFACTOR: clean up duplication or awkward structure only after the test passes.

Keep each loop focused on one observable behavior.

## Backend

- Prefer service-level tests for business rules and controller tests for HTTP contract behavior.
- Use existing test fixtures, builders, and Spring test slices before introducing new test infrastructure.
- Run targeted tests during the loop: `mvn -Dtest=ClassName test` from `backend/`.
- Run `mvn test` once at the end when backend code changed.

## Frontend

- Prefer focused component, hook, or utility coverage when available.
- When the repo has no unit-test harness for the touched UI, use TypeScript, lint, build, and targeted browser checks as the closest useful feedback.
- Run `npm run lint` and `npm run build` from `frontend/` before finishing frontend changes.
- Run `npm run e2e` only when the changed flow needs browser-level coverage.

## Exceptions

- Do not force a new test framework into a small change.
- If no practical narrow test exists, state the gap and use the closest verification command.
- Do not run broad suites during every inner loop.
