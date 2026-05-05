# `@poppy/conformance`

Cross-platform test corpus: `(input JSON document, expected output snapshot per renderer)` pairs that every renderer must pass.

## Status

Established in **Phase 1**, ongoing thereafter. Currently a placeholder.

## Layout (when populated)

- `cases/` — one directory per case, each with:
  - `input.json` — a Poppy document conforming to `@poppy/schema`.
  - `expected.web.html` — expected HTML serialization from `@poppy/client-web`.
  - `expected.android.txt` — expected Compose semantics-tree serialization.
  - `expected.ios.txt` — expected SwiftUI view-tree serialization.
  - `notes.md` — what this case is testing, and why.

CI runs the corpus against every renderer on every PR.

## The bright-line rule

A PR that adds a component or behavior in any renderer must add at least one corpus case for it. See [`CONTRIBUTING.md`](../../CONTRIBUTING.md) and [ADR-0004](../../docs/adr/0004-conformance-corpus.md).
