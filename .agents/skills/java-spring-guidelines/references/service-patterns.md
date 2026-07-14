# Service Patterns

## Structure

- Public service interfaces live in `service/`
- Implementations live in `service/impl/`
- Controllers depend on service interfaces, not implementation classes

## Transactions

- Put transaction boundaries in service methods
- Use `@Transactional(readOnly = true)` for read-heavy methods where it helps clarity
- Use write transactions for create, update, and delete flows

## Validation Flow

- Guard required IDs and critical arguments early
- Throw `BusinessException` for missing records, duplicates, and business-rule failures

## Mapping

- Services return `RDO` types, not entities
- Mapper components own entity-to-DTO transformation
- Keep controller bodies free of mapping logic
