# Agents Setup

Repository-local skills live under `.agents/skills/`.

New agents: before starting work, read the skill that matches your task, plus the guidelines
skill for the layer you touch. Every implementation must pass the matching review skill before
it is considered done.

- `review`: unified change review; routes to `backend-review`/`frontend-review` and adds cross-layer contract, schema, and docs checks
- `backend-developer` / `backend-review`: implementation and mandatory review skills for backend work
- `frontend-developer` / `frontend-review`: implementation and mandatory review skills for frontend work
- `java-spring-guidelines`: backend implementation conventions and packaging rules
- `react-typescript-guidelines`: frontend implementation conventions and folder rules
- `red-green-tdd`: focused TDD loop for logic-bearing changes
- `backplan`: backend/API-led planning
- `orchestrate`: approved plan decomposition and delegation
- `dependency-refresh`: version audit and upgrade workflow

These skills are written against the current Mock Exams repository structure and command set.
The authoritative repo docs are `docs/backend-dev.md`, `docs/frontend-dev.md`,
`docs/API_CONTRACT.md`, `docs/architecture-boundaries.md`, `docs/package-inventory.md`,
and `docs/specs/`.

Packaging rule summary: backend code is class-type first (`controller`, `service`,
`repository`, `entity`, `dto`, `mapper`) with purpose subpackages inside those folders. Frontend
code is role-first by source responsibility (`pages`, `components`, `services`, `hooks`, `store`,
`types`, `utils`, `constants`), with purpose subfolders only inside those roles.
