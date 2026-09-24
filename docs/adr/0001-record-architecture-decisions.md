# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-09-24

## Context

The service is a technical test assessed on design, code quality and correct test results. Reviewers
need to see why the design is what it is, not only what it is. [`requirements.md`](../requirements.md)
already records behaviour decisions (D1–D13), and [`architecture.md`](../architecture.md) describes
the current design. Neither keeps the reasoning and the alternatives behind architectural choices.

## Decision

Record every significant architectural decision as an Architecture Decision Record (ADR) in `docs/adr/`:

- File name: `NNNN-short-title.md`, numbered sequentially and never reused.
- Sections: Status, Date, Context, Decision, Alternatives considered, Consequences.
- Status is one of: Proposed, Accepted, Deprecated, Superseded by ADR-NNNN.
- ADRs are immutable once accepted. To change a decision, write a new ADR that supersedes the old one.
- An ADR is for a **design** choice where a reasonable alternative existed. **Behaviour** decisions
  (status codes, date rules, tie-breaks) stay in the decisions table of `requirements.md`.

## Alternatives considered

- **Only notes in `architecture.md`.** Simpler, but the document describes the current state, so the
  reasoning and rejected options get lost when it changes.
- **ADRs for every decision, including behaviour.** This would duplicate D1–D13 and create two sources of truth.

## Consequences

- The reasoning behind the design is explicit and reviewable.
- `architecture.md` links to the ADR behind each choice.
- A new architectural choice needs a new ADR in the same change as the code.
