/*
 * Snapshot equality test. Per ADR-0008's "snapshots reviewed in PRs" rule,
 * this test asserts that the live render of every valid corpus case produces
 * the SAME normalized snapshot text as is committed in the case directory.
 *
 * If this test fails, either:
 *   (a) the renderer changed and the snapshot is stale → run
 *       `./gradlew snapshotsUpdate`, review the diff, commit;
 *   (b) the renderer regressed → fix the renderer.
 *
 * Snapshots that don't yet exist (case has no `snapshot.android.txt`) are
 * skipped with a clear message — this test is the gate, not the seed.
 */

package dev.poppy.android.snapshot

import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runComposeUiTest
import dev.poppy.android.CorpusLoader
import dev.poppy.android.LocalPoppyImageLoader
import dev.poppy.android.Poppy
import dev.poppy.android.PoppyTheme
import dev.poppy.android.TestHost
import dev.poppy.android.ValidationResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
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
        // Sanity: ensure we actually checked something.
        assertEquals(15, cases.size - missing.size + missing.size)
    }

    private fun renderSnapshot(case: dev.poppy.android.ValidCase): String {
        var captured: String? = null
        runComposeUiTest {
            val result = Poppy.validate(case.documentJson)
            require(result is ValidationResult.Ok) {
                "snapshot test can only render valid cases; ${case.slug} did not validate"
            }
            setContent {
                CompositionLocalProvider(LocalPoppyImageLoader provides SnapshotImageLoader) {
                    PoppyTheme { Poppy(result.document, TestHost()) }
                }
            }
            waitForIdle()
            // Use the unmerged tree: merged semantics collapse Stack/Row/Column
            // wrappers into their text leaves, hiding the structural hierarchy
            // reviewers need to compare against snapshot.web.html.
            val raw = onRoot(useUnmergedTree = true).printToString(maxDepth = Int.MAX_VALUE)
            captured = SnapshotHarness.normalize(raw)
        }
        return captured ?: error("snapshot harness did not capture output for ${case.slug}")
    }
}
