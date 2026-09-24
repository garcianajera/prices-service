# ADR-0007: Contract-first OpenAPI specification, hand-written

- **Status:** Accepted
- **Date:** 2026-09-24
- **Requirements:** FR-1–FR-3, N2, D5, D6, D13

## Context

The HTTP API was described informally in `requirements.md` (section 7.2). It gave an example request
and response, but no exact types, formats, required fields or error shapes. The initial plan was
code-first: springdoc would generate the spec from controller annotations at runtime, so a precise
contract would only exist after the code.

## Decision

- The API contract is [`docs/api/openapi.yaml`](../api/openapi.yaml) (OpenAPI 3.1). It is the
  **source of truth** for the HTTP interface: paths, parameters, schemas, status codes and error bodies.
- The controller and DTOs are **written by hand** to match it. No code is generated.
- The build copies the file onto the classpath (`static/openapi.yaml`). Swagger UI (springdoc) is
  configured to show this file, instead of a spec generated from annotations.
- The controller carries no OpenAPI annotations.
- The spec is consumer-facing: descriptions state behaviour in plain words and never reference internal
  documents or IDs (ADRs, D*, FR*, AT*). Traceability runs the other way: the internal docs reference the
  spec. Notes for maintainers go in YAML comments, which Swagger UI doesn't show.
- The spec is linted with Redocly CLI (`npx @redocly/cli lint docs/api/openapi.yaml`).
- Contract changes go into `openapi.yaml` first, and into the code in the same change.

## Alternatives considered

- **Code-first with springdoc annotations (the previous plan).** It's the least work, and the spec
  can never drift from the code. However, there is no contract before the code exists, the
  annotations clutter the controller, and the generated spec is hard to review in a pull request.
- **Contract-first with openapi-generator.** The generated interface and DTOs can't drift from the
  spec. However, it adds a Gradle plugin, its Spring generator may lag behind Spring Boot 4 and
  Jackson 3, and generated code is harder for reviewers to assess as part of code quality.

## Consequences

- There is a precise, reviewable contract before any code is written. It includes examples for the
  acceptance cases and the error cases.
- The spec and the code are kept in sync by hand. The integration tests (T1) assert every response
  field and status, which catches most drift.
- If drift becomes a problem, add a contract test that validates the integration-test responses
  against `openapi.yaml`. That would need a new test dependency (e.g. `swagger-request-validator-mockmvc`)
  and a new ADR.
- This supersedes the code-first plan for N2. springdoc is kept only to serve Swagger UI.
