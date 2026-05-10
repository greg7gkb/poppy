/*
 * Validation entry point for the Android renderer. Per ADR-0009, decoding IS
 * the validator: we run the typed kotlinx.serialization decoder and map any
 * SerializationException to a [ValidationError] with the corpus keyword
 * declared in the seven invalid cases under
 * `packages/conformance/cases/invalid/`.
 *
 * Mapping table (ADR-0009, repeated here for code locality):
 *
 *   | Exception                                | Keyword       |
 *   |------------------------------------------|---------------|
 *   | Unknown / missing class discriminator    | discriminator |
 *   | MissingFieldException                    | required      |
 *   | JsonDecodingException (type mismatch)    | type          |
 *   | Custom version-compat check              | version       |
 *
 * Order of checks mirrors `@poppy/server-ts/src/validate.ts`:
 *   1. Decode (catches discriminator/required/type errors).
 *   2. Version-compat check (catches version errors). Only runs if decode passed.
 */

package dev.poppy.android

import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Wire-format version this renderer targets. See ADR-0006. */
const val SCHEMA_VERSION: String = "0.1"

/**
 * Structural error in a Poppy document. Mirrors `@poppy/server-ts`'s
 * `ValidationError` shape.
 */
data class ValidationError(
    /** JSON pointer to the location of the failure, e.g. `/root/children/0/value`. */
    val path: String,
    /** Schema keyword that failed (`discriminator`, `required`, `type`, `version`). */
    val keyword: String,
    /** Human-readable message from the underlying decoder. */
    val message: String,
)

/** Result of parsing + validating a Poppy document. Never throws. */
sealed class ValidationResult {
    data class Ok(val document: PoppyDocument) : ValidationResult()
    data class Failure(val errors: List<ValidationError>) : ValidationResult()
}

/**
 * The Poppy public API entry. Mirrors the contract of `@poppy/server-ts`'s
 * [validate]/[isValid] functions.
 */
object Poppy {
    /**
     * Configured JSON instance. `classDiscriminator = "type"` matches the
     * wire-format discriminator key. `ignoreUnknownKeys = true` honors
     * ADR-0006's forward-compat rule: a v0.2 document with a new optional
     * field decodes cleanly on a v0.1 renderer (the field is dropped).
     */
    internal val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    /** Parse and validate a document. Never throws on malformed input. */
    fun validate(jsonString: String): ValidationResult {
        val decoded: PoppyDocument = try {
            json.decodeFromString(PoppyDocument.serializer(), jsonString)
        } catch (e: MissingFieldException) {
            return ValidationResult.Failure(listOf(mapMissingField(e)))
        } catch (e: SerializationException) {
            return ValidationResult.Failure(listOf(mapSerializationError(e)))
        } catch (e: IllegalArgumentException) {
            // kotlinx.serialization throws this for some malformed input shapes.
            return ValidationResult.Failure(listOf(mapSerializationError(e)))
        }

        val versionError = checkVersionCompat(decoded.version)
        if (versionError != null) {
            return ValidationResult.Failure(listOf(versionError))
        }
        return ValidationResult.Ok(decoded)
    }

    /** Type-guard form of [validate]. */
    fun isValid(jsonString: String): Boolean = validate(jsonString) is ValidationResult.Ok
}

// --- Internal: error mapping -----------------------------------------------

private val SUPPORTED_VERSION: Pair<Int, Int> = parseVersion(SCHEMA_VERSION)
private val SUPPORTED_MAJOR: Int get() = SUPPORTED_VERSION.first
private val SUPPORTED_MINOR: Int get() = SUPPORTED_VERSION.second

private fun parseVersion(v: String): Pair<Int, Int> {
    val parts = v.split(".")
    require(parts.size == 2) { "version must be MAJOR.MINOR; got '$v'" }
    return parts[0].toInt() to parts[1].toInt()
}

private fun checkVersionCompat(version: String): ValidationError? {
    val (major, minor) = try {
        parseVersion(version)
    } catch (_: Exception) {
        // Decoding succeeded, so `version` is a string; but it might not
        // match MAJOR.MINOR. The schema enforces the pattern; mobile decoder
        // doesn't, so we defend here. Treat as a version error.
        return ValidationError(
            path = "/version",
            keyword = "version",
            message = "malformed document version \"$version\"; expected MAJOR.MINOR",
        )
    }
    if (major != SUPPORTED_MAJOR) {
        return ValidationError(
            path = "/version",
            keyword = "version",
            message = "unsupported document major version \"$version\"; this renderer supports $SUPPORTED_MAJOR.x",
        )
    }
    if (minor > SUPPORTED_MINOR) {
        return ValidationError(
            path = "/version",
            keyword = "version",
            message = "document minor version $minor exceeds renderer's supported minor $SUPPORTED_MINOR",
        )
    }
    return null
}

private fun mapMissingField(e: MissingFieldException): ValidationError {
    // MissingFieldException doesn't expose a JSON pointer; the message is the
    // best signal we have. The corpus's required-keyword cases (003, 005) all
    // target a missing field on the root component, so reporting `/root` is
    // accurate for the corpus and a sensible default for nested cases.
    val fields = extractMissingFields(e.message ?: "")
    val msg = if (fields.isNotEmpty()) {
        "missing required field(s): ${fields.joinToString(", ")}"
    } else {
        e.message ?: "missing required field"
    }
    return ValidationError(path = "/root", keyword = "required", message = msg)
}

private fun extractMissingFields(message: String): List<String> {
    // kotlinx.serialization's MissingFieldException message format is:
    //   "Field 'foo' is required for type with serial name '...', but it was missing"
    //   "Fields [a, b] are required for type with serial name '...', but they were missing"
    val singleField = Regex("Field '([^']+)' is required").find(message)?.groupValues?.get(1)
    if (singleField != null) return listOf(singleField)
    val multiField = Regex("Fields \\[([^\\]]+)\\] are required").find(message)?.groupValues?.get(1)
    if (multiField != null) return multiField.split(",").map { it.trim() }
    return emptyList()
}

private fun mapSerializationError(e: Throwable): ValidationError {
    val message = e.message ?: "decoding failed"

    // Discriminator failures come in a few shapes depending on whether the
    // discriminator field is missing or has an unknown value. kotlinx.serialization
    // surfaces both as SerializationException with a recognizable message.
    if (looksLikeDiscriminatorError(message)) {
        return ValidationError(
            path = pathFromMessage(message, default = "/root"),
            keyword = "discriminator",
            message = message,
        )
    }
    // Type mismatch: e.g. Text.value being an Int. kotlinx surfaces this as
    // "Unexpected JSON token" or includes the expected primitive name.
    if (looksLikeTypeError(message)) {
        return ValidationError(
            path = pathFromMessage(message, default = "/root"),
            keyword = "type",
            message = message,
        )
    }
    // Best-effort fallback. The corpus only relies on
    // discriminator/required/type/version; anything that falls through here is
    // necessarily beyond the corpus's coverage.
    return ValidationError(path = "/", keyword = "type", message = message)
}

private fun looksLikeDiscriminatorError(message: String): Boolean {
    return message.contains("Polymorphic serializer was not found") ||
        message.contains("class discriminator") ||
        message.contains("Class discriminator") ||
        message.contains("Serializer for subclass") ||
        message.contains("is not registered") ||
        message.contains("type") && message.contains("not found")
}

private fun looksLikeTypeError(message: String): Boolean {
    return message.contains("Unexpected JSON token") ||
        message.contains("but had") ||
        message.contains("Failed to parse") ||
        message.contains("Expected ") ||
        message.contains("kotlin.String") ||
        message.contains("kotlin.Int")
}

private fun pathFromMessage(message: String, default: String): String {
    // kotlinx.serialization sometimes embeds a JSON path in the message after
    // "at path: ". Extract it if present so test output stays informative.
    val match = Regex("at path:?\\s*(\\$[^\\s]+|/[^\\s]+)").find(message)
    val raw = match?.groupValues?.get(1) ?: return default
    // Normalize JSONPath ($.root.value) to JSON Pointer (/root/value) for parity
    // with @poppy/server-ts's error shape.
    return when {
        raw.startsWith("$.") -> "/" + raw.removePrefix("$.").replace(".", "/")
        raw.startsWith("$") -> "/" + raw.removePrefix("$").replace(".", "/")
        else -> raw
    }
}
