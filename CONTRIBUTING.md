# Contributing to Poppy

Thanks for your interest in contributing. Poppy is in early development (pre-v0.1). The API will change frequently — expect that.

## Code of conduct

This project follows the [Contributor Covenant 2.1](CODE_OF_CONDUCT.md). By participating, you agree to uphold it.

## Two bright-line rules

These two rules are non-negotiable. They exist to keep Poppy's renderers in sync and its dependency surface small.

### 1. No new component without schema + corpus

If a pull request introduces a new component, action, or meaningful new behavior in **any** renderer, that PR must also:

1. Define or update the relevant entry in [`packages/schema/`](packages/schema/).
2. Add at least one entry to the [conformance corpus](packages/conformance/) covering the new behavior.

CI runs the corpus against every renderer; renderers that lag behind the schema are explicitly broken builds. See [ADR-0004](docs/adr/0004-conformance-corpus.md) for the rationale.

### 2. No new third-party runtime dependency without discussion

If a pull request adds a new third-party runtime dependency in any package, open a discussion first (issue or draft PR). The bar is high — Poppy aims for a small, auditable surface across four languages, and every dependency is a long-term maintenance commitment. See [ADR-0005](docs/adr/0005-minimize-third-party-dependencies.md) for criteria.

Dev-only dependencies (linters, formatters, test runners) are less constrained but still warrant a brief justification in the PR description.

## Developer prerequisites

Poppy is a polyglot monorepo. You only need the toolchains for the packages you plan to touch.

| Package | Toolchain | Verify with |
|---|---|---|
| `@poppy/*` (TypeScript) | Node.js 20+, pnpm 10+ | `node -v && pnpm -v` |
| `packages/client-android/` | JDK 17+, Android SDK with platform 35 (API 35), Android Studio Iguana or newer (recommended) | `java -version` (look for `17`+); `./gradlew` works inside the package |
| `packages/client-ios/` | Xcode 16+ on macOS with Swift 5.10+ | `xcodebuild -version && swift --version` |

To run the full test suite locally before opening a PR, you'll need all three. CI runs them in isolated jobs, so a missing toolchain locally won't block your PR — it just means you'll discover failures in CI rather than on your machine.

## Workflow

1. Fork the repository and clone your fork.
2. Create a feature branch off `main`.
3. Make your changes. Each package's README documents its own build and test commands.
4. Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) in commit messages — this drives our changelog.
5. Open a pull request against `main`. Fill in the PR template.
6. Pass CI.

## Architecture decisions

For meaningful architectural changes, write or update an ADR in [`docs/adr/`](docs/adr/) using the [template](docs/adr/0000-template.md). ADRs are short, focused, and immutable once accepted (later decisions supersede them).

## Reporting bugs and proposing features

Use the issue templates in [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/).

## Reporting security vulnerabilities

See [`SECURITY.md`](SECURITY.md). Do **not** report security issues in public issues.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
