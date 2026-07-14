---
name: frontend-review
description: >
  Frontend review skill for Mock Exams. Use after every frontend implementation and when reviewing
  React/TypeScript changes for clean code, correctness, error-free behavior, performance, folder
  organization, architecture, accessibility, API state flow, and verification coverage.
---

# Frontend Review

Review only the frontend changes and directly relevant nearby code.

## Required Skills

- Use `react-typescript-guidelines` as the repository convention source.
- Use `dependency-refresh` when dependency or platform versions changed.

## Review Checklist

- Correctness: user-visible behavior, route guards, form flow, validation, API errors, empty states.
- Architecture: API calls in `services/`, data/state helpers in `hooks/`, stores in `store/`, backend types in `types/`, page orchestration in `pages/`.
- Packaging: pages under `pages/`, UI under `components/`, API access under `services/`, hooks under `hooks/`, stores under `store/`, and types under `types/`; crowded folders split by purpose inside those folders.
- Performance: avoid unnecessary rerenders, unstable query keys, repeated expensive work, and stale cache behavior.
- Accessibility and UX: keyboard access, focus flow, labels, responsive behavior, loading/error/empty/success states.
- Verification: lint/build and targeted tests or browser checks for changed behavior.

## Output

Return findings first, ordered by severity, with file references. If no issues are found, say so and mention residual verification risk.
