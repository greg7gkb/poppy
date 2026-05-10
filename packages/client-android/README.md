# `@poppy/client-android`

Kotlin + Jetpack Compose renderer for Poppy v0.1 documents.

## Status

Phase 2 — `v0.2.0-alpha`. The library renders every component in the v0.1
schema and passes the full conformance corpus (15 valid + 7 invalid cases).

## Quick start

```kotlin
// 1. Validate a document.
val result = Poppy.validate(jsonString)
if (result is ValidationResult.Failure) {
    result.errors.forEach { Log.e("poppy", "${it.path} (${it.keyword}): ${it.message}") }
    return
}
val document = (result as ValidationResult.Ok).document

// 2. Implement the host contract — handles actions, errors, URL gating.
val host = object : PoppyHost {
    override fun onAction(action: Action) {
        when (action) {
            is Action.Navigate -> startActivity(Intent(Intent.ACTION_VIEW, action.uri.toUri()))
        }
    }
}

// 3. Render. Wrap in PoppyTheme to override token defaults.
setContent {
    PoppyTheme {
        Poppy(document, host)
    }
}
```

## Public API

| Symbol | Purpose |
|---|---|
| `Poppy.validate(json: String): ValidationResult` | Parse + validate. Never throws. |
| `Poppy.isValid(json: String): Boolean` | Type-guard form. |
| `ValidationResult.Ok(document)` / `Failure(errors)` | Discriminated result. |
| `ValidationError(path, keyword, message)` | Same shape as `@poppy/server-ts`. |
| `PoppyDocument` | Typed document tree. |
| `Component` (sealed) | `Stack`, `Text`, `Image`, `Button`. |
| `Action` (sealed) | `Navigate`. |
| `PoppyHost` | Action dispatch + error surface + URL allowlist. |
| `@Composable Poppy(document, host, modifier)` | Renderer entry. |
| `PoppyTheme(values, content)` | Theme override scope. |
| `LocalPoppyTheme` / `LocalPoppyImageLoader` | Compose `CompositionLocal`s for hooks. |

See [`docs/adr/0010-android-theming.md`](../../docs/adr/0010-android-theming.md)
for the theming + image-loader hooks; see [`docs/adr/0009-mobile-validation-strategy.md`](../../docs/adr/0009-mobile-validation-strategy.md)
for how validation maps `kotlinx.serialization` exceptions to corpus
keywords.

## Theming

```kotlin
PoppyTheme(
    values = PoppyThemeValues(
        spacing = PoppySpacing(md = 12.dp),
        colors = PoppyColors(primary = Color(0xFFE91E63)),
    ),
) {
    Poppy(document, host)
}
```

Defaults match the web client's CSS — see
[`packages/client-web/src/styles/poppy.css`](../client-web/src/styles/poppy.css).

## Image loading

By default, images load through Coil (`io.coil-kt:coil-compose`). Hosts that
prefer Glide / Fresco / a stub for tests override the
`LocalPoppyImageLoader` `CompositionLocal`:

```kotlin
val MyLoader = object : PoppyImageLoader {
    @Composable
    override fun Image(
        url: String,
        contentDescription: String?,
        modifier: Modifier,
        contentScale: ContentScale,
    ) {
        // ...your loader
    }
}

CompositionLocalProvider(LocalPoppyImageLoader provides MyLoader) {
    Poppy(document, host)
}
```

The renderer itself never references Coil types directly.

## Build

Standalone Gradle (Kotlin DSL). No parent settings file — running
`./gradlew` from this directory works in isolation.

```
./gradlew check          # tests + lint  (CI gate)
./gradlew snapshotsUpdate # regenerate snapshot.android.txt for every valid case
./gradlew assemble       # build the AAR
```

Requirements:
- JDK 17 (the Compose K2 compiler requires 17+).
- Android SDK with platform 35 installed; set `sdk.dir` in `local.properties`
  (gitignored) or via the `ANDROID_HOME` env var.

Toolchain pins:
- Kotlin 2.0.21 (K2 compiler).
- AGP 8.7.3.
- Compose BOM 2024.12.01.
- Min API 24 / target 35.
- Gradle 8.10.2.

## Permissions

`AndroidManifest.xml` declares no permissions. Hosts that load images over
the network must add `<uses-permission android:name="android.permission.INTERNET" />`
in their app manifest — library modules cannot grant it.

## Testing

| Test class | What it covers |
|---|---|
| `CorpusTest` | Decodes every valid case; rejects every invalid case with the declared keyword. |
| `BehaviorTest` | The per-case behavioral invariants in each `description.md`. |
| `SnapshotTest` | Asserts every committed `snapshot.android.txt` matches the live render. |
| `UpdateSnapshots` | Gated regenerator for the snapshot files (`./gradlew snapshotsUpdate`). |

Tests run on JVM via Robolectric so `./gradlew check` exercises Compose
without an emulator. See [`docs/adr/0010-android-theming.md`](../../docs/adr/0010-android-theming.md)
§"Robolectric for JVM-runnable Compose UI tests" for the rationale.

## See also

- [ADR-0001 — Schema-first contract](../../docs/adr/0001-schema-first-contract.md)
- [ADR-0004 — Conformance corpus](../../docs/adr/0004-conformance-corpus.md)
- [ADR-0005 — Minimize third-party dependencies](../../docs/adr/0005-minimize-third-party-dependencies.md)
- [ADR-0006 — Schema versioning](../../docs/adr/0006-schema-versioning.md)
- [ADR-0008 — Cross-platform conformance strategy](../../docs/adr/0008-cross-platform-conformance-strategy.md)
- [ADR-0009 — Mobile validation strategy](../../docs/adr/0009-mobile-validation-strategy.md)
- [ADR-0010 — Android theming](../../docs/adr/0010-android-theming.md)
- [Phase 2 plan](../../docs/phase-2-plan.md)
