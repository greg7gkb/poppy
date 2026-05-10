# Poppy — Phase 2 Implementation Plan

## Context

Phase 1 (`v0.1.0-alpha`) shipped: schema v0.1, `@poppy/server-ts`, `@poppy/client-web`, `@poppy/conformance` (15 valid + 7 invalid cases with committed `snapshot.web.html`), CI green on every commit.

Phase 2 brings the wire format to **Android (Kotlin + Jetpack Compose)** and **iOS (Swift + SwiftUI)**. By end of Phase 2, the same JSON document the web client renders today must render to a semantically equivalent native UI on both mobile platforms. The conformance corpus and the schema are frozen for the duration of Phase 2 — any schema gap discovered while implementing the mobile renderers is fixed by adapting the renderer, not by changing the schema. (Real schema gaps are filed for Phase 4 deliberation.)

This plan is grounded in:

- **[ADR-0008](adr/0008-cross-platform-conformance-strategy.md)** — what cross-platform conformance actually means: shared schema (automated), shared corpus inputs + per-platform behavioral assertions (automated, native-language tests), per-platform snapshot files reviewed in PRs (manual layer).
- **[ADR-0002](adr/0002-monorepo-structure.md)** — each package uses native build tooling. No Nx/Turborepo wrapping. Mobile means Gradle and SwiftPM directly.
- **[ADR-0005](adr/0005-minimize-third-party-dependencies.md)** — dep additions are surfaced and justified. This plan introduces several mobile deps; each is justified inline.
- **[ADR-0006](adr/0006-schema-versioning.md)** — `MAJOR.MINOR` document version, mobile renderers reject unknown majors and future minors with the same error shape (`{ keyword: "version", path: "/version" }`) the server-ts validator emits.

The two largest risks: (1) tooling sprawl across two new build systems, two new test runners, and a CI matrix; (2) discovering schema design mistakes only when porting to Compose/SwiftUI. Mitigation for (1) is strict adherence to native idioms — no shared shim layer. Mitigation for (2) is freezing the schema and accepting that a mobile-discovered gap means a renderer-side workaround for v0.1.

## Goals

By end of Phase 2:

- `poppy-android` Compose library renders every component in the v0.1 schema.
- `poppy-ios` SwiftUI library renders every component in the v0.1 schema.
- Both platforms run the full conformance corpus (15 valid + 7 invalid). Invalid cases produce a validation error matching the schema keyword the case declares.
- Per-case `snapshot.android.txt` and `snapshot.ios.txt` files committed for every valid case. Reviewed line-by-line in PRs.
- Behavioral assertions per [ADR-0008](adr/0008-cross-platform-conformance-strategy.md) §2 implemented on each platform: action dispatch, axis layout, alt-text exposure, color token resolution, padding/spacing token mapping.
- CI extended to run Android (Linux runner) and iOS (macOS runner) jobs on every PR and every push to `main`.
- ADRs 0009–0011 documenting mobile validation strategy, Android theming, and iOS theming.
- `git tag v0.2.0-alpha` cut after green CI.

## Non-goals (Phase 2)

- **No schema changes.** v0.1 stays frozen. Any pressure to add components (lists, modals, slots, async data, forms) is logged for Phase 4.
- **No React renderer.** That's Phase 3.
- **No Maven Central / SwiftPM Registry publishing.** Phase 2 ends at git tag, like Phase 1. Distribution is a separate decision.
- **No Kotlin Multiplatform / Compose Multiplatform.** Each platform native, no shared runtime code. KMP is a Phase 3+ consideration if value emerges.
- **No ergonomic builders** (Kotlin DSL or Swift result builders for documents). Mobile renderers consume server-emitted JSON; document construction stays the server's job.
- **No example apps with full UI shells.** A full Android Studio project + Xcode workspace is too much ceremony for a smoke demo. Instead, ship **minimal smoke tests** under `examples/android/` and `examples/ios/` — small Composable / SwiftUI entry points that boot a sample document, ideally runnable via Android Studio's preview annotations and Xcode previews without an app shell.
- **No accessibility audit** beyond the basics defined for Phase 1 (`alt` text required and exposed; semantic widget choices). Deeper a11y is Phase 4.
- **No theming primitives beyond static token override** — mobile theming mirrors what the web client offers (override token defaults at theme scope; no runtime token swapping).

## Open dep additions

Three dependencies introduced by Phase 2. Each surfaced per [ADR-0005](adr/0005-minimize-third-party-dependencies.md):

| Dep | Platform | Used for | Why this and not something else |
|---|---|---|---|
| `kotlinx.serialization` | Android | JSON decoding into sealed-class document tree | JetBrains-blessed standard, reflection-free, plays cleanly with sealed classes (our Component union). Alternative: Moshi — a third-party non-JetBrains lib with comparable size; no decisive win, going with the standard. |
| Compose UI test (`androidx.compose.ui:ui-test-junit4`) | Android | snapshot serialization (`printToString()`) and behavioral assertions | First-party, no real alternative for Compose semantics-tree introspection. |
| Coil (`io.coil-kt:coil-compose`) | Android | async image loading | No first-party Compose async-image primitive. Coil is the de-facto choice in 2026, ~80 kB, Apache 2.0. Hidden behind a `PoppyImageLoader` abstraction so a host can swap to Glide / Fresco / custom by overriding one `CompositionLocal`. |
| ViewInspector | iOS | SwiftUI view-tree introspection for snapshots and assertions | The de-facto third-party tool for SwiftUI testing. Alternatives: Apple's `inspect` (private API, off-limits), pixel snapshot tests (rejected per ADR-0008), or hand-rolled environment introspection (would re-implement most of ViewInspector poorly). MIT-licensed, ~9k stars, actively maintained as of 2025. |

`kotlinx.coroutines` may transitively land via Compose; not added directly. `Codable` is part of the Swift standard library, no dep needed for iOS JSON.

## `poppy-android`

### Public API

```kotlin
package dev.poppy.android

@Serializable
sealed class Component { /* ... */ }

@Serializable
data class PoppyDocument(val version: String, val root: Component)

sealed class ValidationResult {
    data class Ok(val document: PoppyDocument) : ValidationResult()
    data class Failure(val errors: List<ValidationError>) : ValidationResult()
}

data class ValidationError(val path: String, val keyword: String, val message: String)

object Poppy {
    /** Parse and validate a document. Never throws on malformed input. */
    fun validate(json: String): ValidationResult
    fun isValid(json: String): Boolean
}

interface PoppyHost {
    fun onAction(action: Action)
    fun onError(throwable: Throwable) {} // default: no-op (caller logs)
    fun isUrlAllowed(url: String, context: ImageContext): Boolean = defaultAllowImageUrl(url)
}

@Composable
fun Poppy(document: PoppyDocument, host: PoppyHost, modifier: Modifier = Modifier)
```

- Document tree is a `@Serializable sealed class Component`. `kotlinx.serialization` handles dispatch via the `type` discriminator. JSON is decoded once; the rendered Composable receives the typed tree, not raw JSON.
- `validate()` returns a discriminated result. Decoder errors map to `ValidationError` (see "Validation strategy" below).
- The Composable recomposes when the input `PoppyDocument` changes. Stack uses `Row`/`Column` + `Modifier.padding` + `Arrangement.spacedBy`. Image goes through the `PoppyImageLoader` abstraction (see "Image loading abstraction" below).
- `host.onAction(action)` dispatches actions verbatim. Renderer never interprets a URI.

### Validation strategy

Per [ADR-0009](adr/0009-mobile-validation-strategy.md) (to be written in Week 1):

- Decoding is the validator. `kotlinx.serialization`'s `Json { ignoreUnknownKeys = true }` decoder either produces a typed `PoppyDocument` or throws a `SerializationException`. We catch and map to our `ValidationError` shape.
- The discriminator-driven sealed class union catches missing/unknown `type` values. Missing required fields (e.g. `Image.alt`, `Button.action`) throw `MissingFieldException`.
- Wrong field types (e.g. `Text.value: 42`) throw `JsonDecodingException`.
- Mapping table from kotlinx errors to corpus keywords:

| kotlinx exception | path | keyword |
|---|---|---|
| Unknown enum / discriminator | `/root/...` | `discriminator` |
| Missing required field | `/root/...` | `required` |
| Wrong JSON type | `/root/.../field` | `type` |
| Custom version-compat check | `/version` | `version` |

The version-compat check is hand-written, identical in spirit to `@poppy/server-ts`'s, and runs *after* decoding succeeds. Rejects any document whose major doesn't match `SUPPORTED_MAJOR` or whose minor exceeds `SUPPORTED_MINOR`.

- The corpus's invalid cases drive this mapping. CI fails if any invalid case doesn't produce its declared keyword.

### Theming

Per [ADR-0010](adr/0010-android-theming.md) (to be written in Week 2):

- A `PoppyTheme` `CompositionLocal` carries token→value maps. Defaults match web's CSS defaults (`md = 16.dp`, `primary = Color(0xFF0B66FF)`, etc.).
- Apps wrap their tree: `PoppyTheme(spacing = customSpacing, colors = customColors) { Poppy(doc, host) }`.
- Components read tokens via `LocalPoppyTheme.current.spacing[Spacing.MD]`. No raw pixel literals in the renderer.

### Image loading abstraction

There is no first-party Compose async-image primitive that handles the network, caching, and error states we need. We adopt **Coil** (`io.coil-kt:coil-compose`) — the de-facto Compose async-image lib, ~80 kB, MIT — as a Phase 2 dep. Per [ADR-0005](adr/0005-minimize-third-party-dependencies.md), this is justified by the absence of a viable alternative and the cost of writing one ourselves. To keep the dep swappable, the renderer never references Coil types directly; it goes through a small interface:

```kotlin
interface PoppyImageLoader {
    @Composable
    fun Image(
        url: String,
        contentDescription: String?,
        modifier: Modifier,
        contentScale: ContentScale,
    )
}

object CoilImageLoader : PoppyImageLoader {
    @Composable
    override fun Image(...) = AsyncImage(model = url, ...)
}

val LocalPoppyImageLoader = staticCompositionLocalOf<PoppyImageLoader> { CoilImageLoader }
```

Hosts who want to use Glide, Fresco, a stub for tests, or an internal cache override the `CompositionLocal`. Replacing Coil with another lib means rewriting one ~30-line implementation, not threading changes through `PoppyImage`.

This abstraction is documented as part of [ADR-0010](adr/0010-android-theming.md) — theming and the image loader are both `CompositionLocal`-driven extension points.

### Build

- Module: `packages/client-android/`
- `com.android.library` plugin, Kotlin 2.0+, K2 compiler.
- `compileSdk = 35`, `minSdk = 24`, `targetSdk = 35`. Java 17 source/target.
- Compose BOM (latest stable at Phase 2 start; 2024.12.x or newer).
- Gradle 8.x, Kotlin DSL exclusively (`build.gradle.kts`).
- Output: AAR. Not published; consumers vendor.

### Layout

```
packages/client-android/
├── build.gradle.kts
├── settings.gradle.kts        # standalone — not included in any other repo's settings
├── gradle/
│   └── libs.versions.toml     # version catalog (single source of truth for deps)
├── README.md
└── src/
    ├── main/
    │   └── kotlin/dev/poppy/android/
    │       ├── Document.kt    # @Serializable data classes for Component, Action, etc.
    │       ├── Tokens.kt      # Spacing / Color / Size / Weight / Alignment / Fit / Axis enums
    │       ├── Validate.kt    # Json decoder + version-compat + error mapping
    │       ├── Theme.kt       # PoppyTheme + LocalPoppyTheme CompositionLocal
    │       ├── Poppy.kt       # @Composable Poppy(doc, host, modifier)
    │       └── components/
    │           ├── PoppyStack.kt
    │           ├── PoppyText.kt
    │           ├── PoppyImage.kt
    │           └── PoppyButton.kt
    └── test/
        └── kotlin/dev/poppy/android/
            ├── CorpusTest.kt           # iterates valid + invalid corpus, asserts
            ├── BehaviorTest.kt         # ADR-0008 §2 invariants
            └── snapshot/
                └── UpdateSnapshots.kt  # standalone main() to regenerate snapshot.android.txt
```

### Tooling notes

- The Gradle build is **standalone** — `client-android/settings.gradle.kts` declares no parent. This keeps Android tooling self-contained and means `./gradlew` in this directory works without repo-root cooperation.
- Conformance corpus is loaded from `../../packages/conformance/cases/` via `File`. The corpus directory is the API; no Kotlin port of `loadCases()` — we re-implement the loader in ~30 lines per platform.
- Snapshot generator is a JUnit test (or `main()`) that overwrites `snapshot.android.txt` for every valid case. Run via `./gradlew :snapshotsUpdate` (custom task, not part of `check`).

## `poppy-ios`

### Public API

```swift
public struct PoppyDocument: Codable {
    public let version: String
    public let root: Component
}

public enum Component: Codable {
    case stack(Stack)
    case text(Text)
    case image(Image)
    case button(Button)
}

public enum ValidationError: Error {
    case decoding(path: String, keyword: String, message: String)
    case unsupportedVersion(version: String)
}

public enum ValidationResult {
    case ok(PoppyDocument)
    case failure([ValidationError])
}

public protocol PoppyHost {
    func onAction(_ action: Action)
    func onError(_ error: Error)
    func isUrlAllowed(_ url: String, context: ImageContext) -> Bool
}
public extension PoppyHost { /* default no-op for onError, default allowlist for isUrlAllowed */ }

public enum Poppy {
    public static func validate(_ json: Data) -> ValidationResult
    public static func isValid(_ json: Data) -> Bool
}

public struct PoppyView: View {
    public init(document: PoppyDocument, host: PoppyHost)
    public var body: some View
}
```

- `Component` is an enum with associated values. Custom `Codable` implementation switches on the `type` discriminator.
- `validate()` returns a discriminated result; `PoppyView` accepts a typed `PoppyDocument` (you call `validate()` first, then construct the view from `result.document`).
- `host.onAction` dispatches verbatim. Renderer doesn't interpret URIs.

### Validation strategy

Per ADR-0009 (shared with Android): decoding is the validator. Custom `Codable.init(from:)` on `Component` switches on `type` and throws `DecodingError` for missing/unknown discriminators. Standard `Codable` machinery throws for missing required fields and wrong types. Mapping table:

| `DecodingError` case | path | keyword |
|---|---|---|
| `dataCorrupted` (custom: unknown type) | `/root/...` | `discriminator` |
| `keyNotFound` | `/root/.../field` | `required` |
| `typeMismatch` | `/root/.../field` | `type` |
| Custom version-compat | `/version` | `version` |

Same as Android: invalid corpus cases drive the mapping; CI fails if any case doesn't produce its declared keyword.

### Theming

Per [ADR-0011](adr/0011-ios-theming.md) (to be written in Week 2):

- A `PoppyTheme` struct + `EnvironmentValues` extension. Default values match web/Android defaults.
- Apps theme via `.environment(\.poppyTheme, customTheme)`.
- Components read tokens via `@Environment(\.poppyTheme) var theme` and `theme.spacing(.md)`.

### Build

- Module: `packages/client-ios/`
- `Package.swift` at module root, `swift-tools-version: 5.10` (Swift 6 strict concurrency optional).
- Platforms: `.iOS(.v16)`. (iOS 16 is the practical floor in 2026; SwiftUI features used here are all stable since iOS 14, but iOS 16 simplifies test ergonomics with `NavigationStack`.)
- Single library product, single Sources folder, single Tests folder.

### Layout

```
packages/client-ios/
├── Package.swift
├── README.md
└── Sources/
│   └── Poppy/
│       ├── Document.swift     # PoppyDocument + Component enum + Codable conformance
│       ├── Tokens.swift       # Spacing / Color / etc. enums
│       ├── Validate.swift     # decoder + version-compat + error mapping
│       ├── Theme.swift        # PoppyTheme + @Environment integration
│       ├── PoppyView.swift    # the SwiftUI view
│       └── Components/
│           ├── PoppyStack.swift
│           ├── PoppyText.swift
│           ├── PoppyImage.swift
│           └── PoppyButton.swift
└── Tests/
    └── PoppyTests/
        ├── CorpusTests.swift          # iterates valid + invalid corpus
        ├── BehaviorTests.swift        # ADR-0008 §2 invariants
        └── UpdateSnapshots.swift      # XCTest that regenerates snapshot.ios.txt
```

### Tooling notes

- Conformance corpus loaded from `../../packages/conformance/cases/` via `FileManager`. ~30 lines.
- Snapshot regeneration is an XCTest method, gated behind an env var (`POPPY_UPDATE_SNAPSHOTS=1`) so `swift test` doesn't accidentally rewrite committed snapshots. Run via `POPPY_UPDATE_SNAPSHOTS=1 swift test --filter UpdateSnapshots`.

## Conformance corpus changes

The corpus directory grows two snapshot files per valid case. No new cases unless a Phase 2 implementation reveals an ambiguity that needs a regression entry (in which case: add the case to all three platforms in the same PR, per the bright-line rule).

After Phase 2:

```
cases/valid/015-kitchen-sink/
  document.json              # unchanged
  description.md             # extended with behavioral invariants per ADR-0008
  snapshot.web.html          # unchanged
  snapshot.android.txt       # NEW
  snapshot.ios.txt           # NEW
```

`description.md` for each case is amended (in Week 1) to enumerate the behavioral invariants the case is expected to test. Example:

```markdown
# 014-button-basic

A single Button with a navigate action. Verifies that:
- Button renders as the platform's native button widget (`<button type="button">`,
  `Button(action:)` in SwiftUI, `Button` Composable in Compose).
- Clicking/tapping the button calls `host.onAction` with the verbatim action object.
- Button label is the `label` field, exposed to platform accessibility.
```

Per-platform tests assert these explicitly.

## Tooling

| Choice | Pick | Notes |
|---|---|---|
| Android build | Gradle (Kotlin DSL) | Standalone — own `settings.gradle.kts` |
| Kotlin | 2.0+ (K2) | Compose-compatible |
| Android compileSdk / minSdk / targetSdk | 35 / 24 / 35 | Min API 24 covers ~99% of devices in 2026 |
| Compose | BOM, latest stable | Single import for Compose libs |
| JSON | `kotlinx.serialization` | Reflection-free, JetBrains-standard |
| Android tests | JUnit 4 + Compose UI test | First-party for Compose testing |
| iOS build | SwiftPM | `Package.swift` at module root |
| Swift | 5.10+ | 6.x optional but adopt strict concurrency lazily |
| iOS platform | iOS 16+ | Practical 2026 floor |
| JSON | `Codable` (stdlib) | No dep |
| iOS tests | XCTest + ViewInspector | ViewInspector only practical option for SwiftUI tree introspection |
| CI Linux runner | `ubuntu-latest` | For Android; Gradle caches via setup-gradle |
| CI macOS runner | `macos-14` | For iOS; pin to Xcode 16+ |

## CI updates

Add two new jobs to `.github/workflows/ci.yml`. Keep all jobs in one workflow file to start; split if matrix complexity grows.

```yaml
android:
  name: Android Compose
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v6
    - uses: actions/setup-java@v4
      with: { distribution: temurin, java-version: 17 }
    - uses: gradle/actions/setup-gradle@v4
    - working-directory: packages/client-android
      run: ./gradlew check         # tests + lint

ios:
  name: iOS SwiftUI
  runs-on: macos-14
  steps:
    - uses: actions/checkout@v6
    - uses: maxim-lobanov/setup-xcode@v1
      with: { xcode-version: '16.0' }
    - working-directory: packages/client-ios
      run: swift test
```

Branch protection on `main`: require all four jobs (`meta`, `js`, `android`, `ios`) green before merge.

macOS runner cost: GitHub Actions bills macOS at 10× Linux on private repos but is **free for public repos**. The repo is public, so no billing concern, but macOS runner queue depth is higher than Linux; expect 2–5 min queue time. To avoid burning queue time on every push, the `ios` job is restricted to **`pull_request` and `push: branches: [main]`** triggers (no feature-branch pushes). Feature-branch CI is the developer's responsibility to run locally with `swift test`.

## Sequencing

Two independent tracks (Android, iOS) run in parallel via subagents (see "Parallelization via subagents" above). Cross-cutting work is owned by the main session.

**Week 1 — Cross-cutting prep + scaffold (main session)**
- ADR-0009 (mobile validation strategy) drafted, reviewed, merged. Both subagents will read this.
- Corpus `description.md` files updated to enumerate behavioral invariants per case.
- `.github/workflows/ci.yml` extended with `android` and `ios` job stubs (running `echo` initially; real commands wired in once each subagent's package builds). Avoids the chicken-and-egg of needing a green CI on a half-built package.
- Main session writes one-line briefs for the Android and iOS subagents, including pointers to ADRs, schema, corpus, and the Phase 2 plan.

**Weeks 2–3 — Parallel implementation (two subagents)**
- **Android subagent (worktree)**: scaffolds Gradle build, implements decoder + version-compat, all 7 invalid cases pass; implements `Poppy()` Composable + theming + image abstraction; implements snapshot generator and lands 15 `snapshot.android.txt` files; implements behavioral assertions; writes ADR-0010. Returns its worktree branch when done.
- **iOS subagent (worktree)**: same shape — `Package.swift`, decoder, version-compat, `PoppyView`, theming, snapshot generator, 15 `snapshot.ios.txt` files, behavioral assertions, ADR-0011. Returns its worktree branch when done.
- Main session monitors both, intervenes if a subagent gets stuck, and reviews the diffs as they land.

**Week 4 — Merge, hardening, tag (main session)**
- Merge both subagent branches to `main` (one PR per platform for review visibility).
- Wire CI's `android` and `ios` job commands from `echo` to real `./gradlew check` and `swift test`.
- Manual cross-platform review on the kitchen-sink case: open in browser, Android Studio preview, Xcode preview. Document any divergences; either fix or file follow-ups.
- README quickstarts for `packages/client-android/` and `packages/client-ios/`.
- CHANGELOG entry for `0.2.0-alpha`.
- `git tag v0.2.0-alpha`.

## Critical files

- `packages/client-android/build.gradle.kts` — Gradle config, dep versions.
- `packages/client-android/src/main/kotlin/.../Document.kt` — sealed class hierarchy.
- `packages/client-android/src/main/kotlin/.../Validate.kt` — validation entry, error mapping.
- `packages/client-android/src/main/kotlin/.../Poppy.kt` — Composable entry.
- `packages/client-android/src/test/.../CorpusTest.kt` — corpus runner.
- `packages/client-ios/Package.swift` — SwiftPM manifest.
- `packages/client-ios/Sources/Poppy/Document.swift` — Codable enum + structs.
- `packages/client-ios/Sources/Poppy/Validate.swift` — validation entry.
- `packages/client-ios/Sources/Poppy/PoppyView.swift` — SwiftUI entry.
- `packages/client-ios/Tests/PoppyTests/CorpusTests.swift` — corpus runner.
- `.github/workflows/ci.yml` — adds `android` and `ios` jobs.
- `docs/adr/0009-mobile-validation-strategy.md` — new ADR.
- `docs/adr/0010-android-theming.md` — new ADR.
- `docs/adr/0011-ios-theming.md` — new ADR.

## Smaller decisions defaulted (overridable)

- **Android package namespace**: `dev.poppy.android`. Conventional reverse-DNS, easy to find, avoids placeholder `com.poppy.*` collision.
- **Swift module name**: `Poppy`. Short, single-word, mirrors how SwiftUI views work.
- **Min Android API**: 24. **Min iOS**: 16.
- **Image loading on Android**: Coil bundled by default; swappable via `LocalPoppyImageLoader`. See "Image loading abstraction" above.
- **Image loading on iOS**: SwiftUI's `AsyncImage`. No third-party image lib (the stdlib primitive is sufficient).
- **Snapshot file format**: plain text, version-controlled. Android: `printToString()` output verbatim. iOS: ViewInspector tree dump.
- **Examples for mobile**: minimal smoke entry points, no app shell. `examples/android/SmokeTest.kt` boots `Poppy(doc, host)` in a Composable preview; `examples/ios/SmokePreview.swift` does the same with `#Preview`. A developer can render either in their IDE without building an APK or installing on a simulator.
- **Distribution**: Phase 2 ends at `git tag v0.2.0-alpha`. Maven Central / SwiftPM Registry publication is a separate decision per platform.

## Verification

Phase 2 is "done" when:

1. From a clean clone: `cd packages/client-android && ./gradlew check` passes.
2. From a clean clone: `cd packages/client-ios && swift test` passes (on macOS).
3. CI's `meta`, `js`, `android`, and `ios` jobs all pass on `main`.
4. Every valid corpus case has a committed `snapshot.android.txt` and `snapshot.ios.txt`, each line-reviewed in its own PR.
5. Behavioral assertions (per ADR-0008 §2) implemented in both `BehaviorTest.kt` and `BehaviorTests.swift` for action dispatch, axis layout, alt-text exposure, color token resolution, and padding/spacing token mapping. Each invariant runs against the relevant corpus cases (not just the kitchen-sink).
6. Manual cross-platform review on the kitchen-sink case: open in browser, Android emulator, iOS simulator. Snapshot the result. Document any divergences and either fix or file follow-up.
7. ADRs 0009, 0010, 0011 exist and are accepted.
8. `git tag v0.2.0-alpha` exists. CHANGELOG.md has a `0.2.0-alpha` entry.
9. After approval, this plan is committed to `docs/phase-2-plan.md` (parallel to `phase-1-plan.md` and `bootstrap-plan.md`).

## Resolved decisions (from Phase 2 planning round)

The earlier round of "Open questions" closed as follows:

1. ✅ **Min Android API 24 / Min iOS 16.**
2. ✅ **No personal handles in package names or strings.** Android namespace is `dev.poppy.android` (placeholder; find-and-replace at v1 alongside the project name itself). Swift module stays `Poppy`. *Existing `greg7gkb/*` URLs in schema `$id` and `package.json` repository fields are a separate cleanup task — see "Follow-up after Phase 2" below.*
3. ✅ **ViewInspector accepted** as iOS test-only dep.
4. ✅ **Coil included as Android dep**, behind the `PoppyImageLoader` abstraction so it's swappable.
5. ✅ **Minimal smoke tests** in `examples/android/` and `examples/ios/`.
6. ✅ **iOS CI restricted to `pull_request` and `push: branches: [main]`.** Feature-branch testing is the developer's responsibility (`swift test` locally).
7. ✅ **Parallelize via subagents.** See "Parallelization via subagents" below.
8. ✅ **Tag format `v0.2.0-alpha`.** Existing tag `v0.1.0-alpha` already matches; no conversion needed. Going forward, all repo tags follow `v{MAJOR}.{MINOR}.{PATCH}-{prerelease}` (no extra `.0` suffix). Note: npm package versions retain semver-required `.0` suffix (`@poppy/client-android-equivalent` n/a here, but `@poppy/server-ts@0.1.0-alpha.0`) — that's a separate convention dictated by npm/semver and is not changed.

## Parallelization via subagents

Android and iOS are genuinely independent — no shared code, no shared build, no shared test runner. They share only (a) the schema in `packages/schema/`, (b) the corpus in `packages/conformance/cases/`, and (c) the conformance contract in ADR-0008. This makes them an ideal candidate for **subagent parallelization**.

Plan:

- **Main session** handles cross-cutting work that both platforms depend on:
  - **Week 1 prep**: ADR-0009 (mobile validation strategy) drafted and merged — both subagents read this. `description.md` updates in the corpus to add behavioral invariants per case — both subagents test against these.
  - **CI workflow** (`.github/workflows/ci.yml`) updates for both jobs.
  - **Final merge and review** of each subagent's branch back to `main`.
  - **Coordination on naming and conventions** if the subagents diverge.
- **Android subagent**, spawned with `isolation: "worktree"`, gets a self-contained brief:
  - The Phase 2 plan (this file)
  - Pointers to `packages/schema/`, `packages/conformance/`, ADR-0008, ADR-0009 (once written)
  - Scope: implement everything under `packages/client-android/` end-to-end. Land `snapshot.android.txt` for each valid case. Write ADR-0010 for theming.
  - Done criteria: `./gradlew check` passes from a clean checkout, all 7 invalid corpus cases produce their declared keyword, all 15 valid cases produce a committed snapshot.
- **iOS subagent**, spawned in a separate `worktree`, identical structure:
  - Scope: everything under `packages/client-ios/`. Land `snapshot.ios.txt`. Write ADR-0011.
  - Done criteria: `swift test` passes from a clean checkout, same corpus expectations.

Both subagents run concurrently. When each finishes, it returns its worktree branch path. The main session reviews the diff, runs the tests once more, and merges to `main`.

**Constraints worth knowing:**
- Subagents can't see each other's progress or this conversation. Their briefs must be fully self-contained (~3k tokens each).
- A subagent that gets stuck on an under-specified detail will either guess (sometimes wrong) or stop and report back. Quality of the brief matters more than length.
- Each subagent's worktree is a full repo checkout — they can read everything but should only write under their assigned package directory.
- The user reviews two PRs per phase week instead of one. Worth it for the ~2× speedup if both subagents stay on track.

If a subagent goes off-track, the main session intervenes by either re-prompting (continuing the agent with a corrective message) or aborting and re-spawning with a tighter brief. We hold the wheel.

## Follow-up after Phase 2 (out of scope for this plan, tracked here)

- **Remove `greg7gkb` from existing repo references.** The schema files' `$id` URLs (`https://raw.githubusercontent.com/greg7gkb/poppy/v0.1.0-alpha/...`) and the `repository` field in each `package.json` reference the actual GitHub repo at `github.com/greg7gkb/poppy`. Removing the personal handle requires either (a) moving the repo to a `poppy/poppy` org / a different account, (b) replacing the canonical `$id` URLs with domain-agnostic identifiers like `urn:poppy:v0.1:component:stack` (per JSON Schema spec, `$id` need not be a fetchable URL), or (c) leaving the URL pinned to the actual repo and treating the personal handle as a project-internal artifact. **Decision needed before next release.** This plan does not block on it.
- **Maven Central / SwiftPM Registry publishing.** Out of scope for v0.2.0-alpha; revisit when there's an actual external consumer.
- **Migrate root README and repo metadata** from `greg7gkb/poppy` URLs to whatever the resolved location is.
