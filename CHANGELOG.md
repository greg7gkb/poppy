# Changelog

All notable changes to Poppy are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project
adheres to [Semantic Versioning](https://semver.org/) at the package level.
Document wire-format versioning is documented in [ADR-0006](docs/adr/0006-schema-versioning.md).

## [Unreleased]

## [0.2.0-alpha] — 2026-05-09

Phase 2 — native mobile renderers brought to parity with the web client.
The same JSON document the web renderer accepts now renders to native UI
on Android and iOS via the same conformance corpus.

### Added — Android (`poppy-android`)

- **Standalone Gradle module** at `packages/client-android/`. Kotlin DSL,
  Kotlin 2.0+ K2 compiler, Compose BOM, Min API 24 / target 35.
  Namespace: `dev.poppy.android`.
- **`@Serializable` Document hierarchy** mirroring the TypeScript types in
  `@poppy/schema`. `kotlinx.serialization` with `classDiscriminator = "type"`.
- **`Poppy.validate(json)` returns `ValidationResult.Ok | Failure`.** Never
  throws. Maps `kotlinx.serialization` exceptions to corpus keywords per
  ADR-0009 (`discriminator`, `required`, `type`, `version`).
- **`@Composable Poppy(document, host, modifier)`** entry. Stack uses
  `Row`/`Column` + `Modifier.padding` + `Arrangement.spacedBy`. Stretch
  alignment honored via per-child `Modifier.fillMaxWidth/Height()` —
  matches CSS `align-items: stretch` semantics across platforms.
- **`PoppyTheme` `CompositionLocal`** carrying token→value maps. Defaults
  match web. Hosts override at any scope.
- **`PoppyImageLoader` extension point**: Coil bundled by default but the
  renderer never references Coil types directly. Hosts swap by overriding
  `LocalPoppyImageLoader`. Documented in ADR-0010.
- **JVM-runnable Compose UI tests** via Robolectric (test-only dep). CI
  on `ubuntu-latest` exercises the renderer without an emulator.
- 15 committed `snapshot.android.txt` files using the unmerged
  Compose semantics tree.
- ADR-0010 (Android theming) accepted.

### Added — iOS (`poppy-ios`)

- **SwiftPM module** at `packages/client-ios/`. `swift-tools-version: 5.10`,
  `.iOS(.v16)` and `.macOS(.v13)` (so `swift test` runs on the dev's host
  without a simulator). Swift module name: `Poppy`.
- **`Codable` Document with discriminator dispatch.** Custom
  `init(from decoder:)` on `Component` and `Action` enums switches on the
  wire-format `type` field.
- **`Poppy.validate(_:)` returns `ValidationResult.ok | failure`.** Never
  throws. Maps `DecodingError` cases to corpus keywords per ADR-0009.
- **`PoppyView` SwiftUI view** with `init(document:, host:)`. Stretch
  alignment honored via per-child `frame(maxWidth/maxHeight: .infinity)`
  modifier.
- **`PoppyTheme` via `@Environment(\.poppyTheme)`** with web-matching
  defaults. Documented in ADR-0011.
- **`AsyncImage` for image loading.** No third-party image dep on iOS.
- **ViewInspector** for SwiftUI tree introspection in tests (test-only dep,
  ADR-0005-justified).
- 15 committed `snapshot.ios.txt` files using a custom typed-tree dump
  (chosen for stability across Xcode releases over ViewInspector's
  default `print()`).
- ADR-0011 (iOS theming) accepted.

### Added — Conformance and infrastructure

- **Per-case behavioral invariants** in every valid corpus case's
  `description.md` enumerate the cross-platform expectations each
  renderer must verify (action dispatch, axis layout, alt-text
  exposure, color/spacing token resolution, stack `id` exposure, image
  URL passthrough, image `fit` mapping).
- **CI matrix expanded** to four jobs: `meta`, `js`, `android`
  (`ubuntu-latest` + JDK 21 + Android SDK 35), `ios` (`macos-15` +
  Xcode 16). All four required for merge.
- **Subagent briefs** at `docs/phase-2-{android,ios}-brief.md` document
  the spec the Phase 2 implementation subagents executed against.
- **CONTRIBUTING.md** lists per-platform developer toolchain
  prerequisites.
- ADR-0008 (cross-platform conformance strategy) accepted before Phase 2
  began. ADR-0009 (mobile validation strategy) accepted at Phase 2
  Week 1 to spec how decoders map to corpus keywords.

### Notes

- Wire format (schema v0.1) **unchanged** — Phase 2 is purely additive
  renderers.
- Mobile renderers don't run a JSON Schema validator; typed decoding
  is the validation per ADR-0009.
- Cross-platform "agreement" is enforced by (a) shared schema, (b) shared
  corpus inputs + per-platform behavioral assertions, (c) per-platform
  snapshot files reviewed in PRs. Per ADR-0008.
- Snapshot file formats deliberately differ per platform (`snapshot.web.html`
  is normalized HTML; `snapshot.android.txt` is Compose semantics tree;
  `snapshot.ios.txt` is a custom typed-tree dump). Each chose what makes
  sense for its platform; no cross-platform format standardization.
- Not yet published to Maven Central / SwiftPM Registry. Install via git
  tag or vendoring.

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

[Unreleased]: https://github.com/greg7gkb/poppy/compare/v0.2.0-alpha...HEAD
[0.2.0-alpha]: https://github.com/greg7gkb/poppy/releases/tag/v0.2.0-alpha
[0.1.0-alpha]: https://github.com/greg7gkb/poppy/releases/tag/v0.1.0-alpha
