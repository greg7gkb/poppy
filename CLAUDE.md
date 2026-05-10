# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Poppy is a polyglot multi-platform server-driven UI library. A server emits a JSON document conforming to a versioned schema (`packages/schema/`), and renderers on each platform turn it into native UI. Phase 0–2 shipped (TypeScript server lib, web/Android/iOS renderers, conformance corpus). Current tag: `v0.2.0-alpha`. Working name `poppy` is a placeholder for find-and-replace at v1.

## Commands

The repo has three independent build systems. None is a wrapper for the others — each package is built natively. Don't try to invoke Gradle from pnpm or vice-versa.

**TypeScript packages** (`@poppy/schema`, `@poppy/server-ts`, `@poppy/client-web`, `@poppy/conformance`) — pnpm 10 workspace at the repo root:

```sh
pnpm install                 # once
pnpm -r build                # MUST run before typecheck (server-ts exports point to dist/)
pnpm -r typecheck            # tsc --noEmit per package
pnpm -r test                 # 62 tests across server-ts (35) + client-web (27)
pnpm biome ci .              # lint check from repo root only
pnpm format                  # auto-format

# Single test: pnpm --filter @poppy/server-ts test --run -t "version compatibility"
```

`@poppy/schema` and `@poppy/conformance` are source-only (no build). `@poppy/server-ts` precompiles its Ajv validator to `src/generated/validator.js` (gitignored) — `pnpm build` runs `pnpm compile-schema && tsup`.

**Android library** (`packages/client-android/`) — standalone Gradle build, **own `settings.gradle.kts`, no parent**:

```sh
cd packages/client-android
./gradlew check              # JVM unit tests via Robolectric (no emulator)
./gradlew snapshotsUpdate    # regenerate snapshot.android.txt; gated by env var so check never rewrites
./gradlew assemble           # AAR
```

Requires JDK 17+ and Android SDK platform 35.

**iOS library** (`packages/client-ios/`) — SwiftPM:

```sh
cd packages/client-ios
swift test                   # 22 tests; runs via macOS target so no simulator needed
POPPY_UPDATE_SNAPSHOTS=1 swift test --filter UpdateSnapshots   # regenerate snapshot.ios.txt
```

Requires Xcode 16+. `Package.swift` declares both `.iOS(.v16)` and `.macOS(.v13)` so `swift test` runs on the developer's host.

**Mobile example apps** — separate from the libraries:

```sh
# Android: standalone Gradle build with composite include of the library
cd examples/android
./gradlew :app:assembleDebug    # produces APK
./gradlew :app:installDebug     # installs to connected device/emulator

# iOS: SwiftPM macOS executable
cd examples/ios/PoppyExample
swift run                       # opens a macOS window with the rendered demo
```

**Web example**:

```sh
cd examples/web
./run_server.sh                  # serves repo root on :8000; open http://localhost:8000/examples/web/
```

The `run_server.sh` exists because the page imports the bundle via `../../packages/...` — a raw `python3 -m http.server` from `examples/web/` 404s on those paths.

## Architecture (the load-bearing parts)

### The schema is the contract; the corpus is the regression surface

`packages/schema/` (Draft 2020-12 JSON Schema) is the single source of truth. `packages/conformance/cases/` holds the test fixtures: 15 valid cases + 7 invalid cases, each with `document.json`, `description.md` (cross-platform behavioral invariants per [ADR-0008](docs/adr/0008-cross-platform-conformance-strategy.md)), and per-platform snapshot files.

**The bright-line rule** ([CONTRIBUTING.md](CONTRIBUTING.md), [ADR-0004](docs/adr/0004-conformance-corpus.md)): no component lands in any renderer without a schema entry AND a corpus case. The corpus drives implementation across web, Android, and iOS.

### Cross-platform "agreement" is enforced by three layers, not by automated visual diff

[ADR-0008](docs/adr/0008-cross-platform-conformance-strategy.md) is the load-bearing doc. The three layers:

1. **Schema** (CI-automated): every renderer validates the same JSON and rejects invalid cases with the same error keyword (`discriminator`, `required`, `type`, `version`).
2. **Behavioral assertions** (per-platform-automated): each renderer's tests load the shared corpus inputs via `loadCases()`, then assert platform-native behavior — action dispatch, axis layout, alt-text exposure, color/spacing token resolution. The expected invariants live in each case's `description.md`.
3. **Per-platform snapshot files reviewed in PRs** (manual layer): `snapshot.web.html` is normalized HTML; `snapshot.android.txt` and `snapshot.ios.txt` are byte-identical typed-document tree dumps. Snapshot diffs are reviewed line-by-line in PRs — "just regenerated the snapshots" is not a valid PR description.

There is no platform-agnostic semantic IR. There is no cross-platform pixel-diff. Reviewers are expected to read the three snapshot files together when a component PR lands.

### Mobile renderers don't run JSON Schema; decoding is the validator

[ADR-0009](docs/adr/0009-mobile-validation-strategy.md). On Android, `kotlinx.serialization`'s polymorphic decoder rejects unknown discriminators and missing required fields; we map exceptions to the same `{ keyword, path }` shape `@poppy/server-ts` emits. iOS does the equivalent with `Codable` + custom `init(from:)`. A hand-written version-compat check ([ADR-0006](docs/adr/0006-schema-versioning.md)) runs after a successful decode on every platform.

The 7 invalid corpus cases drive the keyword mapping — CI fails if any case doesn't produce its declared keyword on any platform.

### `@poppy/server-ts` precompiles its validator at build time

[ADR-0007](docs/adr/0007-ajv-standalone-precompile.md). Ajv is a `devDependency`. The runtime ships a precompiled validator (`src/generated/validator.js`, gitignored). Zero schema parsing at runtime, no `eval` — CSP-safe. Hand-written `validator.d.ts` is committed so typecheck passes without a build step.

This is also why **CI runs `pnpm -r build` BEFORE `pnpm -r typecheck`**: `@poppy/client-web` imports `@poppy/server-ts`, whose `exports` point to `dist/` (only present post-build). On a clean checkout, typecheck would otherwise fail to resolve the import.

### TypeScript types live in `@poppy/schema`, not `@poppy/server-ts`

The server lib re-exports them. Both `client-web` and `server-ts` consume types from `@poppy/schema`. The schema package has `exports` pointing to `./src/index.ts` (source-only, no dist). `tsup` in `server-ts` inlines the types via `noExternal: [/^@poppy\//]`.

### Mobile theming is `CompositionLocal` (Android) and `EnvironmentValues` (iOS)

[ADR-0010](docs/adr/0010-android-theming.md), [ADR-0011](docs/adr/0011-ios-theming.md). Defaults match the web client's CSS exactly. Hosts override via `PoppyTheme(values = ...)` (Android) or `.environment(\.poppyTheme, ...)` (iOS). Image loading on Android goes through `LocalPoppyImageLoader` (Coil is the default impl, swappable).

### Mobile examples use composite builds

`examples/android/` is a **standalone** Gradle build. It depends on the library via `includeBuild("../../packages/client-android")` and resolves `dev.poppy:poppy-android:0.2.0-alpha` from the included project. No `publishToMavenLocal` step needed.

`examples/ios/PoppyExample/` is a SwiftPM package depending on the library via `.package(path: "../../../packages/client-ios")`. Built as a macOS executable (`swift run`) — the SwiftUI integration code is identical on iOS, but a macOS executable lets `swift run` work without a simulator.

## Conventions in this codebase

- **Conventional commits** with footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`.
- **Push to `main` directly** — solo dev, no PRs. Force-pushing to `main` to amend an unmerged or recently-pushed commit is acceptable when explicitly authorized; never silently. Tags follow `v{MAJOR}.{MINOR}.{PATCH}-{prerelease}` (e.g. `v0.2.0-alpha`).
- **Wire format `version: "MAJOR.MINOR"`** — no patch component in the document. Renderers reject unknown majors and future minors. The 0.1 schema is FROZEN through Phase 2; new components land in 0.2 + after Phase 4 starts.
- **Component `type` is PascalCase** (`Stack`, `Text`, `Image`, `Button`); **action `type` is lowercase** (`navigate`); **field names are camelCase**. The casing distinction is intentional — keeps error messages clean and prevents discriminator overload.
- **CI skips workflow runs for docs-only changes** via `paths-ignore` on `**/*.md`, `docs/**`, `LICENSE`, `NOTICE`, `.gitignore`. Mixed PRs (one .md plus one .kt) still run the full pipeline.
- **No new runtime dependency without discussion** ([ADR-0005](docs/adr/0005-minimize-third-party-dependencies.md)). Surface and justify in the PR description. Test-only deps are less constrained but still warrant a one-liner in commit/PR.
- **Snapshot file formats deliberately differ by platform**, no cross-platform standardization. Web is normalized HTML (because the web renderer's output is HTML); mobile snapshots are typed-document tree dumps (because the document, not the platform widget tree, is the comparison target). Android and iOS snapshots are byte-identical for the same input — that's intentional and tested.

## When making changes

- **Adding a component or token**: schema entry → corpus case (valid + invalid as appropriate) → renderer implementation, all in one PR. The bright-line rule applies.
- **Mobile renderer changes that don't change the document**: behavioral test required; snapshot file likely unchanged (snapshots derive from the typed document tree, not the live render).
- **Schema-level changes**: bump `version` per [ADR-0006](docs/adr/0006-schema-versioning.md). The version compat check in `@poppy/server-ts/src/validate.ts` is the authority — mobile renderers mirror its behavior.
- **CI changes** (`.github/workflows/**`): always trigger CI (deliberately not in `paths-ignore`).
- **For Phase 4 work**: open design questions accumulate in [`docs/phase-4-backlog.md`](docs/phase-4-backlog.md). Add new entries there before they crystallize into `docs/phase-4-plan.md`.
