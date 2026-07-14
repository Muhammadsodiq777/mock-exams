---
name: backend-review
description: >
  Backend review skill for Mock Exams. Use after every backend implementation and when reviewing
  Java/Spring changes for clean code, correctness, error-free behavior, performance, package
  organization, architecture, API contract drift, and test coverage.
---

# Backend Review

Review only the backend changes and directly relevant nearby code.

## Required Skills

- Use `java-spring-guidelines` as the repository convention source.
- Use `dependency-refresh` when dependency or platform versions changed.

## Review Checklist

- Correctness: business rules, authorization, validation, error codes, null handling, edge cases.
- Architecture: thin controllers, service-owned business logic, mapper-owned DTO conversion, repository-owned persistence.
- Packaging: controllers under `controller/`, services under `service/`, repositories under `repository/`, entities under `entity/`, DTOs under `dto/`, and mappers under `mapper/`; crowded folders split by purpose inside those folders.
- Reliability: transaction boundaries, idempotency, soft-delete behavior, schema sync, API contract sync.
- Performance: avoid obvious N+1 queries, unbounded scans, unnecessary writes, and repeated expensive work.
- Tests: targeted coverage for success, permission failure, validation failure, and key negative cases.

## Output

Return findings first, ordered by severity, with file references. If no issues are found, say so and mention residual verification risk.
