# `@poppy/conformance`

Cross-platform test corpus and runner harness for Poppy renderers.

## Status

**Phase 1, Week 2 in progress.** Corpus structure (15 valid + 5 invalid cases) and `loadCases()` runner are in place. Per-case HTML snapshots for the Web renderer land in Week 3 alongside `@poppy/client-web`.

## What's here

```
packages/conformance/
├── cases/
│   ├── valid/                          # cases that must validate AND render
│   │   ├── 001-text-hello/
│   │   │   ├── document.json
│   │   │   ├── description.md
│   │   │   └── snapshot.web.html       # added in Week 3
│   │   └── ...
│   └── invalid/                        # cases that must FAIL validation
│       ├── 001-missing-type/
│       │   ├── document.json
│       │   ├── description.md
│       │   └── expected-error.json     # { keyword, path? }
│       └── ...
└── src/
    ├── index.ts                        # exports loadCases, normalizeHtml, types
    ├── runner.ts                       # loadCases — used by every renderer's tests
    └── normalize-html.ts               # HTML normalization (stub in Week 2; real in Week 3)
```

## Usage

```ts
import { loadCases } from "@poppy/conformance";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const corpusDir = resolve(dirname(fileURLToPath(import.meta.url)), "../../conformance");
const { valid, invalid } = loadCases(corpusDir);

for (const c of valid) {
  // run the renderer on c.document and compare to c.webSnapshot (Week 3+)
}
for (const c of invalid) {
  // expect validate(c.document) to fail with c.expectedError.keyword
}
```

## The bright-line rule

A PR that adds a component or behavior in any renderer must add at least one corpus case for it. See [`CONTRIBUTING.md`](../../CONTRIBUTING.md) and [ADR-0004](../../docs/adr/0004-conformance-corpus.md).

## See also

- [ADR-0001 — Schema-first contract](../../docs/adr/0001-schema-first-contract.md)
- [ADR-0004 — Conformance corpus](../../docs/adr/0004-conformance-corpus.md)
