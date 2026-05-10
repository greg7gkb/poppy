# Phase 2 — iOS subagent brief

This is the self-contained brief for a `general-purpose` subagent assigned to implement `packages/client-ios/` end-to-end. The subagent runs in a `worktree` isolation (own copy of the repo) and reports back when done.

## Goal

Ship `packages/client-ios/`: a Swift + SwiftUI library that renders Poppy v0.1 documents to a native iOS UI. By "done," every invalid conformance corpus case produces a validation error matching its declared keyword, and every valid case has a committed `snapshot.ios.txt` plus passing behavioral assertions.

## Read first (in this order)

1. `docs/phase-2-plan.md` — the overall Phase 2 plan; **your scope is the `poppy-ios` section + cross-cutting items that touch iOS**.
2. `docs/adr/0009-mobile-validation-strategy.md` — your validation approach. Decoding *is* the validator; map `DecodingError` cases to corpus keywords. Read the mapping table.
3. `docs/adr/0008-cross-platform-conformance-strategy.md` — the conformance contract. Per-platform snapshots reviewed in PRs; behavioral assertions per platform; shared corpus inputs.
4. `docs/adr/0006-schema-versioning.md` — version compat rule. You implement the same `{ keyword: "version", path: "/version" }` error shape `@poppy/server-ts` emits.
5. `packages/schema/src/types.ts` — TypeScript types you mirror in Swift structs and a discriminated enum.
6. `packages/conformance/cases/` — corpus. Each case has `document.json` + `description.md` (behavioral invariants); valid cases have `snapshot.web.html` (the web reference); invalid cases have `expected-error.json`.
7. `packages/client-web/src/render.ts` — reference renderer. Useful for matching component-rendering semantics.
8. `packages/server-ts/src/validate.ts` — reference validator. Match the error shape exactly.

## Scope

Create the iOS SwiftUI renderer at `packages/client-ios/`. The exact layout, public API, validation strategy, and theming approach are specified in `docs/phase-2-plan.md` §`poppy-ios`. **Treat the plan as the spec.**

In summary:

- SwiftPM module, `Package.swift` at module root, `swift-tools-version: 5.10`, `.iOS(.v16)` platform.
- Swift module name: `Poppy`.
- Layout per plan §`poppy-ios` → "Layout".
- `Component` is an `enum: Codable` with associated values (`stack`, `text`, `image`, `button`). Custom `init(from decoder:)` switches on the `type` discriminator.
- Validation: typed decode → version-compat check → `ValidationResult.ok | failure`. Never throws. Maps `DecodingError` cases to keywords per ADR-0009.
- Theming: `PoppyTheme` struct + `EnvironmentValues` extension (`@Environment(\.poppyTheme)`). Defaults match web (md = 16, primary = `Color(red: 0.043, green: 0.4, blue: 1)` = #0B66FF, etc. — read `packages/client-web/src/styles/poppy.css` for the exact defaults).
- Image loading: SwiftUI's `AsyncImage`. No third-party image lib.
- Renderer: `PoppyView: View` with `init(document:, host:)`. Per-component views under `Components/`.
- Tests:
  - `CorpusTests.swift`: load every valid + invalid case, assert validation outcomes match `expected-error.json` keywords / parse success.
  - `BehaviorTests.swift`: assert the invariants in each valid case's `description.md`. Read each `description.md` to know what to assert. Use ViewInspector for SwiftUI tree introspection.
  - `UpdateSnapshots.swift`: XCTest method (gated behind env var `POPPY_UPDATE_SNAPSHOTS=1`) that regenerates `snapshot.ios.txt` for every valid case using ViewInspector's tree dump.

Plus:

- A committed `snapshot.ios.txt` for each of the 15 valid cases under `packages/conformance/cases/valid/NNN-slug/`.
- `docs/adr/0011-ios-theming.md` — new ADR documenting the `EnvironmentValues`-based theming. Status: Accepted. Mirror the ADR template at `docs/adr/0000-template.md` and the style of ADRs 0007–0009.
- Updated `packages/client-ios/README.md` to reflect what's actually shipped (the existing one is a Phase 0 placeholder).

## Constraints

- **Do NOT modify**: `packages/schema/`, `packages/conformance/cases/*/document.json`, `packages/conformance/cases/*/description.md`, `packages/conformance/cases/*/expected-error.json`, `packages/conformance/cases/*/snapshot.web.html`, `packages/client-web/`, `packages/server-ts/`, `packages/conformance/src/`. You may read all of them. You add `snapshot.ios.txt` files; that's it for the corpus.
- **Do NOT change the JSON Schema or the v0.1 wire format.** If you find a gap (e.g. a case description says X but the schema doesn't allow X), STOP and report. The user decides whether to extend v0.1 or work around.
- **Do NOT add dependencies beyond** what's listed in `docs/phase-2-plan.md` §"Open dep additions" (ViewInspector). `Codable` is stdlib. If you discover a need for another dep, STOP and report. Per [ADR-0005](adr/0005-minimize-third-party-dependencies.md), every dep is justified.
- **Do NOT push to origin.** Commit locally to a feature branch named `phase-2/ios`. The main session merges later.
- **Do NOT touch Android work** (`packages/client-android/`). A separate subagent handles Android in parallel.
- **Do NOT modify `.github/workflows/ci.yml`.** The `ios` job stub is already present; the main session wires up the real `swift test` command after merging your branch.

## Commit hygiene

Use small logically-grouped commits with conventional-commit messages. Suggested split:

1. `feat(ios): scaffold Package.swift + folder structure`
2. `feat(ios): Codable Document + Component enum with discriminator`
3. `feat(ios): validate() with corpus invalid-case parity`
4. `feat(ios): PoppyTheme + EnvironmentValues`
5. `feat(ios): render Stack / Text / Image / Button`
6. `test(ios): behavioral invariants per ADR-0008 §2`
7. `test(ios): snapshot harness + 15 snapshot.ios.txt`
8. `docs(adr): ADR-0011 iOS theming`
9. `docs(ios): per-package README`

Each commit's footer should include:
```
Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

## Done criteria

1. `cd packages/client-ios && swift test` passes from a clean clone (on macOS).
2. All 7 invalid corpus cases produce a `ValidationError` whose `keyword` matches the case's `expected-error.json`.
3. All 15 valid corpus cases produce a typed `PoppyDocument` and render successfully.
4. Each valid case has a committed `snapshot.ios.txt`.
5. `BehaviorTests.swift` asserts the invariants from each `description.md`. (You don't need a separate test method per case; group invariants by kind and parametrize over cases where it makes sense.)
6. `docs/adr/0011-ios-theming.md` exists, status Accepted, dated 2026-05-09.
7. `packages/client-ios/README.md` updated.
8. Branch `phase-2/ios` exists with all your commits, never pushed.

## Reporting back

Your final response must include:

- The branch name (`phase-2/ios`) and the commit SHA range (`<first>..<last>`).
- A list of files created or modified (paths only; the diff is in git).
- **Any deviations from the plan and why** — e.g. "I used `LazyVStack` instead of `VStack` because…". Explicit deviations let the user assess.
- **Any schema gaps you encountered** — case-by-case, with the smallest reproducible description.
- **Anything you're uncertain about** — explicit "I guessed X here, please verify" beats silent guessing.

If you get stuck on something that isn't unambiguously in the plan or ADRs, STOP and report rather than guessing.

## Working directory and git worktree

You're in a worktree of the repo. Treat the worktree as your sandbox: read freely, write only under `packages/client-ios/`, `packages/conformance/cases/valid/*/snapshot.ios.txt`, `docs/adr/0011-ios-theming.md`. When you commit, use `git -C <worktree-path> commit ...` or just `cd` to the worktree.

The worktree is on a fresh branch automatically. Do not rebase, force-push, or merge to main yourself.

## Note on running iOS tests

You may not have a macOS environment available in your worktree. If `swift test` doesn't work locally:
- Build as much as you can without a working test runner.
- Document what you couldn't run.
- The main session will run `swift test` on the merge to main.

The compile must succeed; tests being unrunnable in your environment is not a blocker.
