// Entry point for the Poppy example app.
//
// Lifecycle: this @main App owns one Scene/Window which hosts ContentView,
// which in turn calls Poppy's library API. The real-world equivalent for a
// host iOS app is the same — drop a `PoppyView(document:host:)` inside any
// SwiftUI view tree.

import SwiftUI

@main
struct PoppyExampleApp: App {
    var body: some Scene {
        WindowGroup("Poppy Example") {
            ContentView()
        }
        // macOS-only: open a reasonably-sized window for the demo. iOS
        // ignores this (the app fills the device).
        #if os(macOS)
        .defaultSize(width: 480, height: 720)
        #endif
    }
}
