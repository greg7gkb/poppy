// Render a Button component as a SwiftUI Button. The action is dispatched
// verbatim to the host's onAction(_:) callback.
//
// Behavioral invariants tested in BehaviorTests.swift:
//
//   - rendered as a SwiftUI Button (verified via ViewInspector .button())
//   - label equals button.label and is exposed to a11y
//   - tap dispatches host.onAction(_:) exactly once with the original Action
//     value (deep-equality)

import SwiftUI

struct PoppyButton: View {
    let button: Button
    let host: PoppyHost

    @Environment(\.poppyTheme) private var theme

    var body: some View {
        SwiftUI.Button(action: { host.onAction(button.action) }) {
            SwiftUI.Text(button.label)
                .font(.system(size: theme.size(.md), weight: theme.weight(.medium)))
                .foregroundColor(theme.color(.default))
                .padding(.vertical, theme.spacing(.sm))
                .padding(.horizontal, theme.spacing(.md))
        }
        .accessibilityLabel(button.label)
        .modifier(PoppyButtonIdentifier(id: button.id))
    }
}

private struct PoppyButtonIdentifier: ViewModifier {
    let id: String?
    func body(content: Content) -> some View {
        if let id = id {
            content.accessibilityIdentifier(id)
        } else {
            content
        }
    }
}
