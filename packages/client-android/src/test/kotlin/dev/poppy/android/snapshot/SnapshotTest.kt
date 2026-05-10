/*
 * Snapshot equality test. Per ADR-0008's "snapshots reviewed in PRs" rule,
 * this test asserts that every valid corpus case decodes to a typed
 * PoppyDocument that serializes to the same text as the committed
 * snapshot.android.txt.
 *
 * If this test fails, either:
 *   (a) the renderer changed and the snapshot is stale → run
 *       `./gradlew snapshotsUpdate`, review the diff, commit;
 *   (b) the renderer regressed → fix the renderer.
 *
 * Snapshots that don't yet exist (case has no `snapshot.android.txt`) are
 * reported with a clear message — this test is the gate, not the seed.
 *
 * Snapshots are derived from the typed PoppyDocument tree (not the live
 * Compose render); see SnapshotRenderer's header comment for why.
 */

package dev.poppy.android.snapshot

import dev.poppy.android.CorpusLoader
import dev.poppy.android.Poppy
import dev.poppy.android.ValidCase
import dev.poppy.android.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SnapshotTest {

    @Test
    fun `every valid case matches its committed snapshot`() {
        val cases = CorpusLoader.loadValidCases()
        val missing = mutableListOf<String>()
        val mismatches = mutableListOf<String>()

        for (case in cases) {
            val expected = case.androidSnapshot
            if (expected == null) {
                missing += case.slug
                continue
            }
            val actual = renderSnapshot(case)
            if (actual != expected) {
                mismatches += "${case.slug}:\n--- expected ---\n$expected\n--- actual ---\n$actual"
            }
        }

        val problems = buildList {
            if (missing.isNotEmpty()) {
                add(
                    "Missing snapshot.android.txt for: ${missing.joinToString()}. " +
                        "Run `./gradlew snapshotsUpdate` to generate.",
                )
            }
            if (mismatches.isNotEmpty()) {
                add("Snapshot mismatches:\n${mismatches.joinToString("\n\n")}")
            }
        }
        if (problems.isNotEmpty()) {
            fail(problems.joinToString("\n\n"))
        }
        assertEquals(15, cases.size)
    }

    private fun renderSnapshot(case: ValidCase): String {
        val result = Poppy.validate(case.documentJson)
        require(result is ValidationResult.Ok) {
            "snapshot test can only render valid cases; ${case.slug} did not validate"
        }
        return SnapshotRenderer.render(result.document)
    }
}
