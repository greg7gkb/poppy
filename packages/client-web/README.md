# `@poppy/client-web`

Native HTML / DOM renderer for Poppy. **The reference renderer** — every other Poppy renderer must visually agree with this one.

## Status

**Phase 1 shipped (`v0.1.0-alpha`).** Renders all v0.1 components (`Stack`, `Text`, `Image`, `Button`) and the `navigate` action. Not yet published to npm; install from source.

## Why "reference renderer"

The web client is the first renderer implemented and the one whose normalized HTML output is committed to the conformance corpus as `snapshot.web.html` for each valid case. Phase 2 renderers (Compose, SwiftUI) are tested against the same input documents and must produce semantically equivalent UI. The web renderer takes a Poppy document and produces real DOM elements directly — no React, no virtual DOM, no UI framework.

## API

```ts
import { render } from "@poppy/client-web";
import "@poppy/client-web/styles.css";

const handle = render(document, container, {
  onAction(action) {
    if (action.type === "navigate") router.push(action.uri);
  },
  onError(err) { console.error(err); },         // optional
  validate: true,                               // default
  isUrlAllowed(url, context) { /* override */ } // optional
});

handle.update(newDocument);
handle.destroy();
```

- `render()` validates the document by default. Failures call `host.onError` and leave the container empty.
- `host.onAction` receives actions verbatim — the renderer never interprets a URI. Hosts decide whether to route, open externally, log, etc.
- `Image.url` runs through `isUrlAllowed` (default: allow `http(s)`, relative URLs, `data:image/*`; reject `javascript:`, `vbscript:`, `file:`, `data:text/html`).
- `Button` listeners are scoped to an `AbortController` so `destroy()` detaches them deterministically. `update()` and `destroy()` are idempotent.

## Theming

The bundled `dist/poppy.css` defines BEM-like classes (`poppy-{component}--{modifier}`) and exposes tokens as `:root` CSS custom properties. Override at any selector to retheme:

```css
:root {
  --poppy-color-primary: #ff5722;
  --poppy-space-md: 12px;
}
```

## Testing

```sh
pnpm --filter @poppy/client-web test                # behavior + corpus snapshots
pnpm --filter @poppy/client-web snapshots:update    # regenerate snapshot.web.html
```

Snapshot diffs in PRs are reviewed line-by-line — the snapshot is the cross-platform contract every Phase 2 renderer must reproduce.

## See also

- [`@poppy/server-ts`](../server-ts/) — types and validation.
- [`@poppy/conformance`](../conformance/) — corpus and snapshot harness.
- [ADR-0004 — Conformance corpus](../../docs/adr/0004-conformance-corpus.md)
- [ADR-0006 — Schema versioning](../../docs/adr/0006-schema-versioning.md)
- [`examples/web/`](../../examples/web/) — runnable browser demo.
