// The Poppy integration demo.
//
// What this file shows, in order:
//
//   1. Poppy.validate(_:) — never throws; returns ValidationResult.ok|failure.
//   2. Implementing PoppyHost — the only required interaction surface.
//      onAction is called when a Button (or future actionable component)
//      fires. onError is called when validation rejects a doc or a URL is
//      blocked. isUrlAllowed is the URL gate.
//   3. PoppyTheme override via .environment(\.poppyTheme, ...) — tweak any
//      token map; defaults match the web client.
//   4. PoppyView(document:host:) — the actual render call. Pure data in,
//      SwiftUI view out.
//
// Run on macOS:    cd examples/ios/PoppyExample && swift run
// Adapt to iOS:    create an Xcode iOS App target, add this Sources/ folder,
//                  add Poppy as a Swift Package dependency.

import SwiftUI
import Poppy

struct ContentView: View {
    /// Last action surfaced from PoppyHost — bound to the on-screen log so
    /// taps demonstrate the host integration without a router.
    @State private var lastEvent: String = "Tap a button to see actions appear here."

    /// Validate the sample document once. In a real app, you'd validate
    /// after every fetch and cache the typed `PoppyDocument`.
    private let validation = Poppy.validate(SampleDocument.kitchenSinkJSON)

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            poppySurface
            Divider()
            actionLog
        }
    }

    // The chrome around the Poppy-rendered surface — purely a host concern.
    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Poppy Example").font(.title3).bold()
            Text("This panel is rendered by the Poppy library from a JSON document.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
    }

    @ViewBuilder
    private var poppySurface: some View {
        switch validation {
        case .ok(let document):
            // (4) The render call. Any token override goes through .environment.
            PoppyView(document: document, host: DemoHost(onEvent: { event in
                lastEvent = event
            }))
            // (3) Theme override — bumping primary to magenta so the demo
            //     visibly differs from the defaults.
            .environment(\.poppyTheme, demoTheme)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .padding()

        case .failure(let errors):
            // The document failed schema validation. Surface for debugging;
            // production hosts likely log this and fall back to native UI.
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Failed to validate document:").font(.headline)
                    ForEach(0..<errors.count, id: \.self) { i in
                        let e = errors[i]
                        Text("\(e.path) (\(e.keyword)): \(e.message)")
                            .font(.system(.caption, design: .monospaced))
                    }
                }
                .padding()
            }
        }
    }

    private var actionLog: some View {
        Text(lastEvent)
            .font(.system(.caption, design: .monospaced))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.secondary.opacity(0.1))
    }

    /// Theme override demo. Defaults match the web client; this swaps
    /// primary to magenta. Read `PoppyTheme.default` to see the full set
    /// of overridable tokens.
    private var demoTheme: PoppyTheme {
        var theme = PoppyTheme.default
        theme.colorValues[.primary] = .pink
        return theme
    }
}

/// (2) The host bridge. PoppyHost is a protocol with three methods, two of
/// which have default implementations.
private struct DemoHost: PoppyHost {
    let onEvent: (String) -> Void

    func onAction(_ action: Action) {
        switch action {
        case .navigate(let nav):
            // Renderer never interprets the URI — the host decides what to
            // do (push a NavigationStack route, open in Safari, log, etc.).
            onEvent("navigate → \(nav.uri)")
        }
    }

    func onError(_ error: Error) {
        onEvent("error: \(error)")
    }

    // isUrlAllowed has a default implementation that allows http(s),
    // relative URLs, and data:image/*. Override here to broaden or tighten.
}

#Preview {
    ContentView()
        .frame(width: 480, height: 720)
}
