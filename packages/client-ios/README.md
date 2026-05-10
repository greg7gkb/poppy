# `poppy-ios`

Swift + SwiftUI renderer for Poppy v0.1 documents.

## Status

Phase 2 — implements the v0.1 schema in full. Parity with `@poppy/client-web` and `poppy-android` enforced by the shared conformance corpus at `../conformance/cases/`.

## Module

- Swift module name: `Poppy`
- Platforms: `.iOS(.v16)`, `.macOS(.v13)` (the macOS target lets `swift test` run on a developer host without a simulator; the renderer is feature-equivalent on both platforms).
- Distribution: SwiftPM only. Not published to a registry in v0.2.0-alpha — consumers vendor or pin to a git tag.
- Test-only dep: [ViewInspector](https://github.com/nalexn/ViewInspector) for SwiftUI view-tree introspection. See [ADR-0005](../../docs/adr/0005-minimize-third-party-dependencies.md) for the justification.

## Quickstart

```swift
import Poppy

let json: Data = ...  // load from server, file, etc.

switch Poppy.validate(json) {
case .ok(let document):
    // Hand the typed document to PoppyView; render anywhere.
    let view = PoppyView(document: document, host: MyHost())
        .environment(\.poppyTheme, .default)  // optional; the default works
case .failure(let errors):
    for err in errors {
        print("\(err.path) (\(err.keyword)): \(err.message)")
    }
}

struct MyHost: PoppyHost {
    func onAction(_ action: Action) {
        if case .navigate(let nav) = action {
            // Route the URI however you want — Poppy never interprets it.
        }
    }
}
```

## Validation

Per [ADR-0009](../../docs/adr/0009-mobile-validation-strategy.md), decoding **is** the validator:

- `JSONDecoder` produces a typed `PoppyDocument` or throws `DecodingError`.
- A custom `init(from:)` on `Component` and `Action` switches on the wire-format `type` discriminator.
- `Poppy.validate(_:)` catches any `DecodingError` and maps it to a `ValidationError(path:, keyword:, message:)` — `keyNotFound` → `required`, `typeMismatch` → `type`, `dataCorrupted` (custom) → `discriminator`.
- A hand-written `checkVersionCompat(_:)` runs after a successful decode and rejects any document whose major doesn't match `SCHEMA_VERSION` or whose minor exceeds the supported minor.

The conformance corpus's 7 invalid cases drive the mapping; CI fails if any case doesn't produce its declared keyword.

## Theming

Per [ADR-0011](../../docs/adr/0011-ios-theming.md), themes flow through `EnvironmentValues`:

```swift
let theme = PoppyTheme(
    spacingValues: [.md: 20, .lg: 32, ...],
    colorValues: [.primary: Color.purple, ...]
)

PoppyView(document: doc, host: host)
    .environment(\.poppyTheme, theme)
```

Defaults match the web client's CSS variables exactly (`md = 16`, `primary = #0B66FF`, etc.).

## Layout

```
packages/client-ios/
├── Package.swift
├── README.md                          (this file)
└── Sources/Poppy/
    ├── Document.swift                 PoppyDocument + Component / Action enums
    ├── Tokens.swift                   Spacing / Size / PoppyColor / Weight / etc.
    ├── Validate.swift                 Poppy.validate / mapDecodingError / checkVersionCompat
    ├── Theme.swift                    PoppyTheme + EnvironmentValues plumbing
    ├── PoppyView.swift                public top-level View; PoppyHost protocol
    └── Components/
        ├── PoppyStack.swift           VStack / HStack
        ├── PoppyText.swift
        ├── PoppyImage.swift           AsyncImage
        └── PoppyButton.swift
└── Tests/PoppyTests/
    ├── CorpusLoader.swift             loads ../../conformance/cases/ via FileManager
    ├── CorpusTests.swift              every valid / invalid case asserted
    ├── BehaviorTests.swift            ADR-0008 §2 invariants
    ├── SnapshotRenderer.swift         document -> snapshot.ios.txt format
    ├── SnapshotTests.swift            committed-snapshot diff
    ├── UpdateSnapshots.swift          POPPY_UPDATE_SNAPSHOTS=1 regenerator
    └── TestHost.swift                 reusable PoppyHost stub
```

## Tests

```sh
swift test                                                # 22 tests, all green
POPPY_UPDATE_SNAPSHOTS=1 swift test --filter UpdateSnapshots   # regenerate snapshot.ios.txt
```

The conformance corpus is loaded directly from `../../packages/conformance/cases/` — not bundled. Tests fail loudly if the corpus moves or if any expected file is missing.

## See also

- [ADR-0001 — Schema-first contract](../../docs/adr/0001-schema-first-contract.md)
- [ADR-0004 — Conformance corpus](../../docs/adr/0004-conformance-corpus.md)
- [ADR-0005 — Minimize third-party dependencies](../../docs/adr/0005-minimize-third-party-dependencies.md)
- [ADR-0006 — Schema versioning](../../docs/adr/0006-schema-versioning.md)
- [ADR-0008 — Cross-platform conformance strategy](../../docs/adr/0008-cross-platform-conformance-strategy.md)
- [ADR-0009 — Mobile validation strategy](../../docs/adr/0009-mobile-validation-strategy.md)
- [ADR-0011 — iOS theming](../../docs/adr/0011-ios-theming.md)
