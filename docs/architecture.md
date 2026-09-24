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
- `PriceQuery` (record): the compact constructor rejects a null `applicationDate` and ids that aren't
  positive, so an invalid query can't exist. The REST adapter validates the same rules first (D6), so a
  client gets a 400 before this check is reached.
- `FindApplicablePriceService` calls the port, then passes the result to `PriceSelector`. If nothing
  is selected, it throws `PriceNotFoundException` (D5).
- It has no `@Service` annotation. `BeanConfiguration` creates it, which keeps the layer framework-free (C3).
- `FindApplicablePriceServiceTest` mocks only the port. It uses the real `PriceSelector`, which is pure
  domain logic, so the test checks the outcome and not how the service calls the selector.

## 6. Infrastructure

### 6.1 REST adapter (inbound)

Error handling: see [ADR-0005](adr/0005-errors-as-problem-details.md). The contract is
[`api/openapi.yaml`](api/openapi.yaml): see [ADR-0007](adr/0007-contract-first-openapi.md). The controller
and DTO are written by hand to match it.
- `PriceController`: `@GetMapping("/api/v1/prices")`, with parameters:
  - `@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", fallbackPatterns = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime applicationDate`. It accepts whole seconds only (D13). When the pattern fails and no fallback
    patterns are set, Spring retries with ISO parsing, which accepts fractional seconds, and so does
    `iso = DATE_TIME`. Setting the same pattern as the only fallback turns that retry off.
  - `@RequestParam @Min(value = 1, message = "must be greater than or equal to 1") long productId`, and the same
    for `brandId`. `@Min(1)` mirrors `minimum: 1` in the contract. The explicit message keeps the problem detail
    in English whatever the locale, because the built-in messages are translated.
  - The ids are primitives like everywhere else. Spring rejects a required parameter that's missing or not a
    number before the method is called, so a wrapper would never hold null here.
- `PriceResponse` (record): `productId, brandId, priceList, startDate, endDate, price, currency`. The dates carry
  `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")`, because Jackson's default drops zero seconds
  (`2020-06-14T00:00`).
- `PriceRestMapper` (`@Component`): domain `Price` → `PriceResponse`.
- `RestExceptionHandler` (`@RestControllerAdvice`) extends `ResponseEntityExceptionHandler` and returns
  `ProblemDetail`. The base class keeps Spring MVC's own errors at their proper status, so an unknown path is
  404 and a `POST` is 405, not 500. Every response goes through one `createResponseEntity` override that sets
  `type` to `about:blank`: Spring 7 leaves `type` out when it isn't set, and the contract requires it.

| Exception                                                    | Status | `detail`                                                        |
|--------------------------------------------------------------|--------|-----------------------------------------------------------------|
| `PriceNotFoundException`                                     | 404    | The exception message, e.g. `No applicable price for brand 1, product 99999 at 2020-06-14T10:00:00.` |
| `MissingServletRequestParameterException`                    | 400    | Spring's own: `Required parameter 'brandId' is not present.`    |
| `MethodArgumentTypeMismatchException` on `applicationDate`   | 400    | `Parameter 'applicationDate' must match yyyy-MM-ddTHH:mm:ss.`   |
| `MethodArgumentTypeMismatchException` on an id               | 400    | `Parameter 'productId' must be an integer.`                     |
| `HandlerMethodValidationException`                           | 400    | `Parameter 'productId' must be greater than or equal to 1.`     |
| Other Spring MVC exceptions (unknown path, method not allowed…) | 4xx | Spring's own                                                    |
| Any other `Exception` (logged)                               | 500    | `An unexpected error occurred.`                                 |

- The controller has no OpenAPI annotations. The contract lives only in `openapi.yaml`.

### 6.2 Persistence adapter (outbound)

Schema initialisation: see [ADR-0004](adr/0004-schema-and-data-with-sql-scripts.md).
- `PriceEntity`: a JPA entity mapped to `PRICES`, with a `protected` no-arg constructor for JPA. It maps the
  audit columns and `ID` but exposes no getters for them, so they never reach the domain. The service is
  read-only, so there are no setters.
- `PriceJpaRepository`: a JPQL `@Query` filtering by brand, product and
  `startDate <= :date AND endDate >= :date`. It has **no** `ORDER BY priority` and **no** `LIMIT` (C5).
  JPQL rather than a derived query: the derived name would be very long and need the date twice, and the
  whole pre-filter is easier to review in one place.
- `PricePersistenceAdapter` (`@Component`) implements `PriceRepositoryPort` and maps entities to the domain
  with `PriceEntityMapper`, an injected `@Component`. It is the only class with a transaction
  (`@Transactional(readOnly = true)`).
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
- `BeanConfiguration` defines `@Bean`s for `PriceSelector` and `FindApplicablePriceUseCase` (a `FindApplicablePriceService`).
- The H2 console is enabled only in development (`developmentOnly` dependency).
- OpenAPI (ADR-0007):
  - `build.gradle` copies `docs/api/openapi.yaml` into the jar as `static/openapi.yaml`
    (`processResources { from('docs/api') { include 'openapi.yaml'; into 'static' } }`).
  - springdoc shows that file in Swagger UI (`springdoc.swagger-ui.url=/openapi.yaml`).
  - The spec springdoc generates from the code must not be presented as the contract. It can't be switched
    off: `springdoc.api-docs.enabled=false` also removes Swagger UI, which depends on it. So
    `springdoc.paths-to-match=/none` restricts it to no paths, and `/v3/api-docs` returns a spec with empty
    `paths`. Left alone, it would contradict the contract: it declares `applicationDate` as `date-time`, which
    allows offsets and fractional seconds (D13).
  - `OpenApiServingTest` guards all three: the file is served byte for byte, Swagger UI's config points to
    it, and the generated spec has no paths.

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
| —   | REST slice             | `PriceController` + `RestExceptionHandler` | `@WebMvcTest`, mocked use case          | Parameter binding, JSON shape (strict, incl. seconds and scale), full problem bodies for 400 (every B12/B13 variant), 404 and 500 (no internal details), and Spring's own 404 (unknown path) and 405 as problem+json. |
| —   | OpenAPI serving        | `static/openapi.yaml` + springdoc config   | `@SpringBootTest` + MockMvc             | The contract is served byte for byte, Swagger UI's config points to it, and the generated spec has no paths (`OpenApiServingTest`). |
| T1  | Integration            | Full application (`PriceAcceptanceTest`)   | `@SpringBootTest` + MockMvc, H2         | AT-1–AT-5 and B1–B7: the whole 200 body is compared strictly (`JsonCompareMode.STRICT`), so all fields are checked and extra fields fail. B8–B13: status plus the problem+json content type, `status` and `title`. Parameterized with `@CsvSource` tables that mirror the requirements. |
| T4  | Architecture           | Package dependencies (`ArchitectureTest`)  | ArchUnit                                | Layer rule C2; no Spring, JPA, Jakarta, Hibernate or Jackson dependencies in `domain` or `application` (C3); no field injection. |

- Test naming: `should<Expected>_when<Condition>`, or `@DisplayName` with a readable sentence.
- Test dependencies: JUnit 5, AssertJ and Mockito come with the Spring Boot test starters. ArchUnit
  (`com.tngtech.archunit:archunit-junit5`) must be added to `build.gradle`.
