// Render a Stack — the only container component in v0.1. Maps `axis` to
// SwiftUI's HStack/VStack, applies spacing and padding through the theme,
// and honors the alignment token.
//
// Behavioral invariants tested in BehaviorTests.swift:
//
//   - axis: vertical -> VStack (children top-to-bottom)
//   - axis: horizontal -> HStack (children left-to-right in LTR)
//   - spacing token resolves through theme.spacing(_:); never hard-coded
//   - padding token resolves through theme.spacing(_:); never hard-coded
//   - id, when present, is applied as accessibilityIdentifier so tests can
//     find the stack and so platform a11y exposes a stable identifier
//     (mirrors the web renderer's `id` attribute and the kitchen-sink
//     case's "screen-root" expectation)

import SwiftUI

struct PoppyStack: View {
    let stack: Stack
    let host: PoppyHost

    @Environment(\.poppyTheme) private var theme

    var body: some View {
        let spacingValue = theme.spacing(stack.spacing ?? .none)
        let paddingValue = theme.spacing(stack.padding ?? .none)
        let alignment = stack.alignment ?? .start

        // Apply the .padding modifier directly to the VStack / HStack rather
        // than to an outer Group. This keeps the padding modifier discoverable
        // by ViewInspector via .find(ViewType.VStack.self).padding() and lets
        // SwiftUI lay the container out flush against the padding bounds.
        switch stack.axis {
        case .vertical:
            VStack(alignment: alignment.swiftUIHorizontal, spacing: spacingValue) {
                ForEach(Array(stack.children.enumerated()), id: \.offset) { _, child in
                    ComponentView(component: child, host: host)
                        .applyStretch(alignment: alignment, axis: .vertical)
                }
            }
            .padding(paddingValue)
            .modifier(PoppyIdentifier(id: stack.id))
        case .horizontal:
            HStack(alignment: alignment.swiftUIVertical, spacing: spacingValue) {
                ForEach(Array(stack.children.enumerated()), id: \.offset) { _, child in
                    ComponentView(component: child, host: host)
                        .applyStretch(alignment: alignment, axis: .horizontal)
                }
            }
            .padding(paddingValue)
            .modifier(PoppyIdentifier(id: stack.id))
        }
    }
}

private struct PoppyIdentifier: ViewModifier {
    let id: String?
    func body(content: Content) -> some View {
        if let id = id {
            content.accessibilityIdentifier(id)
        } else {
            content
        }
    }
}

private extension View {
    /// If the alignment is `.stretch`, expand the child along the axis
    /// perpendicular to the stack's main axis. SwiftUI lacks a direct
    /// equivalent of CSS's `align-items: stretch` so we use frame(maxWidth:
    /// .infinity) / maxHeight: .infinity to approximate.
    @ViewBuilder
    func applyStretch(alignment: PoppyAlignment, axis: Axis) -> some View {
        if alignment.isStretch {
            switch axis {
            case .vertical: self.frame(maxWidth: .infinity)
            case .horizontal: self.frame(maxHeight: .infinity)
            }
        } else {
            self
        }
    }
}
