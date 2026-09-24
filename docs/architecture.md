# Prices Service — Architecture and Design

> How the service is built. What it must do is defined in [`requirements.md`](requirements.md). IDs
> such as FR-4, AR-1, D3, C5, T2, AT-1 and B1 refer to that document. If this document and the requirements
> disagree, the requirements win. The reasoning behind each design choice is in the
> [Architecture Decision Records](adr/README.md).

## 1. Layers and dependency rule (C1–C3)

See [ADR-0002](adr/0002-hexagonal-architecture.md).

```
infrastructure  ──►  application  ──►  domain
(adapters, Spring)   (use cases)       (model, rules, ports)
```

| Layer            | Contains                                                  | May depend on             | Must not contain                    |
|------------------|-----------------------------------------------------------|---------------------------|-------------------------------------|
| `domain`         | Model, business rules, outbound port, domain exceptions   | Java standard library     | Spring, JPA, Jakarta, HTTP          |
| `application`    | Use case interfaces (inbound ports) and implementations   | `domain`                  | Spring annotations, JPA, HTTP       |
| `infrastructure` | REST and persistence adapters, Spring configuration        | `application`, `domain`   | Business rules                      |

## 2. Package structure

```
com.store.prices
├── PricesServiceApplication
├── domain
│   ├── model/        Price (record): long brandId, long productId, long priceList, startDate, endDate,
│   │                 int priority, BigDecimal amount, java.util.Currency currency
│   │                 └─ isApplicableAt(LocalDateTime): inclusive range check (D3)
│   ├── service/      PriceSelector: picks the applicable price from candidates using streams (C5)
│   ├── port/         PriceRepositoryPort (outbound port): findCandidates(long brandId, long productId, LocalDateTime applicationDate)
│   └── exception/    PriceNotFoundException
├── application
│   ├── port/in/      FindApplicablePriceUseCase (inbound port, interface)
│   │                 └─ PriceQuery (record): applicationDate, productId, brandId
│   └── service/      FindApplicablePriceService implements FindApplicablePriceUseCase
└── infrastructure
    ├── adapter/in/rest/          PriceController, PriceResponse (DTO), PriceRestMapper,
    │                             RestExceptionHandler (problem+json)
    ├── adapter/out/persistence/  PriceEntity (JPA), PriceJpaRepository (Spring Data),
    │                             PricePersistenceAdapter implements PriceRepositoryPort,
    │                             PriceEntityMapper
    └── config/                   BeanConfiguration
```

## 3. Request flow

```
HTTP GET /api/v1/prices
  → PriceController           validates and binds params, builds PriceQuery
  → FindApplicablePriceUseCase (FindApplicablePriceService)
      → PriceRepositoryPort.findCandidates(...)   → PricePersistenceAdapter → H2
      → PriceSelector.select(candidates, date)    → Optional<Price>
      → empty → throw PriceNotFoundException
  → PriceRestMapper → PriceResponse (200)
  → errors → RestExceptionHandler → problem+json (400 / 404 / 500)
```

## 4. Domain

### 4.1 `Price`
- An immutable `record`. Ids and priority are primitives (`long`, `int`), so they can never be null. The
  database columns are `NOT NULL` anyway.
- `currency` is a `java.util.Currency`. That's plain Java, so the core stays framework-free, and it rejects
  invalid codes. The entity keeps the `CHAR(3)` value as a `String`, and the mapper converts it.
- The compact constructor checks the invariants: `startDate`, `endDate`, `amount` and `currency` are
  non-null, `endDate >= startDate`, `priority >= 0` and `amount >= 0`.
- `isApplicableAt(LocalDateTime date)` returns `!date.isBefore(startDate) && !date.isAfter(endDate)`,
  so both ends are inclusive (D3).

### 4.2 `PriceSelector` — selection algorithm (FR-4, D3, D4, D14, C5)

See [ADR-0003](adr/0003-price-selection-in-domain-with-streams.md).

```java
public Optional<Price> select(List<Price> candidates, LocalDateTime applicationDate) {
    return candidates.stream()
            .filter(price -> price.isApplicableAt(applicationDate))
            .max(Comparator.comparingInt(Price::priority)
                           .thenComparing(Price::startDate)
                           .thenComparingLong(Price::priceList));
}
```

- The domain re-applies the date filter. That keeps the rule complete and testable without a
  database, and makes the database filter only an optimisation.
- It returns `Optional`, never `null`.
- The last tie-break, `priceList` (D14), makes the result deterministic even though the candidates arrive
  in no particular order. ADR-0003 shows the comparator without it: the ADR records *where* the rule
  lives, and the tie-breaks themselves are behaviour, defined in `requirements.md`.
- `PriceSelector` is a plain class with no dependencies. `BeanConfiguration` registers it as a bean.

### 4.3 `PriceRepositoryPort`
```java
List<Price> findCandidates(long brandId, long productId, LocalDateTime applicationDate);
```
It returns the candidate prices of that brand and product. These may include more prices than
actually apply, and they come in no particular order. It never picks the winner (C5).

## 5. Application

- `FindApplicablePriceUseCase`: `Price find(PriceQuery query)`.
- `FindApplicablePriceService` calls the port, then passes the result to `PriceSelector`. If nothing
  is selected, it throws `PriceNotFoundException` (D5).
- It has no `@Service` annotation. `BeanConfiguration` creates it, which keeps the layer framework-free (C3).

## 6. Infrastructure

### 6.1 REST adapter (inbound)

Error handling: see [ADR-0005](adr/0005-errors-as-problem-details.md). The contract is
[`api/openapi.yaml`](api/openapi.yaml): see [ADR-0007](adr/0007-contract-first-openapi.md). The controller
and DTO are written by hand to match it.
- `PriceController`: `@GetMapping("/api/v1/prices")`, with parameters:
  - `@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime applicationDate`.
    The strict pattern rejects fractional seconds (D13). `iso = DATE_TIME` would accept them.
  - `@RequestParam @Positive Long productId`
  - `@RequestParam @Positive Long brandId`
- `PriceResponse` (record): `productId, brandId, priceList, startDate, endDate, price, currency`.
- `RestExceptionHandler` (`@RestControllerAdvice`) returns `ProblemDetail`:

| Exception                                                                                   | Status |
|---------------------------------------------------------------------------------------------|--------|
| `PriceNotFoundException`                                                                    | 404    |
| `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `HandlerMethodValidationException` / `ConstraintViolationException` | 400 |
| Any other `Exception` (logged; generic message)                                             | 500    |

- The controller has no OpenAPI annotations. The contract lives only in `openapi.yaml`.

### 6.2 Persistence adapter (outbound)

Schema initialisation: see [ADR-0004](adr/0004-schema-and-data-with-sql-scripts.md).
- `PriceEntity`: a JPA entity mapped to `PRICES`. It includes the audit columns, which never reach the domain.
- `PriceJpaRepository`: a derived query or a JPQL query filtering by brand, product and
  `startDate <= :date AND endDate >= :date`. It has **no** `ORDER BY priority` and **no** `LIMIT` (C5).
- `PricePersistenceAdapter` implements `PriceRepositoryPort` and maps entities to the domain with `PriceEntityMapper`.
- Schema and data are loaded from `src/main/resources/schema.sql` and `data.sql`, with
  `spring.jpa.hibernate.ddl-auto=none`.
- No fixed `spring.datasource.url`. Spring Boot creates a uniquely named in-memory H2 database for
  each application context, so extra test contexts never re-run `schema.sql` against a database
  that already exists.
- `data.sql` has the rows of [`source/prices.csv`](source/prices.csv), with the source dates
  (`2020-06-14-00.00.00`) converted to `TIMESTAMP '2020-06-14 00:00:00'` literals (D1).
- Index: `CREATE INDEX IDX_PRICES_LOOKUP ON PRICES (BRAND_ID, PRODUCT_ID, START_DATE, END_DATE)`.

#### Physical data model

| Column           | SQL type (H2)                             | Java type (entity / domain) | SQL constraints                          |
|------------------|-------------------------------------------|-----------------------------|------------------------------------------|
| `ID`             | `BIGINT GENERATED BY DEFAULT AS IDENTITY` | `Long` (entity only)        | `PRIMARY KEY`                            |
| `BRAND_ID`       | `BIGINT`                                  | `long`                      | `NOT NULL`                               |
| `START_DATE`     | `TIMESTAMP(0)`                            | `LocalDateTime`             | `NOT NULL`                               |
| `END_DATE`       | `TIMESTAMP(0)`                            | `LocalDateTime`             | `NOT NULL`, `CHECK (END_DATE >= START_DATE)` |
| `PRICE_LIST`     | `BIGINT`                                  | `long`                      | `NOT NULL`                               |
| `PRODUCT_ID`     | `BIGINT`                                  | `long`                      | `NOT NULL`                               |
| `PRIORITY`       | `INT`                                     | `int`                       | `NOT NULL`, `CHECK (PRIORITY >= 0)`      |
| `PRICE`          | `DECIMAL(10,2)`                           | `BigDecimal`                | `NOT NULL`, `CHECK (PRICE >= 0)`         |
| `CURR`           | `CHAR(3)`                                 | `String` (entity) / `Currency` (domain) | `NOT NULL`                   |
| `LAST_UPDATE`    | `TIMESTAMP(0)`                            | `LocalDateTime` (entity only) | `NOT NULL`                             |
| `LAST_UPDATE_BY` | `VARCHAR(50)`                             | `String` (entity only)      | `NOT NULL`                               |

- `TIMESTAMP` is without time zone, which matches D2.
- Every id is `BIGINT`/`long`, to be consistent and to leave room to grow. The `NOT NULL` columns map to
  primitives, so a null can't be represented. Only the generated `ID` is a `Long`.
- The audit columns and `ID` exist only in the entity. The domain `Price` doesn't carry them.

### 6.3 Configuration
- `BeanConfiguration` defines `@Bean`s for `PriceSelector` and `FindApplicablePriceService`.
- The H2 console is enabled only in development (`developmentOnly` dependency).
- OpenAPI (ADR-0007):
  - `build.gradle` copies `docs/api/openapi.yaml` into the jar as `static/openapi.yaml`
    (`processResources { from('docs/api') { include 'openapi.yaml'; into 'static' } }`).
  - springdoc shows that file in Swagger UI (`springdoc.swagger-ui.url=/openapi.yaml`). The spec it
    generates from the code must not be presented as the contract. Choose the exact springdoc
    properties that achieve this (disabling or hiding `/v3/api-docs`) during implementation, and
    check them against the running app.

## 7. Coding guidelines
- Use `record`s for immutable data: domain model, query, DTO.
- Use constructor injection only.
- Keep mappers explicit: entity → domain and domain → DTO. Don't map with reflection.
- Use `BigDecimal` for money, with scale 2 as stored.
- Don't write `null` checks in the business flow. Use `Optional`, and throw domain exceptions.

## 8. Test strategy (T1–T4)

Integration test approach: see [ADR-0006](adr/0006-integration-tests-with-mockmvc.md).

| Req | Level                  | Target                                     | Tools                                   | What it covers                                                                                             |
|-----|------------------------|--------------------------------------------|-----------------------------------------|------------------------------------------------------------------------------------------------------------|
| T2  | Domain unit            | `Price.isApplicableAt`                     | JUnit 5, AssertJ                        | Inside, before, after, exactly at start, exactly at end.                                                   |
| T2  | Domain unit (streams)  | `PriceSelector`                            | JUnit 5 (`@ParameterizedTest`), AssertJ | Empty list; single match; highest priority wins; tie-breaks (D4, D14); non-applicable ignored; AT-1–AT-5 with in-memory fixtures. |
| T3  | Application unit       | `FindApplicablePriceService`               | JUnit 5, Mockito, AssertJ               | Port called with the query values; selected price returned; `PriceNotFoundException` when nothing applies. No Spring context. |
| —   | Persistence slice      | `PricePersistenceAdapter` + JPA query      | `@DataJpaTest`                          | Pre-filter against the seed data, boundary dates included; entity→domain mapping.                          |
| —   | Seed data              | `schema.sql` + `data.sql`                  | `@DataJpaTest`, `JdbcTemplate`          | `PRICES` holds exactly the rows of `docs/source/prices.csv`, field by field (`SeedDataTest`).               |
| —   | REST slice             | `PriceController` + `RestExceptionHandler` | `@WebMvcTest`, mocked use case          | Parameter binding, JSON shape, 400 for missing or malformed params and fractional seconds, 404 mapping.   |
| T1  | Integration            | Full application (`PriceAcceptanceTest`)   | `@SpringBootTest` + MockMvc, H2         | AT-1–AT-5 and B1–B7: the whole 200 body is compared strictly (`JsonCompareMode.STRICT`), so all fields are checked and extra fields fail. B8–B13: status plus the problem+json content type, `status` and `title`. Parameterized with `@CsvSource` tables that mirror the requirements. |
| T4  | Architecture           | Package dependencies (`ArchitectureTest`)  | ArchUnit                                | Layer rule C2; no Spring, JPA, Jakarta, Hibernate or Jackson dependencies in `domain` or `application` (C3); no field injection. |

- Test naming: `should<Expected>_when<Condition>`, or `@DisplayName` with a readable sentence.
- Test dependencies: JUnit 5, AssertJ and Mockito come with the Spring Boot test starters. ArchUnit
  (`com.tngtech.archunit:archunit-junit5`) must be added to `build.gradle`.
