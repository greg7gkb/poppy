# ADR-0007: Ajv standalone precompile for validation

## Status

Accepted — 2026-05-05

## Context

`@poppy/server-ts` validates Poppy documents against the JSON Schema in `@poppy/schema`. Per [ADR-0005](0005-minimize-third-party-dependencies.md) we minimize third-party runtime dependencies, and validation is the first place a real third-party dep was needed.

We considered four options (see Phase 1 plan):

| Option | Pros | Cons |
|---|---|---|
| Ajv standalone precompile | Validator is generated as plain JS at build time. Zero schema-parsing at runtime. No `eval` (CSP-safe). Smallest runtime footprint. Best error messages. | Build-time complexity (a small generation step). |
| Ajv runtime | Mature, fast. | Ships as a runtime dep (~120 KB). Code-gen at runtime via `eval`/`new Function`. CSP-incompatible. |
| Hand-rolled mini-validator (~300–500 LOC) | Zero deps. | We own the validator's bugs. Error message quality typically lags a mature library. |
| Lighter validator (e.g. `@cfworker/json-schema`) | Smaller (~30 KB), no eval. | Less battle-tested than Ajv. |

## Decision

We use **Ajv with standalone code generation**:

- `ajv` is a **`devDependency`** of `@poppy/server-ts`. Never runtime.
- `packages/server-ts/scripts/compile-schema.mjs` runs at build time. It loads every `.json` file under `packages/schema/schemas/`, registers them with Ajv 2020 (`discriminator: true`, `strict: false`, `code.esm: true`), and emits a precompiled standalone validator at `packages/server-ts/src/generated/validator.js`.
- `validator.js` is **gitignored**. CI regenerates it as part of `pnpm build`.
- A hand-written `validator.d.ts` next to the generated `.js` provides the TypeScript type contract so the package type-checks before any `.js` exists on disk.
- The build script also smoke-validates every document under `packages/schema/examples/` against the schema and exits non-zero on any failure. This catches schema-level breakage early, without waiting for the full test suite.

## Consequences

**Positive**

- Runtime ships zero schema-parsing code. Bundle size for adopters is smaller; cold start is faster.
- No `eval` in the runtime — adopters running under strict Content Security Policy can use `@poppy/server-ts` (and a future precompiled validator embedded in `@poppy/client-web`) without exception.
- Ajv's `discriminator` keyword gives clean error messages on type mismatches (`expected one of Stack/Text/Image/Button, got 'Buton'`) instead of useless `oneOf` failure noise.
- The build-time smoke pass against `examples/` catches schema-level mistakes the moment they happen — before the test suite runs.

**Negative**

- A generation step is part of the build. New contributors must run `pnpm compile-schema` (invoked transitively by `pnpm build` and `pnpm test`) before tests pass.
- The generated `validator.js` is invisible to git history. Reviewers must trust that the input schema and the generation script are correct; we mitigate by running the smoke pass in CI on every PR.
- Tied to Ajv's API (`discriminator` keyword, `strict: false`, ESM standalone output). Switching validators later means rewriting the script. Acceptable cost.

## Build pipeline

```
packages/server-ts/
├── scripts/
│   └── compile-schema.mjs    # reads ../../schema/schemas/, writes src/generated/validator.js
├── src/
│   ├── generated/
│   │   ├── validator.d.ts    # committed; type contract
│   │   └── validator.js      # gitignored; emitted by compile-schema
│   ├── validate.ts           # imports default from "./generated/validator.js"
│   └── index.ts
└── tsup.config.ts            # bundles src/index.ts to dist/, inlining validator.js
```

`pnpm build` runs `pnpm compile-schema && tsup`. `pnpm test` runs `pnpm compile-schema && vitest run`. `pnpm typecheck` is independent — it relies on `validator.d.ts` being committed.

## Alternatives Considered

See the table in Context. The headline ones rejected:

- **Ajv runtime.** The CSP concern alone justifies precompile. For server-side use it would be acceptable, but `@poppy/client-web` is also expected to want validation in some configurations and CSP is real on the web.
- **Hand-rolled validator.** The schema uses `oneOf` with `discriminator`, `unevaluatedProperties`, recursive `$ref`s, and shared token enums. Writing a correct validator for this is a meaningful effort, and the error-message ergonomics would lag Ajv's. The bright-line dep cost (one devDep) is much lower than the implementation cost.
- **`@cfworker/json-schema`.** Smaller and `eval`-free, but precompile-Ajv gives us both small runtime AND mature error messages — strictly better for our case.

## References

- [Ajv standalone validation code](https://ajv.js.org/standalone.html)
- ADR-0001 — schema-first contract.
- ADR-0005 — minimize third-party dependencies (this is the first real test of that policy).
- ADR-0006 — schema versioning.
