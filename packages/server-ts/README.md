# `@poppy/server-ts`

TypeScript server library for emitting valid Poppy documents.

## Status

Planned for **Phase 1**. Currently a placeholder.

## Goals

- Type-safe builder API for constructing Poppy documents.
- Validation against [`@poppy/schema`](../schema/) before emission.
- Zero or near-zero runtime dependencies — a JSON Schema validator is the most likely first dependency, to be discussed per [ADR-0005](../../docs/adr/0005-minimize-third-party-dependencies.md).

## See also

- [ADR-0001 — Schema-first contract](../../docs/adr/0001-schema-first-contract.md)
