# `@poppy/schema`

JSON Schema definitions for Poppy documents — **the source of truth**. Every renderer and the server library validate against these schemas.

## Status

Planned for **Phase 1**. Currently a placeholder.

## What lives here (when populated)

- `poppy.schema.json` — root schema for a Poppy document.
- `components/` — per-component schemas (`Stack`, `Text`, `Image`, `Button`).
- `actions/` — action schemas (`navigate`, ...).
- `examples/` — small JSON examples used in docs and tests.

## See also

- [ADR-0001 — Schema-first contract](../../docs/adr/0001-schema-first-contract.md)
- [ADR-0004 — Conformance corpus](../../docs/adr/0004-conformance-corpus.md)
