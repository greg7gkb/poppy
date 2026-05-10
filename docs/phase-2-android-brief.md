# Phase 2 — Android subagent brief

This is the self-contained brief for a `general-purpose` subagent assigned to implement `packages/client-android/` end-to-end. The subagent runs in a `worktree` isolation (own copy of the repo) and reports back when done.

## Goal

Ship `packages/client-android/`: a Kotlin + Jetpack Compose library that renders Poppy v0.1 documents to a native Android UI. By "done," every invalid conformance corpus case produces a validation error matching its declared keyword, and every valid case has a committed `snapshot.android.txt` plus passing behavioral assertions.

## Read first (in this order)

1. `docs/phase-2-plan.md` — the overall Phase 2 plan; **your scope is the `poppy-android` section + cross-cutting items that touch Android**.
2. `docs/adr/0009-mobile-validation-strategy.md` — your validation approach. Decoding *is* the validator; map `kotlinx.serialization` exceptions to corpus keywords. Read the mapping table.
3. `docs/adr/0008-cross-platform-conformance-strategy.md` — the conformance contract. Per-platform snapshots reviewed in PRs; behavioral assertions per platform; shared corpus inputs.
4. `docs/adr/0006-schema-versioning.md` — version compat rule. You implement the same `{ keyword: "version", path: "/version" }` error shape `@poppy/server-ts` emits.
5. `packages/schema/src/types.ts` — TypeScript types you mirror in Kotlin data classes / sealed class.
6. `packages/conformance/cases/` — corpus. Each case has `document.json` + `description.md` (behavioral invariants); valid cases have `snapshot.web.html` (the web reference); invalid cases have `expected-error.json`.
7. `packages/client-web/src/render.ts` — reference renderer. Useful for matching component-rendering semantics.
8. `packages/server-ts/src/validate.ts` — reference validator. Match the error shape exactly.

## Scope

Create the Android Compose renderer at `packages/client-android/`. The exact layout, public API, validation strategy, and theming approach are specified in `docs/phase-2-plan.md` §`poppy-android`. **Treat the plan as the spec.**

In summary:

- Standalone Gradle module (Kotlin DSL, own `settings.gradle.kts`, no parent).
- Kotlin 2.0+ K2 compiler, Compose BOM (latest stable as of 2026), Min API 24 / target 35, Java 17.
- Package namespace: `dev.poppy.android`.
- Layout per plan §`poppy-android` → "Layout".
- `kotlinx.serialization`-driven `@Serializable sealed class Component`. `Json { classDiscriminator = "type"; ignoreUnknownKeys = true }`.
- Validation: typed decode → version-compat check → `ValidationResult.Ok | Failure`. Never throws. Maps decoder exceptions to keywords per ADR-0009.
- Theming: `PoppyTheme` `CompositionLocal`. Defaults match web (md = 16.dp, primary = `Color(0xFF0B66FF)`, etc. — read `packages/client-web/src/styles/poppy.css` for the exact defaults).
- Image loading: bundle Coil (`io.coil-kt:coil-compose`), but route through a `PoppyImageLoader` interface so the dep is swappable via `LocalPoppyImageLoader`. Default impl is `CoilImageLoader`. See plan §"Image loading abstraction".
- Renderer: `@Composable Poppy(document, host, modifier)`. Per-component composables under `components/`.
- Tests:
  - `CorpusTest.kt`: load every valid + invalid case, assert validation outcomes match `expected-error.json` keywords / parse success.
  - `BehaviorTest.kt`: assert the invariants in each valid case's `description.md`. Read each `description.md` to know what to assert.
  - `UpdateSnapshots`: standalone JUnit test or `main()` that regenerates `snapshot.android.txt` for every valid case using `SemanticsNodeInteraction.printToString()` or equivalent. Gated so `./gradlew check` doesn't accidentally rewrite committed snapshots.

Plus:

- A committed `snapshot.android.txt` for each of the 15 valid cases under `packages/conformance/cases/valid/NNN-slug/`.
- `docs/adr/0010-android-theming.md` — new ADR documenting the `CompositionLocal`-based theming + image loader extension points. Status: Accepted. Mirror the ADR template at `docs/adr/0000-template.md` and the style of ADRs 0007–0009.
- Updated `packages/client-android/README.md` to reflect what's actually shipped (the existing one is a Phase 0 placeholder).

## Constraints

- **Do NOT modify**: `packages/schema/`, `packages/conformance/cases/*/document.json`, `packages/conformance/cases/*/description.md`, `packages/conformance/cases/*/expected-error.json`, `packages/conformance/cases/*/snapshot.web.html`, `packages/client-web/`, `packages/server-ts/`, `packages/conformance/src/`. You may read all of them. You add `snapshot.android.txt` files; that's it for the corpus.
- **Do NOT change the JSON Schema or the v0.1 wire format.** If you find a gap (e.g. a case description says X but the schema doesn't allow X), STOP and report. The user decides whether to extend v0.1 or work around.
- **Do NOT add dependencies beyond** what's listed in `docs/phase-2-plan.md` §"Open dep additions" (`kotlinx.serialization`, Compose UI test, Coil). If you discover a need for another dep, STOP and report. Per [ADR-0005](adr/0005-minimize-third-party-dependencies.md), every dep is justified.
- **Do NOT push to origin.** Commit locally to a feature branch named `phase-2/android`. The main session merges later.
- **Do NOT touch iOS work** (`packages/client-ios/`). A separate subagent handles iOS in parallel.
- **Do NOT modify `.github/workflows/ci.yml`.** The `android` job stub is already present; the main session wires up the real `./gradlew check` command after merging your branch.

## Commit hygiene

Use small logically-grouped commits with conventional-commit messages. Suggested split:

1. `feat(android): scaffold Gradle module + version catalog`
2. `feat(android): @Serializable Document hierarchy`
3. `feat(android): validate() with corpus invalid-case parity`
4. `feat(android): PoppyTheme + LocalPoppyImageLoader`
5. `feat(android): render Stack / Text / Image / Button`
6. `test(android): behavioral invariants per ADR-0008 §2`
7. `test(android): snapshot harness + 15 snapshot.android.txt`
8. `docs(adr): ADR-0010 Android theming`
9. `docs(android): per-package README`

Each commit's footer should include:
```
Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

## Done criteria

1. `cd packages/client-android && ./gradlew check` passes from a clean clone.
2. All 7 invalid corpus cases produce a `ValidationError` whose `keyword` matches the case's `expected-error.json`.
3. All 15 valid corpus cases produce a typed `PoppyDocument` and render successfully.
4. Each valid case has a committed `snapshot.android.txt`.
5. `BehaviorTest.kt` asserts the invariants from each `description.md`. (You don't need a separate test method per case; group invariants by kind and parametrize over cases where it makes sense.)
6. `docs/adr/0010-android-theming.md` exists, status Accepted, dated 2026-05-09.
7. `packages/client-android/README.md` updated.
8. Branch `phase-2/android` exists with all your commits, never pushed.

## Reporting back

Your final response must include:

- The branch name (`phase-2/android`) and the commit SHA range (`<first>..<last>`).
- A list of files created or modified (paths only; the diff is in git).
- **Any deviations from the plan and why** — e.g. "I used `Text` instead of `BasicText` because…". Explicit deviations let the user assess.
- **Any schema gaps you encountered** — case-by-case, with the smallest reproducible description.
- **Anything you're uncertain about** — explicit "I guessed X here, please verify" beats silent guessing.

If you get stuck on something that isn't unambiguously in the plan or ADRs, STOP and report rather than guessing.

## Working directory and git worktree

You're in a worktree of the repo. Treat the worktree as your sandbox: read freely, write only under `packages/client-android/`, `packages/conformance/cases/valid/*/snapshot.android.txt`, `docs/adr/0010-android-theming.md`. When you commit, use `git -C <worktree-path> commit ...` or just `cd` to the worktree.

The worktree is on a fresh branch automatically. Do not rebase, force-push, or merge to main yourself.
