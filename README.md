# Poppy

[![CI](https://github.com/greg7gkb/poppy/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/greg7gkb/poppy/actions/workflows/ci.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **Status: v0.2.0-alpha.** Working name; subject to change. Wire format will iterate.

Server-driven UI library for **Android Compose**, **iOS SwiftUI**, and **the web**.

A server emits a JSON document conforming to a versioned schema, and any Poppy renderer turns it into native UI on its platform — identically, by design.

## Why Poppy

Server-driven UI lets product teams ship UI changes without app updates and unify presentation logic across platforms. Existing solutions are either closed source (e.g. Airbnb's Ghost), tied to a single platform, or limited in scope. Poppy is open source, multi-platform from day one, and built around a single schema that all renderers must honor.

## Quickstart

The packages are not published to a registry yet (Phase 2 ships at `v0.2.0-alpha`). Clone and build from source:

```sh
git clone https://github.com/greg7gkb/poppy
cd poppy
pnpm install
pnpm -r build       # TypeScript packages
```

Mobile renderers build independently with their native toolchains. See [`CONTRIBUTING.md`](CONTRIBUTING.md#developer-prerequisites) for required toolchains.

### Server: emit and validate a document

```ts
import { validate, type PoppyDocument } from "@poppy/server-ts";

const doc: PoppyDocument = {
  version: "0.1",
  root: {
    type: "Stack",
    axis: "vertical",
    spacing: "md",
    children: [
      { type: "Text", value: "Hello, Poppy", weight: "bold" },
      {
        type: "Button",
        label: "Open profile",
        action: { type: "navigate", uri: "app://profile" },
      },
    ],
  },
};

const result = validate(doc);
if (!result.ok) throw new Error(result.errors[0].message);
res.json(result.document);
```

### Web client: render into a container

```ts
import { render } from "@poppy/client-web";
import "@poppy/client-web/styles.css";

render(await fetchDoc(), document.getElementById("app")!, {
  onAction(action) {
    if (action.type === "navigate") router.push(action.uri);
  },
});
```

See [`examples/web/`](examples/web/) for a runnable browser demo.

### Android client: render into a Composable

```kotlin
val result = Poppy.validate(jsonString)
val document = (result as ValidationResult.Ok).document   // handle Failure first

setContent {
    PoppyTheme {
        Poppy(document, host = object : PoppyHost {
            override fun onAction(action: Action) { /* route */ }
        })
    }
}
```

See [`packages/client-android/`](packages/client-android/) for full API and theming docs.

### iOS client: render into a SwiftUI view

```swift
import Poppy

switch Poppy.validate(jsonData) {
case .ok(let document):
    PoppyView(document: document, host: MyHost())
        .environment(\.poppyTheme, .default)   // optional override
case .failure(let errors):
    // surface to user
}
```

See [`packages/client-ios/`](packages/client-ios/) for full API and theming docs.

## Repository layout

This is a polyglot monorepo. Each package uses its native build tooling.

| Path | Purpose | Status |
|---|---|---|
| `packages/schema/` | JSON Schema definitions — the source of truth | v0.1 shipped |
| `packages/server-ts/` | TypeScript server library that emits valid JSON | v0.1 shipped |
| `packages/client-web/` | Native HTML renderer (reference renderer) | v0.1 shipped |
| `packages/conformance/` | Cross-platform test corpus | v0.1 shipped |
| `packages/client-android/` | Kotlin + Jetpack Compose renderer | v0.2 shipped |
| `packages/client-ios/` | Swift + SwiftUI renderer | v0.2 shipped |
| `packages/client-react/` | Optional React renderer | Phase 3 |
| `packages/creator/` | Web app for designing screens | Phase 3 |
| `examples/` | Sample apps using Poppy | per phase |
| `docs/adr/` | Architecture Decision Records | active |

## Roadmap

- **Phase 0** — Foundations, governance, ADRs. *(shipped)*
- **Phase 1** — Schema v0.1, TypeScript server library, native HTML web client, conformance corpus. *(shipped — `v0.1.0-alpha`)*
- **Phase 2** — Android Compose + iOS SwiftUI clients. *(shipped — `v0.2.0-alpha`)*
- **Phase 3** — Optional React renderer + Creator web app.
- **Phase 4** — Schema iteration: theming primitives, slots, async data, accessibility.
- **Phase 5 (optional)** — Design system layer: tokens, variants, themes.

See [`docs/phase-1-plan.md`](docs/phase-1-plan.md) and [`docs/phase-2-plan.md`](docs/phase-2-plan.md) for the per-phase specifications, and [`CHANGELOG.md`](CHANGELOG.md) for the release log.

## Architecture, in two sentences

A single JSON Schema is the source of truth for what a Poppy document can contain. A shared **conformance corpus** of `(input JSON → expected render)` pairs is run against every renderer on every PR — and **no component lands in any renderer until it's in the schema and the corpus**.

## Architecture decision records

- [ADR-0001 — Schema-first contract](docs/adr/0001-schema-first-contract.md)
- [ADR-0002 — Monorepo structure](docs/adr/0002-monorepo-structure.md)
- [ADR-0003 — Apache 2.0 license](docs/adr/0003-apache-2-license.md)
- [ADR-0004 — Conformance corpus](docs/adr/0004-conformance-corpus.md)
- [ADR-0005 — Minimize third-party dependencies](docs/adr/0005-minimize-third-party-dependencies.md)
- [ADR-0006 — Schema versioning](docs/adr/0006-schema-versioning.md)
- [ADR-0007 — Ajv standalone precompile](docs/adr/0007-ajv-standalone-precompile.md)
- [ADR-0008 — Cross-platform conformance strategy](docs/adr/0008-cross-platform-conformance-strategy.md)
- [ADR-0009 — Mobile renderer validation strategy](docs/adr/0009-mobile-validation-strategy.md)
- [ADR-0010 — Android theming](docs/adr/0010-android-theming.md)
- [ADR-0011 — iOS theming](docs/adr/0011-ios-theming.md)

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Poppy is in early development and the API surface is not yet stable; expect breaking changes between alpha releases.

## Security

See [`SECURITY.md`](SECURITY.md) for how to report vulnerabilities.

## License

Apache License 2.0 — see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
