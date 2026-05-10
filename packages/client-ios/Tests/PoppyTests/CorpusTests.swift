// Iterate every conformance case and assert the validator's verdict.
//
// Per the brief's done criteria: every invalid case must produce a
// ValidationError whose `keyword` matches the case's expected-error.json,
// and every valid case must decode to a typed PoppyDocument.

import XCTest
@testable import Poppy

final class CorpusTests: XCTestCase {

    // MARK: - Invalid cases

    func testEveryInvalidCaseFailsWithExpectedKeyword() throws {
        let cases = try CorpusLoader.loadInvalid()
        XCTAssertFalse(cases.isEmpty, "Expected at least one invalid case in the corpus")

        for c in cases {
            let result = Poppy.validate(c.base.documentJSON)
            switch result {
            case .ok:
                XCTFail("Expected case \(c.base.slug) to fail validation, got .ok")
            case .failure(let errors):
                XCTAssertFalse(errors.isEmpty, "Case \(c.base.slug) returned .failure with no errors")
                let keywords = errors.map(\.keyword)
                XCTAssertTrue(
                    keywords.contains(c.expectedKeyword),
                    "Case \(c.base.slug): expected keyword \"\(c.expectedKeyword)\", got \(keywords)"
                )
                if let expectedPath = c.expectedPath {
                    let paths = errors.map(\.path)
                    XCTAssertTrue(
                        paths.contains(expectedPath),
                        "Case \(c.base.slug): expected path \"\(expectedPath)\", got \(paths)"
                    )
                }
            }
        }
    }

    func testInvalidCorpusHasSevenCases() throws {
        let cases = try CorpusLoader.loadInvalid()
        XCTAssertEqual(cases.count, 7, "The conformance corpus is expected to have 7 invalid cases")
    }

    // MARK: - Valid cases

    func testEveryValidCaseDecodesSuccessfully() throws {
        let cases = try CorpusLoader.loadValid()
        XCTAssertFalse(cases.isEmpty, "Expected at least one valid case in the corpus")

        for c in cases {
            let result = Poppy.validate(c.base.documentJSON)
            switch result {
            case .ok(let doc):
                XCTAssertEqual(doc.version, "0.1", "Case \(c.base.slug): unexpected version \(doc.version)")
            case .failure(let errors):
                XCTFail("Case \(c.base.slug) failed validation: \(errors)")
            }
        }
    }

    func testValidCorpusHasFifteenCases() throws {
        let cases = try CorpusLoader.loadValid()
        XCTAssertEqual(cases.count, 15, "The conformance corpus is expected to have 15 valid cases")
    }

    // MARK: - Version-compat (also exercised via cases 006 and 007)

    func testVersionCompatRejectsUnknownMajor() {
        XCTAssertEqual(checkVersionCompat("999.0")?.keyword, "version")
        XCTAssertEqual(checkVersionCompat("1.0")?.keyword, "version")
    }

    func testVersionCompatRejectsFutureMinor() {
        XCTAssertEqual(checkVersionCompat("0.99")?.keyword, "version")
    }

    func testVersionCompatAcceptsCurrentVersion() {
        XCTAssertNil(checkVersionCompat("0.1"))
    }

    func testVersionCompatRejectsMalformed() {
        XCTAssertEqual(checkVersionCompat("v1")?.keyword, "version")
        XCTAssertEqual(checkVersionCompat("1.0.0")?.keyword, "version")
        XCTAssertEqual(checkVersionCompat("")?.keyword, "version")
    }
}
