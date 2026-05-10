# Poppy iOS example app

A minimal Poppy application demonstrating how to integrate the `Poppy` SwiftPM library. Renders a sample Poppy document and routes button taps back to the host as on-screen log entries.

Built as a **SwiftPM macOS executable** so the smoke test runs from the command line on any developer's Mac without spinning up an iOS Simulator. The integration code (`ContentView.swift`, `SampleDocument.swift`, `PoppyExampleApp.swift`) is identical to what you'd ship in a real iOS app — Poppy's Package.swift declares both `.iOS(.v16)` and `.macOS(.v13)`, and the example's SwiftUI code uses only API that's available on both.

## What this example shows

The integration loop, in four steps inside `Sources/PoppyExample/ContentView.swift`:

1. **`Poppy.validate(_:)`** — never throws; returns `ValidationResult.ok(document)` or `.failure(errors)`.
2. **Implementing `PoppyHost`** (the `DemoHost` struct) — the only required interaction surface. Three methods, two with default implementations:
   - `onAction(_:)` — fires when a Button (or future actionable component) activates. The action object is forwarded **verbatim** — the renderer never interprets URIs.
   - `onError(_:)` — surfaces validation failures and rejected URLs.
   - `isUrlAllowed(_:context:)` — gates `<img>` URLs. Default rejects `javascript:`, `vbscript:`, `file:`, and non-image `data:` URIs.
3. **`.environment(\.poppyTheme, customTheme)`** — overrides token defaults. Defaults match the web client's CSS; this demo bumps `primary` to magenta to make the override visible.
4. **`PoppyView(document:host:)`** — the actual render call. Pure data in, SwiftUI view out.

## Run it

```sh
# From this directory:
cd PoppyExample
swift run
```

A macOS window opens with the rendered Poppy document. Click any button to see actions appear in the bottom log strip.

To regenerate the build from scratch:

```sh
swift package clean
swift build && swift run
```

## Adapting this to your own iOS app

This example is built as a macOS executable for development convenience. To deploy a Poppy-powered screen to iOS:

1. Create an Xcode iOS App target.
2. Add Poppy as a Swift Package dependency:
   - In Xcode: **File → Add Package Dependencies…** → Add Local… → select `packages/client-ios/`.
   - Or in your iOS app's `Package.swift` if you have one:
     ```swift
     .package(path: "../poppy/packages/client-ios")
     // or once published:
     .package(url: "https://github.com/.../poppy", from: "0.2.0-alpha")
     ```
3. Drop `Sources/PoppyExample/ContentView.swift` and `SampleDocument.swift` into your iOS app target. Replace `PoppyExampleApp` with your app's existing `@main App`.

The integration code (Poppy import, validate, PoppyHost, PoppyView) is identical.

## Requirements

- Xcode 16+ (Swift 5.10+).
- macOS 13+ to run the example as a desktop app.

## See also

- [`packages/client-ios/README.md`](../../packages/client-ios/README.md) — full library API reference.
- [`docs/adr/0011-ios-theming.md`](../../docs/adr/0011-ios-theming.md) — theming via `EnvironmentValues`.
