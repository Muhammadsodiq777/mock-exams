# Testing And Build

## Commands

- Targeted test loop: `mvn -Dtest=ClassName test`
- Full verification: `mvn test`
- Local run: `mvn spring-boot:run`

## Testing Guidance

- For pure logic, prefer narrow unit or service-level tests
- For controller and persistence behavior, use the smallest Spring-backed test that proves the behavior
- Do not expand a small backend task into a full test infrastructure migration

## OpenAPI And Docs

- If `OpenApiConfig` exists, API shape changes should be checked for OpenAPI impact
