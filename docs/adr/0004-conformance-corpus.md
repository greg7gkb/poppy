# ADR-0004: Conformance corpus and the bright-line component rule

## Status

Accepted — 2026-05-05

## Context

The hardest problem in a multi-platform SDUI library is keeping the renderers in sync. Per-renderer type checks and lints can verify that each renderer is internally consistent, but they cannot verify that two renderers produce equivalent output for the same input.

Industry experience with multi-platform UI — shared design systems, hybrid apps, Adaptive Cards — consistently shows that **without a shared executable specification of behavior, platforms diverge silently**, and divergences are usually noticed only by users in production.

## Decision

Poppy maintains a **conformance corpus** at [`packages/conformance/`](../../packages/conformance/): a curated set of `(input JSON document, expected output snapshots per renderer)` pairs.

- **Web renderer:** outputs HTML; the snapshot is the serialized HTML tree.
- **Android Compose renderer:** outputs a Compose semantics tree; the snapshot is its serialized form.
- **iOS SwiftUI renderer:** outputs a view-inspector tree; the snapshot is its serialized form.
- **React renderer (Phase 3):** outputs HTML; the snapshot is the serialized HTML tree.

CI runs the entire corpus against every renderer on every PR. Snapshot mismatches fail the build.

### The bright-line rule

A pull request that adds or meaningfully changes a component or action in **any** renderer must also:

1. Add or update the schema entry in `packages/schema/`.
2. Add at least one entry to `packages/conformance/` exercising the new behavior.

A PR that adds renderer-only behavior — even behavior that "obviously" matches the existing schema — is rejected. The corpus is the proof.

## Consequences

**Positive**

- Renderer drift becomes a CI failure, not a production bug.
- A new renderer can be added by simply running the corpus and fixing failures.
- The corpus doubles as documentation: it shows exactly what every component is supposed to render.
- The bright-line rule front-loads design discussion to schema PRs, where it belongs.

**Negative**

- Adds friction to PRs that add components: contributors must also write schema and corpus entries. This is intentional.
- Snapshot tests are sensitive to incidental differences (whitespace, attribute order). Renderers must produce normalized output; we will need normalization tooling per language.
- Maintaining the corpus across major schema versions is non-trivial. We accept this cost.

## Alternatives Considered

- **Per-renderer unit tests only.** Rejected: cannot detect inter-renderer divergence.
- **Visual regression / pixel-diff tests as the primary mechanism.** Rejected: too noisy across platforms with different fonts, anti-aliasing, and pixel ratios. A future ADR may add visual tests as a complement.
- **A reference renderer that other renderers must imitate, without an explicit corpus.** Rejected: implicit specs are still implicit. We want the spec to be data, not code.

## References

- Microsoft Adaptive Cards explorer (a public example of the corpus pattern): https://adaptivecards.io/explorer/
