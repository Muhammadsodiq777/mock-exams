---
name: dependency-refresh
description: >
  Dependency and platform upgrade workflow for Mock Exams. Use when the user asks to update, audit,
  modernize, or check latest releases for backend or frontend dependencies, package managers,
  Java/Spring, React/Vite, Playwright, or TypeScript. Requires
  verifying current upstream versions before recommending or changing version pins.
---

# Mock Exams Dependency Refresh

Use this skill for dependency/version work. Verify current upstream state before editing dependency files.

## Workflow

1. Read `backend/pom.xml`, `frontend/package.json`, lockfiles, and the relevant code using the dependency.
2. Verify current upstream versions from official docs, Maven metadata, npm metadata, or vendor release notes.
3. Classify each candidate as patch, minor, or major.
4. Prefer low-risk patch upgrades first; do not bundle unrelated major migrations into feature work.
5. For major upgrades, create or request a plan that covers code changes, migration guides, lockfile updates,
   config changes, tests, and rollback risk.
6. Update docs and skills when the accepted project baseline changes.

## Guardrails

- Do not assume "latest" is compatible with this app.
- Do not perform major version upgrades without an explicit upgrade task.
- For TanStack Query v5 code, prefer `isLoading` over deprecated `isInitialLoading`.
- Run `npm install` only when changing frontend dependency pins or lockfiles.
- Run Maven dependency resolution/build only when changing backend dependency pins.

## Verification

- Backend dependency change: `mvn test` from `backend/`.
- Frontend dependency change: `npm run lint` and `npm run build` from `frontend/`.
- Browser tooling or Vite change: verify `npm run dev` serves the app when dev server/build behavior is affected (there is no `e2e` script yet).
