// Token enums for Poppy v0.1.
//
// These mirror packages/schema/src/types.ts and the JSON Schema files in
// packages/schema/schemas/tokens/. The conformance corpus catches drift.
//
// Each token's raw value matches the wire-format string exactly so Codable's
// synthesised init(from:) handles decoding directly.

import Foundation

public enum Spacing: String, Codable, Sendable, Equatable, Hashable, CaseIterable {
    case none
    case xs
    case sm
    case md
    case lg
    case xl
}

public enum Size: String, Codable, Sendable, Equatable, Hashable, CaseIterable {
    case xs
    case sm
    case md
    case lg
    case xl
}

public enum PoppyColor: String, Codable, Sendable, Equatable, Hashable, CaseIterable {
    case `default`
    case primary
    case secondary
    case danger
    case success
}

public enum Weight: String, Codable, Sendable, Equatable, Hashable, CaseIterable {
    case regular
    case medium
    case bold
}

public enum PoppyAlignment: String, Codable, Sendable, Equatable, Hashable, CaseIterable {
    case start
    case center
    case end
    case stretch
}

public enum Fit: String, Codable, Sendable, Equatable, Hashable, CaseIterable {
    case contain
    case cover
    case fill
}

public enum Axis: String, Codable, Sendable, Equatable, Hashable, CaseIterable {
    case horizontal
    case vertical
}

/// Hint passed to `PoppyHost.isUrlAllowed(_:context:)` so a host can apply
/// different allowlists per use site (image vs. future media types).
public enum ImageContext: String, Sendable, Equatable, Hashable {
    case image
}
