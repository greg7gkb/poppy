// swift-tools-version: 5.10
//
// Poppy iOS example app.
//
// Demonstrates how a host integrates the Poppy SwiftPM library. Builds as a
// macOS executable (`swift run`) so the smoke test works on a developer's
// machine without spinning up an iOS simulator. The Poppy library compiles
// for both iOS 16+ and macOS 13+; the integration code in this example uses
// only API that's identical on both platforms.
//
// To deploy this app to iOS, create an Xcode iOS App target and add Poppy
// as a Swift Package dependency:
//
//   .package(path: "../../../packages/client-ios")  // for local development
//   // or, once published:
//   .package(url: "https://github.com/.../poppy", from: "0.2.0-alpha")
//
// Then drop ContentView.swift into your iOS App target. The integration code
// is identical.

import PackageDescription

let package = Package(
    name: "PoppyExample",
    platforms: [
        // macOS for the local smoke test (swift run); the integration code
        // works identically on iOS 16+.
        .macOS(.v13),
        .iOS(.v16),
    ],
    dependencies: [
        // The library under demonstration. Local path so this example
        // tracks whatever's in the monorepo.
        .package(path: "../../../packages/client-ios"),
    ],
    targets: [
        .executableTarget(
            name: "PoppyExample",
            dependencies: [
                .product(name: "Poppy", package: "client-ios"),
            ],
            path: "Sources/PoppyExample"
        ),
    ]
)
