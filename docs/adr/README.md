# Architecture Decision Records

Architecture decisions for the prices service. The format and rules are in ADR-0001. Behaviour
decisions (the D entries) are in [`../requirements.md`](../requirements.md), not here.

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-hexagonal-architecture.md) | Hexagonal architecture with a framework-free core | Accepted |
| [0003](0003-price-selection-in-domain-with-streams.md) | Price selection rule in the domain, implemented with streams | Accepted |
| [0004](0004-schema-and-data-with-sql-scripts.md) | Schema and seed data with SQL scripts, not Hibernate DDL | Accepted |
| [0005](0005-errors-as-problem-details.md) | Errors as RFC 9457 Problem Details from a single handler | Accepted |
| [0006](0006-integration-tests-with-mockmvc.md) | Integration tests with `@SpringBootTest` and MockMvc | Accepted |
| [0007](0007-contract-first-openapi.md) | Contract-first OpenAPI specification, hand-written | Accepted |
