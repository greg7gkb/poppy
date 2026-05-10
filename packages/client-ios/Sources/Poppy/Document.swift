// Typed representation of a Poppy v0.1 document.
//
// The discriminator on `Component` is the wire-format `type` field, with
// values `Stack`, `Text`, `Image`, `Button`. Action's discriminator is
// lowercase per the schema convention (currently only `navigate`).
//
// Custom Codable on `Component` and `Action` is required because Swift's
// synthesised Codable cannot dispatch on a string discriminator. The
// implementation maps decoder failures to ValidationError keywords:
//
//   - Missing or unknown `type` value      → DecodingError.dataCorrupted
//                                            (Validate.swift maps to "discriminator")
//   - Missing required field on a known type → DecodingError.keyNotFound
//                                              (maps to "required")
//   - Wrong JSON type for a known field    → DecodingError.typeMismatch
//                                            (maps to "type")
//
// See ADR-0009 for the validation strategy and mapping rules.

import Foundation

public struct PoppyDocument: Codable, Sendable, Equatable {
    /// Optional canonical schema URL. Editors use this for autocomplete; renderers ignore it.
    public let schemaURL: String?
    /// Wire-format version, MAJOR.MINOR. Required.
    public let version: String
    /// The single root component.
    public let root: Component

    enum CodingKeys: String, CodingKey {
        case schemaURL = "$schema"
        case version
        case root
    }

    public init(version: String, root: Component, schemaURL: String? = nil) {
        self.version = version
        self.root = root
        self.schemaURL = schemaURL
    }
}

// MARK: - Components

public enum Component: Codable, Sendable, Equatable {
    case stack(Stack)
    case text(Text)
    case image(Image)
    case button(Button)

    /// Raw discriminator value, useful for tests / debugging.
    public var typeName: String {
        switch self {
        case .stack: return "Stack"
        case .text: return "Text"
        case .image: return "Image"
        case .button: return "Button"
        }
    }

    /// Optional `id` of the component (every component carries an optional id).
    public var id: String? {
        switch self {
        case .stack(let s): return s.id
        case .text(let t): return t.id
        case .image(let i): return i.id
        case .button(let b): return b.id
        }
    }

    private enum DiscriminatorKey: String, CodingKey {
        case type
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DiscriminatorKey.self)
        // A missing `type` field is just as much a discriminator failure as an
        // unknown one — the corpus's `001-missing-type` case requires keyword
        // "discriminator" for `{ "value": "Hello" }` (no type).
        let typeString: String
        do {
            typeString = try container.decode(String.self, forKey: .type)
        } catch {
            throw DecodingError.dataCorruptedError(
                forKey: .type,
                in: container,
                debugDescription: "missing or non-string `type` discriminator on component"
            )
        }
        // Decode the rest of the keys against the appropriate concrete struct.
        // Each struct's synthesised Codable handles missing/wrong-typed fields.
        let single = try decoder.singleValueContainer()
        switch typeString {
        case "Stack":
            self = .stack(try single.decode(Stack.self))
        case "Text":
            self = .text(try single.decode(Text.self))
        case "Image":
            self = .image(try single.decode(Image.self))
        case "Button":
            self = .button(try single.decode(Button.self))
        default:
            throw DecodingError.dataCorruptedError(
                forKey: .type,
                in: container,
                debugDescription: "unknown component type \"\(typeString)\""
            )
        }
    }

    /// Encoding is intentionally lossy with respect to the `type` discriminator
    /// — the renderer is decode-only by design (the server emits documents).
    /// We provide a best-effort encoder so Codable conformance is symmetric,
    /// but the round-trip output omits the `type` field. If the host needs a
    /// fully round-trippable form, treat the original JSON `Data` as the
    /// canonical representation. See ADR-0009.
    public func encode(to encoder: Encoder) throws {
        var single = encoder.singleValueContainer()
        switch self {
        case .stack(let s): try single.encode(s)
        case .text(let t): try single.encode(t)
        case .image(let i): try single.encode(i)
        case .button(let b): try single.encode(b)
        }
    }
}

public struct Stack: Codable, Sendable, Equatable {
    public let id: String?
    public let axis: Axis
    public let children: [Component]
    public let spacing: Spacing?
    public let padding: Spacing?
    public let alignment: PoppyAlignment?

    /// Synthesised Codable would also try to decode the discriminator `type`
    /// field. That's harmless on decode (we drop it), but listing the keys
    /// explicitly documents the wire shape.
    enum CodingKeys: String, CodingKey {
        case id, axis, children, spacing, padding, alignment
    }

    public init(
        id: String? = nil,
        axis: Axis,
        children: [Component],
        spacing: Spacing? = nil,
        padding: Spacing? = nil,
        alignment: PoppyAlignment? = nil
    ) {
        self.id = id
        self.axis = axis
        self.children = children
        self.spacing = spacing
        self.padding = padding
        self.alignment = alignment
    }
}

public struct Text: Codable, Sendable, Equatable {
    public let id: String?
    public let value: String
    public let color: PoppyColor?
    public let size: Size?
    public let weight: Weight?

    enum CodingKeys: String, CodingKey {
        case id, value, color, size, weight
    }

    public init(
        id: String? = nil,
        value: String,
        color: PoppyColor? = nil,
        size: Size? = nil,
        weight: Weight? = nil
    ) {
        self.id = id
        self.value = value
        self.color = color
        self.size = size
        self.weight = weight
    }
}

public struct Image: Codable, Sendable, Equatable {
    public let id: String?
    public let url: String
    public let alt: String
    public let width: Double?
    public let height: Double?
    public let fit: Fit?

    enum CodingKeys: String, CodingKey {
        case id, url, alt, width, height, fit
    }

    public init(
        id: String? = nil,
        url: String,
        alt: String,
        width: Double? = nil,
        height: Double? = nil,
        fit: Fit? = nil
    ) {
        self.id = id
        self.url = url
        self.alt = alt
        self.width = width
        self.height = height
        self.fit = fit
    }
}

public struct Button: Codable, Sendable, Equatable {
    public let id: String?
    public let label: String
    public let action: Action

    enum CodingKeys: String, CodingKey {
        case id, label, action
    }

    public init(id: String? = nil, label: String, action: Action) {
        self.id = id
        self.label = label
        self.action = action
    }
}

// MARK: - Actions

public enum Action: Codable, Sendable, Equatable {
    case navigate(NavigateAction)

    public var typeName: String {
        switch self {
        case .navigate: return "navigate"
        }
    }

    private enum DiscriminatorKey: String, CodingKey {
        case type
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DiscriminatorKey.self)
        let typeString: String
        do {
            typeString = try container.decode(String.self, forKey: .type)
        } catch {
            throw DecodingError.dataCorruptedError(
                forKey: .type,
                in: container,
                debugDescription: "missing or non-string `type` discriminator on action"
            )
        }
        let single = try decoder.singleValueContainer()
        switch typeString {
        case "navigate":
            self = .navigate(try single.decode(NavigateAction.self))
        default:
            throw DecodingError.dataCorruptedError(
                forKey: .type,
                in: container,
                debugDescription: "unknown action type \"\(typeString)\""
            )
        }
    }

    /// Lossy with respect to the `type` discriminator — see Component.encode(to:).
    public func encode(to encoder: Encoder) throws {
        var single = encoder.singleValueContainer()
        switch self {
        case .navigate(let n): try single.encode(n)
        }
    }
}

public struct NavigateAction: Codable, Sendable, Equatable {
    public let uri: String

    enum CodingKeys: String, CodingKey {
        case uri
    }

    public init(uri: String) {
        self.uri = uri
    }
}
