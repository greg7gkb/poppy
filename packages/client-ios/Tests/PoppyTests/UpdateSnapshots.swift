// Regenerate snapshot.ios.txt files for every valid corpus case.
//
// This test is gated behind POPPY_UPDATE_SNAPSHOTS=1 so a normal `swift test`
// invocation never overwrites committed snapshots. Snapshot regeneration is
// a deliberate developer act per ADR-0008.
//
// Run via:
//
//   POPPY_UPDATE_SNAPSHOTS=1 swift test --filter UpdateSnapshots
//
// Then review the diff with `git diff packages/conformance/cases/valid/`
// and commit only if the new snapshots are intentionally correct.

import XCTest
@testable import Poppy

final class UpdateSnapshots: XCTestCase {

    func testRegenerateAllValidSnapshots() throws {
        guard ProcessInfo.processInfo.environment["POPPY_UPDATE_SNAPSHOTS"] == "1" else {
            // Bail with a clear message rather than running silently. The
            // snapshot harness is intentionally a separate gated test from
            // SnapshotTests so the two can coexist in the same test target.
            print("[UpdateSnapshots] POPPY_UPDATE_SNAPSHOTS != 1; skipping regeneration.")
            return
        }

        let cases = try CorpusLoader.loadValid()
        XCTAssertFalse(cases.isEmpty)

        var written: [String] = []
        for c in cases {
            let result = Poppy.validate(c.base.documentJSON)
            guard case .ok(let doc) = result else {
                XCTFail("Case \(c.base.slug) failed validation: \(result.errors)")
                continue
            }
            let snapshot = SnapshotRenderer.render(doc)
            let url = c.base.folder.appendingPathComponent("snapshot.ios.txt")
            try snapshot.write(to: url, atomically: true, encoding: .utf8)
            written.append(c.base.slug)
        }

        print("[UpdateSnapshots] Wrote \(written.count) snapshot.ios.txt files: \(written.joined(separator: ", "))")
    }
}
