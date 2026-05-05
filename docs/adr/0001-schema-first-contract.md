# ADR-0001: JSON Schema as the contract

## Status

Accepted — 2026-05-05

## Context

Poppy renders the same JSON document on four platforms — Android Compose, iOS SwiftUI, native HTML, and (eventually) React. Each platform has its own UI primitives, layout model, and rendering quirks. Without a strict, shared specification, the renderers will drift: a server author cannot trust that what works on web works on iOS, and a client author cannot tell whether a divergence is a bug or an intentional platform-specific behavior.

We need a single source of truth that:

- Defines the exact shape of every Poppy document.
- Is machine-readable so it can validate inputs and outputs across all four target languages.
- Is human-readable so contributors can refer to it without running tooling.
- Can be versioned independently of any single renderer.

## Decision

A single set of **JSON Schema (Draft 2020-12)** files under [`packages/schema/`](../../packages/schema/) is the authoritative specification of the Poppy wire format.

- The server library emits documents and validates them against the schema before transmission.
- Every client renderer validates incoming documents against the schema before rendering. Validation failures are explicit errors, not silent fallbacks.
- The conformance corpus (see [ADR-0004](0004-conformance-corpus.md)) consists of inputs that conform to the schema and expected outputs that all renderers must produce.
- Renderers may not interpret JSON fields that are absent from the schema. New fields require a schema PR first.

The schema is versioned with the package using SemVer. A breaking schema change is a major version bump.

## Consequences

**Positive**

- Renderers cannot drift. Either a behavior is in the schema (and must be supported) or it is not (and must be ignored).
- Documentation can be generated from the schema, reducing the cost of keeping docs in sync with code.
- A new platform can be added by writing a renderer that satisfies the schema and the corpus, without coordination across other renderers.
- Tooling (validators, type generators, editor integrations) can be built on standard JSON Schema infrastructure that already exists in every target language.

**Negative**

- Schema-first imposes upfront design discipline: a new component requires a schema PR before any renderer code is written.
- JSON Schema is verbose; the schema package will be larger than a hand-rolled DSL.
- Some platform-specific concerns (e.g. Compose's `Modifier`, SwiftUI's environment) cannot live in the schema and must be expressed indirectly or kept out of the wire format entirely.

## Alternatives Considered

- **Hand-rolled types per language, kept in sync by review.** Rejected: known to drift; review cannot catch every divergence; no machine-checkable contract.
- **Protocol Buffers / FlatBuffers as the wire format.** Rejected: while these provide strong contracts, they impose a binary format that is awkward to inspect during development, and the per-language tooling for UI components is less mature than for JSON.
- **Smithy or OpenAPI.** Rejected: these are designed for API surface specification, not for nested structural data with discriminated unions like a UI tree.
- **TypeScript types as source of truth, exported to JSON Schema** (e.g. via Zod or TypeBox). Considered for ergonomics. Deferred: we can adopt a TS-authoring layer later that emits the JSON Schema, without changing the contract that JSON Schema is the spec.

## References

- JSON Schema 2020-12: https://json-schema.org/specification.html
- Microsoft Adaptive Cards (a similar schema-first SDUI project): https://adaptivecards.io/
