# ADR-0012: React renderer strategy — fresh TSX, mutually-normalized HTML

## Status

Accepted — 2026-05-16

## Context

Phase 3.1 adds `@poppy/client-react` — a React-idiomatic renderer that consumes the same wire format and passes the same conformance corpus as `@poppy/client-web`. The web client is framework-free DOM (it constructs nodes via `document.createElement`, attaches listeners through an `AbortController`, and exposes a small `render() → { update, destroy }` API). React adopters can use the web client today by wrapping it in a `useEffect`, but the result feels foreign in React codebases: no JSX tree, no React DevTools introspection, no portals, no error boundaries, no Suspense story.

Two architectural options exist for the React package. The decision below picks one; this ADR records the trade-offs so future renderers (e.g. a hypothetical Vue or Solid client) inherit a consistent design philosophy.

### Option A — Wrap the imperative DOM renderer

The React component holds a ref, calls `render(doc, ref.current, host)` in a `useEffect`, and calls `destroy()` on unmount.

```tsx
function Poppy({ document, host }: PoppyProps) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const instance = render(document, ref.current!, host);
    return () => instance.destroy();
  }, [document, host]);
  return <div ref={ref} />;
}
```

- Single source of rendering logic — React can't diverge from web because it IS web.
- The DOM under the wrapper is opaque to React: no JSX inspection, no DevTools, no portal targets, no React-managed event handlers, no error boundary catch surface (errors thrown imperatively don't propagate to React).
- The component is a leaf from React's perspective — it can never expose React-typed children or composition slots.

### Option B — Fresh TSX implementation

Each component (`Stack`, `Text`, `Image`, `Button`) is a small TSX function returning a JSX tree. CSS is imported from `@poppy/client-web/styles.css` so visual output stays identical. Validation, URL allowlist, and token resolution stay in `@poppy/server-ts` / `@poppy/schema` — only the JSX mapping is duplicated.

```tsx
function PoppyStack({ node, host }: { node: Stack; host: PoppyHost }) {
  return (
    <div className={cn('poppy-stack', `poppy-stack--${node.axis}`, ...)}>
      {node.children.map((child, i) => (
        <PoppyComponent key={child.id ?? i} node={child} host={host} />
      ))}
    </div>
  );
}
```

- Native React tree: DevTools shows component names, hooks work, portals are possible, error boundaries catch render-time exceptions.
- Logic duplication is limited to the ~80 lines of TSX-per-component. Validation and token resolution stay shared.
- Risk: visual / structural drift from the web renderer. The corpus exists to catch this.

### Why the corpus enforces parity either way

Per [ADR-0008](0008-cross-platform-conformance-strategy.md) the conformance contract is the schema + corpus + per-platform snapshots, not the renderer implementation. `@poppy/conformance` ships a `normalize-html` serializer that already canonicalizes attribute order, class order, whitespace, and self-closing tags via a jsdom round-trip. Any rendering technology whose normalized HTML matches the committed `snapshot.web.html` is conformant by the existing definition.

This means option B carries an *empirically falsifiable* divergence risk — the corpus catches it on every CI run — not a *theoretical* one. The same property is why we accept four implementations of the renderer at all.

## Decision

`@poppy/client-react` is implemented as **a fresh TSX renderer (option B)**, not a wrap of `@poppy/client-web` (option A).

Concretely:

1. **TSX components** under `packages/client-react/src/components/` produce the same DOM structure as `@poppy/client-web`. CSS imported from `@poppy/client-web/styles.css` — single visual source of truth.
2. **Validation** flows through `@poppy/server-ts`'s `validate()`. The React package never re-implements validation; it imports it. URL allowlist for `<img>` is the same predicate, exported from `@poppy/server-ts`.
3. **Type definitions** (`PoppyDocument`, `Component`, `Action`, `PoppyHost`) are re-exported from `@poppy/schema` via `@poppy/server-ts`. Inlined by `tsup`'s `noExternal: [/^@poppy\//]`.
4. **Conformance** is verified by **mutually-normalized HTML comparison**. For each valid corpus case the test:
   - Renders the document through React into a detached jsdom container.
   - Renders the same document through `@poppy/client-web` into another detached container.
   - Normalizes both via `@poppy/conformance`'s `normalize-html`.
   - Asserts equality between the two normalized outputs.
   - Asserts equality of the React output with the committed `snapshot.web.html`.
5. **No new snapshot file** in the corpus directory unless React produces an artifact `normalize-html` can't strip cleanly. Falling back to a committed `snapshot.react.html` is an explicit escape hatch, not a default.
6. **React renderer is client-only** for v0.3. The built ESM emits a top-level `'use client'` directive (`tsup`'s `banner.js`). RSC compatibility is a Phase 4+ consideration.
7. **React version floor: `^19.0.0`**, peer-dep'd. The host pins React's exact version; the renderer never declares a direct dep on React.

### Test mechanic

`react-dom/server`'s `renderToStaticMarkup` is the test serialization path. It produces clean HTML without hydration markers (the alternative — `ReactDOM.createRoot(container).render(...)` then reading `container.innerHTML` — works too but adds React's hydration scaffolding in some configurations). `renderToStaticMarkup` is a `react-dom` function; no extra dep.

```ts
import { renderToStaticMarkup } from 'react-dom/server';
import { normalizeHtml } from '@poppy/conformance';

const reactHtml = normalizeHtml(renderToStaticMarkup(<Poppy document={doc} host={host} />));
const webHtml = normalizeHtml(renderHtmlViaWebClient(doc, host));
expect(reactHtml).toBe(webHtml);
expect(reactHtml).toBe(committedSnapshot);
```

### Realistic divergence risks and mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| React 19 hydration / data-attribute artifacts | Low — `renderToStaticMarkup` strips them | Use `renderToStaticMarkup` for the conformance test path. |
| TSX whitespace differences (text nodes between elements) | Medium — biggest realistic source of diff | Compose children only via `{children}` / `node.children.map(...)`; never embed literal whitespace text in TSX. |
| Boolean-attribute serialization (`disabled` with/without `=""`) | Low | Existing normalizer canonicalizes via jsdom round-trip. |
| Inline `style` attribute formatting (object → CSS string) | None in v0.1 | Schema has no inline style props in v0.1; revisit if Phase 4 introduces them. |
| Self-closing tag rules (`<img />` vs `<img>`) | None | jsdom round-trip normalizes both forms identically. |
| `className` vs `class` attribute | None | `react-dom` emits `class` in the serialized HTML; symmetric with `@poppy/client-web`. |

### Escape hatch

If React 19.x produces an artifact `normalize-html` can't strip cleanly — and the artifact is stable across React patch versions — there are two acceptable resolutions, in order of preference:

1. **Extend `normalize-html`** to canonicalize the artifact (preferred — keeps a single snapshot file as the canonical record).
2. **Commit `snapshot.react.html`** alongside `snapshot.web.html` for each valid case. Same model as the mobile platforms' split between `snapshot.android.txt` and `snapshot.ios.txt`.

Option 2 is a real escape hatch, not a planned outcome. Choose it only after option 1 proves infeasible.

## Consequences

**Positive**

- **React-idiomatic surface.** Adopters get a native React tree: hooks, DevTools, portals, error boundaries, refs, composition. The API matches React conventions (`<Poppy document={doc} host={host} />`, `usePoppyDocument(json)`, `<PoppyErrorBoundary fallback={...}>`).
- **Mutually-normalized comparison strengthens the conformance contract**: any divergence in either direction (React adds a quirk OR web client drifts) is caught by the same test.
- **No new snapshot file by default** — the committed `snapshot.web.html` remains the canonical record across web + React renderers.
- **Shared validation and URL safety** — `@poppy/server-ts` stays the single source of validation truth; React renderer can't diverge on what's valid.
- **Phase 4 RSC story is incremental.** A fresh TSX renderer can grow per-component server / client boundaries later; a wrap of the imperative DOM renderer cannot.

**Negative**

- **Visual / structural drift risk.** The TSX components are physically separate code from the web client's. The corpus mitigates this but doesn't eliminate it — a bug introduced in one renderer that the corpus doesn't cover would land. Phase 4's component additions need corpus cases that exercise both renderers identically.
- **TSX whitespace is a real engineering target.** "Don't embed literal whitespace in TSX" is a discipline the renderer must maintain through component churn. Linting via the existing Biome config helps but doesn't fully prevent it.
- **Two sets of small per-component code** to maintain when CSS class names or DOM structure evolve. Mitigation: the React components are explicitly thin (each ~10 lines of TSX) and mirror the structure of `packages/client-web/src/render.ts` one-to-one.
- **`'use client'` banner pins us to client-only rendering** for v0.3. Adopters using RSC will get a clear error rather than partial support; documentation must call this out.

## Alternatives Considered

- **Option A (wrap `@poppy/client-web`).** Discussed in §Context. Rejected: defeats the point of a React-idiomatic package. React adopters expect to see Poppy components in DevTools, attach refs to children, use error boundaries — none of which work through an opaque wrapper.
- **Shared cross-platform virtual-DOM IR.** Rejected per [ADR-0008](0008-cross-platform-conformance-strategy.md)'s "no platform-agnostic IR" stance. Each renderer uses its native idiom; conformance flows through schema + corpus, not a shared abstraction.
- **Render into the same DOM `@poppy/client-web` uses, then hand the resulting nodes to React via `dangerouslySetInnerHTML`.** Rejected: loses event handler scoping (React can't manage listeners on nodes it didn't create), loses ref forwarding, and re-introduces the security concerns `dangerouslySetInnerHTML` carries even with sanitized input.
- **`react-dom/server`'s `renderToString` instead of `renderToStaticMarkup` for the test path.** Rejected: `renderToString` adds React's hydration scaffolding (`data-reactroot` in pre-18, comment markers in 18+); `renderToStaticMarkup` produces clean HTML matching what a React-only consumer would see in a non-hydrated SSR.
- **Commit `snapshot.react.html` from day one.** Rejected as a default: a single snapshot per case is a stronger contract than per-renderer snapshots. The escape hatch exists if needed.

## References

- [ADR-0001 — Schema-first contract](0001-schema-first-contract.md) — schema is the authority; renderers conform.
- [ADR-0004 — Conformance corpus](0004-conformance-corpus.md) — bright-line rule and the per-case snapshot model.
- [ADR-0005 — Minimize third-party dependencies](0005-minimize-third-party-dependencies.md) — React added as peer-dep; `react-dom/server` used for testing without an additional dep.
- [ADR-0008 — Cross-platform conformance strategy](0008-cross-platform-conformance-strategy.md) — three-layer conformance model; per-platform snapshot files reviewed in PRs.
- [ADR-0009 — Mobile validation strategy](0009-mobile-validation-strategy.md) — the precedent of "decoding is the validator on platforms that don't run JSON Schema." React inherits the web client's `@poppy/server-ts`-based validation; this ADR doesn't change that.
- [`docs/phase-3-plan.md`](../phase-3-plan.md) — Phase 3 plan; this ADR is referenced from the "Implementation strategy" and "Snapshot strategy" sections.
- [React 19 release notes](https://react.dev/blog/2024/04/25/react-19) — context for `^19.0.0` peer-dep.
- [`react-dom/server` documentation](https://react.dev/reference/react-dom/server/renderToStaticMarkup) — the test serialization path.
