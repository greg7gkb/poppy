# `@poppy/schema`

JSON Schema definitions for Poppy documents — **the source of truth**. Every renderer and the server library validate against these schemas.

## Status

**Phase 1, in progress.** Schema files for v0.1 are committed; package build and pnpm workspace wiring come in a subsequent Phase 1 session. The package is not yet installable from npm.

## What's here

```
packages/schema/
├── schemas/
│   ├── poppy.schema.json              # entry: $defs for Component / Action unions
│   ├── components/
│   │   ├── stack.schema.json
│   │   ├── text.schema.json
│   │   ├── image.schema.json
│   │   └── button.schema.json
│   ├── actions/
│   │   └── navigate.schema.json
│   └── tokens/
│       ├── spacing.schema.json
│       ├── size.schema.json
│       ├── color.schema.json
│       ├── weight.schema.json
│       ├── alignment.schema.json
│       ├── fit.schema.json
│       └── axis.schema.json
├── examples/                          # sample documents, also used by the corpus
│   ├── 01-text.json
│   ├── 02-stack-vertical.json
│   ├── 03-image.json
│   ├── 04-button-navigate.json
│   └── 05-kitchen-sink.json
└── src/
    ├── index.ts                       # package entry — exports the TS types
    └── types.ts                       # hand-written TS types mirroring the schema
```

## Schema overview (v0.1)

A Poppy document:

```json
{
  "$schema": "https://raw.githubusercontent.com/greg7gkb/poppy/v0.1.0/packages/schema/schemas/poppy.schema.json",
  "version": "0.1",
  "root": { "type": "Stack", "axis": "vertical", "children": [ ... ] }
}
```

- **Components:** `Stack`, `Text`, `Image`, `Button`.
- **Actions:** `navigate { uri }` — opaque URI dispatched to host's `onAction` callback.
- **Tokens:** all spacing / sizing / color / weight / alignment / fit / axis values are semantic enums (`xs`/`sm`/`md`/`lg`/`xl`, `default`/`primary`/`secondary`/`danger`/`success`, etc.). Each renderer maps tokens to its native scale.

See [`docs/phase-1-plan.md`](../../docs/phase-1-plan.md) for the full Phase 1 specification and rationale.

## Conventions

- Component `type` discriminators are PascalCase: `Stack`, `Text`, `Image`, `Button`.
- Action `type` discriminators are lowercase: `navigate`.
- Field names are camelCase.
- Schema files use Draft 2020-12 with `additionalProperties: false` on each component body, `unevaluatedProperties: false` on the root, and an Ajv `discriminator` keyword on each oneOf union for clean error messages.
- `Image.alt` is required — accessibility from day one.
- `url` and `uri` fields validate only `minLength: 1` (no `format: "uri"`) — hosts may use any deep-link scheme.

## Versioning

See [ADR-0006](../../docs/adr/0006-schema-versioning.md). Document `version` is `MAJOR.MINOR` (no patch). Forward-compat within a major; renderers ignore unknown fields on known components but reject unknown component types.

## See also

- [ADR-0001 — Schema-first contract](../../docs/adr/0001-schema-first-contract.md)
- [ADR-0004 — Conformance corpus](../../docs/adr/0004-conformance-corpus.md)
- [ADR-0005 — Minimize third-party dependencies](../../docs/adr/0005-minimize-third-party-dependencies.md)
- [ADR-0006 — Schema versioning](../../docs/adr/0006-schema-versioning.md)
