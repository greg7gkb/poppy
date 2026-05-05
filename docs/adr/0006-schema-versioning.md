# ADR-0006: Poppy schema versioning

## Status

Accepted — 2026-05-05

## Context

Poppy ships a wire format that multiple independent renderers (Android Compose, iOS SwiftUI, native HTML web, optionally React) consume. The format will evolve over time — new components, new optional fields, breaking renames, deprecations.

We need a versioning scheme that:

1. Lets renderers detect documents they cannot safely render, and refuse explicitly.
2. Lets renderers handle additive forward changes gracefully (a v0.2 document arriving at a v0.1 renderer ignores unknown fields rather than crashing).
3. Distinguishes between *the document's wire-format version* and *the schema package's release version* — they often coincide but must be conceptually independent.
4. Is simple to inspect manually (a human reading a JSON document should identify its version at a glance).
5. Works for the cross-platform reality: Kotlin, Swift, and JS/TS validators must all agree on the rules.

## Decision

### Document version

Every Poppy document carries a top-level `version` field of the form `"MAJOR.MINOR"` — a string, two non-negative integers separated by a dot, no patch component, no leading `v`:

```json
{ "version": "0.1", "root": { ... } }
```

- Required field. Documents without `version` are rejected by validators.
- The two-number form is deliberate: patch-level changes do not happen in a wire format. Bug fixes to the schema package do not change which documents validate.

### Compatibility rules

A renderer that supports up to `MAJOR.MINOR` accepts a document with `MAJOR'.MINOR'` if and only if:

- `MAJOR' === MAJOR` (matching major version), AND
- `MINOR' <= MINOR` (document is the same or older minor than the renderer)

Documents with a different major, or with a higher minor than the renderer supports, are rejected with an explicit error. They are NOT silently best-effort rendered.

### Forward compatibility within a major

Within a major version, only additive changes are allowed:

- Adding a new optional field to an existing component: allowed, no version bump.
- Adding a new component type: allowed, **minor** bump.
- Adding a new value to an existing enum token: minor bump.

Renderers MUST ignore unknown fields on known component types. This lets a v0.2 server-emitted document with a new optional field render correctly on a v0.1 renderer (the field is simply ignored).

Renderers MUST NOT silently render unknown component types. A document containing `"type": "Carousel"` against a renderer that does not know `Carousel` is rejected — silently dropping it would degrade the UI in ways the host cannot detect.

### Breaking changes

A breaking schema change requires a major version bump. Examples:

- Removing or renaming a field.
- Changing a field's type or enum values in a non-additive way.
- Adding a required field to an existing component.
- Removing a component type.
- Tightening validation on an existing field (e.g. adding a stricter `pattern`).

### Deprecation policy

Deprecated fields are marked in the schema description (`"Deprecated since v0.X. Removed in v1.0."`) and continue to validate until the next major. Renderers SHOULD log a warning when encountering deprecated fields.

### Schema package version

The `@poppy/schema` npm package follows standard semver. The package version and the document `version` are tracked separately:

- `@poppy/schema@0.1.0` ships document version `"0.1"`.
- `@poppy/schema@0.1.1` (patch) is a bug fix in the schema files (e.g. fixing a description typo, tightening a description, adding examples) that does not change which documents validate. Document version stays `"0.1"`.
- `@poppy/schema@0.2.0` ships document version `"0.2"` — additive changes since `0.1`.
- `@poppy/schema@1.0.0` ships document version `"1.0"` — first stability commitment.

The two-token alignment (`MAJOR.MINOR` of document equals `MAJOR.MINOR` of package) is convention, not enforcement. Patch versions never appear in the document `version` field.

### `$schema` URL

Documents MAY include a top-level `$schema` field with a URL pointing to the canonical schema for that version, e.g.:

```
https://raw.githubusercontent.com/greg7gkb/poppy/v0.1.0/packages/schema/schemas/poppy.schema.json
```

This is purely an editor/tooling convenience. It is not used by renderers — the `version` field is authoritative.

## Consequences

**Positive**

- Renderers fail loudly on incompatibility instead of silently degrading.
- Forward-compat across minors lets us iterate the schema without breaking deployed renderers.
- Clear separation between "document wire format" and "schema package release" prevents the confusion that creeps in when a single semver drives both.
- Simple for humans to read and reason about. `"0.1"` is a string anyone can interpret.
- The compatibility rule is small enough to test exhaustively in the conformance corpus.

**Negative**

- Two-place version coordination (document `version` + schema package release) is more bookkeeping than a single semver.
- The "additive within a major" rule requires discipline; one accidentally-required field breaks every downstream renderer.
- Renderer authors must implement the comparison rule correctly. We will document it precisely and exercise it in conformance cases.

## Alternatives Considered

- **Full semver in the document** (`"version": "0.1.3"`). Rejected: patch versions in a wire format are meaningless; documents cannot meaningfully be "patch 3 of 0.1".
- **Per-component versioning** (each component carries its own version). Rejected: fragile, hard to reason about, makes documents verbose. Adaptive Cards does property-level version annotations and pays for it (every property carries a version annotation, and clients must parse all of them to know what's safe).
- **Single semver tying document and package together**. Rejected: conflates schema fixes (no behavior change) with format changes; means we must publish a new format version whenever we fix a typo in a description.
- **Numeric version** (`"version": 0.1`). Rejected: floating-point representation of a version is a footgun (`0.10` parses identically to `0.1`).

## References

- [SemVer 2.0.0](https://semver.org/) — the package versioning model.
- [JSON Schema 2020-12](https://json-schema.org/specification.html) — the validation spec.
- ADR-0001 — schema-first contract.
- ADR-0004 — conformance corpus (which will include version-compat cases).
