# Prices Service

A Spring Boot REST service that answers one question: **which price applies to this product of this brand at
this moment?** Given an application date, a product and a brand, it returns the single applicable price list
and its final price. Brand `1` is referred to as STORE Z.

Java 21 · Spring Boot 4.1 · Spring Web MVC · Spring Data JPA · H2 (in-memory) · Gradle

## Prerequisites

- **JDK 21**, installed locally. The build asks for a Java 21 toolchain but doesn't download one.
- Nothing else. There is no external database or other service. Gradle comes with the wrapper (`./gradlew`).

## Run it

```bash
./gradlew bootRun
```

Or build the jar and run it:

```bash
./gradlew bootJar
java -jar build/libs/prices-service-0.0.1-SNAPSHOT.jar
```

The service listens on `http://localhost:8080`. At startup it creates the `PRICES` table in an in-memory H2
database from `schema.sql`, and loads the seed data from `data.sql`. Those are the rows of
[`docs/source/prices.csv`](docs/source/prices.csv), the data from the statement.

## Try it

```
GET /api/v1/prices?applicationDate={yyyy-MM-ddTHH:mm:ss}&productId={id}&brandId={id}
```

```bash
curl 'http://localhost:8080/api/v1/prices?applicationDate=2020-06-14T10:00:00&productId=35455&brandId=1'
```

```json
{"productId":35455,"brandId":1,"priceList":1,"startDate":"2020-06-14T00:00:00","endDate":"2020-12-31T23:59:59","price":35.50,"currency":"EUR"}
```

The five requests from the original statement, for product `35455` of brand `1`:

| Case | `applicationDate`     | Price list | Price     |
|------|-----------------------|------------|-----------|
| AT-1 | `2020-06-14T10:00:00` | 1          | 35.50 EUR |
| AT-2 | `2020-06-14T16:00:00` | 2          | 25.45 EUR |
| AT-3 | `2020-06-14T21:00:00` | 1          | 35.50 EUR |
| AT-4 | `2020-06-15T10:00:00` | 3          | 30.50 EUR |
| AT-5 | `2020-06-16T21:00:00` | 4          | 38.95 EUR |

Errors are returned as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `application/problem+json`. When no
price applies, including for an unknown product or brand, the status is `404`:

```bash
curl 'http://localhost:8080/api/v1/prices?applicationDate=2020-06-14T10:00:00&productId=99999&brandId=1'
```

```json
{"detail":"No applicable price for brand 1, product 99999 at 2020-06-14T10:00:00.","instance":"/api/v1/prices","status":404,"title":"Not Found","type":"about:blank"}
```

A missing or malformed parameter, a non-positive id, or a date with fractional seconds returns `400`:

```bash
curl 'http://localhost:8080/api/v1/prices?applicationDate=2020-06-14T10:00:00.500&productId=35455&brandId=1'
```

```json
{"detail":"Parameter 'applicationDate' must match yyyy-MM-ddTHH:mm:ss.","instance":"/api/v1/prices","status":400,"title":"Bad Request","type":"about:blank"}
```

A few rules the statement leaves open:
- both ends of a date range are inclusive;
- dates have no time zone and whole-second precision;
- when two prices share the highest priority, the one with the latest start date wins, then the one with the
  highest price list.

All such decisions are listed in [`docs/requirements.md`](docs/requirements.md) (D1–D14).

## API documentation

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **Contract:** <http://localhost:8080/openapi.yaml>, served from [`docs/api/openapi.yaml`](docs/api/openapi.yaml)

The contract is written by hand (OpenAPI 3.1), and the controller is written to match it. The contract is never
generated from the code ([ADR-0007](docs/adr/0007-contract-first-openapi.md)).

## Tests

```bash
./gradlew build                                        # compile and run every test
./gradlew test                                         # tests only
./gradlew test --tests 'com.store.prices.acceptance.PriceAcceptanceTest'   # a single class
```

| Level        | What it covers                                                                  | Tests |
|--------------|---------------------------------------------------------------------------------|-------|
| Acceptance   | AT-1–AT-5 and the edge cases B1–B13, through the full application (MockMvc) against the real seed data, nothing mocked | `PriceAcceptanceTest` |
| Domain       | The date-range check and the stream-based price selection, including tie-breaks | `PriceTest`, `PriceSelectorTest`, `PriceNotFoundExceptionTest` |
| Use case     | Querying the port and selecting the price, or failing when none applies, with no Spring context | `FindApplicablePriceServiceTest`, `PriceQueryTest` |
| Persistence  | The database pre-filter, including exact boundaries, and the mapping to the domain | `PricePersistenceAdapterTest`, `SeedDataTest` |
| REST         | Parameter binding, the JSON shape, and every error body against the contract   | `PriceControllerTest` |
| API docs     | The hand-written contract is served unchanged and shown in Swagger UI          | `OpenApiServingTest` |
| Architecture | Layer dependencies and a framework-free core, enforced with ArchUnit          | `ArchitectureTest` |

The case IDs (AT-n, Bn) come from [`docs/requirements.md`](docs/requirements.md). Every test of an AT or B case
starts its name with the case ID.

## Design at a glance

A hexagonal architecture with three layers. Dependencies point inwards only:

```
infrastructure  ──►  application  ──►  domain
(REST, JPA, Spring)  (use case)        (model, selection rule, ports)
```

- `domain` and `application` are plain Java, with no Spring, JPA or HTTP code. Spring wires them with `@Bean`
  methods.
- The database query only **pre-filters** candidates by brand, product and date. The winning price is chosen in
  the domain with the Stream API, never with `ORDER BY` or `LIMIT` in SQL
  ([ADR-0003](docs/adr/0003-price-selection-in-domain-with-streams.md)).
- JPA entities and REST DTOs never reach the domain. Mapper classes convert between them.

The full design is in [`docs/architecture.md`](docs/architecture.md), and the reasons behind it are in the
[Architecture Decision Records](docs/adr/README.md).

## How it was built

The project was built documentation-first and outside-in:

1. **Specification before code.** The statement was turned into [`requirements.md`](docs/requirements.md), with
   an ID for every rule, decision and acceptance case. It was followed by
   [`architecture.md`](docs/architecture.md), the ADRs and the OpenAPI contract.
2. **Acceptance tests first.** After the build and seed-data setup, all AT and B cases were written as
   integration tests before any production code. They were run once to check they failed for the right reason,
   then committed disabled. The ArchUnit rules were enabled from the start.
3. **One slice at a time,** from the inside out: domain → use case → persistence → REST. Each slice's unit or
   slice tests were written first and committed together with the code that makes them pass, keeping the build
   green at every commit.
4. **Done when the acceptance tests pass.** The REST slice enabled them, and all of them pass without any
   expected value having changed.

Development was AI-assisted with [Claude Code](https://claude.com/claude-code), following the rules in
[`CLAUDE.md`](CLAUDE.md). Each slice was reviewed against the docs before it was committed.

## Project layout

```
src/main/java/com/store/prices
├── domain/            Price, PriceSelector, PriceRepositoryPort, PriceNotFoundException
├── application/       FindApplicablePriceUseCase, PriceQuery, FindApplicablePriceService
└── infrastructure/
    ├── adapter/in/rest/          controller, DTO, mapper, error handler
    ├── adapter/out/persistence/  JPA entity, repository, adapter, mapper
    └── config/                   bean wiring
src/main/resources     application.properties, schema.sql, data.sql
docs/
├── requirements.md    what the service must do (the source of truth)
├── architecture.md    how it is built
├── adr/               why it is built this way
├── api/openapi.yaml   the HTTP contract
└── source/prices.csv  the seed data from the statement
```

## H2 console

Available with `./gradlew bootRun` only, at <http://localhost:8080/h2-console>. The JDBC URL is generated for
each run and printed in the startup log (`Database available at 'jdbc:h2:mem:…'`). The user is `sa`, with an
empty password.
