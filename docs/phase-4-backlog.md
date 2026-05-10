# Phase 4 — backlog

This is a holding pen for Phase 4 design questions and tasks that surface during Phase 0–3 work. Each entry should crystallize into a section of the eventual `docs/phase-4-plan.md` when Phase 4 actually starts.

Phase 4's working title is "Schema iteration: theming primitives, slots, async data, accessibility." Items here either propose new schema additions or call out gaps in the existing v0.1 contract.

---

## 1. Dialog / modal presentation surface

### Context

Mobile and web hosts routinely surface UI in dialogs, sheets, modals, popovers — presentation primitives that are heavily platform-idiomatic. v0.1 has no concept of presentation context: a Poppy document renders inline wherever the host invokes the renderer. That's enough for a simple screen body but doesn't address the "show this content in a dialog" use case.

### Design question

Two paths, with a possible hybrid:

**A. Add a `Dialog` (or `Modal` / `Sheet`) component to the schema.**
The server can request a dialog by emitting a document whose root or a child is a Dialog. Each renderer maps it to a native primitive (`DialogFragment` / Compose `Dialog` / SwiftUI `.sheet` / web `<dialog>`). Dispatch on dismissal flows back through `host.onAction`.

- *Pros:* Server controls when dialogs appear; cross-platform parity is enforceable via the corpus; same wire-format-as-source-of-truth principle.
- *Cons:* Dialog UX is platform-idiomatic in ways the schema can't easily abstract — modality, dismissal gestures, system back behavior, accessibility focus management, sizing presets all differ. We'd be making opinionated cross-platform choices that hosts often want to override. Composition rules become subtle (can a Dialog contain a Stack containing another Dialog?).

**B. Hosts render Poppy content inside their own dialog system.**
Poppy emits regular documents. The host wraps the rendered output in whatever dialog UI it already uses. No schema change.

- *Pros:* Host owns presentation entirely; no schema burden; trivially mixes with the host's existing dialog stack.
- *Cons:* Server has no way to *request* a dialog — that's now out-of-band coordination (e.g. the action URI carries a `?presentation=modal` hint, or a separate metadata channel).

**C. Hybrid — presentation hint on the document.**
Document grows an optional top-level `presentation: "inline" | "modal" | "sheet"` hint. Hosts decide whether/how to honor it.

- *Pros:* Schema acknowledges the use case without dictating implementation; backward-compatible (default `inline` matches current behavior).
- *Cons:* A "hint" the host can ignore is a weak contract; tests can't strongly assert behavior.

### Minimum Phase 4 deliverables

Regardless of which path is chosen:

1. **Demonstrate the dialog use case in `examples/{android,ios}/`**, even if the renderer doesn't gain new components. This shows the integration pattern (host wraps `Poppy(document, host)` in a `Dialog` Composable / `.sheet` modifier) and validates that the existing `host.onAction` can carry dismissal signals via, e.g., a `dismiss://` URI scheme the example app handles.
2. **Document the chosen contract** in an ADR (probably ADR-0012 "Dialog and presentation surfaces") so Phase 5+ renderers know what to support.

### Sub-considerations

- Does the host wrap Poppy with its own dialog *and* the document declares its own framing inside (header, padding) — and if so, are we now redundantly framing the content?
- If a Poppy document inside a host dialog dispatches `host.onAction(navigate("..."))`, what's the expected behavior — does the dialog dismiss, or does the host route inside the dialog? This needs documenting regardless of which schema path we pick.
- A11y focus: when a Poppy content opens inside a dialog, the host's a11y stack expects focus to enter and exit the dialog. Renderer responsibility vs host responsibility.

---

## 2. Richer Compose ↔ Poppy / SwiftUI ↔ Poppy integration in examples

### Context

The current `examples/{android,ios}/` apps render a single Poppy document at the screen root. Real-world hosts mix Poppy content with native UI: a Compose `LazyColumn` whose individual rows are Poppy-rendered; a SwiftUI `NavigationStack` where some screens are Poppy, others are native; a Compose `ModalBottomSheet` whose content is Poppy.

The existing examples cover the "drop in a screen" case but not these compositions. New users can't tell from the current examples whether mixing is supported, what the lifecycle looks like, or where the interop seams sit.

### Phase 4 deliverables

Extend `examples/{android,ios}/` with at least one demo each of:

- **Mixed lazy list**: native list container, Poppy renders each row from a per-row document. Verifies repeated `Poppy()` calls inside a recycler/lazy parent don't leak state, that each row's `host.onAction` carries enough context for the host to know which row fired.
- **Poppy inside a host dialog/sheet** (overlaps with task 1).
- **State hoisted by the host**: the host owns scroll position, search input, etc.; Poppy renders the dependent content reactively. Verifies the renderer doesn't need to own its own state to be useful.

Each demo gets a comment block explaining what integration pattern it demonstrates, mirroring the structure of `PoppyDemoScreen.kt` and `ContentView.swift`.

### Sub-considerations

- Recomposition cost when the same `PoppyDocument` re-renders frequently. Profile in the lazy-list demo.
- For SwiftUI: does `PoppyView` behave correctly when its parent is `LazyVStack`, where init/deinit semantics differ from `VStack`?
- Should the example demos move out of `examples/{android,ios}/` into `examples/{android,ios}/<scenario>/` subdirectories once we have multiple? Probably yes; revisit when count > 2.

---

## How to add to this backlog

Append a new numbered section. Keep entries scoped to a single design question or task. When Phase 4 starts, this file is the input to `docs/phase-4-plan.md`.
