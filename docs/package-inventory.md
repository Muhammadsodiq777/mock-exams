# Package Inventory

This inventory records the package and folder ownership rules from plan `0066-packaging-rules-and-repackaging.md`.

## Backend Rule

Backend Java packages are class-type first:

- `controller/` for Spring MVC controllers, with purpose subpackages such as `controller/admin/` or `controller/teacher/`.
- `service/` and `service/impl/` for service contracts and implementations, with purpose subpackages such as `service/payments/` or `service/schedules/`.
- `repository/` for persistence access, with purpose subpackages such as `repository/schedules/`.
- `entity/` for JPA entities and constants, with purpose subpackages such as `entity/schedules/`.
- `dto/` for request and response DTOs, with purpose subpackages such as `dto/schedules/`.
- `mapper/` for mapping components, with purpose subpackages when needed.
- `config/`, `api/`, and `error/` remain cross-cutting support packages.

New backend work must not add feature-first trees such as `auth/controller`, `teachers/dto`, or `users/entity`.

## Backend Inventory

| Current package | Current shape | Target package | Status |
| --- | --- | --- | --- |
| `controller` | Class-type root controllers for most student, public, teacher, admin, payment, report, review, file, AI, and test flows. | Keep under `controller/`; split crowded purpose groups into `controller/admin/`, `controller/student/`, `controller/teacher/`, and other class-type purpose subpackages as touched. | Current standard package. |
| `dto` | Class-type root DTOs for most API contracts. | Keep under `dto/`; move crowded purpose groups into `dto/<purpose>/` as touched by approved plans. | Current standard package. |
| `entity` | Class-type root entities and domain constants. | Keep under `entity/`; place new larger domains under `entity/<purpose>/`. | Current standard package. |
| `repository` | Class-type root repositories and specifications. | Keep under `repository/`; place new larger domains under `repository/<purpose>/`. | Current standard package. |
| `mapper` | Class-type root mappers. | Keep under `mapper/`; place new larger domains under `mapper/<purpose>/`. | Current standard package. |
| `service` | Class-type root services, `service/impl`, and purpose subpackages for provider clients. | Keep under `service/`; use `service/<purpose>/` for cohesive provider/domain helpers and `service/impl/` for implementations. | Current standard package. |
| `api` | API envelope and metadata records. | Keep `api/`. | Cross-cutting support. |
| `config` | Environment, provider, clock, CORS, and storage configuration. | Keep `config/`. | Cross-cutting support. |
| `error` | API error and exception handling. | Keep `error/`. | Cross-cutting support. |
| `auth/controller`, `auth/dto`, `auth/mapper`, `auth/service` | Feature-first authentication tree. | `controller/auth`, `dto/auth`, `mapper/auth`, `service/auth`, and `service/impl` or `service/auth/impl` depending on the migration plan. | Legacy feature-first; move only in a focused auth repackaging task. |
| `teachers/controller`, `teachers/dto`, `teachers/entity`, `teachers/mapper`, `teachers/repository`, `teachers/service` | Feature-first teacher profile tree. | `controller/teacher`, `dto/teacher`, `entity/teachers`, `mapper/teacher`, `repository/teachers`, `service/teachers`. | Legacy feature-first; new teacher-schedule work should use class-type packages rather than extend this tree. |
| `admin/controller`, `admin/dto`, `admin/mapper`, `admin/service` | Feature-first admin user/profile support tree. | `controller/admin`, `dto/admin`, `mapper/admin`, `service/admin`. | Legacy feature-first; move in a focused admin repackaging task. |
| `users/entity`, `users/repository` plus root user constants | Feature-first user identity tree. | `entity/users`, `repository/users`; constants may move to `entity/users` or a class-type support package during the same task. | Legacy feature-first; move after auth/admin imports are isolated. |
| `students/controller` | Feature-first student session controller. | `controller/student`. | Legacy feature-first; low-risk move candidate once schedule work is stable. |

Suggested repackaging order:

1. Move `students/controller` to `controller/student` because it has the smallest surface.
2. Move `admin/*` after checking admin controller/service imports.
3. Move `auth/*` with focused auth controller/service tests.
4. Move `teachers/*` after teacher profile moderation and public profile tests are green.
5. Move `users/*` last because it is imported across auth, payments, profiles, and content access.

## Frontend Rule

Frontend source folders are role first:

- `pages/` for route containers, with role subfolders such as `pages/student/`, `pages/teacher/`, and `pages/admin/`.
- `components/` for reusable UI, layout, routing, and feature presentation components.
- `services/` for API clients and route constants.
- `hooks/` for React Query and stateful helpers.
- `store/` for Zustand stores.
- `types/` for backend-facing and domain types.
- `utils/`, `constants/`, `app/`, `assets/`, and `test-support/` for support code.

The current frontend already follows the role-first rule. Continue splitting crowded folders by purpose inside the role folder, and do not add feature-first trees that mix pages, services, hooks, stores, and types under one feature directory without a dedicated architecture plan.
