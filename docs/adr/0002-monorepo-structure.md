# ADR-0002: Single polyglot monorepo

## Status

Accepted — 2026-05-05

## Context

Poppy spans four target languages (TypeScript, Kotlin, Swift, plus build/web tooling) and at least nine logical packages (schema, server-ts, client-web, client-react, client-android, client-ios, conformance, creator, examples). Two structural options:

1. **Polyrepo** — one Git repository per package or per language.
2. **Monorepo** — one Git repository for everything.

The dominant pattern of change in Poppy is *cross-cutting* — adding a component touches the schema, every renderer, and the conformance corpus simultaneously. That favors monorepos. Per-language tool isolation favors polyrepos.

## Decision

Poppy is a **single Git monorepo** at `github.com/greg7gkb/poppy`. Each package uses its native build tooling (npm/pnpm, Gradle, SwiftPM); we do **not** adopt a meta-build orchestrator (Nx, Turborepo, Bazel) at this stage.

## Consequences

**Positive**

- A single PR can change the schema, every renderer, and the conformance corpus together. This is the dominant change pattern.
- One CI configuration. One issue tracker. One contributor base.
- The conformance corpus can be exercised against every renderer in a single CI run with no cross-repo coordination.
- New contributors clone one repo and have everything.

**Negative**

- The repo will eventually be larger than any individual contributor needs; sparse checkout is an option if it becomes painful.
- Per-language tooling is heterogeneous; CI must run multiple toolchains.
- We resist introducing a meta-build system. If/when that pressure becomes real, this decision can be revisited in a successor ADR.

## Alternatives Considered

- **Polyrepo (one repo per package).** Rejected: cross-cutting changes (the dominant case) become a coordination problem.
- **Monorepo with Nx or Turborepo.** Deferred: Nx and Turborepo are JS-centric and integrate awkwardly with Gradle and SwiftPM. Adopt later if the JS-side toolchain warrants it.
- **Monorepo with Bazel.** Rejected for now: high upfront complexity, steep learning curve, would slow Phase 0–2 velocity. Reconsider when CI times exceed ~15 minutes.

## References

- Why Google stores billions of lines of code in a single repository: https://research.google/pubs/why-google-stores-billions-of-lines-of-code-in-a-single-repository/
