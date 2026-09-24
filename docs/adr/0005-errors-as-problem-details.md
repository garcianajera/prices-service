# ADR-0005: Errors as RFC 9457 Problem Details from a single handler

- **Status:** Accepted
- **Date:** 2026-09-24
- **Requirements:** D5, D6, D13, B8–B13

## Context

The API must return 404 when no price applies (D5), and 400 for missing, malformed or invalid input,
including fractional seconds (D6, D13). Unexpected errors must not expose internal details. Clients
need a consistent, machine-readable error body.

## Decision

- Return every error as an RFC 9457 `application/problem+json` body, using Spring's `ProblemDetail`.
- Map exceptions to statuses in one `@RestControllerAdvice` class (`RestExceptionHandler`) in the REST adapter:
  - `PriceNotFoundException` (domain) → 404.
  - Parameter binding and validation exceptions (missing parameter, type mismatch, bad date format,
    constraint violations) → 400.
  - Any other exception → 500, with a generic message. The details are logged, not returned.
- The domain throws only domain exceptions and knows nothing about HTTP.

## Alternatives considered

- **A custom error JSON (`{ "code", "message" }`).** Just as simple, but it's a home-made format that
  clients must learn. The standard is supported by Spring out of the box.
- **`ResponseStatusException` thrown from the controller or the use case.** Less code, but HTTP
  concerns would leak into the application layer, breaking C3, and the error mapping would be
  spread across classes.
- **Returning `ResponseEntity<Optional<...>>` with an empty 404 body.** The client gets no
  explanation, and that's inconsistent with the 400 responses.

## Consequences

- All errors share one documented format, and it can be described once in OpenAPI.
- Error mapping lives in one place and can be tested with a REST slice test.
- A new error case needs a domain exception, or a known framework exception, plus one handler method.
