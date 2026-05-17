# Poppy — Phase 3 Implementation Plan

## Context

Phase 1 (`v0.1.0-alpha`) shipped the wire format, the TypeScript server lib, the native HTML web renderer, and the conformance corpus. Phase 2 (`v0.2.0-alpha`, 2026-05-09) brought the same corpus to Android Compose and iOS SwiftUI. Schema v0.1 has now been ratified across three independent renderers with no wire-format change required.

Phase 3 closes out the renderer matrix and adds the first authoring tool, split into two sub-phases:

- **Phase 3.1 — `@poppy/client-react`**: a React-idiomatic renderer (hooks, TSX, error boundary) that passes the same conformance corpus. Includes `examples/react/`.
- **Phase 3.2 — `@poppy/creator`**: a browser-based authoring tool that produces Poppy documents and previews them live via `@poppy/client-react`.

Phase 3.1 ships and lands fully before Phase 3.2 starts — one domain at a time. Both sub-phases roll up into a single `v0.3.0-alpha` tag at the end.

The schema and corpus remain **frozen at v0.1** for the duration of Phase 3. Any gap the renderer or Creator surfaces is logged for Phase 4 deliberation; we do not bend the wire format mid-phase. This is the same discipline that protected Phase 2.

This plan is grounded in:

- **[ADR-0001](adr/0001-schema-first-contract.md)** — schema-first contract. Phase 3 adds no new schema authority.
- **[ADR-0004](adr/0004-conformance-corpus.md)** — bright-line rule. Phase 3 ships no component the corpus doesn't already cover.
- **[ADR-0005](adr/0005-minimize-third-party-dependencies.md)** — minimize 3p deps; each addition surfaced and justified inline.
- **[ADR-0008](adr/0008-cross-platform-conformance-strategy.md)** — three-layer conformance (schema, behavioral, snapshot). Phase 3 extends the snapshot layer with React's HTML output and verifies it normalizes to byte-identical `snapshot.web.html`.

The two largest risks: (1) the React renderer drifts from `@poppy/client-web` and the corpus catches it only after divergence; (2) the Creator's UX scope balloons because authoring tools are inherently fractal. Mitigation for (1) is **mutually-normalized HTML comparison** with the web client — both renderers' output runs through the existing `normalize-html` and we assert equality; see "Snapshot strategy" below for the realistic caveats. Mitigation for (2) is the tightly scoped Creator deliverable list below: tree-and-inspector editor only, no canvas/drag-drop, no persistence beyond import/export.

## Goals

By end of Phase 3:

**Phase 3.1 — React renderer:**

- `@poppy/client-react@0.3.0-alpha.0` — React 19 component + hook, error boundary, peer-dep on React. Passes all 22 corpus cases.
- **Mutually-normalized HTML comparison test** asserting that React's output normalizes to the same shape as the web renderer's output for every valid case, and matches the committed `snapshot.web.html` (modulo any documented React-specific normalizer extension — see "Snapshot strategy").
- **React-specific behavioral tests** beyond the cross-platform corpus checks (see "React-specific test surface" below): StrictMode double-invocation safety, unmount cleanup, prop-change re-render stability, error boundary catches render-time exceptions.
- `examples/react/` — minimal React app that mirrors the existing `examples/web/` demo using `@poppy/client-react`. Completes before Phase 3.2 starts.
- ADR-0012 (React renderer strategy) drafted and accepted.

**Phase 3.2 — Creator:**

- `@poppy/creator@0.3.0-alpha.0` — Vite-built React app: tree view + property inspector + live preview + JSON import/export. Runs locally via `pnpm dev`. **Stateless** — no backend, no `localStorage`, no autosave. State exists only in-memory for the session; users export to a file to persist.
- **Creator test surface** (Vitest + `@testing-library/react`):
  - Reducer unit tests for every action (add child, remove node, update field, import JSON, reset).
  - Round-trip integrity: load each of the 15 valid corpus documents, serialize back, assert deep-equal to original.
  - Schema-driven inspector: for each Phase 1 component, assert the inspector renders the expected fields with correct enum options.
  - Validation feedback: import a malformed document (each invalid corpus case), assert the inspector / status bar surface the declared keyword + path.
  - Component tests for tree pane (selection updates inspector) and toolbar (import modal accepts JSON, export modal produces JSON).
- ADR-0013 (Creator architecture) drafted and accepted.

**Both:**

- CI runs the new packages via the existing `js` job — no workflow change unless run-time exceeds ~3 min, in which case the job is split.
- **Schema `$id` URLs switched from `greg7gkb`-pinned GitHub raw URLs to URN-style identifiers** (e.g. `urn:poppy:v0.1:schema:component:stack`). One-PR change to `packages/schema/schemas/*.json` + `@poppy/server-ts` references. Must land before `v0.3.0-alpha` tag. See Phase 3 backlog below.
- `git tag v0.3.0-alpha` cut after green CI at end of Phase 3.2.

## Non-goals (Phase 3)

- **No schema changes.** v0.1 stays frozen. Any pressure to add components (lists, modals, slots, async data, forms, theming primitives) is logged in `docs/phase-4-backlog.md` for Phase 4 deliberation.
- **No React Server Components / RSC support.** `@poppy/client-react` is a `'use client'`-friendly client-only renderer for v0.3. RSC compatibility is a Phase 4+ consideration once the schema gains async-data semantics.
- **No React Native renderer.** Indefinitely deferred. `@poppy/client-react` targets React-DOM only; Phase 2's Compose/SwiftUI cover native mobile.
- **No canvas / drag-drop authoring in Creator.** Tree view + property inspector is the v0.3 interaction model. Canvas-based positioning is a Phase 4+ exploration.
- **No persistence in Creator — fully stateless across reloads.** No `localStorage`, no backend, no autosave. State exists only in-memory; export-to-file is the only way to keep work. This is a deliberate simplification for v0.3; a "restore last session" feature is a Phase 4+ consideration if user feedback motivates it.
- **No Creator hosting / deployment.** Creator runs via `pnpm dev` for v0.3. Static hosting (GitHub Pages, Netlify) is a separate decision after the UX stabilizes.
- **No collaborative editing in Creator.** Single-user, single-tab.
- **No NPM publishing.** Like Phases 1 and 2, Phase 3 ends at a git tag. Distribution is a separate decision.
- **No new conformance corpus cases.** The 15 valid + 7 invalid cases remain the contract. If Phase 3 surfaces an ambiguity that needs a regression entry, it is added to all four renderers (web, React, Android, iOS) in the same PR per the bright-line rule.

## Open dep additions

Three new runtime / dev deps are introduced. Each surfaced per [ADR-0005](adr/0005-minimize-third-party-dependencies.md):

| Dep | Package | Used for | Why this and not something else |
|---|---|---|---|
| `react`, `react-dom` (peer) | `@poppy/client-react`, `@poppy/creator` | The React runtime. | Peer dep on the renderer (the host owns React's version). Direct dep on Creator (an end-user app pinning its own React). React 19 targeted — current as of 2026; concurrent rendering is stable. |
| `vite` (devDep) | `@poppy/creator` | Build/dev server for the Creator web app. | The de-facto React app build tool in 2026, Apache-style permissive license, well-maintained. Alternative: webpack — slower dev loop, heavier config. Parcel — fine but smaller community for React-app scaffolding. Vite wins on dev iteration speed and minimal config. |
| `@vitejs/plugin-react` (devDep) | `@poppy/creator` | React JSX + Fast Refresh in Vite. | First-party Vite plugin, no real alternative for the official Vite-React combination. |

**Not added:**
- No automatic-JSX-runtime polyfill — React 19's automatic runtime needs no extra dep.
- **No state-management library (Redux / Zustand / Jotai).** Creator's state surface in v0.3 is small (document tree, selected-node ID, validation result) — `useReducer` + Context handles it cleanly. Redux Toolkit's value (DevTools time-travel, undo/redo, middleware, async-action ergonomics) becomes load-bearing in Phase 4 when undo/redo, multi-document tabs, or async data land. To keep migration mechanical, the reducer is written in a `createSlice`-shaped style: pure transitions keyed by string action types, no closure-captured side effects. Swapping to Redux Toolkit later is a wrap-in-`createSlice` change, not a rewrite. **If you want Redux in Phase 3.2 anyway, flag it before Week 3** — the rest of the plan is unaffected.
- No drag-and-drop library — Creator's v0.3 is tree + inspector, no DnD.
- No code-editor library (Monaco / CodeMirror) — the JSON import/export panel is a plain `<textarea>` for v0.3. A real code editor is a Phase 4 consideration once the textarea proves limiting.
- No Playwright / Cypress — Creator's E2E surface is small enough for `@testing-library/react` + Vitest. Cross-browser E2E is deferred.
- `@testing-library/react` and `jsdom` are already transitively available (`jsdom` is a devDep on `@poppy/client-web`); we add `@testing-library/react` once.

## `@poppy/client-react` (Phase 3.1)

### Public API

```tsx
import { Poppy, usePoppyDocument, PoppyErrorBoundary } from '@poppy/client-react';
import type { PoppyDocument, PoppyHost, Action } from '@poppy/client-react';

// Component-form: validate-on-render. Errors flow to host.onError.
<Poppy
  document={docOrJson}
  host={{ onAction: (a) => router.navigate(a.uri) }}
/>

// Hook-form: validate ahead of render; consumer chooses what to render on failure.
const result = usePoppyDocument(json); // { ok: true, document } | { ok: false, errors }
if (!result.ok) return <ErrorList errors={result.errors} />;
return <Poppy document={result.document} host={host} />;

// Optional error boundary for unexpected render-time errors.
<PoppyErrorBoundary fallback={<Fallback />}>
  <Poppy document={doc} host={host} />
</PoppyErrorBoundary>
```

- `document` accepts either a validated `PoppyDocument` (typed) or a raw JSON string / parsed object (validated internally). Mirrors the ergonomics of `@poppy/client-web`'s `render()` which also validates by default.
- `host` is the same `PoppyHost` shape as the web client (`onAction`, optional `onError`, optional `isUrlAllowed`). Types are re-exported from `@poppy/server-ts`.
- `usePoppyDocument(json)` memoizes the validation result on the JSON identity, so re-renders with the same document don't re-validate.
- `PoppyErrorBoundary` is a class component (React still requires class for error boundaries in 2026) with a small footprint.

### Implementation strategy — fresh TSX, not wrap-the-DOM-renderer

Two paths considered:

**A. Wrap `@poppy/client-web`'s imperative `render()`.** The React component creates a `<div>` ref, calls `render(doc, ref.current, host)` in a `useEffect`, and calls `destroy()` on unmount.

- *Pros:* Single source of rendering logic; React renderer can't diverge from web renderer because it IS the web renderer.
- *Cons:* The DOM under the wrapper is opaque to React — no React tree inspection, no React DevTools introspection, no portals, no event-handler ergonomics. Feels foreign to React adopters. Defeats the point of a React-idiomatic package.

**B. Fresh TSX implementation that produces the same HTML.** Each component (`Stack`, `Text`, `Image`, `Button`) is a small TSX function. CSS imports `@poppy/client-web/styles.css` for visual identity. Conformance is verified by **rendering React's output into a detached DOM, normalizing the HTML through `@poppy/conformance`'s `normalize-html`, and asserting equality with the same normalizer applied to the web renderer's output** (see Snapshot strategy below).

- *Pros:* Native React tree (DevTools, hooks, portals, event handler scoping, suspense-ready); each component is ~10 lines; the corpus (not the renderer) is the contract.
- *Cons:* Logic duplicated between renderers. Mitigation: token resolution / URL allowlist / validation all live in `@poppy/server-ts` and `@poppy/schema` — only the TSX mapping is duplicated, and that's small.

**Going with B.** The bright-line argument: the corpus's `snapshot.web.html` *is* the contract. Any rendering technology that produces equivalent normalized HTML satisfies it. Hosting the React renderer in TSX rather than as an `innerHTML` shim respects how React users actually integrate UI libraries. ADR-0012 documents this decision.

### Validation

Identical strategy to `@poppy/client-web`: `@poppy/server-ts`'s `validate()` is the authority. `Poppy` component calls `validate()` when `document` is a raw input; with a typed `PoppyDocument` it skips validation. `usePoppyDocument` is the hook form. Validation errors flow to `host.onError(...)` (component form) or are returned as the `failure` arm of the discriminated result (hook form).

The version-compat check is unchanged — `@poppy/server-ts` owns it; React renderer trusts the validated document.

### URL safety

The `<img>` URL allowlist (per ADR-0001 spirit and `@poppy/client-web`'s implementation) is re-applied. `host.isUrlAllowed` defaults to the same predicate the web client uses. Re-exported from `@poppy/server-ts` so the predicate is shared, not duplicated.

### Snapshot strategy

**Goal:** no new snapshot file. The committed `snapshot.web.html` stays the canonical record.

**Test mechanic:** for each valid corpus case the test (a) renders through React into a detached jsdom container, (b) renders through `@poppy/client-web` into another detached container, (c) normalizes both via `@poppy/conformance`'s existing `normalize-html`, (d) asserts the two normalized strings are equal, (e) also asserts the normalized React output equals the committed `snapshot.web.html` (catches drift in either direction). This mutually-normalized comparison is symmetric — if React introduces a quirk, we know it's React-specific; if the web client drifts, we catch that too.

**Realistic risks, with intended mitigations:**

| Risk | Likelihood | Mitigation |
|---|---|---|
| React 19 hydration / data-attribute artifacts | Low for client-only render with no Suspense boundaries | Use `react-dom`'s `renderToStaticMarkup` for the test path — strips hydration markers. |
| TSX whitespace differences (text nodes between elements) | Medium — biggest realistic source of diff | Compose children explicitly via `{children}` only; no inline literal whitespace. Normalizer's existing whitespace handling absorbs the rest. |
| Boolean-attribute serialization (`disabled` with vs without `=""`) | Low — DOM serialization is identical post-`setAttribute` | Normalizer already collapses. If not, extend `normalize-html` to canonicalize. |
| `style` attribute formatting (object → string) | Low — Phase 1 schema has no inline styles | n/a in v0.1; revisit if Phase 4 introduces style props. |
| Self-closing tag rules (`<img />` vs `<img>`) | None — HTML spec collapses both | Normalizer already canonicalizes via jsdom round-trip. |

**Escape hatch:** if React produces an artifact we can't strip cleanly (low likelihood, but possible), the fallback is committing `snapshot.react.html` alongside `snapshot.web.html` — same contract surface as Phase 2's split between `snapshot.android.txt` and `snapshot.ios.txt`. Decision: only fall back if forced; document in ADR-0012 either way.

So "byte-identical" is the *target*, not a load-bearing claim. The realistic position is: byte-identical after a normalizer that already exists, with two small extensions if Week 2 turns up specific React-induced artifacts.

### React-specific test surface

Beyond the cross-platform corpus + behavioral assertions (ADR-0008 §2 invariants), the React renderer needs tests that exercise *React-specific* concerns the web client doesn't have:

- **StrictMode double-invocation safety:** wrap `<Poppy>` in `<React.StrictMode>`; assert `host.onAction` fires exactly once per click. (React strict mode double-invokes render and effect setup in dev to surface bugs; our renderer must be idempotent.)
- **Unmount cleanup:** mount `<Poppy>`, click a Button, unmount, click again — assert no late `host.onAction` fires and no console errors about updates on unmounted components.
- **Prop-change re-render stability:** mount with document A, change to document B with one differing leaf, assert React reconciles in place (selected DOM nodes preserved) and no full unmount/remount of unchanged subtrees.
- **Identical-prop stability:** re-render with the same `document` reference; assert `validate()` is called at most once (memoization works).
- **Error boundary catches render-time exceptions:** wrap a `<Poppy>` whose document somehow triggers a render-time throw (constructed by mocking a component) in `<PoppyErrorBoundary>`; assert the fallback renders.
- **Hook ergonomics:** `usePoppyDocument(json)` returns stable references when input JSON string is identical (referential equality on the result object), so consumers can `useMemo` downstream off it.
- **Host validation flow:** `<Poppy document="invalid json">` calls `host.onError` with a structured `ValidationError[]`, not a thrown exception that crashes React.

These live in `packages/client-react/test/react-specific.test.tsx`, separate from `corpus.test.tsx` and `behavior.test.tsx`.

### Build

- pnpm workspace package: `packages/client-react/`
- Bundler: `tsup` (same as `@poppy/client-web` — consistent toolchain)
- Output: ESM only, with `dist/index.d.ts` types
- Peer deps: `react ^19`, `react-dom ^19`
- Direct deps: `@poppy/server-ts` (validation), `@poppy/schema` (types — already inlined by `tsup noExternal`)

### Layout

```
packages/client-react/
├── package.json
├── tsconfig.json
├── tsup.config.ts
├── vitest.config.ts
├── README.md
├── src/
│   ├── index.ts                  # public exports
│   ├── Poppy.tsx                 # the <Poppy> component
│   ├── usePoppyDocument.ts       # validation hook
│   ├── PoppyErrorBoundary.tsx    # error boundary class
│   └── components/
│       ├── PoppyStack.tsx
│       ├── PoppyText.tsx
│       ├── PoppyImage.tsx
│       └── PoppyButton.tsx
└── test/
    ├── corpus.test.tsx              # iterates valid + invalid corpus
    ├── behavior.test.tsx            # ADR-0008 §2 invariants (action dispatch, etc.)
    ├── render-matches-web.test.tsx  # mutually-normalized HTML equivalence
    └── react-specific.test.tsx     # StrictMode, unmount, prop-change, error boundary, hooks
```

## `@poppy/creator` (Phase 3.2)

### Architecture

Vite + React 19 single-page app. Everything client-side. Build output is a static bundle servable from any host. No SSR. **Stateless across reloads** — no `localStorage`, no backend, no autosave. State lives in `useReducer` + Context for the session only. Users export-to-file to persist work; reloading the page is a clean slate.

State lives in `useReducer` + Context — no external state library (see "Open dep additions" for the Redux deferral rationale).

Three-pane layout:

```
+-------------------+-------------------+-------------------+
|   Document Tree   |  Property Insp.   |   Live Preview    |
|                   |                   |                   |
|  Stack            |  type: Stack      |   ┌────────────┐  |
|  ├ Text "Hello"   |  axis: horiz.     |   │  Hello     │  |
|  ├ Image          |  spacing: md      |   │  [img]     │  |
|  └ Button "Go"    |  align: center    |   │  ( Go )    │  |
|                   |                   |   └────────────┘  |
+-------------------+-------------------+-------------------+
| [Import JSON]  [Export JSON]  [Copy]  [New]  [Examples ▾] |
+-----------------------------------------------------------+
```

- **Tree pane:** clickable nodes; selected node drives the inspector. "+" buttons on Stack nodes add children (only Phase 1 components are offered; the picker reads `@poppy/schema`'s component union). Delete via keyboard or context menu.
- **Inspector pane:** auto-generated form from the selected component's schema. Enum fields render as `<select>`s; string fields as `<input>`s; required-vs-optional surfaced. Schema-driven means the inspector picks up Phase 4 components for free once they ship.
- **Preview pane:** `<Poppy>` from `@poppy/client-react` renders the live document. `host.onAction` shows a toast ("would navigate to …") so users can see action wiring without leaving the tool.
- **Toolbar:** import/export JSON via textarea modal; copy to clipboard; "New" resets to a starter document; "Examples" loads any of the 15 valid corpus documents.

### Validation in the editor

Every edit re-runs `@poppy/server-ts`'s `validate()`. Errors surface in the inspector (per-field) and a status bar (document-level). Invalid documents still render the tree; preview falls back to an error banner when validation fails. This makes Creator a useful schema-conformance tool too: users can paste a document, see exactly which keyword + path failed.

### Persistence

**None.** Creator is stateless across reloads. The only inputs and outputs are:

- **Import:** paste JSON into a modal, or drag-drop a `.json` file (uses File API).
- **Export:** copy to clipboard, or download as `poppy-document.json` via a hidden `<a download>`.

No `localStorage`, no autosave, no File System Access API. A reload returns to the empty starter state. This is deliberate v0.3 simplification — restoring a session is a Phase 4+ feature if usage patterns demand it.

### Test surface

Creator is a UI app, but a small one. The reducer-driven architecture is testable without a browser; Vitest + `@testing-library/react` + jsdom is the entire harness.

- **Reducer unit tests** — every action: `addChild`, `removeNode`, `updateField`, `selectNode`, `importDocument`, `reset`. One test per action covering happy path + edge cases (removing the root, adding to a non-Stack, updating a nonexistent path).
- **Round-trip integrity** — for each of the 15 valid corpus documents: `dispatch(importDocument(doc))` → read state → assert deep-equal to the original. Catches any reducer that mutates or normalizes input.
- **Schema-driven inspector** — for each Phase 1 component (`Stack`, `Text`, `Image`, `Button`): assert the inspector renders the expected required and optional fields, that enum fields render as `<select>` with the right options, and that required-vs-optional is visually distinguished.
- **Validation feedback** — for each of the 7 invalid corpus documents: import it, assert the status bar surfaces the declared `keyword` + `path`, and the inspector highlights the relevant field (if applicable).
- **Tree pane interaction** — render the tree from a multi-component document; click a child; assert the inspector now reflects the clicked node's fields.
- **Toolbar flows** — Import modal accepts valid JSON via paste and via drag-drop; Export modal renders the current document as formatted JSON; Copy-to-clipboard writes the expected payload.
- **Live preview integration** — after a reducer update, assert `<Poppy>` re-renders with the new document. (Lightweight smoke; the renderer's own tests cover correctness.)

No Playwright. No visual regression. No cross-browser E2E. Scope these in Phase 4 if Creator gains complexity.

### Build

- `packages/creator/`
- Vite + `@vitejs/plugin-react`
- `pnpm dev` — Vite dev server, opens `http://localhost:5173/`
- `pnpm build` — static bundle to `dist/`
- `pnpm test` — Vitest unit + component tests
- `pnpm typecheck` — `tsc --noEmit`

### Layout

```
packages/creator/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── vitest.config.ts
├── index.html
├── README.md
├── src/
│   ├── main.tsx                  # React entry
│   ├── App.tsx                   # three-pane layout
│   ├── state/
│   │   ├── reducer.ts            # editor state machine
│   │   └── context.tsx
│   ├── panes/
│   │   ├── TreePane.tsx
│   │   ├── InspectorPane.tsx
│   │   └── PreviewPane.tsx
│   ├── toolbar/
│   │   ├── Toolbar.tsx
│   │   ├── ImportModal.tsx
│   │   └── ExportModal.tsx
│   ├── inspector/
│   │   ├── fields.tsx            # field renderers by schema type
│   │   └── component-defaults.ts # "new Button" default shape, etc.
│   └── styles.css
└── test/
    ├── reducer.test.ts
    ├── inspector.test.tsx
    └── roundtrip.test.ts
```

## `examples/react/`

Mirrors `examples/web/` in structure. Runs the React renderer against the kitchen-sink corpus document.

```
examples/react/
├── index.html
├── package.json
├── vite.config.ts
├── src/
│   ├── main.tsx
│   └── App.tsx                   # <Poppy document={kitchenSink} host={...} />
└── README.md
```

`pnpm dev` opens a Vite dev server. The host wires `onAction` to a `console.log` and a toast so the user can see action dispatch live. This example demonstrates the "drop in a screen" integration pattern for React adopters.

## CI updates

Two paths. Recommend **A**.

**A. Extend the existing `js` job.** It already runs `pnpm -r build && pnpm -r typecheck && pnpm -r test`, which picks up new packages automatically. Add `@testing-library/react` and `react`/`react-dom` to the relevant packages' devDeps; no workflow change needed.

**B. Split into `js-core` and `js-react`.** Only worth doing if `js` runtime exceeds 3 min.

If A holds (likely — pnpm runs in parallel), the only CI change is updating branch protection's required-checks list if package additions change it (they shouldn't — same job name).

The `meta` job (Biome) covers `packages/client-react/**` and `packages/creator/**` automatically once `biome.json` is checked (Biome lints the workspace).

## Tooling

| Choice | Pick | Notes |
|---|---|---|
| React version | 19.x | Stable concurrent rendering, automatic JSX runtime, modern hooks. |
| React renderer build | `tsup` | Matches `@poppy/client-web`. ESM-only output. |
| Creator build | Vite + `@vitejs/plugin-react` | De-facto React app toolchain in 2026. |
| Tests (both packages) | Vitest + `@testing-library/react` + `jsdom` | Same Vitest as the rest of the TS workspace. jsdom already present. |
| State management (Creator) | `useReducer` + Context | App is small; external libs aren't justified. |
| Code editor in Creator | `<textarea>` | Phase 3 scope. Monaco/CodeMirror considered for Phase 4. |
| Styles (renderer) | Imports `@poppy/client-web/styles.css` | Single CSS source of truth across web renderers. |
| Styles (Creator) | Hand-written CSS in `src/styles.css` | Small app, no styling lib. |
| TypeScript | 5.6+ | Same as workspace. |
| Node.js | ≥ 20 | Same as workspace `engines`. |

## Sequencing

**Strict gate:** Phase 3.1 (renderer + `examples/react/`) lands fully — green CI, ADR-0012 accepted — before Phase 3.2 (Creator) starts. One domain at a time. This keeps the Creator's design grounded in a working renderer rather than a moving target.

### Phase 3.1 — React renderer

**Week 1 — ADR-0012 + scaffolding**
- ADR-0012 (React renderer strategy: fresh TSX, mutually-normalized HTML, snapshot escape hatch) drafted, reviewed, merged.
- React renderer scaffolding: `package.json`, `tsconfig`, `tsup` config, empty exports. CI confirms green with placeholders.

**Week 2 — implementation**
- Component implementations (`PoppyStack`, `PoppyText`, `PoppyImage`, `PoppyButton`).
- `<Poppy>` wrapper, `usePoppyDocument` hook, `PoppyErrorBoundary`.
- Corpus test: every valid case's React output mutually-normalizes to the web output. Every invalid case produces its declared keyword via `validate()`.
- Behavioral tests per ADR-0008 §2 (action dispatch, axis layout, alt-text, color tokens, padding tokens).
- React-specific test surface (StrictMode, unmount, prop-change, error boundary, hook ergonomics).
- README + TSDoc.

**Week 3 — `examples/react/` + 3.1 close-out**
- `examples/react/` smoke app: Vite + `<Poppy>` rendering the kitchen-sink corpus document with action toasts wired.
- Cross-renderer manual sanity check on kitchen-sink (web + React + Android + iOS open simultaneously). Document any divergences. Either fix or file Phase 4 follow-ups.
- Phase 3.1 milestone: green CI, ADR-0012 accepted, `examples/react/` runnable.

### Phase 3.2 — Creator

**Week 4 — ADR-0013 + scaffolding**
- ADR-0013 (Creator architecture: stateless, client-only, tree + inspector, useReducer + Context) drafted, reviewed, merged.
- Vite scaffolding, three-pane layout shell, empty reducer.

**Week 5 — implementation**
- Reducer + state machine (createSlice-shaped).
- Tree pane with add/remove/select.
- Inspector pane with schema-driven field generation.
- Live preview using `@poppy/client-react`.
- Toolbar: import (paste + drag-drop), export (download + copy), reset, examples picker.

**Week 6 — tests + close-out**
- Full Creator test surface (reducer, round-trip, inspector, validation feedback, tree, toolbar, preview).
- Creator README + screenshot in repo README.
- CHANGELOG entry for `0.3.0-alpha`.
- README updates (status table, roadmap, package list).
- `git tag v0.3.0-alpha` after green CI.

Parallelization is **not used** in Phase 3 — the dependency between sub-phases makes single-track sequencing simpler and the deliverables are individually small enough that subagent overhead isn't worth it.

## Critical files

- `packages/client-react/src/index.ts` — public exports.
- `packages/client-react/src/Poppy.tsx` — entry component.
- `packages/client-react/test/render-matches-web.test.tsx` — mutually-normalized HTML contract.
- `packages/client-react/test/react-specific.test.tsx` — StrictMode / unmount / prop-change / error boundary.
- `packages/creator/src/App.tsx` — three-pane shell.
- `packages/creator/src/state/reducer.ts` — editor state machine (createSlice-shaped).
- `packages/creator/src/inspector/fields.tsx` — schema-driven inspector.
- `examples/react/src/App.tsx` — React example entry.
- `docs/adr/0012-react-renderer-strategy.md` — fresh-TSX vs wrap decision.
- `docs/adr/0013-creator-architecture.md` — stateless client-only, tree + inspector.

## Smaller decisions defaulted (overridable)

- **React peer-dep range:** `^19.0.0`. React 19 is stable as of late 2024; in 2026 it's well-adopted. Drop React 18 support — supporting both adds complexity for marginal value at v0.3-alpha.
- **No `react-dom` in dependencies for the renderer:** peer-dep'd. Hosts pin their own DOM runtime.
- **Component naming:** `<Poppy>` not `<PoppyDocument>`. Mirrors the Android `Poppy()` Composable and iOS `PoppyView`.
- **Server-component compatibility:** out of scope for v0.3. The package emits a top-level `'use client'` directive in its built ESM so RSC consumers get a clear error rather than a silent break. (`tsup` handles this via `banner.js`.)
- **Creator local dev URL:** `http://localhost:5173/` (Vite default).
- **Creator starter document:** the `001-text-basic` corpus case. Smallest valid document, gives users a starting point.
- **Creator JSON formatting:** 2-space indent on export, matches corpus convention.
- **Snapshot strategy for React:** no `snapshot.react.html` unless forced. Both renderers' outputs run through `normalize-html`; tests assert mutual equivalence and equality with the committed `snapshot.web.html`. Escape hatch documented in ADR-0012.

## Verification

Phase 3 is "done" when:

1. From a clean clone: `pnpm install && pnpm -r build && pnpm -r typecheck && pnpm -r test` passes (no new commands).
2. `cd packages/creator && pnpm dev` opens the Creator and renders the starter document; clicking tree nodes updates the inspector; editing fields updates the preview; reload returns to the empty starter state (no persistence).
3. `cd examples/react && pnpm dev` renders the kitchen-sink document with action toasts wired.
4. Every valid corpus case's React render normalizes to the same shape as the web render (mutual equivalence) and matches the committed `snapshot.web.html` (possibly modulo a documented `normalize-html` extension landed in this phase). Every invalid case produces its declared keyword through `@poppy/server-ts`'s `validate()`.
5. Behavioral assertions (ADR-0008 §2) pass in `packages/client-react/test/behavior.test.tsx`. React-specific tests (StrictMode, unmount, prop-change, error boundary, hook ergonomics) pass in `packages/client-react/test/react-specific.test.tsx`.
6. Creator's test surface passes: reducer unit tests, round-trip integrity on all 15 valid corpus documents, schema-driven inspector renders correct fields per component, validation feedback surfaces declared keyword + path for all 7 invalid documents.
7. Manual cross-renderer review on the kitchen-sink case: web, React, Android, iOS open side-by-side. Document any divergences. Either fix or file Phase 4 follow-ups.
8. ADRs 0012 and 0013 exist and are accepted.
9. **Schema `$id` URLs migrated to URN-style identifiers.** No `$id` in `packages/schema/schemas/*.json` points at `raw.githubusercontent.com/greg7gkb/...` anymore. (`package.json` `repository` fields may still point at the actual repo — only schema `$id` is domain-agnostic.)
10. README and CHANGELOG updated for `0.3.0-alpha`. Roadmap entry for Phase 3 marked done.
11. `git tag v0.3.0-alpha` cut. Branch protection green.
12. `docs/phase-3-plan.md` (this file) committed.

## Resolved decisions (from Phase 3 planning round)

1. ✅ **Single `v0.3.0-alpha` tag** covering Phase 3.1 (renderer + example) + Phase 3.2 (Creator).
2. ✅ **Fresh TSX implementation** of the React renderer, not a wrap of `@poppy/client-web`. Conformance via mutually-normalized HTML comparison; ADR-0012 documents.
3. ✅ **Tree + property inspector** for the Creator; no canvas / DnD in Phase 3. Canvas authoring logged for Phase 4.
4. ✅ **React 19 only** (peer-dep `^19.0.0`). No React 18 backport in this phase.
5. ✅ **Client-only with `'use client'` banner.** RSC compatibility deferred to Phase 4 once schema gains async-data semantics.
6. ✅ **No parallelization** — Phase 3.1 lands fully (renderer + `examples/react/`) before Phase 3.2 starts. One domain at a time.
7. ✅ **Creator is stateless across reloads.** No `localStorage`, no autosave, no backend. Import/export to file only. Restoring sessions is a Phase 4+ consideration.
8. ✅ **No Redux in Phase 3.2.** `useReducer` + Context with a `createSlice`-shaped reducer for mechanical migration when Phase 4 needs undo/redo or middleware. (Override callable before Week 3 if you want to introduce it now.)
9. ✅ **Snapshot strategy: aim for byte-identical via mutual normalization, with documented escape hatch** (commit `snapshot.react.html` only if forced). Realistic risks and mitigations enumerated in §Snapshot strategy.
10. ✅ **React-specific test surface** added: StrictMode safety, unmount cleanup, prop-change stability, error boundary, hook ergonomics. Sits alongside the cross-platform corpus + behavioral tests.
11. ✅ **Creator test surface promoted to Goals**: reducer unit, round-trip integrity, schema-driven inspector, validation feedback, tree interaction, toolbar flows, preview smoke.
12. ✅ **TSX terminology** used throughout — these are TypeScript files, not vanilla JSX.
13. ✅ **No React Native renderer.** Confirmed out of scope, indefinitely.

## Phase 3 backlog (must close before `v0.3.0-alpha` tag)

These are tracked Phase 3 work items that don't fit cleanly into 3.1 or 3.2 but must land before the phase closes. Slot them into Week 3 or Week 6 (the close-out weeks) as capacity allows.

- **Switch schema `$id` URLs to URN-style identifiers.** Today every schema file's `$id` is pinned to `https://raw.githubusercontent.com/greg7gkb/poppy/v0.1.0-alpha/...`, hardcoding a personal GitHub handle into the canonical wire format. **Resolution: option (b) from the Phase 2 follow-up list** — switch `$id` to URN-style identifiers like `urn:poppy:v0.1:schema:component:stack`. JSON Schema spec allows non-fetchable `$id`, so editors and validators don't need a resolvable URL. This is a one-PR change touching `packages/schema/schemas/*.json` and the `$id` references in `@poppy/server-ts`. Schema *content* is unchanged — only the identifier string. Wire format version stays `0.1`; no document-level break.
  - Sub-task: audit every `package.json`'s `repository` field. Those can keep pointing at `github.com/greg7gkb/poppy` (the actual repo location) — it's only the schema `$id` that needs to be domain-agnostic. Confirm scope before the PR.
  - Sub-task: update ADR-0001 (or add a footnote) explaining the URN convention.

## Out of scope for this plan (deferred follow-ups)

- **NPM publishing** of `@poppy/client-react`. Same posture as Phases 1 and 2: tag the git release; publish is a separate decision.
- **Creator static hosting** (GitHub Pages / Netlify / Vercel). The Creator is shipped as source-buildable for v0.3. Hosting is a Phase 3.5 decision once the UX stabilizes.
- **`@poppy/client-react` API for streaming / suspense / async data.** Needs schema-level support first; logged for Phase 4 backlog.
- **`tsup` → `tsdown` migration.** Upstream `tsup` is no longer actively maintained as of late 2025; the project's README points users to [`tsdown`](https://github.com/rolldown/tsdown) (a Rolldown-backed drop-in successor from the same author). Phase 3 keeps `tsup` for both `@poppy/client-react` and `@poppy/client-web` so the toolchain stays consistent within the phase, but the migration should land before any wider release. Tracked as a post-Phase-3 follow-up — evaluate after `v0.3.0-alpha` is cut, plan a single PR that flips all TS packages together, and verify the precompiled validator pipeline (`@poppy/server-ts`) still works with `tsdown`.
- **Moving the repo to an org account.** Option (a) from the Phase 2 follow-up list is explicitly *not* pursued — user has elected URN-style `$id`s (option b) over creating a GitHub org.
