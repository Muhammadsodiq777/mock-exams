---
name: frontend-developer
description: >
  Frontend implementation skill for Mock Exams. Use when implementing or changing React/TypeScript
  code under `frontend/`, especially pages, components, services, hooks, stores, types, routing,
  API integration, stateful behavior, or frontend verification. Requires frontend-review before work
  is considered done.
---

# Frontend Developer

Use this skill to implement frontend changes cleanly and safely.

## Required Skills

- Use `react-typescript-guidelines` for app structure, services, hooks, state, types, i18n, Tailwind, and verification.
- Use `red-green-tdd` for logic-bearing behavior changes and bug fixes.
- Use `dependency-refresh` when changing or auditing frontend dependency versions.

## Workflow

1. Read the relevant docs and nearby frontend code before editing.
2. Add or update the narrowest useful test or verification path for logic-bearing behavior.
3. Keep API calls in `services/`, reusable data helpers in `hooks/`, stores in `store/`, backend-facing types in `types/`, and page orchestration in `pages/`.
4. Follow the role-first folder rule; split crowded folders by purpose inside the same role folder, such as `pages/student/`, `components/test-builder/`, `services/admin/`, `hooks/attempts/`, or `types/payments/`.
5. Handle loading, empty, error, and success states explicitly.
6. Run lint/build or narrower useful checks while iterating.
7. Run or self-apply `frontend-review` before finishing; fix clean-code, correctness, performance, packaging, architecture, accessibility, and verification issues it finds.

## Output

Report the implementation summary, changed files, verification results, and whether frontend review passed or what remains.
