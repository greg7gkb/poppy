# Poppy Android example app

A minimal Android application demonstrating how to integrate `dev.poppy:poppy-android` as a Gradle dependency. Renders a sample Poppy document and routes button clicks back to the host as Toasts.

## What this example shows

The integration loop, in five steps inside `app/src/main/kotlin/dev/poppy/example/PoppyDemoScreen.kt`:

1. **`Poppy.validate(jsonString)`** — never throws; returns `ValidationResult.Ok(document)` or `Failure(errors)`.
2. **Implementing `PoppyHost`** — the only required interaction surface. Three optional methods:
   - `onAction(action)` — fires when a Button (or future actionable component) activates. The action object is forwarded **verbatim** — the renderer never interprets URIs.
   - `onError(throwable)` — surfaces validation failures and rejected URLs. Default is a no-op; you almost always want to override.
   - `isUrlAllowed(url, context)` — gates `<img>` URLs. Default rejects `javascript:`, `vbscript:`, `file:`, and non-image `data:` URIs.
3. **`PoppyTheme(values)`** — overrides the default token→value maps. Defaults match the web client's CSS; this demo bumps `primary` to magenta to make the override visible.
4. **`LocalPoppyImageLoader`** — swap the default Coil-backed loader for any `PoppyImageLoader` impl (Glide, Fresco, an in-memory stub for tests). Demonstrated as a no-op override here; comments show the swap pattern.
5. **`Poppy(document, host, modifier)`** — the actual render call. Pure data in, Composable out.

## Run it

```sh
# From this directory:
./gradlew :app:installDebug         # installs onto a connected device or emulator
adb shell am start dev.poppy.example/.MainActivity
```

Or build the APK without installing:

```sh
./gradlew :app:assembleDebug
ls app/build/outputs/apk/debug/
```

The `:app:assembleDebug` task takes about 30 seconds on a cold cache, ~3 seconds incremental.

## How the build resolves the library

This is a standalone Gradle build — `examples/android/` has its own `settings.gradle.kts` and Gradle wrapper. It uses Gradle's **composite build** to depend on the local library:

```kotlin
// settings.gradle.kts
includeBuild("../../packages/client-android")
```

The library declares `group = "dev.poppy"` and `version = "0.2.0-alpha"` in its `build.gradle.kts`. The example app references it as a normal Maven coordinate:

```kotlin
// app/build.gradle.kts
implementation("dev.poppy:poppy-android:0.2.0-alpha")
```

When `:app:assembleDebug` runs, Gradle sees the coordinate, walks the included build's project metadata, and resolves it to the local library's outputs — no `publishToMavenLocal` step needed.

## Adapting this to your own app

Drop these dependencies into your existing app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.poppy:poppy-android:0.2.0-alpha")
}
```

Once Poppy is on Maven Central, that's the entire integration step (besides writing your `PoppyHost`). Until then, vendor the AAR or use `includeBuild("path/to/poppy/packages/client-android")` per the pattern this example uses.

## Requirements

- JDK 17+ (the library targets Java 17 bytecode; CI uses JDK 21).
- Android SDK with platform 35 installed.
- Either set `ANDROID_HOME` or create `local.properties` in this directory with `sdk.dir=/path/to/Android/sdk`.

## See also

- [`packages/client-android/README.md`](../../packages/client-android/README.md) — full library API reference.
- [`docs/adr/0010-android-theming.md`](../../docs/adr/0010-android-theming.md) — theming + image loader extension points.
