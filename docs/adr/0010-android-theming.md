# ADR-0010: Android theming and image-loader extension points

## Status

Accepted — 2026-05-09

## Context

Phase 2 ships `@poppy/client-android` — a Jetpack Compose renderer for v0.1
documents. Two cross-cutting concerns are unique to the Android renderer and
deserve a written rationale: **how hosts customize design tokens** (spacing,
color, size, weight) and **how hosts swap the image-loading library** away
from the bundled Coil dependency.

Both are choices the web renderer (`@poppy/client-web`) handles via CSS
custom properties and the platform's native `<img>` tag respectively. Compose
has neither equivalent; the hooks must be Compose-idiomatic. We pick the
mechanism most Compose libraries reach for: `CompositionLocal`.

The image-loader question is also entangled with [ADR-0005](0005-minimize-third-party-dependencies.md).
We ship Coil bundled by default — there is no first-party Compose async-image
primitive that handles network, caching, error/loading states and the cost
of writing one ourselves outweighs the dependency cost. But we recognize
Coil isn't the only option in the ecosystem (Glide, Fresco, Landscapist), and
hosts with an existing image stack must be able to opt out.

This ADR is also where the Phase 2 Android subagent's discovered
non-obvious pieces of the implementation are written down — most notably,
the testing strategy. `./gradlew check` runs unit tests, not connected
device tests, so getting Compose UI tests to actually run requires
**Robolectric**, which the Phase 2 plan does not enumerate as an explicit
dep. We adopt it as a test-only dep and explain the reasoning here.

## Decision

### Theming via `CompositionLocal`

The active theme is a `PoppyThemeValues` data class carrying four sub-bundles:
`PoppySpacing`, `PoppySizes`, `PoppyWeights`, `PoppyColors`. Each is a
plain `data class` with one field per token name, defaulted to the values
from `packages/client-web/src/styles/poppy.css` (`md = 16.dp`,
`primary = Color(0xFF0B66FF)`, etc.).

A `staticCompositionLocalOf<PoppyThemeValues>` exposes the active theme to
component composables. A `PoppyTheme(values, content)` Composable is the host
entry point — it wraps `CompositionLocalProvider(LocalPoppyTheme provides ...)`.
Component composables read tokens via `LocalPoppyTheme.current.spacing[token]`.

```kotlin
// Host code
PoppyTheme(values = PoppyThemeValues(colors = myColors)) {
    Poppy(document, host)
}

// Renderer code (PoppyText.kt)
val theme = LocalPoppyTheme.current
Text(
    text = text.value,
    color = theme.colors[text.color ?: Color.DEFAULT],
    fontSize = theme.sizes[text.size ?: Size.MD],
    ...
)
```

Hosts override any subset of tokens by passing a custom
`PoppyThemeValues(spacing = customSpacing)` — defaults stay for everything
they don't touch.

`staticCompositionLocalOf` (vs the more granular `compositionLocalOf`) is the
right choice because token changes are rare; on the rare event of a runtime
theme swap, invalidating the entire subtree is fine.

### Image loading via swappable `PoppyImageLoader`

The renderer never references Coil types directly. Instead, it dispatches to
`LocalPoppyImageLoader.current.Image(url, contentDescription, modifier, contentScale)`.
The interface is intentionally narrow — four parameters, one Composable
function — so swapping is a ~30-line replacement.

```kotlin
interface PoppyImageLoader {
    @Composable
    fun Image(url: String, contentDescription: String?, modifier: Modifier, contentScale: ContentScale)
}

object CoilImageLoader : PoppyImageLoader {
    @Composable
    override fun Image(url, contentDescription, modifier, contentScale) {
        AsyncImage(model = url, ...)
    }
}

val LocalPoppyImageLoader = staticCompositionLocalOf<PoppyImageLoader> { CoilImageLoader }
```

The default is `CoilImageLoader`. Hosts swap loaders by overriding the
`CompositionLocal`:

```kotlin
val GlideLoader = object : PoppyImageLoader { /* impl using Glide */ }
CompositionLocalProvider(LocalPoppyImageLoader provides GlideLoader) {
    Poppy(document, host)
}
```

Tests use a recording loader that captures URL + contentDescription without
hitting the network — the production renderer doesn't change.

### Robolectric for JVM-runnable Compose UI tests

Compose UI tests (`androidx.compose.ui.test.junit4`) traditionally require
either an emulator (`./gradlew connectedAndroidTest`) or Robolectric for
JVM-only runs. The Phase 2 plan's `./gradlew check` done-criterion implies
JVM-only — `check` runs unit tests, not connected device tests, and a
Linux CI runner without an emulator can only execute the JVM kind.

We adopt **Robolectric** as a `testImplementation` dependency:

- `org.robolectric:robolectric:4.14.1`
- Test classes use `@RunWith(RobolectricTestRunner::class)`,
  `@Config(sdk = [34], manifest = Config.NONE)`,
  `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.

Robolectric is **not** listed in Phase 2 plan §"Open dep additions". We add
it because the alternative — moving the entire test suite under
`src/androidTest/` and requiring an emulator for `./gradlew check` — would
fail the brief's "passes from a clean clone" rule on any Linux CI runner.

Robolectric is a test-only dep; it does not ship in the AAR and adds zero
weight to consumers. It is widely used for exactly this purpose (Compose UI
tests on JVM) and is officially supported by the Compose UI test API
(`runComposeUiTest` works under RobolectricTestRunner since 1.5).

### Snapshot-tree representation

Snapshots are derived from the **typed `PoppyDocument` tree** — a custom
walker (`SnapshotRenderer`) emits an indented `Type attr=value` format
that mirrors the iOS renderer's output byte-for-byte for every v0.1
case. This means:

- All token attributes (`color`, `size`, `weight`, `spacing`, `padding`,
  `alignment`, `axis`, `fit`) appear in the snapshot. A regression that
  changes a token is caught.
- All Stack containers appear in the tree, including their attributes.
- The format is identical across machines, JDK versions, and Robolectric
  releases — there's no Compose runtime involved.
- iOS and Android `snapshot.{ios,android}.txt` files are byte-identical
  for the same input document.

We **don't** use Compose's `printToString()` semantics-tree dump because
it's built for accessibility, not styling: it captures `Text`,
`ContentDescription`, `Role`, and `TestTag` but not visual tokens. Plain
layout containers (Column, Row) without semantics modifiers don't even
emit nodes. A snapshot test built on it can't catch the most common kind
of regression — a token applied wrong — and can't see structural changes
in the layout tree.

Snapshots are regenerated by `./gradlew snapshotsUpdate` (not part of
`check`). The regenerator is gated by an env var so it can never
accidentally rewrite committed snapshots during a normal test run.

### `alignment: stretch`

Compose's `Alignment.Horizontal`/`Vertical` enums for Column/Row don't have
a Stretch member, but the same semantics are achievable by passing
`Modifier.fillMaxWidth()` (Column) or `Modifier.fillMaxHeight()` (Row) to
each child. `PoppyStack` does this when `alignment == STRETCH`, mirroring
the SwiftUI renderer's per-child `frame(maxWidth/maxHeight: .infinity)`
approach and matching CSS `align-items: stretch`.

The container's outer alignment stays `Start`/`Top` in this case — the
per-child fill modifier is what produces the stretch semantics. The
snapshot captures the document-level `alignment=stretch` token, so a
regression that changed the schema mapping would be caught — but a
regression that broke the per-child fill modifier (without changing the
document) would not. Pixel screenshot tests, if added in Phase 4, would
close that gap.

## Consequences

**Positive**

- Theming feels native to Compose. Hosts already familiar with Material3
  theming reach for `CompositionLocalProvider` instinctively; we don't
  invent a parallel mechanism.
- Image-loader swap is one `CompositionLocal` override, not a vendored fork
  or a build-flavor switch. Hosts vendor any image lib without changing
  Poppy code.
- `./gradlew check` actually exercises the renderer, including UI behavior
  and snapshot equality, without an emulator. CI on `ubuntu-latest` can
  validate every PR.
- Snapshot harness is deterministic across Robolectric/SDK upgrades —
  normalization strips the run-specific bits.

**Negative**

- Robolectric was not in the Phase 2 plan's enumerated deps. We adopt it as
  a test-only dep and document the rationale here; it adds ~10MB to the
  test classpath but zero to the shipped AAR. If the policy is strict no-
  unenumerated-deps even for tests, the alternative is to gate the UI tests
  behind `connectedCheck` (requires emulator) and have CI accept a less-
  thorough `check`.
- The snapshot reflects the typed document tree, not the live Compose
  render. A renderer regression that didn't change what the document
  decodes to (e.g. an off-by-one in modifier application) wouldn't surface
  in the snapshot diff. Behavioral tests via Robolectric cover the live
  Compose path; pixel screenshots (Phase 4) would catch the rest.
- The snapshot is byte-identical to iOS's for the same input — that's
  intentional (any divergence between platforms is itself a bug). It's
  *different* from `snapshot.web.html`, which captures HTML structure.
  Per ADR-0008 this is expected; cross-platform parity is enforced by
  shared schema + behavioral assertions + manual PR review, not by a
  single canonical snapshot format.

## Alternatives Considered

- **Material3 theming integration (`MaterialTheme.colorScheme`).** Reuse
  Material3's `colorScheme`, `typography`, `shapes`. Rejected: forces hosts
  to think in Material design tokens (`primary`, `onPrimary`, `surface`),
  which don't map to Poppy's tokens (`primary`, `secondary`, `danger`,
  `success`). Better to keep Poppy's namespace separate so hosts can theme
  Poppy independently of their app's Material theme.

- **Static `object Tokens` with `var` overrides.** Simple, but mutable global
  state is hostile to Compose's recomposition model and causes test
  pollution.

- **Builder DSL for theme construction** (`PoppyTheme { spacing { md = 12.dp } }`).
  Slightly nicer for incremental overrides but adds API surface for
  marginal ergonomic gain. Defer to v1.

- **Bundle no image loader; require host to provide one.** Forces every host
  to implement `PoppyImageLoader` even if they're happy with Coil. The
  expected case is "use the default and forget about it"; making that the
  zero-friction path is correct. Hosts who need to swap pay only the cost
  of the swap, not the cost of bootstrapping.

- **Use Compose UI test on a real emulator** (`./gradlew connectedCheck`).
  Rejected as the gate for `./gradlew check` because the brief and the plan
  both name `./gradlew check` as the done-criterion command, and Linux CI
  runners don't have GPU emulators readily available without managed-device
  setup that significantly inflates CI time. Robolectric runs the same UI
  tests in seconds.

- **Use Paparazzi for snapshot-style tests.** Pixel-snapshot library that
  runs without an emulator. Rejected because per ADR-0008, Phase 2 explicitly
  uses semantics-tree snapshots, not pixel snapshots — pixel snapshots add
  font/anti-aliasing/dpi noise that trips on every Compose / SDK upgrade.

## References

- [ADR-0002 — Monorepo structure](0002-monorepo-structure.md) — each package
  uses native build tooling. Android = Gradle.
- [ADR-0005 — Minimize third-party dependencies](0005-minimize-third-party-dependencies.md) —
  the policy this ADR adds Coil and Robolectric under.
- [ADR-0008 — Cross-platform conformance strategy](0008-cross-platform-conformance-strategy.md) —
  defines what snapshot.android.txt is for.
- [ADR-0009 — Mobile validation strategy](0009-mobile-validation-strategy.md) —
  the validation approach the renderer assumes.
- [Compose CompositionLocal docs](https://developer.android.com/jetpack/compose/compositionlocal)
- [Coil — Compose async image library](https://coil-kt.github.io/coil/compose/)
- [Robolectric — Android tests on JVM](https://robolectric.org/)
- [`runComposeUiTest` reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/package-summary#runComposeUiTest)
