# Poppy — Phase 1 Implementation Plan

## Context

Phase 0 (governance, ADRs, package skeleton, GitHub repo `greg7gkb/poppy`) is complete and committed. Phase 1 establishes the contract that every later phase rides on:

- **Schema v0.1** — the wire format
- **`@poppy/server-ts`** — typed builder + validator
- **`@poppy/client-web`** — native HTML reference renderer
- **`@poppy/conformance`** — the cross-platform regression test surface
- CI gates that enforce all four

Phase 1 is the highest-stakes phase. The schema we ship locks in field names and shapes that every Phase 2+ renderer (Compose, SwiftUI, optionally React) must honor. The conformance corpus we set up here is the regression test surface for years.

This plan is grounded in two inputs: (a) reference research on Microsoft Adaptive Cards (closest open prior art), and (b) an independent design pass from a Plan agent. Four locked-in decisions from a clarification round with the project lead:

1. **Semantic tokens, not raw values** for spacing/padding/text size/color.
2. **Ajv standalone precompile** for validation (Ajv as devDep, generated validator JS shipped — zero runtime parsing).
3. **GitHub raw URL pinned to release tag** for schema `$id`.
4. **ESM only** for TS packages.

## Goals

By end of Phase 1:

- `@poppy/schema@0.1.0-alpha` — JSON Schema files, hand-written TS types, examples.
- `@poppy/server-ts@0.1.0-alpha` — `validate()`, `isValid()`, ergonomic builders.
- `@poppy/client-web@0.1.0-alpha` — `render()` for all Phase 1 components, sanitization, themeable via CSS custom properties.
- `@poppy/conformance@0.1.0-alpha` — 20 corpus cases (15 valid + 5 invalid).
- CI runs lint, typecheck, schema validation, unit tests, and the web conformance suite on every PR.
- `git tag v0.1.0-alpha` cut after green CI.

## Non-goals (Phase 1)

- Mobile renderers (Phase 2)
- Theming primitives beyond static CSS variables in the web client (Phase 5)
- Async data, slots, modals, lists, forms (Phase 4+)
- Deep accessibility (Phase 4) — Phase 1 covers basics: required `alt`, native `<button>`, `aria-label` propagation
- Image loading state, error fallback, retry (Phase 4)
- Doc site (Phase 1.5 or later)
- NPM publish (cut git tag; publish gated on a separate decision)
- React renderer (Phase 3)

## Schema v0.1

### Document root

```json
{
  "$schema": "https://raw.githubusercontent.com/greg7gkb/poppy/v0.1.0-alpha/packages/schema/schemas/poppy.schema.json",
  "version": "0.1",
  "root": <Component>
}
```

- `$schema` *(optional)* — URL to canonical schema. Used by editors. Pinned to a release tag so URL is stable and retrievable.
- `version` *(required)* — `"MAJOR.MINOR"` string. Renderers reject unknown majors and forward-compat across minors (ignore unknown fields within known components).
- `root` *(required)* — single component. Multiple top-level children is what `Stack` is for.

### Component shape

- Discriminator: `type: string`, **PascalCase** (`Stack`, `Text`, `Image`, `Button`).
- All other fields: **camelCase**.
- Optional `id: string` on every component (future diffing/hot-reload; no enforced uniqueness in v0.1).

### Tokens

Spacing, text size, text color, weight, image fit, alignment, axis are all **enum tokens** in v0.1. Each renderer maps tokens to native scales.

```
spacing:    none | xs | sm | md | lg | xl
size:       xs   | sm | md | lg | xl                      (text size)
color:      default | primary | secondary | danger | success
weight:     regular | medium | bold
fit:        contain | cover | fill                        (image)
alignment:  start   | center | end | stretch              (cross-axis)
axis:       horizontal | vertical                          (stack)
```

Default mappings (per platform):

| Token | Web (CSS px) | Android (dp) | iOS (pt) |
|---|---|---|---|
| `none` | 0 | 0 | 0 |
| `xs` | 4 | 4 | 4 |
| `sm` | 8 | 8 | 8 |
| `md` | 16 | 16 | 16 |
| `lg` | 24 | 24 | 24 |
| `xl` | 32 | 32 | 32 |

Color tokens map to neutral defaults in v0.1 via CSS custom properties (`--poppy-color-default` etc.). Hosts override by redefining variables. Phase 5 introduces a real design-system layer.

### Components

#### `Stack`
```json
{
  "type": "Stack",
  "id": "string?",
  "axis": "horizontal" | "vertical",
  "children": "Component[]",
  "spacing":   "none|xs|sm|md|lg|xl",
  "padding":   "none|xs|sm|md|lg|xl",
  "alignment": "start|center|end|stretch"
}
```

`alignment` is **cross-axis** alignment (perpendicular to `axis`). Document this clearly — likely confusion point. Empty `children: []` is valid (useful for conditional rendering).

Mappings:
- HTML: `<div data-poppy-stack data-axis="...">` with classes `poppy-stack poppy-stack--horizontal poppy-spacing-md poppy-padding-md poppy-align-start`.
- Compose: `Row`/`Column` + `Modifier.padding(...)` + `Arrangement.spacedBy(...)` + alignment.
- SwiftUI: `HStack`/`VStack` + `.padding()` + alignment modifiers.

#### `Text`
```json
{
  "type": "Text",
  "id": "string?",
  "value":  "string",                          // required, plain text only
  "color":  "default|primary|secondary|danger|success",
  "size":   "xs|sm|md|lg|xl",
  "weight": "regular|medium|bold"
}
```

**Plain text only.** No markdown, no inline spans. Adaptive Cards ships limited markdown and pays for it cross-platform — defer to a separate `RichText` component in v0.2+ if needed.

#### `Image`
```json
{
  "type": "Image",
  "id": "string?",
  "url":    "string",     // required, minLength 1, NOT format:"uri"
  "alt":    "string",     // required (accessibility-first)
  "width":  "number?",    // optional, intrinsic if absent (logical px)
  "height": "number?",
  "fit":    "contain|cover|fill"
}
```

`url` validated only `minLength: 1` — hosts may need custom schemes; per-platform image loaders have their own URL semantics. Web sanitization rejects `javascript:` / `vbscript:` / non-`image/*` `data:` URIs by default; configurable via `host.isUrlAllowed`.

#### `Button`
```json
{
  "type": "Button",
  "id": "string?",
  "label":  "string",
  "action": "Action"
}
```

No icon, no variant in v0.1 — variants come with the design system in Phase 5.

### Action: `navigate`

```json
{ "type": "navigate", "uri": "string" }
```

- `uri` is **opaque** to the renderer; validated only `minLength: 1`. No `format: "uri"`.
- Renderer dispatches the action verbatim to the host's `onAction` callback. Host owns routing.
- **Action `type` values are lowercase** (`"navigate"`); component `type` values are PascalCase. Casing distinction is intentional — clean error messages, no overload confusion.

### Schema file layout

```
packages/schema/
├── package.json
├── README.md
├── schemas/
│   ├── poppy.schema.json              # entry: $id, $defs, root component oneOf
│   ├── components/
│   │   ├── stack.schema.json
│   │   ├── text.schema.json
│   │   ├── image.schema.json
│   │   └── button.schema.json
│   ├── actions/
│   │   └── navigate.schema.json
│   └── tokens/
│       ├── spacing.schema.json
│       ├── size.schema.json
│       ├── color.schema.json
│       ├── weight.schema.json
│       ├── alignment.schema.json
│       ├── fit.schema.json
│       └── axis.schema.json
├── examples/                          # mirrors corpus valid/* for docs/playground
│   ├── 01-text.json
│   ├── 02-stack-with-text.json
│   └── ...
└── src/
    └── index.ts                       # re-exports parsed JSON + TS types
```

### JSON Schema 2020-12 conventions

- `"$schema": "https://json-schema.org/draft/2020-12/schema"` on every file.
- **`unevaluatedProperties: false`** at top level of every component schema (composes correctly with `$ref`; `additionalProperties` does not).
- Component union via `oneOf` with Ajv `discriminator: { propertyName: "type" }` for clean error messages on type mismatches (e.g. `"type": "Buton"` → `"expected one of Stack/Text/Image/Button"`).
- Token enums as separate files in `tokens/`, referenced via `$ref`.
- Recursive component definition — `Stack.children` references the root component oneOf via `$ref`.

### Versioning

- Schema package follows semver. `0.1.0-alpha.X` during finalization, `0.1.0` at release.
- Document `version` field is the wire-format version, conceptually independent of schema package version.
- ADR-0006 *(new)* codifies versioning policy before schema files merge.

## `@poppy/server-ts`

### Public API

```ts
export type {
  PoppyDocument, Component,
  Stack, Text, Image, Button,
  Action, NavigateAction
} from "./types";

export const SCHEMA_VERSION = "0.1";

export interface ValidationError {
  path: string;       // JSON pointer, e.g. "/root/children/0/value"
  message: string;
  keyword: string;    // schema keyword that failed
}

export type ValidationResult =
  | { ok: true; document: PoppyDocument }
  | { ok: false; errors: ValidationError[] };

export function validate(doc: unknown): ValidationResult;
export function isValid(doc: unknown): doc is PoppyDocument;

export const builders: {
  stack(opts: StackOptions, children: Component[]): Stack;
  text(value: string, opts?: TextOptions): Text;
  image(url: string, alt: string, opts?: ImageOptions): Image;
  button(label: string, action: Action): Button;
  navigate(uri: string): NavigateAction;
  doc(root: Component, version?: string): PoppyDocument;
};
```

- `validate()` returns a discriminated result — never throws on invalid input. (Server libs that throw on user input are hostile.)
- `isValid()` is a TS type guard for ergonomic narrowing.
- Builders produce plain objects identical to typed literals — purely a convenience.

### Types strategy

**Hand-written** TS types in `src/types.ts`. ~150 lines for Phase 1. Conformance corpus catches drift between schema and types.

We do **not** generate types from JSON Schema (poor output for discriminated unions). We do **not** invert and use Zod/TypeBox as source (puts a runtime dep at the heart of the contract; bad for Kotlin/Swift consumers in Phase 2).

### Validation: Ajv standalone precompile

- **Build time:** `ajv-cli` compiles all schema files into a plain JavaScript validator at `src/generated/validator.js`.
- **Runtime:** `validate.ts` imports the generated validator. Zero schema-parsing at runtime, no eval (CSP-safe), smallest possible footprint.
- **Ajv and `ajv-cli` are devDependencies** — never runtime.
- Generated file is gitignored; CI regenerates it as part of `pnpm build`.
- ADR-0007 *(new)* documents the Ajv-precompile decision.

### Build & distribution

- TypeScript source, target ES2022, **ESM only**, single bundle via `tsup`.
- Output: `dist/index.js` + `dist/index.d.ts`.
- Tree-shakable: builders importable individually.
- `package.json`:
  ```json
  "type": "module",
  "exports": {
    ".": { "types": "./dist/index.d.ts", "import": "./dist/index.js" }
  }
  ```

### Layout

```
packages/server-ts/
├── package.json
├── README.md
├── tsconfig.json
├── tsup.config.ts
├── src/
│   ├── index.ts             # public API
│   ├── types.ts             # PoppyDocument, Component, Action, ...
│   ├── validate.ts          # wraps generated validator
│   ├── builders.ts
│   └── generated/           # gitignored
│       └── validator.js
├── scripts/
│   └── compile-schema.ts    # ajv-cli driver
└── tests/
    ├── validate.spec.ts
    └── builders.spec.ts
```

## `@poppy/client-web`

### Public API

```ts
import type { Action, PoppyDocument } from "@poppy/schema";

export interface PoppyHost {
  /** Called when a Button (or future actionable component) fires. */
  onAction: (action: Action) => void;
  /** Validate before render. Default: true. */
  validate?: boolean;
  /** URL allowlist override. Default: built-in safe set. */
  isUrlAllowed?: (url: string, context: "image") => boolean;
  /** Error reporter. Default: console.error. */
  onError?: (err: Error) => void;
}

export interface RenderResult {
  update: (doc: unknown) => void;   // full re-render in v0.1; diffing is Phase 4+
  destroy: () => void;              // detaches event listeners; idempotent
}

export function render(
  document: unknown,
  container: HTMLElement,
  host: PoppyHost
): RenderResult;
```

### Architecture

- **Native DOM, no virtual DOM, no framework.** `document.createElement`.
- `update()` does a full re-render in v0.1.
- **CSS classes + CSS custom properties for theming**, not inline styles. Ship `dist/poppy.css` with class definitions and `:root` defaults for tokens. Hosts theme by overriding `--poppy-*` variables.
- Class naming: BEM-like, `poppy-{component}--{modifier}`. Data attributes for richer queries.

### Sanitization

- All text content via `textContent` only — never `innerHTML`.
- `Image.url` runs through `host.isUrlAllowed`. Default allow: `http:`, `https:`, relative URLs, `data:image/*`. Reject everything else.
- `navigate.uri` is **never** rendered into the DOM. Only passed to `host.onAction`. XSS via uri is a host concern.
- Buttons render as `<button type="button">`, never `<a>`. Host decides whether to navigate via `window.location` or a router.

### Browser target

- Last 2 versions of Chrome, Firefox, Safari, Edge.
- ES2020 syntax, ESM via `<script type="module">`.

### Layout

```
packages/client-web/
├── package.json
├── README.md
├── tsconfig.json
├── tsup.config.ts
├── src/
│   ├── index.ts
│   ├── render.ts
│   ├── components/
│   │   ├── stack.ts
│   │   ├── text.ts
│   │   ├── image.ts
│   │   └── button.ts
│   ├── sanitize.ts
│   └── styles/
│       └── poppy.css
└── tests/
    ├── render.spec.ts
    └── sanitize.spec.ts
```

## `@poppy/conformance`

### Layout

```
packages/conformance/
├── package.json
├── README.md
├── cases/
│   ├── valid/
│   │   ├── 001-text-hello/
│   │   │   ├── document.json
│   │   │   ├── snapshot.web.html
│   │   │   └── description.md
│   │   ├── 002-text-with-color-primary/
│   │   ├── ...
│   │   └── 015-kitchen-sink/
│   └── invalid/
│       ├── 001-missing-type/
│       │   ├── document.json
│       │   ├── expected-error.json     # { keyword, path }
│       │   └── description.md
│       └── ...
└── src/
    ├── index.ts                        # exports loadCases(), Case type
    ├── normalize-html.ts               # deterministic HTML serializer
    └── runner.ts                       # cross-package test harness
```

`runner.ts` is exported from `@poppy/conformance` and consumed by `@poppy/client-web`'s test suite. Phase 2 renderers will use the same harness without duplication.

### HTML snapshot normalization

Implemented in `normalize-html.ts`, ~80 lines:

1. Parse with `DOMParser` (jsdom in tests).
2. Walk DOM tree; for each element emit:
   - lowercase tag name
   - attributes sorted alphabetically (lowercase names, double-quoted values)
   - classes sorted alphabetically and de-duplicated
   - children processed recursively
3. Whitespace between block elements collapsed to single newlines.
4. Self-closing rules per HTML spec (`<img>`, `<br>`, no trailing slash).
5. Entity normalization (`&amp;` etc.).

Custom (~80 LOC) over Prettier — cheap, deterministic, no heavy devDep.

### Update workflow

```bash
pnpm conformance:update     # regenerates expected snapshots
pnpm test                   # fails on diff
```

CONTRIBUTING amendment: snapshot diffs in PRs are reviewed line-by-line. *"just regenerated the snapshots"* is not a valid PR description.

### Phase 1 case list (20)

**Valid (15):**
1. `text-hello` — minimal `Text`
2. `text-with-color-primary`
3. `text-with-size-lg`
4. `text-with-weight-bold`
5. `text-all-options` — color + size + weight
6. `image-basic` — url + alt only
7. `image-with-dimensions`
8. `image-with-fit-cover`
9. `stack-vertical-two-texts`
10. `stack-horizontal-two-texts`
11. `stack-with-spacing-lg`
12. `stack-with-padding-md`
13. `stack-nested` — stack inside stack, mixed axes
14. `button-basic` — label + navigate
15. `kitchen-sink` — every component, deeply nested

**Invalid (5):**
16. `missing-type` — component lacks `type`
17. `unknown-component-type` — `"type": "Heading"`
18. `image-missing-alt` — proves `alt` is required
19. `text-wrong-value-type` — `value: 42`
20. `button-missing-action`

20 is the right Phase 1 number. Each case is a forever-maintenance commitment.

## Tooling

| Choice | Pick | Notes |
|---|---|---|
| Package manager | **pnpm 9** | Best monorepo support |
| Workspace | **pnpm workspaces** | Native to pnpm |
| TypeScript | latest stable | Standard |
| Linter / formatter | **Biome** | Single tool, fast |
| Test runner | **Vitest** | ESM-native |
| DOM in tests | **jsdom** | Full DOM coverage |
| Build | **tsup** | esbuild, ESM single-file |
| Validator (build-time) | **Ajv + ajv-cli** | devDep, precompile to plain JS |
| CI | GitHub Actions | Already set up |

## CI updates

Add a `js` job alongside the existing `meta` job in `.github/workflows/ci.yml`:

```yaml
js:
  name: TypeScript packages
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: pnpm/action-setup@v3
    - uses: actions/setup-node@v4
      with: { node-version: 20, cache: pnpm }
    - run: pnpm install --frozen-lockfile
    - run: pnpm biome ci .          # lint + format check
    - run: pnpm -r typecheck        # tsc --noEmit per package
    - run: pnpm -r build            # generates Ajv standalone validator
    - run: pnpm -r test             # vitest, includes corpus
```

Branch protection on `main`: require `meta` and `js` to pass before merge.

## Sequencing

Schema first, then corpus, then renderer. Three-week target with ~half a week of slack.

**Week 1 — Schema + tooling**
- pnpm workspace, Biome, Vitest, tsup, base `tsconfig` at repo root.
- All four component schemas, `navigate` action, token enums in `packages/schema/`.
- Hand-written TS types in `packages/server-ts/src/types.ts`.
- Ajv-precompile pipeline: `pnpm --filter @poppy/server-ts build` produces a working validator.
- ADR-0006 (versioning) and ADR-0007 (Ajv precompile) merged.

**Week 2 — server-ts validation + corpus skeleton**
- `validate()` and `isValid()` working against all 20 corpus inputs (validation only, no snapshots yet).
- All 20 corpus directories created with `document.json` and `description.md`.
- `expected-error.json` for invalid cases.
- `runner.ts` harness exported from `@poppy/conformance`.

**Week 3 — Web client + snapshots**
- `render()` for all four components in `@poppy/client-web`.
- Sanitization layer.
- `normalize-html.ts`.
- Snapshot files generated and reviewed line-by-line.
- Builders in `@poppy/server-ts`.
- Per-package READMEs updated.

**Week 4 (slack) — Hardening + release**
- Manual smoke test in a real browser via `examples/web/`.
- README quickstart pasteable by an outside contributor.
- `git tag v0.1.0-alpha`. NPM publish gated on a separate decision.

## Critical files

- `packages/schema/schemas/poppy.schema.json` — entry-point schema.
- `packages/schema/schemas/components/*.schema.json` — per-component.
- `packages/schema/schemas/tokens/*.schema.json` — shared token enums.
- `packages/server-ts/src/types.ts` — TS mirror of schema.
- `packages/server-ts/src/validate.ts` — public validation entry.
- `packages/server-ts/scripts/compile-schema.ts` — Ajv standalone codegen driver.
- `packages/client-web/src/render.ts` — public render entry.
- `packages/client-web/src/sanitize.ts` — URL allowlist + textContent rules.
- `packages/client-web/src/styles/poppy.css` — class defs + CSS custom properties.
- `packages/conformance/src/normalize-html.ts` — deterministic serializer.
- `packages/conformance/src/runner.ts` — shared test harness.
- `.github/workflows/ci.yml` — adds `js` job.
- `docs/adr/0006-schema-versioning.md` — new ADR.
- `docs/adr/0007-ajv-standalone-precompile.md` — new ADR.

## Smaller decisions defaulted (overridable)

- Empty `Stack` (`children: []`) is **valid**.
- Action `type` values are **lowercase**; component `type` values are **PascalCase**.
- Default text color name is **`default`**.
- React renderer is **out of scope** (Phase 3).
- Tokens package (`@poppy/tokens`) is **deferred** to Phase 2 when mobile renderers also need values; Phase 1 web client encodes mappings in CSS.
- NPM publish is **deferred**; Phase 1 ends at git tag.

## Verification

Phase 1 is "done" when:

1. `pnpm install && pnpm -r build && pnpm -r test` passes from a clean clone.
2. CI on `main` runs and passes the full pipeline.
3. `packages/conformance/cases/valid/` has 15 cases, each with a generated and reviewed `snapshot.web.html`.
4. `packages/conformance/cases/invalid/` has 5 cases, each with `expected-error.json`.
5. `examples/web/` has a runnable HTML page that loads the bundle and renders a sample document.
6. Manual smoke test in Chrome / Safari / Firefox: button click fires `onAction` with the navigate URI; image renders; stacks lay out correctly.
7. `git tag v0.1.0-alpha` exists.
8. ADRs 0006 and 0007 exist.
9. After approval, this plan is copied to `docs/phase-1-plan.md` in the repo (parallel to `docs/bootstrap-plan.md`) and committed.
