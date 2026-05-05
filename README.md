# Poppy

> **Status: alpha — pre-v0.1.** Working name; subject to change. No stable API yet.

Server-driven UI library for **Android Compose**, **iOS SwiftUI**, and **the web**.

A server emits a JSON document conforming to a versioned schema, and any Poppy renderer turns it into native UI on its platform — identically, by design.

## Why Poppy

Server-driven UI lets product teams ship UI changes without app updates and unify presentation logic across platforms. Existing solutions are either closed source (e.g. Airbnb's Ghost), tied to a single platform, or limited in scope. Poppy is open source, multi-platform from day one, and built around a single schema that all renderers must honor.

## Repository layout

This is a polyglot monorepo. Each package uses its native build tooling.

| Path | Purpose | Status |
|---|---|---|
| `packages/schema/` | JSON Schema definitions — the source of truth | Planned (Phase 1) |
| `packages/server-ts/` | TypeScript server library that emits valid JSON | Planned (Phase 1) |
| `packages/client-web/` | Native HTML renderer (reference renderer) | Planned (Phase 1) |
| `packages/client-react/` | Optional React renderer | Planned (Phase 3) |
| `packages/client-android/` | Kotlin + Jetpack Compose renderer | Planned (Phase 2) |
| `packages/client-ios/` | Swift + SwiftUI renderer | Planned (Phase 2) |
| `packages/conformance/` | Cross-platform test corpus | Planned (Phase 1) |
| `packages/creator/` | Web app for designing screens | Planned (Phase 3) |
| `examples/` | Sample apps using Poppy | Planned (per phase) |
| `docs/adr/` | Architecture Decision Records | Active |

## Roadmap

- **Phase 0 (current):** Foundations, governance, ADRs.
- **Phase 1:** Schema v0.1, TypeScript server library, native HTML web client, conformance corpus.
- **Phase 2:** Android Compose + iOS SwiftUI clients (parallelizable).
- **Phase 3:** Optional React renderer + Creator web app.
- **Phase 4:** Schema iteration — theming primitives, slots, async data, accessibility.
- **Phase 5 (optional):** Design system layer — tokens, variants, themes.

See [`docs/bootstrap-plan.md`](docs/bootstrap-plan.md) for the full bootstrap plan.

## Architecture, in two sentences

A single JSON Schema is the source of truth for what a Poppy document can contain. A shared **conformance corpus** of `(input JSON → expected render)` pairs is run against every renderer on every PR — and **no component lands in any renderer until it's in the schema and the corpus**.

## Architecture decision records

- [ADR-0001 — Schema-first contract](docs/adr/0001-schema-first-contract.md)
- [ADR-0002 — Monorepo structure](docs/adr/0002-monorepo-structure.md)
- [ADR-0003 — Apache 2.0 license](docs/adr/0003-apache-2-license.md)
- [ADR-0004 — Conformance corpus](docs/adr/0004-conformance-corpus.md)
- [ADR-0005 — Minimize third-party dependencies](docs/adr/0005-minimize-third-party-dependencies.md)

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Poppy is in early development and the API surface is not yet stable; expect breaking changes between every alpha release.

## Security

See [`SECURITY.md`](SECURITY.md) for how to report vulnerabilities.

## License

Apache License 2.0 — see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
