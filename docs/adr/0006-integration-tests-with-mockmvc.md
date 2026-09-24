# ADR-0006: Integration tests with `@SpringBootTest` and MockMvc

- **Status:** Accepted
- **Date:** 2026-09-24
- **Requirements:** FR-6, T1, AT-1–AT-5, B1–B13

## Context

The acceptance cases must be integration tests. They run the full application against the real H2
seed data, with no layer mocked, and assert every response field (T1). There are two standard ways
to call the endpoint:

- **MockMvc** inside `@SpringBootTest`: the whole Spring context runs, and requests go through the
  real `DispatcherServlet`, filters, controller, advice and message converters, but without opening a
  network port.
- **A real HTTP port** (`webEnvironment = RANDOM_PORT`) with an HTTP client such as `RestTestClient`:
  requests go through the embedded server and the network stack as well.

## Decision

- Use `@SpringBootTest` + `@AutoConfigureMockMvc`, calling the endpoint with MockMvc.
- Write the acceptance cases as one parameterized test per table (AT and B), with each case's ID in
  its display name, and assert every response field.
- Don't use `@MockitoBean` in these tests. Every layer is real, down to H2.

## Alternatives considered

- **`RANDOM_PORT` with `RestTestClient`.** It also covers the embedded server and HTTP
  serialisation, but it's slower, needs port handling, and adds nothing this service needs: there
  are no filters, security or custom server configuration.
- **`@WebMvcTest` with a mocked use case.** Fast, but it doesn't touch the database or the selection
  rule, so it can't satisfy T1. It's kept only as an extra slice test for the REST adapter.

## Consequences

- The acceptance tests exercise controller → use case → domain → persistence → H2 end to end, and run quickly.
- The embedded server's own behaviour isn't tested. That's accepted, because there is no custom
  server configuration.
- If filters, security or server settings are added later, the decision should be revisited with a
  new ADR.
