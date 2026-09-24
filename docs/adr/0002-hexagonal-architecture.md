# ADR-0002: Hexagonal architecture with a framework-free core

- **Status:** Accepted
- **Date:** 2026-09-24
- **Requirements:** AR-1, C1–C4, C6, T4

## Context

The reviewer asked to apply hexagonal architecture correctly with three layers (AR-1). The service is
small: one query, one table. The architecture must be clear without becoming ceremony. The core must
be testable without Spring or a database (AR-2).

## Decision

- Three layers, with dependencies pointing inwards only: `infrastructure → application → domain`.
  - `domain`: the model, business rules, the outbound port (`PriceRepositoryPort`) and domain exceptions.
  - `application`: the inbound port (`FindApplicablePriceUseCase`) and its implementation.
  - `infrastructure`: the REST adapter (inbound), the persistence adapter (outbound) and Spring configuration.
- `domain` and `application` are **framework-free**: no Spring, JPA, Jakarta or HTTP imports, and no
  `@Service` or `@Component`.
- Beans for the core are created by `@Bean` methods in `infrastructure/config/BeanConfiguration`.
- Models don't leak across layers. JPA entities and REST DTOs are mapped explicitly to and from the
  domain model.
- An ArchUnit test enforces the dependency rule and the framework-free core (T4).

## Alternatives considered

- **Classic layered MVC (controller → service → repository).** Less code, but the business rule
  depends on JPA and Spring, and it wouldn't meet AR-1.
- **Hexagonal, but with `@Service` on the application services.** This is common and needs less
  wiring. However, it puts a Spring dependency in the core and weakens C3. The explicit wiring is
  about 10 lines and shows the boundary clearly.
- **Separate Gradle modules per layer.** The compiler would enforce the boundaries, but that's
  excessive for one use case. ArchUnit gives the same guarantee in a single module.
- **Using JPA entities as the domain model.** No mappers needed, but persistence concerns (IDs, audit
  columns, annotations) would leak into the domain, breaking C6.

## Consequences

- The domain and use case can be unit-tested with plain JUnit and Mockito, with no Spring context.
- There is some extra code: two mappers and a configuration class. It's accepted, because the
  boundaries are the point of the exercise.
- Replacing H2 or REST would only touch `infrastructure`.
- Any new core class must be registered in `BeanConfiguration`.
