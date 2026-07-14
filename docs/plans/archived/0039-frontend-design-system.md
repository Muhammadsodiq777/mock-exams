# Frontend Design System

> Add reusable UI primitives and layout patterns.

## Depends On

- `0036-frontend-app-shell-routing.md`

## Context

The app will include dense student players, teacher builders, admin tables, payment screens, and dashboards. A small reusable design system is needed before these pages land so controls, loading states, dialogs, tables, and validation feedback stay consistent.

## Scope

### In scope

- Buttons, inputs, selects, tabs, tables, cards for repeated items, modals, badges, empty states, alerts, loaders, and pagination.
- Responsive page containers.
- IELTS test-player layout primitives.
- Admin table patterns.
- Form field wrappers with labels, help text, validation errors, and disabled/loading states.
- Rich-content display primitives for sanitized backend HTML/documents where later builder/player plans need them.

### Out of scope

- Full visual redesign.
- One-off page content.
- Pixel-perfect component library documentation site.

## Key Decisions

- Components live under `components/ui` unless they are domain-specific and actually reused within a feature area.
- Use Tailwind utilities and shared component props rather than introducing a heavy UI framework without a separate decision.
- Keep cards for repeated items and framed tools; dashboards/admin surfaces should remain dense and scannable.
- Every input/control must support loading, disabled, error, and accessible labeling states.
- Avoid role/domain language inside generic UI primitives.

## What To Implement

- Components under `components/ui`.
- Tailwind tokens and utility patterns.
- Accessibility basics for labels, focus states, dialogs, and buttons.
- Table, pagination, filter bar, badge/status, modal/dialog, toast/alert, skeleton/loader, empty state, and error boundary primitives.
- Layout primitives for app pages, dashboard grids, admin tables, and IELTS split-pane test player views.
- Rich text renderer component that safely consumes backend-sanitized rendered HTML or structured content output.
- Component examples in nearby usage docs or lightweight story/demo page if helpful.

## Tests

- Build and lint pass.
- Components handle long labels without overflow.
- Loading, empty, and error states are available.
- Form fields associate labels/errors correctly for accessibility.
- Dialogs trap focus and close predictably.
- Tables remain usable with empty, loading, error, and paginated data.

## Documentation Sync

- Update `docs/frontend-dev.md` with component usage rules.
- Update feature plans if a shared primitive changes route/page implementation assumptions.

## Acceptance Criteria

- Feature pages can reuse consistent controls instead of inventing local UI.
- Student, teacher, and admin pages can be built with predictable, accessible states and no duplicated control patterns.
