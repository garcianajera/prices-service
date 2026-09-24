# CLAUDE.md

Spring Boot REST service that returns the applicable price for a brand, product and application
date. It is a technical test, assessed on design, code quality and correct test results.

**Status:** docs only. The code is still the Spring Initializr scaffold, and the service has not been
implemented yet.

## Source of truth

Read these before implementing or changing behaviour:

1. [`docs/requirements.md`](docs/requirements.md): **what** the service must do. It defines the IDs used
   everywhere: requirements (FR, AR), acceptance cases (AT, B), decisions (D), constraints (C),
   testing requirements (T), non-functional requirements (N) and open questions (Q).
2. [`docs/architecture.md`](docs/architecture.md): **how** it's built. It covers packages, class names,
   the selection algorithm, the physical data model and the test strategy.
3. [`docs/adr/`](docs/adr/README.md): **why** it's built this way. These are the Architecture Decision Records.
4. [`docs/api/openapi.yaml`](docs/api/openapi.yaml): the **HTTP contract** (OpenAPI 3.1, hand-written, ADR-0007).

If the two disagree, `requirements.md` wins. If the code disagrees with the docs, stop and ask.
Don't quietly pick one.

- **Behaviour changes** (new rule, status code, field, decision) go into `requirements.md` first, then the code.
- **Design changes** go into `architecture.md` in the same change as the code.
- **API changes** (paths, parameters, fields, status codes, error bodies) go into `openapi.yaml` first. Then
  update the hand-written controller and DTOs to match. Don't generate code from the spec, and don't add
  OpenAPI annotations to the controller. Spec descriptions are for API consumers: no ADR or
  requirement IDs in them. Maintainer notes go in YAML comments.
- **New architectural decisions** need a new ADR. Accepted ADRs are never edited: supersede them with a new one.
- **Open questions (Q)** must not be resolved in code on your own. Ask.
- **Refer by ID** (e.g. D13, AT-2), not by section number.

## Scope

- The service is read-only: a single price query endpoint. Don't add create, update or delete
  endpoints, authentication, or brand and product master data.
- Don't add dependencies beyond the stack below without asking.
- Write code, docs, comments and commit messages in English. The original statement is in Spanish.

## Commands

```bash
./gradlew build                                       # compile + all tests (must pass before any commit)
./gradlew test                                        # all tests
./gradlew test --tests 'com.store.prices.SomeTest'    # single test class
./gradlew bootRun                                     # run on http://localhost:8080
npx @redocly/cli lint docs/api/openapi.yaml           # lint the API contract after editing it
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Example: `curl 'http://localhost:8080/api/v1/prices?applicationDate=2020-06-14T10:00:00&productId=35455&brandId=1'`
- H2 console: `http://localhost:8080/h2-console`, once enabled with `spring.h2.console.enabled=true` (not set yet).

## Stack

Java 21, Spring Boot 4.1, Gradle (Groovy DSL), Spring Web MVC, Spring Data JPA, Bean Validation,
H2, springdoc-openapi. Tests use JUnit 5, AssertJ, Mockito and ArchUnit (not yet in `build.gradle`).
Base package: `com.store.prices`.

## Architecture rules (hexagonal — non-negotiable)

- Layers are `domain`, `application` and `infrastructure`. Dependencies point inwards only:
  `infrastructure → application → domain`.
- `domain` and `application` contain **no** Spring, JPA, Jakarta or HTTP imports. They have no
  `@Service`, `@Component` or `@Transactional` either. Their beans are wired by `@Bean` methods in
  `infrastructure/config`.
- Transactions go only in the persistence adapter, as `@Transactional(readOnly = true)`.
- The price selection rule lives in a domain service and uses the **Stream API**. The database
  query only pre-filters candidates. It must never `ORDER BY priority` or `LIMIT` to pick the winner.
- JPA entities and REST DTOs never cross into the domain. Map explicitly with mapper classes.
- An ArchUnit test enforces these rules. Never weaken it to make a build pass.

## Code conventions

- Use `record`s for immutable data (domain model, queries, DTOs), and validate invariants in compact constructors.
- Use constructor injection only, never field injection.
- Money is `BigDecimal`, never `double`/`float`. Dates are `LocalDateTime` without a time zone, at second precision.
- Return `Optional` rather than `null`. A missing price becomes a domain exception, which the REST
  layer turns into a 404.
- Errors are RFC 9457 `ProblemDetail`, returned from a single global exception handler.
- Schema and seed data come from `schema.sql` and `data.sql` (`ddl-auto=none`). The seed data must
  match the seed data table in `requirements.md` and `docs/source/prices.csv` exactly.
- Keep comments for the "why", not the "what".

## Gotchas

**Spring Boot 4 is not Boot 3.** Don't use Boot 3 idioms:
- Jackson 3 is under `tools.jackson.*`, not `com.fasterxml.jackson.*`.
- Use `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`). `@MockBean` no longer exists.
- The test annotations moved to modular packages:
  - `org.springframework.boot.webmvc.test.autoconfigure` for `@WebMvcTest` and `@AutoConfigureMockMvc`;
  - `org.springframework.boot.data.jpa.test.autoconfigure` for `@DataJpaTest`.
- The starters are modular too (`spring-boot-starter-webmvc`, plus `*-test` starters per module).

**Date parsing (D13).** Bind `applicationDate` with `@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")`.
`iso = DATE_TIME` accepts fractional seconds, which would break B12.

## Testing

- T1–T4 in `requirements.md` must be satisfied. Start each test name with its case ID, e.g.
  `@DisplayName("AT-2: 2020-06-14 16:00 → price list 2, 25.45 EUR")`.
- The AT and B cases are **integration tests** (`@SpringBootTest`, real H2 seed data, nothing mocked),
  and they assert every response field.
- Domain and use case tests are plain unit tests, with no Spring context.
- Expected values come from the AT and B tables in `requirements.md`. Never change an expected value
  to make a test pass.
- New rules come with their tests in the same change. For bug fixes, first write a failing test that
  reproduces the bug.

## Confidentiality

- **Never** write the real brand name from the original test statement anywhere: code, tests, docs,
  commit messages or comments. Brand `1` is always "STORE Z".
- Don't commit the original PDF statement. Its transcription with the name replaced is in
  `requirements.md` Appendix A.

## Git

- Main branch: `master`. Make small, focused commits using [Conventional Commits](https://www.conventionalcommits.org)
  (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).
- `./gradlew build` must pass before committing.
