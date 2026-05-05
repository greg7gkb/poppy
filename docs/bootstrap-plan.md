# Poppy — Server-Driven UI Library — Project Bootstrap Plan

## Context

Greenfield project at `/Users/gkb/code/sdui` (currently empty). Working name: **`poppy`** (placeholder, easy find-and-replace later). Goal: an **open-source, multi-platform server-driven UI library** in which a server emits a JSON document conforming to a well-defined schema, and four client renderers (Android Compose, iOS SwiftUI, native Web HTML, optionally React) display it identically.

Why this is hard, and why design choices matter early:
- Four independent rendering surfaces will drift unless the contract is rigid and verified.
- The project is open source, so quality, governance, and clarity matter much more than for an internal lib.
- The user wants high documentation and code quality maintained over time.
- Several sub-projects exist (server lib, 4× client libs, conformance harness, sample apps, separate "creator" web app) so sequencing matters.

This plan covers the **bootstrap** (Phase 0): governance, repo skeleton, ADRs, GitHub remote, and a clear roadmap for Phases 1–5. Implementation of Phase 1+ is out of scope here and will get its own plan.

## Architecture (high level)

- **Schema-first contract.** A single JSON Schema is the source of truth. Server validates against it on emit. Each client validates on receive. Documentation is generated from it where possible.
- **Conformance corpus.** Shared bank of `(input JSON → expected render snapshot)` pairs. Web emits HTML; Android/iOS emit semantic-tree snapshots. Every client must pass on every PR. **Bright-line rule: no component lands in any client until it's in the schema and corpus.**
- **Polyglot single monorepo.** One source of truth, easy cross-cutting changes. Each language uses its native tooling (npm/pnpm for TS, Gradle for Kotlin, SwiftPM for Swift) — no Nx/Turborepo lock-in since polyglot makes those awkward.
- **License: Apache 2.0** — the patent grant matters for a UI library.
- **Minimize third-party dependencies.** New runtime dependencies require discussion and an ADR-style note. Aim is a small, auditable surface — important for OSS trust and for keeping clients lean across four languages.

## Sequencing roadmap

- **Phase 0 — Foundations (this plan).** Repo skeleton, governance, ADRs, CI scaffolding stubs, GitHub remote at `greg7gkb/poppy`.
- **Phase 1 — Schema + reference renderer.** Schema v0.1 + TS server lib + native HTML web client + conformance corpus.
- **Phase 2 — Mobile clients (parallelizable).** Android Compose + iOS SwiftUI, both passing the same corpus.
- **Phase 3 — Optional React renderer + Creator web app.** Creator waits for schema stability or it'll be rewritten three times.
- **Phase 4 — Schema iteration.** Theming primitives, slots, async data, deeper action model, accessibility metadata, additional components.
- **Phase 5 — Design system layer (optional).** Token primitives (color/spacing/typography/radii/motion), component variants, light/dark themes, host-app theme injection. Mirrors the design-system layer most companies maintain on top of platform UI kits.

## Phase 1 design choices already locked in

These are decisions made during planning conversation — recorded here so they're not lost. Will be promoted to formal ADRs when Phase 1 starts.

- **Layout primitive: unified `Stack`** with `axis: "horizontal" | "vertical"`. Replaces separate `Row`/`Column`. Maps to `HStack`/`VStack` (SwiftUI), `Row`/`Column` (Compose), `display:flex; flex-direction:row|column` (HTML). Smaller schema, identical expressiveness.
- **Actions are URI-based deep links.** Action shape: `{ "type": "navigate", "uri": "poppy://screen/profile/abc123" }`. The renderer **does not interpret** the URI — it dispatches it to the host app via a callback/listener. The host app owns routing (Android intents, iOS universal links, web router, etc.). This keeps `poppy` platform-agnostic about app navigation and lets adopters keep their existing deep-link infrastructure.

Schema v0.1 element set (target for Phase 1): `Stack`, `Text`, `Image`, `Button`, plus the URI-based `navigate` action.

## Phase 0 Deliverables (this PR)

Files to create in `/Users/gkb/code/sdui`:

**Top-level:**
- `README.md` — project overview, status (alpha, pre-v0.1), repo layout, quickstart placeholder, links to ADRs.
- `LICENSE` — Apache 2.0 full text.
- `NOTICE` — Apache 2.0 attribution stub.
- `CONTRIBUTING.md` — how to contribute, code style, conventional commits, the bright-line rule about schema/corpus, **the minimize-3p-deps policy** (new dep requires PR-level discussion).
- `CODE_OF_CONDUCT.md` — Contributor Covenant 2.1.
- `SECURITY.md` — how to report vulnerabilities.
- `.gitignore` — sensible defaults for TS/Kotlin/Swift/macOS.
- `.editorconfig` — consistent indentation across languages.

**Architecture decision records:**
- `docs/adr/0000-template.md` — ADR template (MADR-style).
- `docs/adr/0001-schema-first-contract.md` — JSON Schema as source of truth.
- `docs/adr/0002-monorepo-structure.md` — single polyglot monorepo over split repos.
- `docs/adr/0003-apache-2-license.md` — Apache 2.0 over MIT/BSD/GPL/MPL, with patent-grant rationale.
- `docs/adr/0004-conformance-corpus.md` — corpus + bright-line rule.
- `docs/adr/0005-minimize-third-party-dependencies.md` — policy + review criteria for new deps.

**Plan reference:**
- `docs/bootstrap-plan.md` — copy of this plan file, kept in repo for posterity.

**Package skeleton (empty dirs, each with a README explaining purpose + current status):**
- `packages/schema/README.md` — JSON Schema definitions; the source of truth.
- `packages/server-ts/README.md` — TypeScript server library.
- `packages/client-web/README.md` — Native HTML/DOM renderer (reference).
- `packages/client-react/README.md` — Optional React renderer (Phase 3).
- `packages/client-android/README.md` — Kotlin + Compose renderer.
- `packages/client-ios/README.md` — Swift + SwiftUI renderer.
- `packages/conformance/README.md` — Shared `(input → expected output)` corpus.
- `packages/creator/README.md` — Web app for designing screens (Phase 3).
- `examples/README.md` — Sample apps using the libraries.

**GitHub:**
- `.github/PULL_REQUEST_TEMPLATE.md` — checklist incl. *"If this adds a component, it's in the schema and corpus."* and *"No new third-party deps without discussion."*
- `.github/ISSUE_TEMPLATE/bug_report.md`, `feature_request.md`.
- `.github/workflows/ci.yml` — minimal stub, expanded in Phase 1.

**Repo init + remote:**
- `git init` and an initial commit on `main`.
- `gh repo create greg7gkb/poppy --public --description "..." --source=. --remote=origin --push`.
- Repo name: **`poppy`** (matches working project name; rename later is straightforward via GitHub).

## Defaults locked in by this plan

| Decision | Default | Rationale |
|---|---|---|
| Working name | `poppy` | Per user direction; placeholder, easy find-and-replace. |
| License | Apache 2.0 | Patent grant matters for a UI lib; corporate-friendly. |
| Repo layout | Single polyglot monorepo | Cross-cutting changes in one PR; one CI; one issue tracker. |
| Build orchestration | Each package uses native tooling (no Nx/Turborepo) | Polyglot makes meta-build-systems awkward. |
| Schema authoring | Raw JSON Schema (Draft 2020-12) | Universally understood; portable to all four targets. |
| Phase 1 server lib | TypeScript | Fastest iteration alongside the web reference renderer. |
| GitHub host | `greg7gkb/poppy`, public | Per user direction. |
| ADR format | MADR-style markdown | Lightweight, well-known. |
| Commit style | Conventional Commits | Enables automated changelog. |
| Code of Conduct | Contributor Covenant 2.1 | Standard. |
| 3p dependency policy | Minimize; new deps require discussion | Per user direction. |

## Critical files to be created

- `/Users/gkb/code/sdui/README.md` — front door.
- `/Users/gkb/code/sdui/docs/adr/0001-schema-first-contract.md` — anchors the design philosophy.
- `/Users/gkb/code/sdui/docs/adr/0003-apache-2-license.md` — license rationale.
- `/Users/gkb/code/sdui/docs/adr/0004-conformance-corpus.md` — anchors the quality model.
- `/Users/gkb/code/sdui/docs/adr/0005-minimize-third-party-dependencies.md` — dep policy.
- `/Users/gkb/code/sdui/CONTRIBUTING.md` — encodes the bright-line rule + dep policy.
- `/Users/gkb/code/sdui/docs/bootstrap-plan.md` — durable copy of this plan.

## Reuse / external references

No internal code yet. External references worth mining when we get to Phase 1:
- **Microsoft Adaptive Cards** (open-source, schema-driven, multi-renderer — closest existing analogue) — `https://adaptivecards.io/`
- **JSON Schema 2020-12** — `https://json-schema.org/`
- **Contributor Covenant 2.1** — `https://www.contributor-covenant.org/`
- **MADR template** — `https://adr.github.io/madr/`
- **Conventional Commits 1.0.0** — `https://www.conventionalcommits.org/`

## Verification

After Phase 0 execution:
1. `ls -la /Users/gkb/code/sdui` shows the file tree above.
2. `cat /Users/gkb/code/sdui/README.md` clearly explains what poppy is, status (alpha), and links to ADRs.
3. The five ADRs each state Decision / Context / Consequences and read coherently as a set.
4. `git log` shows a single clean initial commit on `main`.
5. `git remote -v` shows `origin` pointing to `https://github.com/greg7gkb/poppy.git`.
6. `gh repo view greg7gkb/poppy` shows the public repo populated with the initial commit.
7. `CONTRIBUTING.md` clearly states (a) the schema/corpus bright-line rule and (b) the minimize-3p-deps policy.
8. Each `packages/*/README.md` explains the package's purpose and current status (placeholder, Phase X).
9. `/Users/gkb/code/sdui/docs/bootstrap-plan.md` matches this plan file.

## Out of scope for this plan

- Phase 1+ implementation (schema files, server lib code, web client code, mobile client code, creator app).
- Final project name (we ship Phase 0 with placeholder `poppy`; rename is a future PR).
- Doc-site framework choice (deferred to Phase 1 or later).
- CI implementation beyond a stub.
- Adding any third-party runtime dependencies (Phase 1 will introduce a JSON Schema validator with prior discussion).

## Open items the user may want to revisit before I execute

- Is `poppy` acceptable as both repo name on GitHub and working project name in code? (Defaulted: yes.)
- Repo description text: *"Server-driven UI library for Android Compose, iOS SwiftUI, and the web."* — OK or want to tweak?
- Public visibility from day one, or should I create private and flip to public later? (Defaulted: public from day one per user direction.)
