# ADR-0008: Cross-platform conformance strategy

## Status

Accepted — 2026-05-09

## Context

[ADR-0004](0004-conformance-corpus.md) introduced the conformance corpus and the bright-line component rule, and sketched a snapshot-per-renderer approach: web outputs HTML, Compose outputs a semantics tree, SwiftUI outputs an inspector tree, etc. That sketch was correct in spirit but glossed over a load-bearing question: **what does it mean for two renderers to "agree"?**

Phase 1 shipped only the web renderer, so the question was deferrable. Phase 2 introduces Compose and SwiftUI, which produce native widget trees — they have no representation in common with HTML or with each other. A snapshot of one cannot be diffed against a snapshot of another. We need to be explicit about what conformance means in practice before mobile work starts.

The question surfaced concretely when describing `snapshot.web.html` as "the cross-platform contract Phase 2 renderers must reproduce." That overstates what the file does. `snapshot.web.html` is a serialized HTML tree — useful for catching web-renderer regressions, useless as a target for an Android view hierarchy. Pretending otherwise would invite bugs the corpus appears to prevent but does not.

## Decision

Cross-platform conformance is enforced through three mechanisms, ranked by automation:

1. **The schema (fully automated, identical across platforms).** Every renderer validates input documents against the same JSON Schema and emits the same error shape — including the [ADR-0006](0006-schema-versioning.md) version-compat error. Validation parity is a CI gate, not a per-platform concern. This catches the largest class of divergence: "platform X accepted a document platform Y rejected" is impossible by construction.

2. **Behavioral assertions over shared corpus inputs (automated per platform).** Every renderer's test suite consumes `@poppy/conformance` via `loadCases()` and runs platform-native assertions over the same input documents. The assertions encode invariants that all platforms must honor:
   - Button activation dispatches the action verbatim to the host (`onAction(action)` on web; `OnClickListener`/`onTapGesture` equivalents on mobile).
   - Stack with `axis: "vertical"` arranges children top-to-bottom; `axis: "horizontal"` arranges left-to-right (LTR locales).
   - `Image.alt` is exposed to the platform's accessibility tree.
   - Color tokens resolve to the host's themed value (queried by token name, not by RGB).
   - Spacing/padding tokens map to the platform's default scale.

   These assertions are cross-platform in **specification** but platform-native in **implementation** — Compose tests use `SemanticsNodeInteraction`, SwiftUI tests use `ViewInspector`, web tests use the DOM API. The shared corpus inputs ensure they exercise the same surface area.

3. **Per-platform snapshots reviewed in PRs (manual cross-platform check).** Each renderer adds its own snapshot kind to each corpus case:

   ```
   cases/valid/015-kitchen-sink/
     document.json              # shared input
     description.md             # shared
     snapshot.web.html          # @poppy/client-web
     snapshot.android.txt       # Compose: SemanticsNodeInteraction.printToString()
     snapshot.ios.txt           # SwiftUI: ViewInspector tree dump
   ```

   Snapshot diffs are reviewed line-by-line. When a PR adds or changes a component, all platform snapshots land in the same PR, and the reviewer's job includes checking that the three files describe semantically equivalent UI. "Just regenerated the snapshots" remains an unacceptable PR description (per ADR-0004).

   This is **the manual layer.** We accept that human review catches things automation cannot, in exchange for not building a platform-agnostic intermediate representation we don't yet need.

### What this is not

- **Not pixel-diffing across platforms.** Different fonts, anti-aliasing, default insets, and pixel ratios make pixel comparison across platforms infeasible. Pixel/screenshot tests *within* a platform (Compose Paparazzi, SwiftUI snapshot tests) are useful for catching same-platform regressions across releases and may be added later — they do not constitute cross-platform conformance.

- **Not a platform-agnostic semantic IR.** Defining a `snapshot.semantic.json` that all renderers serialize to, and diffing those, is tempting but premature. It requires every renderer to maintain a serializer that stays in lockstep with the schema, doubles the per-component PR cost, and solves a problem we have not yet felt. Reconsider when manual review starts missing real divergences.

- **Not an enforced cross-platform behavioral test runner.** Each platform implements its own behavioral tests in its own language. We don't build a meta-runner that asserts "the same behavior expectation passed on all three platforms" — instead, the **assertion list is documented per case** in `description.md` so platform implementers know what invariants they must encode.

## Consequences

**Positive**

- Honest about the contract. Contributors don't expect automation that doesn't exist, and reviewers know their job includes cross-platform sanity-check on every component PR.
- Each platform uses idiomatic snapshot tools (`SemanticsNodeInteraction`, `ViewInspector`, normalized HTML) — none has to bend to a foreign abstraction.
- The shared corpus inputs (`document.json`) and the schema cover ~80% of conformance automatically. The remaining 20% — visual layout, accessibility wiring, action dispatch — is exactly where automated cross-platform tools struggle even in mature codebases.
- The bright-line rule (ADR-0004) extends naturally: a new component requires schema entry + corpus case + per-platform snapshots. Reviewers see all of them in one PR.

**Negative**

- Cross-platform divergence in the visual/structural layer is caught by humans, not CI. A reviewer who skims the three snapshots can miss a real bug. We mitigate by requiring snapshot diffs to be reviewed as carefully as code diffs.
- Maintenance cost scales with platforms. Adding a fourth renderer (e.g. React) means generating a fourth snapshot per case. Acceptable for the small Phase 1 corpus; we may revisit if the corpus grows past ~100 cases.
- Behavioral assertions are duplicated per platform. The assertion list lives in `description.md` and in three test files. A spec change touches all four. We accept this as the cost of platform-native test code.

## Implementation, Phase 2 onward

Per-platform snapshot file names:

| Platform | File | Generator |
|---|---|---|
| Web | `snapshot.web.html` | `@poppy/client-web` `update-snapshots.ts` (exists) |
| Android Compose | `snapshot.android.txt` | Compose test that calls `printToString()` on the SemanticsNode tree |
| iOS SwiftUI | `snapshot.ios.txt` | SwiftUI test using `ViewInspector` tree dump |
| React (Phase 3) | covered by `snapshot.web.html` | React renderer outputs HTML; same normalizer applies |

Each renderer's package adds an `update-snapshots` script that mirrors the web one: load the corpus, render every valid case, write the snapshot back into the case directory. CI runs the test suite (which compares against committed snapshots) but **not** the update script — snapshot regeneration is a deliberate developer act.

`description.md` for each case documents the behavioral invariants the case is expected to test, so platform authors know what to assert beyond the snapshot. This format is informal in v0.1; it can be promoted to structured YAML in a later ADR if the count of invariants per case grows past what's readable as prose.

## Alternatives Considered

- **Platform-agnostic semantic IR (`snapshot.semantic.json`).** Define a normalized tree format that captures intent (`{ type: "stack", axis: "vertical", spacing: "md", children: [...] }`) and have every renderer serialize to it. Pros: automated cross-platform diffs become possible. Cons: every renderer maintains a serializer; the IR has to evolve in lockstep with the schema; the IR itself becomes a contract that can rot. **Rejected for now**, defer to Phase 3+ if review-by-eyeball misses real divergences.

- **Pixel screenshots as the cross-platform contract.** Generate PNG screenshots on each platform and compare. Pros: catches actual visual divergence. Cons: noisy across font rendering, anti-aliasing, retina/non-retina, default OS chrome, light/dark mode. False positives would dominate. **Rejected as primary mechanism**; may be added per-platform for same-platform regression catching.

- **One reference renderer; others must match its output.** The web renderer is "truth", and Android/iOS tests assert their output produces a tree that round-trips through some adapter back to the web HTML. **Rejected**: it forces mobile UI design to follow what HTML can express, which would penalize platform-native idioms (e.g. SwiftUI gestures, Compose semantic actions).

- **No per-platform snapshots, only behavioral assertions.** Skip the snapshot files, rely entirely on assertion-style tests. Pros: less ceremony. Cons: snapshots catch unintended structural changes that no specific assertion is watching for. The Phase 1 web corpus has already caught accidental class-name changes in client-web that no behavioral test would have flagged. **Rejected**: snapshots are cheap insurance.

## References

- [ADR-0001 — Schema-first contract](0001-schema-first-contract.md)
- [ADR-0004 — Conformance corpus and the bright-line component rule](0004-conformance-corpus.md) — supersedes 0004's "snapshot per renderer" sketch with this concrete strategy.
- [ADR-0006 — Schema versioning](0006-schema-versioning.md)
- Microsoft Adaptive Cards uses a similar pattern: shared JSON test cases, per-platform tests, no automated cross-platform structural diff. https://adaptivecards.io/explorer/
- [SemanticsNodeInteraction.printToString()](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/SemanticsNodeInteraction#printToString) — Compose
- [ViewInspector](https://github.com/nalexn/ViewInspector) — SwiftUI runtime tree introspection
