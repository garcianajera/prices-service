# ADR-0003: Price selection rule in the domain, implemented with streams

- **Status:** Accepted
- **Date:** 2026-09-24
- **Requirements:** FR-4, AR-3, C5, D3, D4, T2

## Context

The core algorithm picks the applicable price: among the prices of a brand and product whose range
contains the application date (D3), the highest priority wins, with ties broken by D4. The reviewer
asked for the algorithms to be solved with streams and unit-tested (AR-3).

The rule could live in either of two places:
- **in the database**, e.g. `WHERE ... ORDER BY priority DESC, start_date DESC LIMIT 1`, or
- **in the domain**, working on a list of candidate prices.

## Decision

- The domain service `PriceSelector` owns the rule and implements it with the Stream API:
  `filter(price -> price.isApplicableAt(date))` followed by
  `max(comparingInt(Price::priority).thenComparing(Price::startDate))`, returning `Optional<Price>`.
- The outbound port `findCandidates(brandId, productId, applicationDate)` returns candidates. The
  persistence adapter pre-filters them by brand, product and date range for efficiency. It does **not**
  order by priority or limit the result.
- The domain re-applies the date filter. The rule is complete in the domain, and the database filter
  is only an optimisation.

## Alternatives considered

- **`ORDER BY priority DESC LIMIT 1` in the query.** One row transferred and less code, but the
  business rule would be hidden in SQL. It could only be tested against a database, and AR-3 wouldn't be met.
- **Load all prices of the product and filter everything in memory.** This keeps the database dumb,
  but transfers rows that can never apply, and it scales poorly.
- **An imperative loop instead of streams.** Equally correct, but it doesn't meet AR-3, and it's less
  declarative.

## Consequences

- The rule is readable in one place and unit-testable with in-memory fixtures (T2).
- The database returns every overlapping candidate instead of one row. For realistic data (a few
  overlapping price lists per product and moment) the cost is negligible.
- The date check exists twice, in the query and in the domain. The domain version is authoritative,
  and the integration tests (T1) cover both together.
- Changing the tie-break (Q1) only touches the domain comparator and its tests.
