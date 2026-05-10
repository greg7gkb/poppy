// Compare each valid corpus case's rendered SnapshotRenderer output against
// its committed snapshot.ios.txt. The UpdateSnapshots harness (separate test
// method, gated by POPPY_UPDATE_SNAPSHOTS=1) regenerates the files.

import XCTest
@testable import Poppy

final class SnapshotTests: XCTestCase {

    func testEveryValidCaseMatchesItsCommittedSnapshot() throws {
        // Skip the comparison when running in update mode — UpdateSnapshots
        // is the test that overwrites files in that mode.
        if updateMode() { return }

        let cases = try CorpusLoader.loadValid()
        XCTAssertFalse(cases.isEmpty)

        for c in cases {
            let result = Poppy.validate(c.base.documentJSON)
            guard case .ok(let doc) = result else {
                XCTFail("Case \(c.base.slug) failed validation: \(result.errors)")
                continue
            }
            let actual = SnapshotRenderer.render(doc)
            guard let committed = c.snapshotIOS else {
                XCTFail(
                    """
                    Case \(c.base.slug) is missing snapshot.ios.txt. \
                    Run POPPY_UPDATE_SNAPSHOTS=1 swift test --filter UpdateSnapshots to regenerate.
                    """
                )
                continue
            }
            if actual != committed {
                XCTFail(
                    """
                    Case \(c.base.slug): snapshot.ios.txt out of date.

                    --- expected (committed) ---
                    \(committed)
                    --- actual (renderer output) ---
                    \(actual)

                    If the change is intentional, run:
                      POPPY_UPDATE_SNAPSHOTS=1 swift test --filter UpdateSnapshots
                    """
                )
            }
        }
    }

    private func updateMode() -> Bool {
        return ProcessInfo.processInfo.environment["POPPY_UPDATE_SNAPSHOTS"] == "1"
    }
}
