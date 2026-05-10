// Validate Poppy v0.1 documents.
//
// Per ADR-0009, decoding *is* the validator on iOS. The typed Codable tree
// (see Document.swift) catches missing required fields, wrong-typed fields,
// and missing/unknown component discriminators natively. After a successful
// decode, a hand-written version-compat check (per ADR-0006) rejects
// documents whose major doesn't match SUPPORTED_MAJOR or whose minor exceeds
// SUPPORTED_MINOR. Together these cover every keyword in the corpus.
//
// Maps DecodingError → ValidationError keyword:
//
//   .keyNotFound          → "required"
//   .typeMismatch         → "type"
//   .dataCorrupted        → "discriminator" (our custom Component/Action throw it
//                           for missing/unknown `type`)
//   .valueNotFound        → "required" (treats null on a non-optional field
//                           as effectively a missing field)
//   anything else          → "type" (a defensive fallback; all kinds we expect
//                           to see in the corpus are explicitly handled above)
//
// The path is built from the DecodingError's coding-key chain, prefixed with
// "/" — matching the JSON-pointer style @poppy/server-ts emits.

import Foundation

/// Wire-format version this build of Poppy supports. Mirrors `SCHEMA_VERSION`
/// in packages/schema/src/types.ts.
public let SCHEMA_VERSION: String = "0.1"

public struct ValidationError: Error, Sendable, Equatable {
    /// JSON pointer to the location of the failure, e.g. "/root/children/0/value".
    public let path: String
    /// The conformance keyword that failed: "discriminator", "required", "type", "version".
    public let keyword: String
    /// Human-readable message from the underlying decoder, lightly cleaned.
    public let message: String

    public init(path: String, keyword: String, message: String) {
        self.path = path
        self.keyword = keyword
        self.message = message
    }
}

public enum ValidationResult: Sendable {
    case ok(PoppyDocument)
    case failure([ValidationError])

    public var isOk: Bool {
        if case .ok = self { return true }
        return false
    }

    public var document: PoppyDocument? {
        if case .ok(let d) = self { return d }
        return nil
    }

    public var errors: [ValidationError] {
        if case .failure(let e) = self { return e }
        return []
    }
}

/// Top-level entry point for the renderer's validation. Mirrors the public
/// API in @poppy/server-ts. Never throws on user input.
///
/// Order of checks:
///   1. Decode JSON → typed tree. On failure, map the DecodingError to a
///      ValidationError with the appropriate keyword and return Failure.
///   2. If decoding succeeded, run the version-compat check. If incompatible,
///      return Failure with a single version error.
///   3. Otherwise return Ok(document).
public enum Poppy {
    public static func validate(_ json: Data) -> ValidationResult {
        let decoder = JSONDecoder()
        let document: PoppyDocument
        do {
            document = try decoder.decode(PoppyDocument.self, from: json)
        } catch let decodingError as DecodingError {
            return .failure([mapDecodingError(decodingError)])
        } catch {
            return .failure([
                ValidationError(
                    path: "/",
                    keyword: "type",
                    message: "JSON parse failure: \(error.localizedDescription)"
                ),
            ])
        }

        if let versionError = checkVersionCompat(document.version) {
            return .failure([versionError])
        }

        return .ok(document)
    }

    /// Type-guard form. Returns true iff `validate(json)` is `.ok`.
    public static func isValid(_ json: Data) -> Bool {
        return validate(json).isOk
    }
}

// MARK: - Internals

/// Map a DecodingError to a ValidationError. Public for unit tests; not
/// part of the renderer's stable API.
public func mapDecodingError(_ error: DecodingError) -> ValidationError {
    switch error {
    case .keyNotFound(let key, let context):
        let path = jsonPointer(from: context.codingPath)
        return ValidationError(
            path: path,
            keyword: "required",
            message: "missing required field \"\(key.stringValue)\" at \(path.isEmpty ? "/" : path)"
        )
    case .typeMismatch(_, let context):
        let path = jsonPointer(from: context.codingPath)
        return ValidationError(
            path: path.isEmpty ? "/" : path,
            keyword: "type",
            message: context.debugDescription
        )
    case .dataCorrupted(let context):
        // Our custom Component/Action init throws .dataCorrupted for missing
        // or unknown `type`. The codingPath ends at the `type` key in that
        // case, so we trim it back to the parent component object's path —
        // which is what the corpus expected-error.json files reference.
        let path = jsonPointer(from: trimmingTrailingTypeKey(context.codingPath))
        return ValidationError(
            path: path.isEmpty ? "/" : path,
            keyword: "discriminator",
            message: context.debugDescription
        )
    case .valueNotFound(_, let context):
        let path = jsonPointer(from: context.codingPath)
        return ValidationError(
            path: path.isEmpty ? "/" : path,
            keyword: "required",
            message: context.debugDescription
        )
    @unknown default:
        return ValidationError(
            path: "/",
            keyword: "type",
            message: "unexpected DecodingError: \(error)"
        )
    }
}

/// Run the version-compat check. Returns nil on compatible, a populated
/// ValidationError on incompatible. Mirrors @poppy/server-ts's checkVersionCompat.
public func checkVersionCompat(_ version: String) -> ValidationError? {
    let parts = version.split(separator: ".")
    guard parts.count == 2,
          let major = Int(parts[0]),
          let minor = Int(parts[1]) else {
        // The Codable layer doesn't enforce the MAJOR.MINOR pattern (the JSON
        // Schema does), so we get here for malformed strings like "v1" or
        // "1.0.0". Treat as a version error (the only kind that touches
        // /version in the corpus).
        return ValidationError(
            path: "/version",
            keyword: "version",
            message: "malformed document version \"\(version)\"; expected MAJOR.MINOR"
        )
    }

    let supported = parseSupportedVersion()
    if major != supported.major {
        return ValidationError(
            path: "/version",
            keyword: "version",
            message: "unsupported document major version \"\(version)\"; this renderer supports \(supported.major).x"
        )
    }
    if minor > supported.minor {
        return ValidationError(
            path: "/version",
            keyword: "version",
            message: "document minor version \(minor) exceeds renderer's supported minor \(supported.minor)"
        )
    }
    return nil
}

private func parseSupportedVersion() -> (major: Int, minor: Int) {
    let parts = SCHEMA_VERSION.split(separator: ".")
    let major = parts.count > 0 ? Int(parts[0]) ?? 0 : 0
    let minor = parts.count > 1 ? Int(parts[1]) ?? 0 : 0
    return (major, minor)
}

/// Build a JSON pointer from a chain of CodingKey. Numeric coding keys (array
/// indexes) are kept as their integer; everything else as the stringValue.
/// Empty input yields the empty string (the caller substitutes "/" if needed).
private func jsonPointer(from codingPath: [CodingKey]) -> String {
    if codingPath.isEmpty { return "" }
    var parts: [String] = []
    for key in codingPath {
        if let intValue = key.intValue {
            parts.append("\(intValue)")
        } else {
            parts.append(escapePointerToken(key.stringValue))
        }
    }
    return "/" + parts.joined(separator: "/")
}

/// Escape `~` and `/` per RFC 6901.
private func escapePointerToken(_ token: String) -> String {
    return token
        .replacingOccurrences(of: "~", with: "~0")
        .replacingOccurrences(of: "/", with: "~1")
}

/// Drop a trailing `type` key from a coding-path. Used for discriminator
/// errors so the path points to the component object, not its `type` field —
/// matching the corpus convention.
private func trimmingTrailingTypeKey(_ codingPath: [CodingKey]) -> [CodingKey] {
    if let last = codingPath.last, last.stringValue == "type" {
        return Array(codingPath.dropLast())
    }
    return codingPath
}
