# Persistence Patterns

## Entities

- Most entities extend `BaseEntity` with `createdAt`, `updatedAt`, and `deletedAt`
- Use `@Entity`, `@Table`, `@Id`, and `GenerationType.IDENTITY`
- Lombok `@Getter` and `@Setter` are normal in entities here

## Soft Delete

- Soft delete is an established pattern in this codebase
- Active queries usually filter on `deletedAt is null`
- Delete flows often mark `deletedAt` instead of removing rows

## Repositories

- Use Spring Data JPA repositories
- Derived queries are preferred when readable
- Use JPQL `@Query` when custom search logic is clearer than a derived method name

## Schema Source of Truth

- Schema files live in `backend/src/main/resources/`
- If a persistence change alters tables, indexes, or seeded data, update schema files deliberately
