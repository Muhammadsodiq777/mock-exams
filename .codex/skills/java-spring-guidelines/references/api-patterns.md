# API Patterns

## Route Shape

- Business endpoints live under `/api/v1/`
- Auth endpoints are separate from business routes

## Controller Conventions

- Annotate controllers with `@RestController` and `@Validated`
- Return `ResponseEntity<?>`
- Build paging with `PageRequest` and explicit sort fields in the controller
- Keep controllers responsible for HTTP concerns only: path params, query params, validation, response codes

## DTO Naming

- `*CDO`: create command/input
- `*UDO`: update or patch command/input
- `*RDO`: read/response output
- `PageRDO<T>` is the standard paginated wrapper for backend responses

## Validation and Errors

- Use Jakarta validation on DTOs and parameters
- Prefer explicit user-facing validation messages
- Business failures surface through `BusinessException` and `ApiExceptionHandler`

## API Consumer Awareness

When changing response shape or endpoint behavior, check:

- `frontend/src/services/backend.service.ts`
- `frontend/src/types/backend.ts`
- The affected page or hook using the endpoint
