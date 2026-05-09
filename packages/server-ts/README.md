# `@poppy/server-ts`

TypeScript server library for emitting and validating Poppy documents.

## Status

**Phase 1 shipped (`v0.1.0-alpha`).** Validation against schema v0.1 with full ADR-0006 version-compat enforcement. Not yet published to npm.

## API

```ts
import { validate, isValid, type PoppyDocument } from "@poppy/server-ts";

const result = validate(await fetchPoppyDocument());

if (result.ok) {
  // result.document is now typed as PoppyDocument
  return result.document;
}

for (const err of result.errors) {
  console.error(`${err.path}: ${err.message} (${err.keyword})`);
}
```

`validate()` returns a discriminated result and **never throws** on invalid input. `isValid()` is the TypeScript type guard form.

The package re-exports all the document types from `@poppy/schema` (`PoppyDocument`, `Component`, `Stack`, `Text`, `Image`, `Button`, `Action`, `NavigateAction`, plus the token unions) so consumers don't need to import from two packages.

### Version compatibility (ADR-0006)

`validate()` enforces the wire-format compatibility rule defined in [ADR-0006](../../docs/adr/0006-schema-versioning.md): documents declaring an unknown major or a future minor are rejected with a single error:

```ts
{ keyword: "version", path: "/version", message: "..." }
```

Centralizing this here means Phase 2 renderers (Android, iOS) can defer the check to their own equivalents and emit the same error shape — the conformance corpus pins the contract.

## Build pipeline

This package uses **Ajv standalone precompile** (see [ADR-0007](../../docs/adr/0007-ajv-standalone-precompile.md)). Ajv is a `devDependency`; the runtime ships a precompiled validator with no `eval`, no schema parsing, and no Ajv import.

- `pnpm compile-schema` — generates `src/generated/validator.js` from `../schema/schemas/`.
- `pnpm build` — runs `compile-schema` then `tsup` to bundle.
- `pnpm typecheck` — `tsc --noEmit`.
- `pnpm test` — runs `compile-schema` then `vitest run`.

`src/generated/validator.js` is gitignored. A committed `validator.d.ts` provides the type contract so typechecks pass without a build.

## See also

- [`@poppy/schema`](../schema/) — the JSON Schema source of truth and the TypeScript types this package re-exports.
- [`@poppy/client-web`](../client-web/) — the reference renderer that consumes documents this library produces.
- [ADR-0006 — Schema versioning](../../docs/adr/0006-schema-versioning.md)
- [ADR-0007 — Ajv standalone precompile](../../docs/adr/0007-ajv-standalone-precompile.md)
