// swift-tools-version: 5.10
//
// Poppy — iOS SwiftUI renderer for Poppy v0.1 documents.
//
// This package builds a single library `Poppy`, plus a `PoppyTests` test target
// that consumes the shared conformance corpus at ../conformance/cases/.
//
// ViewInspector is the only third-party dependency, used in tests only for
// SwiftUI view-tree introspection. See ADR-0005 (minimize third-party
// dependencies) for the justification, and ADR-0011 (iOS theming) for how
// the runtime API uses SwiftUI's EnvironmentValues.

import PackageDescription

let package = Package(
    name: "Poppy",
    platforms: [
        .iOS(.v16),
    ],
    products: [
        .library(
            name: "Poppy",
            targets: ["Poppy"]
        ),
    ],
    dependencies: [
        // Test-only: SwiftUI view-tree introspection. There is no first-party
        // alternative; pixel snapshots are explicitly rejected per ADR-0008.
        .package(
            url: "https://github.com/nalexn/ViewInspector",
            from: "0.10.0"
        ),
    ],
    targets: [
        .target(
            name: "Poppy",
            path: "Sources/Poppy"
        ),
        .testTarget(
            name: "PoppyTests",
            dependencies: [
                "Poppy",
                .product(name: "ViewInspector", package: "ViewInspector"),
            ],
            path: "Tests/PoppyTests",
            resources: [
                // Tests load the conformance corpus directly from the
                // monorepo via a path computed from #file in CorpusLoader.
                // No bundle resources required.
            ]
        ),
    ]
)
