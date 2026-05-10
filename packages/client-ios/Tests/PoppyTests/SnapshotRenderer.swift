// Render a typed PoppyDocument to the stable plain-text format committed
// alongside each valid corpus case as `snapshot.ios.txt`.
//
// Why a custom dumper instead of ViewInspector's tree dump?
//
// 1. We control the format completely — line-stable, no implicit AnyView
//    noise, no stripping of internal SwiftUI machinery.
// 2. The output mirrors `snapshot.web.html`'s spirit: every component on
//    its own line(s) in document order, each line capturing the structural
//    shape (axis, fit, padding, spacing, alignment, etc.) reviewers compare.
// 3. The output is identical regardless of macOS / Xcode version, so
//    snapshots don't churn between CI matrix entries.
//
// Format example (for case 015-kitchen-sink):
//
//   PoppyDocument version="0.1"
//   Stack id="screen-root" axis=vertical padding=lg spacing=md alignment=stretch
//     Stack axis=horizontal spacing=sm alignment=center
//       Image url="…" alt="User avatar" width=48 height=48 fit=cover
//       Stack axis=vertical spacing=xs
//         Text size=lg weight=bold value="Greg"
//         Text size=sm color=secondary value="Signed in"
//     Text color=primary value="You have 3 unread notifications."
//     Stack axis=horizontal spacing=sm alignment=end
//       Button label="Dismiss" action.type=navigate action.uri="poppy://notifications/dismiss-all"
//       Button label="View all" action.type=navigate action.uri="poppy://notifications"
//
// The format is deliberately simple: indentation = depth, key=value pairs
// in deterministic order, double-quoted strings. No JSON / YAML — the snapshot
// is a human-readable diff target, not a machine-consumed format.

import Foundation
@testable import Poppy

enum SnapshotRenderer {

    static func render(_ document: PoppyDocument) -> String {
        var lines: [String] = []
        lines.append("PoppyDocument version=\"\(document.version)\"")
        renderComponent(document.root, depth: 1, into: &lines)
        return lines.joined(separator: "\n") + "\n"
    }

    private static func renderComponent(_ component: Component, depth: Int, into lines: inout [String]) {
        switch component {
        case .stack(let s):
            lines.append(line(depth: depth, head: "Stack", attrs: stackAttrs(s)))
            for child in s.children {
                renderComponent(child, depth: depth + 1, into: &lines)
            }
        case .text(let t):
            lines.append(line(depth: depth, head: "Text", attrs: textAttrs(t)))
        case .image(let i):
            lines.append(line(depth: depth, head: "Image", attrs: imageAttrs(i)))
        case .button(let b):
            lines.append(line(depth: depth, head: "Button", attrs: buttonAttrs(b)))
        }
    }

    // MARK: - Per-component attribute lists
    //
    // Order is fixed for stability. Optional fields are emitted only when set.

    private static func stackAttrs(_ s: Stack) -> [(String, String)] {
        var a: [(String, String)] = []
        if let id = s.id { a.append(("id", quoted(id))) }
        a.append(("axis", s.axis.rawValue))
        if let p = s.padding { a.append(("padding", p.rawValue)) }
        if let sp = s.spacing { a.append(("spacing", sp.rawValue)) }
        if let al = s.alignment { a.append(("alignment", al.rawValue)) }
        return a
    }

    private static func textAttrs(_ t: Text) -> [(String, String)] {
        var a: [(String, String)] = []
        if let id = t.id { a.append(("id", quoted(id))) }
        if let c = t.color { a.append(("color", c.rawValue)) }
        if let s = t.size { a.append(("size", s.rawValue)) }
        if let w = t.weight { a.append(("weight", w.rawValue)) }
        a.append(("value", quoted(t.value)))
        return a
    }

    private static func imageAttrs(_ i: Image) -> [(String, String)] {
        var a: [(String, String)] = []
        if let id = i.id { a.append(("id", quoted(id))) }
        a.append(("url", quoted(i.url)))
        a.append(("alt", quoted(i.alt)))
        if let w = i.width { a.append(("width", trimDouble(w))) }
        if let h = i.height { a.append(("height", trimDouble(h))) }
        if let f = i.fit { a.append(("fit", f.rawValue)) }
        return a
    }

    private static func buttonAttrs(_ b: Button) -> [(String, String)] {
        var a: [(String, String)] = []
        if let id = b.id { a.append(("id", quoted(id))) }
        a.append(("label", quoted(b.label)))
        switch b.action {
        case .navigate(let n):
            a.append(("action.type", "navigate"))
            a.append(("action.uri", quoted(n.uri)))
        }
        return a
    }

    // MARK: - Formatting helpers

    private static func line(depth: Int, head: String, attrs: [(String, String)]) -> String {
        let indent = String(repeating: "  ", count: depth)
        let body = attrs.map { "\($0.0)=\($0.1)" }.joined(separator: " ")
        return body.isEmpty ? "\(indent)\(head)" : "\(indent)\(head) \(body)"
    }

    private static func quoted(_ s: String) -> String {
        let escaped = s
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
        return "\"\(escaped)\""
    }

    private static func trimDouble(_ d: Double) -> String {
        // Render integral doubles (200.0) as "200", non-integral (1.5) as "1.5".
        if d.rounded() == d {
            return String(Int(d))
        }
        return String(d)
    }
}
