# ADR-0011: iOS theming via SwiftUI EnvironmentValues

## Status

Accepted — 2026-05-09

## Context

Per [ADR-0006](0006-schema-versioning.md) and the Phase 2 plan, every platform renderer must honor a fixed token vocabulary (`Spacing`, `Size`, `Color`, `Weight`, `Alignment`, `Fit`) and let hosts override the default values without forking the renderer. The web client does this through CSS custom properties on `:root` (override any `--poppy-*` variable at a parent scope). Android does it through a `CompositionLocal` (per ADR-0010). iOS needs the equivalent SwiftUI mechanism.

SwiftUI offers three idiomatic places to plumb host-provided values down a view tree:

1. **`@Environment` + `EnvironmentKey`** — Apple's first-party mechanism for ambient values that flow implicitly. Hosts override via `.environment(\.key, value)` at any ancestor; descendants read via `@Environment(\.key) var value`. The default value lives on the `EnvironmentKey`. This is how SwiftUI itself ships `\.colorScheme`, `\.locale`, `\.font`, etc.

2. **`@EnvironmentObject` (or `@Observable` on iOS 17+)** — for reference-typed observable values whose changes should trigger redraws. Heavier than necessary for static token tables; hosts that want runtime token swapping would benefit, but Phase 2 explicitly defers runtime swapping (see Phase 2 plan §"Non-goals").

3. **Init-injected configuration struct** — pass a `PoppyTheme` directly to `PoppyView.init`. Simple but doesn't compose: hosts wrapping a Poppy subtree in a custom container would have to thread the theme through manually.

The web client's `--poppy-*` CSS-custom-properties solution is conceptually closest to option (1): defaults at the renderer's scope, override at any ancestor scope. Option (2) is overkill for v0.1's static tokens. Option (3) loses the "ambient" property that makes themes ergonomic in nested layouts.

## Decision

iOS theming uses **SwiftUI's `@Environment` + `EnvironmentKey` mechanism**.

Concretely (`packages/client-ios/Sources/Poppy/Theme.swift`):

```swift
public struct PoppyTheme: Sendable {
    public var spacingValues: [Spacing: CGFloat]
    public var sizeValues: [Size: CGFloat]
    public var colorValues: [PoppyColor: Color]
    public var weightValues: [Weight: Font.Weight]

    public func spacing(_ token: Spacing) -> CGFloat
    public func size(_ token: Size) -> CGFloat
    public func color(_ token: PoppyColor) -> Color
    public func weight(_ token: Weight) -> Font.Weight

    public static let `default` = PoppyTheme()  // matches web defaults
}

private struct PoppyThemeKey: EnvironmentKey {
    static let defaultValue: PoppyTheme = .default
}

public extension EnvironmentValues {
    var poppyTheme: PoppyTheme {
        get { self[PoppyThemeKey.self] }
        set { self[PoppyThemeKey.self] = newValue }
    }
}
```

Hosts override:

```swift
PoppyView(document: doc, host: myHost)
    .environment(\.poppyTheme, customTheme)
```

Components read:

```swift
struct PoppyText: View {
    let text: Text
    @Environment(\.poppyTheme) private var theme

    var body: some View {
        SwiftUI.Text(text.value)
            .font(.system(size: theme.size(text.size ?? .md), weight: theme.weight(text.weight ?? .regular)))
            .foregroundColor(theme.color(text.color ?? .default))
    }
}
```

### Default values

`PoppyTheme.default` mirrors the web client's `:root` CSS variables in `packages/client-web/src/styles/poppy.css` exactly:

| Token | Value | Source |
|---|---|---|
| `spacing(.none)` | 0 | `--poppy-space-none: 0` |
| `spacing(.xs)` | 4 | `--poppy-space-xs: 4px` |
| `spacing(.sm)` | 8 | `--poppy-space-sm: 8px` |
| `spacing(.md)` | 16 | `--poppy-space-md: 16px` |
| `spacing(.lg)` | 24 | `--poppy-space-lg: 24px` |
| `spacing(.xl)` | 32 | `--poppy-space-xl: 32px` |
| `size(.xs)` … `size(.xl)` | 12, 14, 16, 20, 28 | `--poppy-size-*` |
| `color(.default)` | `#111` | `--poppy-color-default` |
| `color(.primary)` | `#0B66FF` | `--poppy-color-primary` |
| `color(.secondary)` | `#6B7280` | `--poppy-color-secondary` |
| `color(.danger)` | `#D4351C` | `--poppy-color-danger` |
| `color(.success)` | `#1E7C2C` | `--poppy-color-success` |
| `weight(.regular)` | `.regular` (≈400) | `--poppy-weight-regular: 400` |
| `weight(.medium)` | `.medium` (≈500) | `--poppy-weight-medium: 500` |
| `weight(.bold)` | `.bold` (≈700) | `--poppy-weight-bold: 700` |

The behavioral test suite (`BehaviorTests.swift`) asserts that color/spacing/size/weight tokens resolve through the theme — never against a hard-coded literal — so changes to the defaults can't accidentally drift between the renderer and the test expectations.

### Naming

Two conventions force a small naming choice:

- The schema has token enums named `Color` and `Alignment`. Both names are taken by SwiftUI (`SwiftUI.Color`, `SwiftUI.Alignment`).
- Renaming the enums avoids ambiguity. We chose `PoppyColor` and `PoppyAlignment` — the prefix is explicit, mirrors how Apple disambiguates its own conflicting types (`SwiftUI.Image` vs `UIKit.UIImage`), and produces tolerable site call-out (`text.color = .primary` is unaffected; only the type name carries the prefix).
- `Spacing`, `Size`, `Weight`, `Fit`, `Axis` keep their original names — none collide with SwiftUI types in scope.

### What this is not

- **Not a runtime token swapping mechanism.** The Phase 2 plan explicitly defers that (§"Non-goals" — "No theming primitives beyond static token override"). Hosts can swap themes between renders by remounting, but a single render uses one immutable `PoppyTheme`.
- **Not multiple parallel themes.** Only one `\.poppyTheme` is in scope at a time; nesting overrides as SwiftUI's `\.environment` does (innermost wins).
- **Not extensible by adding new token kinds at runtime.** The token enums are part of the v0.1 schema; new tokens require a schema change (per [ADR-0006](0006-schema-versioning.md)) and a new field on `PoppyTheme`.

## Consequences

**Positive**

- First-party SwiftUI mechanism. No custom propagation layer; idiomatic for SwiftUI hosts.
- Defaults are co-located with the type via `EnvironmentKey.defaultValue`, so a host that never sets `\.poppyTheme` still gets a working renderer with web-equivalent defaults.
- Tests can pass an explicit theme via `.environment(\.poppyTheme, theme)` and query the same theme to assert that token resolution flows through it (rather than asserting raw RGB / pixel values, which would entangle tests with default values that may evolve).
- Multiple Poppy subtrees in one host can carry different themes by overriding at the appropriate ancestor scope.

**Negative**

- `@Environment` reads work only inside `View.body`. Code paths outside SwiftUI (e.g. a token-resolved value computed at construction time) can't see the theme; this is fine for v0.1 because all token resolution happens during view rendering.
- `EnvironmentValues` is a singleton conceptually — a host that wants two simultaneous renderings of the same document with different themes (for split-screen comparisons in a tool) must wrap each subtree separately. Same limitation as the web's CSS-custom-properties model.
- `PoppyTheme` is a value type; hosts that want observable theme changes must wrap it in their own `@Observable` and emit `\.environment(\.poppyTheme, currentTheme)` themselves. We accept this — Phase 2 doesn't ship runtime theming.

## Alternatives Considered

- **`@EnvironmentObject` / `@Observable` reference type.** Rejected for v0.1: heavier than necessary for static tokens; pulls in observation overhead; shifts the API from value semantics to reference semantics for no immediate benefit. Reconsider in Phase 4 if dynamic theming becomes a goal.
- **Init-injected `PoppyTheme` parameter on `PoppyView`.** Rejected: doesn't propagate naturally through nested host views; forces every host that wraps Poppy in another container to thread the theme. Loses the ambient property that makes themes useful in compositional UIs.
- **Static singleton (`PoppyTheme.current`).** Rejected: process-global state breaks SwiftUI's preview model and prevents per-subtree theming.
- **Shared cross-platform `Theme` IR with per-platform adapters.** Rejected per [ADR-0008](0008-cross-platform-conformance-strategy.md)'s "no platform-agnostic IR" stance: each platform uses its native theming mechanism; conformance is enforced through the schema and per-case behavioral tests, not a shared theme abstraction.

## References

- [ADR-0005 — Minimize third-party dependencies](0005-minimize-third-party-dependencies.md)
- [ADR-0006 — Schema versioning](0006-schema-versioning.md) — token vocabulary stays frozen within a major.
- [ADR-0008 — Cross-platform conformance strategy](0008-cross-platform-conformance-strategy.md) — behavioral tests query the theme, not literals.
- [ADR-0009 — Mobile validation strategy](0009-mobile-validation-strategy.md) — the validation parallel: each platform uses its native idiom.
- [ADR-0010 — Android theming](0010-android-theming.md) — the Compose `CompositionLocal` parallel to this ADR.
- [SwiftUI EnvironmentKey](https://developer.apple.com/documentation/swiftui/environmentkey) — Apple's documentation for the mechanism.
- `packages/client-web/src/styles/poppy.css` — the default token values this ADR mirrors.
