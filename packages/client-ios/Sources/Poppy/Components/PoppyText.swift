// Render a Text component with optional color, size, and weight tokens.
//
// Behavioral invariants tested in BehaviorTests.swift:
//
//   - rendered text content equals text.value
//   - color resolves through theme.color(.primary) etc.; never hard-coded
//   - size resolves through theme.size(.lg) etc.; never hard-coded
//   - weight resolves through theme.weight(.bold) etc.; never hard-coded
//   - default tokens applied when fields are nil (md size, regular weight,
//     default color)

import SwiftUI

struct PoppyText: View {
    let text: Text

    @Environment(\.poppyTheme) private var theme

    var body: some View {
        let sizeValue = theme.size(text.size ?? .md)
        let weightValue = theme.weight(text.weight ?? .regular)
        let colorValue = theme.color(text.color ?? .default)

        SwiftUI.Text(text.value)
            .font(.system(size: sizeValue, weight: weightValue))
            .foregroundColor(colorValue)
            .modifier(PoppyTextIdentifier(id: text.id))
    }
}

private struct PoppyTextIdentifier: ViewModifier {
    let id: String?
    func body(content: Content) -> some View {
        if let id = id {
            content.accessibilityIdentifier(id)
        } else {
            content
        }
    }
}
