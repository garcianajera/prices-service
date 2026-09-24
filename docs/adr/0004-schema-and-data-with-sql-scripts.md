# ADR-0004: Schema and seed data with SQL scripts, not Hibernate DDL

- **Status:** Accepted
- **Date:** 2026-09-24
- **Requirements:** FR-5, N1

## Context

The service uses an in-memory H2 database initialised with the four seed rows (FR-5). The schema
needs constraints (`NOT NULL`, `CHECK`), exact types (`DECIMAL(10,2)`, `TIMESTAMP(0)`, `CHAR(3)`) and
a lookup index. They are defined in `architecture.md`.

## Decision

- Define the schema in `src/main/resources/schema.sql` and the seed data in `src/main/resources/data.sql`.
- Set `spring.jpa.hibernate.ddl-auto=none`, so Hibernate never creates or changes tables.
- Initialise with Spring Boot's SQL initialisation (`spring.sql.init.mode=embedded`, the default for H2).
- Hibernate validates the mappings through the integration and persistence tests, which run against
  the real schema.

## Alternatives considered

- **Hibernate `ddl-auto=create-drop` plus `data.sql`.** No schema file, but the generated DDL
  controls the types and constraints, not us. `CHECK` constraints and the index would need extra
  annotations. It also hides the schema from reviewers.
- **Flyway (or Liquibase) migrations.** This is the production-grade choice for a real database, and
  the natural next step. For an in-memory database that is recreated on every start, versioned
  migrations add a dependency and ceremony without any benefit.

## Consequences

- The schema is explicit, reviewable and matches the physical data model in `architecture.md`.
- `schema.sql` and the `@Entity` mapping must be kept in sync by hand. Any mismatch fails the tests.
- Moving to a persistent database later means replacing the scripts with Flyway migrations. That
  would be a new ADR superseding this one.
