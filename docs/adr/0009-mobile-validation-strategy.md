# ADR-0009: Mobile renderer validation strategy

## Status

Accepted — 2026-05-09

## Context

`@poppy/server-ts` validates documents using a precompiled JSON Schema validator (Ajv standalone, see [ADR-0007](0007-ajv-standalone-precompile.md)). The JSON Schema files in `@poppy/schema/` are the source of truth, and the Ajv-compiled artifact is what runs at request time.

The Phase 2 mobile renderers — `poppy-android` (Kotlin + Compose) and `poppy-ios` (Swift + SwiftUI) — also need to validate documents. The conformance corpus has 7 invalid cases, and per [ADR-0008](0008-cross-platform-conformance-strategy.md) §1, every renderer must reject them with the same error keyword (`discriminator`, `required`, `type`, `version`).

The natural question is whether mobile renderers should **port the JSON Schema validation** (e.g. via `medeia-validator` for Kotlin, a Swift JSON Schema lib) or **rely on the platform's typed JSON decoder** (`kotlinx.serialization` on Android, `Codable` on iOS) to enforce the schema by virtue of the type system.

Both approaches have precedent. Adaptive Cards uses platform-native typed decoders; OpenAPI's mobile codegen tools tend to ship runtime schema validators. The choice shapes how we write code, what dependencies we add, and how we map errors to the corpus keywords.

## Decision

Mobile renderers use **typed JSON decoding as the primary validation mechanism**, augmented by a hand-written **version-compatibility check** (per [ADR-0006](0006-schema-versioning.md)). No JSON Schema validator runs on mobile.

### Rationale

1. **The schema *is* the type system, twice over.** The TypeScript types in `@poppy/schema/src/types.ts` and the JSON Schema files describe the same shape. Mobile data classes (Kotlin) and structs/enums (Swift) are a third equivalent expression of the same shape. Keeping all four in lockstep is a maintenance cost we pay regardless; running a JSON Schema validator on mobile would be a fifth artifact to keep aligned.

2. **Typed decoders catch the same errors at the same layer.** The seven invalid corpus cases probe four error kinds: missing required field, wrong field type, unknown discriminator, unknown major version. Typed JSON decoders detect the first three natively, with explicit error types we can map cleanly. Version compat is one comparison after a successful decode.

3. **Native error messages.** A `kotlinx.serialization.MissingFieldException("alt")` localizes naturally; a JSON Schema error like `"keyword: required, params: { missingProperty: 'alt' }"` is a foreign idiom on Android. iOS gets the same benefit with `DecodingError.keyNotFound`.

4. **No new heavy deps.** Schema validators on JVM (medeia-validator, jsonschemafriend) and Swift (Sourcery-driven validators, Yams + JSONSchema) all ship significant runtime payloads. Skipping them aligns with [ADR-0005](0005-minimize-third-party-dependencies.md). `kotlinx.serialization` is already justified for decoding regardless; `Codable` is stdlib.

5. **Ergonomics.** A consumer of `poppy-android` ends up with `PoppyDocument` as a typed value, ready to render. They don't need to understand JSON Schema vocabulary or parse error keywords — they get an exhaustive `when` on `Component` and the compiler enforces the rest.

### What this means in practice

#### Android

Document tree is a `@Serializable sealed class`:

```kotlin
@Serializable
sealed class Component {
    @Serializable @SerialName("Stack")  data class Stack(...)  : Component()
    @Serializable @SerialName("Text")   data class Text(...)   : Component()
    @Serializable @SerialName("Image")  data class Image(...)  : Component()
    @Serializable @SerialName("Button") data class Button(...) : Component()
}
```

Decoded with `Json { classDiscriminator = "type"; ignoreUnknownKeys = true }`. Each `kotlinx.serialization` exception maps to a corpus keyword:

| Exception | Maps to keyword | Example trigger |
|---|---|---|
| `SerializationException("Polymorphic serializer was not found for class discriminator ...")` | `discriminator` | `{ "type": "Heading" }` (unknown) or `{}` (missing) |
| `MissingFieldException` | `required` | Image without `alt` |
| `JsonDecodingException` (type mismatch) | `type` | `Text.value: 42` |
| Custom version-compat | `version` | `version: "999.0"` or `version: "0.99"` |

#### iOS

Document tree is an enum + structs:

```swift
public enum Component: Codable {
    case stack(Stack), text(Text), image(Image), button(Button)
    public init(from decoder: Decoder) throws { /* switch on "type" discriminator */ }
}
```

Custom `Codable.init(from:)` switches on the `type` field. Standard `Codable` machinery handles missing/wrong-typed fields:

| `DecodingError` case | Maps to keyword | Example trigger |
|---|---|---|
| `dataCorrupted` (custom: unknown type) | `discriminator` | `{ "type": "Heading" }` |
| `keyNotFound` | `required` | Image without `alt` |
| `typeMismatch` | `type` | `Text.value: 42` |
| Custom version-compat | `version` | `version: "999.0"` or `version: "0.99"` |

#### Public API mirrors `@poppy/server-ts`

```kotlin
// Kotlin
sealed class ValidationResult {
    data class Ok(val document: PoppyDocument) : ValidationResult()
    data class Failure(val errors: List<ValidationError>) : ValidationResult()
}

object Poppy {
    fun validate(json: String): ValidationResult     // never throws
    fun isValid(json: String): Boolean
}
```

```swift
// Swift
public enum ValidationResult {
    case ok(PoppyDocument)
    case failure([ValidationError])
}

public enum Poppy {
    public static func validate(_ json: Data) -> ValidationResult   // never throws
    public static func isValid(_ json: Data) -> Bool
}
```

Both follow the same contract as the TS validator: discriminated result, exhaustive error list, never throws on user input.

### Order of checks

1. Run the typed decoder. If it throws, map the exception to a `ValidationError` with the appropriate keyword and return `Failure`.
2. If decoding succeeded, run the version-compat check (string-parse the `version` field, compare against `SUPPORTED_MAJOR`/`SUPPORTED_MINOR`). If incompatible, return `Failure([versionError])`.
3. Otherwise return `Ok(document)`.

This matches the order in `@poppy/server-ts/src/validate.ts`: decode/schema first, version compat second, with version-compat being the only check that runs *after* successful structural validation.

### What we sacrifice

- **No keyword-by-keyword error parity with server-ts.** If the JSON Schema gains a `pattern: "^[a-z]+$"` constraint on some field, mobile decoders won't natively enforce it — we'd need to add an explicit post-decode check on each platform. So far the schema doesn't use such keywords; v0.1 stays simple.
- **No JSON-pointer-style paths in errors automatically.** `MissingFieldException` reports `"alt"` not `"/root/alt"`. The error mapper hand-constructs paths from the decoder's context. Acceptable for a small set of components; we accept the cost.
- **Mobile and server validators can drift in edge cases.** A document that the Ajv validator accepts could in principle be rejected by a mobile decoder (or vice versa). The conformance corpus's invalid cases are the regression surface — adding a new invalid case requires implementing it on all three platforms.

## Consequences

**Positive**

- Mobile validation is essentially free: `kotlinx.serialization` and `Codable` are already needed for decoding the document into a typed tree. We're not adding a separate validation layer.
- No JSON Schema runtime dep on mobile. Smaller binary, fewer transitive deps.
- Errors are platform-idiomatic. Test code reads naturally: `assertFailsWith<MissingFieldException>` on Android, `XCTAssertThrowsError(...) { error in case DecodingError.keyNotFound = error }` on iOS.
- The corpus's invalid cases drive the implementation: each platform's decoder must produce the declared keyword for each case. A failure on one platform but not another is caught immediately by CI.

**Negative**

- Schema features that aren't expressible as types (regex patterns, `minLength`, custom formats) require explicit per-platform checks if we add them. The current schema sticks to type-shaped constraints; this is a v0.2+ concern.
- Three-way (or four-way, with React in Phase 3) drift risk. Mitigated by the corpus.
- The error-keyword mapping is hand-written per platform. Each new keyword the corpus relies on requires updating two mappers. The set is small (currently 4: `discriminator`, `required`, `type`, `version`); we revisit if it grows past ~10.

## Alternatives Considered

- **Port the Ajv-compiled validator to JVM/Swift.** Rejected: Ajv outputs JS; no equivalent precompile pipeline exists for JVM/Swift. We'd need a parallel implementation in each language's schema vocabulary.
- **Use a runtime JSON Schema validator on each mobile platform** (e.g. `medeia-validator`, `jsonschemafriend` on JVM; `JSONSchema` on Swift). Rejected: introduces a runtime dep that does work the typed decoder already does. Error messages are non-native. Bigger binary.
- **Skip validation on mobile entirely, trust server-ts.** Rejected: this assumes documents always come from a Poppy server (not always true — a document could be cached, hand-edited, A/B-tested by a third party). Even when the server is trusted, defending the renderer against malformed input is cheap and worthwhile.
- **Decode to a generic JSON tree first, then validate against schema, then map to typed structs.** Rejected: three-pass when one pass works. The typed decoder *is* the validator.

## Implementation notes for Phase 2 subagents

The Android and iOS subagents implementing this should:

1. **Land all 7 invalid corpus cases first** — `001-missing-type` through `007-future-minor-version`. Wire the test harness so each invalid case calls `validate()`, asserts the result is `Failure`, asserts at least one error has the keyword declared in `expected-error.json`. Test fails loudly if any case doesn't produce its declared keyword.
2. **Hand-build the error mapper** — a small `mapDecodingError(throwable, path)` function that switches on the platform's exception types and emits `ValidationError(path, keyword, message)`. ~30–60 LOC per platform.
3. **Hand-build the version-compat check** — `checkVersionCompat(version: String): ValidationError?`, identical in spirit to `@poppy/server-ts/src/validate.ts`. Runs after successful decode.
4. **Test the validator before writing the renderer.** A green corpus invalid suite proves the validator is sound; the renderer can then be implemented against `Ok(document)` results without worrying about malformed input.

## References

- [ADR-0001 — Schema-first contract](0001-schema-first-contract.md)
- [ADR-0005 — Minimize third-party dependencies](0005-minimize-third-party-dependencies.md)
- [ADR-0006 — Schema versioning](0006-schema-versioning.md)
- [ADR-0007 — Ajv standalone precompile](0007-ajv-standalone-precompile.md) — the server-side approach this complements.
- [ADR-0008 — Cross-platform conformance strategy](0008-cross-platform-conformance-strategy.md) — defines what "validate" means cross-platform.
- [kotlinx.serialization polymorphism with sealed classes](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md)
- [Swift Codable with discriminated unions](https://www.swiftbysundell.com/articles/codable-synthesis-for-swift-enums/)
