// PoppyTheme: token defaults and EnvironmentValues plumbing.
//
// Per ADR-0011, theming is a SwiftUI EnvironmentValues extension. Defaults
// match the web client's CSS custom properties in
// packages/client-web/src/styles/poppy.css, so the same document looks
// equivalent on web and iOS without per-platform tweaking.
//
// Hosts override the theme via:
//
//     PoppyView(document: doc, host: host)
//         .environment(\.poppyTheme, customTheme)
//
// Components read the theme via @Environment(\.poppyTheme) and resolve
// tokens by name (`theme.spacing(.md)`, `theme.color(.primary)`, etc.) —
// never by hard-coding RGB or pixel values, so behavioral tests can query
// the theme rather than asserting literal values.

import SwiftUI

public struct PoppyTheme: Sendable {
    public var spacingValues: [Spacing: CGFloat]
    public var sizeValues: [Size: CGFloat]
    public var colorValues: [PoppyColor: Color]
    public var weightValues: [Weight: Font.Weight]

    public init(
        spacingValues: [Spacing: CGFloat] = PoppyTheme.defaultSpacingValues,
        sizeValues: [Size: CGFloat] = PoppyTheme.defaultSizeValues,
        colorValues: [PoppyColor: Color] = PoppyTheme.defaultColorValues,
        weightValues: [Weight: Font.Weight] = PoppyTheme.defaultWeightValues
    ) {
        self.spacingValues = spacingValues
        self.sizeValues = sizeValues
        self.colorValues = colorValues
        self.weightValues = weightValues
    }

    public func spacing(_ token: Spacing) -> CGFloat {
        return spacingValues[token] ?? 0
    }

    public func size(_ token: Size) -> CGFloat {
        return sizeValues[token] ?? PoppyTheme.defaultSizeValues[.md]!
    }

    public func color(_ token: PoppyColor) -> Color {
        return colorValues[token] ?? PoppyTheme.defaultColorValues[.default]!
    }

    public func weight(_ token: Weight) -> Font.Weight {
        return weightValues[token] ?? .regular
    }

    // MARK: - Defaults
    //
    // These mirror packages/client-web/src/styles/poppy.css. Any change here
    // must also be reflected on web and Android, and is a Phase 2+ schema /
    // theming-policy decision.

    public static let defaultSpacingValues: [Spacing: CGFloat] = [
        .none: 0,
        .xs: 4,
        .sm: 8,
        .md: 16,
        .lg: 24,
        .xl: 32,
    ]

    public static let defaultSizeValues: [Size: CGFloat] = [
        .xs: 12,
        .sm: 14,
        .md: 16,
        .lg: 20,
        .xl: 28,
    ]

    public static let defaultColorValues: [PoppyColor: Color] = [
        .default: Color(red: 0x11 / 255.0, green: 0x11 / 255.0, blue: 0x11 / 255.0),  // #111
        .primary: Color(red: 0x0B / 255.0, green: 0x66 / 255.0, blue: 0xFF / 255.0),  // #0B66FF
        .secondary: Color(red: 0x6B / 255.0, green: 0x72 / 255.0, blue: 0x80 / 255.0),  // #6B7280
        .danger: Color(red: 0xD4 / 255.0, green: 0x35 / 255.0, blue: 0x1C / 255.0),  // #D4351C
        .success: Color(red: 0x1E / 255.0, green: 0x7C / 255.0, blue: 0x2C / 255.0),  // #1E7C2C
    ]

    public static let defaultWeightValues: [Weight: Font.Weight] = [
        .regular: .regular,
        .medium: .medium,
        .bold: .bold,
    ]

    public static let `default` = PoppyTheme()
}

// MARK: - EnvironmentValues plumbing

private struct PoppyThemeKey: EnvironmentKey {
    static let defaultValue: PoppyTheme = .default
}

public extension EnvironmentValues {
    var poppyTheme: PoppyTheme {
        get { self[PoppyThemeKey.self] }
        set { self[PoppyThemeKey.self] = newValue }
    }
}

// MARK: - SwiftUI conveniences

extension PoppyAlignment {
    /// Map a Poppy alignment token to SwiftUI's HorizontalAlignment for use
    /// inside a vertical (column-axis) Stack.
    var swiftUIHorizontal: HorizontalAlignment {
        switch self {
        case .start: return .leading
        case .center: return .center
        case .end: return .trailing
        case .stretch: return .leading  // SwiftUI lacks a true stretch alignment;
                                        // we approximate via .leading + frame(maxWidth:)
                                        // applied at the child layer.
        }
    }

    /// Map a Poppy alignment token to SwiftUI's VerticalAlignment for use
    /// inside a horizontal (row-axis) Stack.
    var swiftUIVertical: VerticalAlignment {
        switch self {
        case .start: return .top
        case .center: return .center
        case .end: return .bottom
        case .stretch: return .top
        }
    }

    /// Returns true if this alignment requests stretch behavior. Components
    /// use this to decide whether to apply `.frame(maxWidth: .infinity)` /
    /// `.frame(maxHeight: .infinity)` to their children.
    var isStretch: Bool {
        return self == .stretch
    }
}
