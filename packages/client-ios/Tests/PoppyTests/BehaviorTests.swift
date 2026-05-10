// Behavioral assertions per ADR-0008 §2 — every renderer must honor these
// invariants regardless of platform. The set is grouped by invariant kind
// and parametrised over the corpus cases that exercise each invariant.
//
// Implementation notes:
//
//   - We use ViewInspector to traverse the SwiftUI view tree at runtime.
//   - PoppyView's components read the theme via @Environment, which means
//     ViewInspector traversal works after we apply .environment(\.poppyTheme,
//     theme) at the top of the test view. We pass the default theme so the
//     assertions match the host-default expectations in description.md.
//   - For action dispatch we use a TestHost that records every onAction call.

import XCTest
import SwiftUI
import ViewInspector
@testable import Poppy

final class BehaviorTests: XCTestCase {

    // MARK: - Helpers

    /// Decode a corpus document or fail the test.
    private func loadDocument(slug: String) throws -> PoppyDocument {
        let cases = try CorpusLoader.loadValid()
        guard let c = cases.first(where: { $0.base.slug == slug }) else {
            XCTFail("Corpus case \(slug) not found")
            throw NSError(domain: "Poppy.tests", code: 1)
        }
        let result = Poppy.validate(c.base.documentJSON)
        switch result {
        case .ok(let doc): return doc
        case .failure(let errs):
            XCTFail("Corpus case \(slug) failed validation unexpectedly: \(errs)")
            throw NSError(domain: "Poppy.tests", code: 2)
        }
    }

    private func makeView(_ doc: PoppyDocument, host: PoppyHost = TestHost()) -> some View {
        return PoppyView(document: doc, host: host)
            .environment(\.poppyTheme, .default)
    }

    // MARK: - Action dispatch (cases 014, 015)

    /// Case 014: tapping a Button must call host.onAction with the verbatim
    /// Action; the renderer must not parse, prefix, or transform the URI.
    func testButtonDispatchesActionVerbatim() throws {
        let doc = try loadDocument(slug: "014-button-basic")
        let host = TestHost()
        let view = makeView(doc, host: host)

        let inspected = try view.inspect().find(ViewType.Button.self)
        try inspected.tap()

        XCTAssertEqual(host.actions.count, 1, "Expected exactly one action dispatched on tap")
        guard case .navigate(let nav) = host.actions.first else {
            XCTFail("Expected NavigateAction, got \(String(describing: host.actions.first))")
            return
        }
        XCTAssertEqual(nav.uri, "poppy://next")
    }

    /// Case 015: each button in the kitchen sink dispatches its own URI verbatim.
    func testKitchenSinkButtonsDispatchSeparately() throws {
        let doc = try loadDocument(slug: "015-kitchen-sink")
        let host = TestHost()
        let view = makeView(doc, host: host)

        let buttons = try view.inspect().findAll(ViewType.Button.self)
        XCTAssertEqual(buttons.count, 2, "Kitchen sink has two buttons (Dismiss, View all)")

        try buttons[0].tap()
        try buttons[1].tap()

        XCTAssertEqual(host.actions.count, 2)
        let uris: [String] = host.actions.map {
            if case .navigate(let n) = $0 { return n.uri }
            return ""
        }
        XCTAssertTrue(uris.contains("poppy://notifications/dismiss-all"))
        XCTAssertTrue(uris.contains("poppy://notifications"))
    }

    // MARK: - Axis layout (cases 009, 010, 013)

    /// Case 009: axis: vertical -> VStack
    func testVerticalStackUsesVStack() throws {
        let doc = try loadDocument(slug: "009-stack-vertical-two-texts")
        let view = makeView(doc)
        XCTAssertNoThrow(
            try view.inspect().find(ViewType.VStack.self),
            "Vertical stack must render as a VStack"
        )
    }

    /// Case 010: axis: horizontal -> HStack
    func testHorizontalStackUsesHStack() throws {
        let doc = try loadDocument(slug: "010-stack-horizontal-two-texts")
        let view = makeView(doc)
        XCTAssertNoThrow(
            try view.inspect().find(ViewType.HStack.self),
            "Horizontal stack must render as an HStack"
        )
    }

    /// Case 013: nested axes — outer VStack contains an HStack which contains
    /// a VStack. Each axis governs only its own direct children.
    func testNestedStackPreservesEachAxis() throws {
        let doc = try loadDocument(slug: "013-stack-nested")
        let view = makeView(doc)
        let inspected = try view.inspect()
        // Find at least one VStack and at least one HStack inside the tree.
        let vstacks = inspected.findAll(ViewType.VStack.self)
        let hstacks = inspected.findAll(ViewType.HStack.self)
        XCTAssertGreaterThanOrEqual(vstacks.count, 1, "Expected at least one VStack")
        XCTAssertGreaterThanOrEqual(hstacks.count, 1, "Expected at least one HStack")
    }

    /// Case 009: children render in document order (First above Second).
    func testStackChildrenInDocumentOrder() throws {
        let doc = try loadDocument(slug: "009-stack-vertical-two-texts")
        let view = makeView(doc)
        let texts = try view.inspect().findAll(ViewType.Text.self)
        let strings: [String] = texts.compactMap { try? $0.string() }
        XCTAssertEqual(strings, ["First", "Second"])
    }

    // MARK: - Alt-text exposure (cases 006, 007, 008, 015)

    /// Per ADR-0008 §2, the Image's alt must be exposed to the platform's
    /// accessibility tree. On SwiftUI we set .accessibilityLabel(alt).
    /// Use ViewInspector's find(viewWithAccessibilityLabel:) which walks
    /// the modifier chain looking for a matching label, so we don't have to
    /// pin the assertion to a specific view layer.
    func testImageAltIsExposedToAccessibilityLabel() throws {
        let cases = ["006-image-basic", "007-image-with-dimensions", "008-image-with-fit-cover"]
        for slug in cases {
            let doc = try loadDocument(slug: slug)
            let view = makeView(doc)
            // The alt text we expect varies per case; pull it from the model.
            guard case .image(let img) = doc.root else {
                XCTFail("\(slug): root expected to be Image")
                continue
            }
            XCTAssertNoThrow(
                try view.inspect().find(viewWithAccessibilityLabel: img.alt),
                "\(slug): expected a view with accessibilityLabel \"\(img.alt)\""
            )
        }
    }

    // MARK: - Color token resolution (cases 002, 005)

    /// The `primary` color token must resolve through the host theme, not
    /// a hard-coded value. We assert the rendered Text's foregroundColor
    /// equals theme.color(.primary) for the default theme.
    func testColorTokenResolvesThroughTheme() throws {
        let doc = try loadDocument(slug: "002-text-with-color-primary")
        let theme = PoppyTheme.default
        let view = PoppyView(document: doc, host: TestHost())
            .environment(\.poppyTheme, theme)

        let text = try view.inspect().find(ViewType.Text.self)
        let foreground = try text.attributes().foregroundColor()
        XCTAssertEqual(foreground, theme.color(.primary), "primary color must equal theme.color(.primary)")
    }

    // MARK: - Padding token mapping (case 012)

    /// padding: md must map to theme.spacing(.md) (16 by default). The
    /// padding modifier lives directly on the VStack (see PoppyStack.body).
    func testPaddingTokenMapsToThemeSpacing() throws {
        let doc = try loadDocument(slug: "012-stack-with-padding-md")
        let theme = PoppyTheme.default
        let view = PoppyView(document: doc, host: TestHost())
            .environment(\.poppyTheme, theme)

        let vstack = try view.inspect().find(ViewType.VStack.self)
        let padding = try vstack.padding()
        let expected = theme.spacing(.md)
        // SwiftUI applies padding(_:) as uniform on all four edges.
        XCTAssertEqual(padding.top, expected, accuracy: 0.001)
        XCTAssertEqual(padding.leading, expected, accuracy: 0.001)
        XCTAssertEqual(padding.bottom, expected, accuracy: 0.001)
        XCTAssertEqual(padding.trailing, expected, accuracy: 0.001)
    }

    // MARK: - Spacing token mapping (case 011)

    /// spacing: lg must map to theme.spacing(.lg) (24 by default). We assert
    /// the VStack carries spacing equal to that value.
    func testSpacingTokenMapsToThemeSpacing() throws {
        let doc = try loadDocument(slug: "011-stack-with-spacing-lg")
        let theme = PoppyTheme.default
        let view = PoppyView(document: doc, host: TestHost())
            .environment(\.poppyTheme, theme)

        let vstack = try view.inspect().find(ViewType.VStack.self)
        let spacing = try vstack.spacing()
        let expected = theme.spacing(.lg)
        XCTAssertEqual(spacing ?? -1, expected, accuracy: 0.001)
    }

    // MARK: - Text content + token sanity (cases 001, 003, 004, 005)

    /// Every Text case renders the literal text from the document.
    func testTextContentMatchesDocument() throws {
        let cases = [
            ("001-text-hello", "Hello"),
            ("003-text-with-size-lg", "Large text"),
            ("004-text-with-weight-bold", "Bold text"),
            ("005-text-all-options", "All options"),
        ]
        for (slug, expected) in cases {
            let doc = try loadDocument(slug: slug)
            let view = makeView(doc)
            let text = try view.inspect().find(ViewType.Text.self)
            let s = try text.string()
            XCTAssertEqual(s, expected, "\(slug): text content mismatch")
        }
    }

    /// Case 015: the root Stack's id "screen-root" must be exposed as a
    /// stable identifier (accessibilityIdentifier on iOS).
    func testRootStackIdentifierIsExposed() throws {
        let doc = try loadDocument(slug: "015-kitchen-sink")
        let view = makeView(doc)
        // Find any view bearing the "screen-root" accessibility identifier.
        let found = try view.inspect().find(viewWithAccessibilityIdentifier: "screen-root")
        XCTAssertNotNil(found, "Expected to find a view with accessibilityIdentifier \"screen-root\"")
    }
}
