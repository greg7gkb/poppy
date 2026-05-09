# Changelog

All notable changes to Poppy are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project
adheres to [Semantic Versioning](https://semver.org/) at the package level.
Document wire-format versioning is documented in [ADR-0006](docs/adr/0006-schema-versioning.md).

## [Unreleased]

## [0.1.0-alpha] — 2026-05-09

The first tagged release. Establishes the Phase 1 contract: a JSON Schema, a
TypeScript validator, a web renderer, and a cross-platform conformance corpus.

### Added — Schema (`@poppy/schema`)

- **Wire format `version: "0.1"`** with components `Stack`, `Text`, `Image`,
  `Button` and the `navigate` action. `Image.alt` is required.
- **Semantic tokens** for spacing/padding (`none`/`xs`/`sm`/`md`/`lg`/`xl`),
  text size, color (`default`/`primary`/`secondary`/`danger`/`success`),
  weight, alignment (`start`/`center`/`end`/`stretch`), image fit
  (`contain`/`cover`/`fill`), and stack axis (`horizontal`/`vertical`).
- 13 JSON Schema files (Draft 2020-12) with `unevaluatedProperties: false` and
  Ajv `discriminator` keywords on each `oneOf` union.
- Hand-written TypeScript types mirroring the schema; `SCHEMA_VERSION` constant
  exported alongside.
- 5 example documents at `packages/schema/examples/`.
- ADR-0006 documents the `MAJOR.MINOR` versioning rule.

### Added — Server (`@poppy/server-ts`)

- `validate(doc)` returning a discriminated `{ ok, document } | { ok, errors }`
  result. **Never throws** on invalid input.
- `isValid(doc)` TypeScript type guard.
- **ADR-0006 version compatibility check** built into `validate()`: rejects
  unknown majors and future minors with `{ keyword: "version", path: "/version" }`.
- **Ajv standalone precompile pipeline** (ADR-0007). Ajv is a `devDependency`
  only; the runtime ships a precompiled validator with no `eval` and no schema
  parsing — CSP-safe.
- ESM-only output via tsup.

### Added — Web client (`@poppy/client-web`)

- `render(doc, container, host)` returning `{ update, destroy }`. Validation
  runs by default; failures surface through `host.onError`.
- Native DOM, no framework dependency. Stack uses flexbox, Button is a real
  `<button type="button">`, Image enforces `alt`.
- **URL allowlist for `<img>`** (ADR-0001 spirit): `http(s)`, relative URLs,
  `data:image/*` allowed; `javascript:`, `vbscript:`, `file:`, `data:text/html`
  rejected. Overridable via `host.isUrlAllowed`.
- `Button` listeners scoped to an `AbortController` so `destroy()` detaches
  them deterministically.
- BEM-like class naming (`poppy-{component}--{modifier}`); tokens exposed as
  `:root --poppy-*` CSS custom properties for host theming. `poppy.css` ships
  with sensible defaults.

### Added — Conformance (`@poppy/conformance`)

- 15 valid + 7 invalid corpus cases under `cases/{valid,invalid}/NNN-slug/`.
- `loadCases(corpusRoot)` runner consumed by `server-ts` and `client-web`
  test suites; will be reused by Phase 2 mobile renderers.
- `normalize-html` deterministic serializer (jsdom-backed): alphabetized
  attributes, alphabetized de-duplicated classes, void-element rules,
  indented for human review.
- Per-case `snapshot.web.html` files for every valid case — the cross-platform
  contract Phase 2 renderers must reproduce.

### Added — Tooling and infrastructure

- pnpm 10 workspace, Biome 1.9 (lint+format), Vitest 2, tsup 8, TypeScript 5.9.
- GitHub Actions CI: lint → build → typecheck → test, on every push and PR.
  (Build runs before typecheck so cross-package imports resolve on a clean
  checkout.)
- Governance: `LICENSE`, `NOTICE`, `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`,
  `SECURITY.md`, ADRs 0001–0007, Phase 1 plan at `docs/phase-1-plan.md`.

### Notes

- Not yet published to npm. Install from source.
- Mobile renderers are the next milestone (Phase 2). The conformance corpus
  is the contract they must satisfy.

[Unreleased]: https://github.com/greg7gkb/poppy/compare/v0.1.0-alpha...HEAD
[0.1.0-alpha]: https://github.com/greg7gkb/poppy/releases/tag/v0.1.0-alpha
