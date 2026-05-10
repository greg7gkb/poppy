/*
 * Snapshot regenerator. Per docs/phase-2-plan.md §"poppy-android" → "Tooling
 * notes" and the brief §"Tests":
 *
 *   - Gated behind the env var `POPPY_UPDATE_SNAPSHOTS=1`.
 *   - When the env var is set, the test rewrites snapshot.android.txt for
 *     every valid case.
 *   - When the env var is NOT set, the test is a no-op so `./gradlew check`
 *     never accidentally rewrites committed snapshots.
 *
 * Invoke explicitly:
 *
 *     ./gradlew snapshotsUpdate
 *
 * (The Gradle task in build.gradle.kts sets the env var and filters to this
 * test class.)
 */

package dev.poppy.android.snapshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runComposeUiTest
import dev.poppy.android.CorpusLoader
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class UpdateSnapshots {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun regenerateAllValidSnapshots() {
        val enabled = System.getenv("POPPY_UPDATE_SNAPSHOTS") == "1"
        assumeTrue(
            "POPPY_UPDATE_SNAPSHOTS not set — skipping snapshot regeneration. " +
                "Run `./gradlew snapshotsUpdate` to enable.",
            enabled,
        )

        // Each Compose test rule can only host one composition. We can't loop
        // setContent over multiple cases on a single rule. Instead, we use a
        // companion mechanism: the JUnit-rule `composeRule` is recreated per
        // test method. We could split this into 15 @Tests, but a cleaner
        // approach is to write the snapshot for the case named in a system
        // property and let an outer driver loop over cases.
        //
        // Simpler still: collect the cases, render each one in a fresh
        // composition by recreating the rule manually. ComposeContentTestRule
        // doesn't expose a public reset, but `setContent` can be called
        // exactly once per rule lifecycle. We work around this by writing a
        // helper that recreates the testRule semantics inline.
        //
        // Pragmatic choice: regenerate one case per test method. JUnit
        // parametrization with @RunWith would conflict with Robolectric.
        // Instead, we drive this from a parametrized `@Test` that isn't
        // really parametrized — we render the next case each invocation by
        // tracking a static cursor.
        val written = mutableListOf<String>()
        val failures = mutableListOf<String>()
        for (case in CorpusLoader.loadValidCases()) {
            try {
                val snapshot = renderInFreshRule(case)
                val file = File(case.dir, "snapshot.android.txt")
                file.writeText(snapshot)
                written += "${case.slug} → ${file.relativeTo(File(".").canonicalFile)}"
            } catch (t: Throwable) {
                failures += "${case.slug}: ${t.message}"
            }
        }
        if (failures.isNotEmpty()) {
            fail("Failed to regenerate snapshots:\n${failures.joinToString("\n")}")
        }
        println("Wrote ${written.size} snapshot(s):")
        written.forEach { println("  $it") }
    }

    /**
     * Render a single case in a fresh composition. We bypass the JUnit
     * composeRule (which only supports one setContent per test method) by
     * driving the snapshot harness against a freshly-created Activity-less
     * compose rule per case.
     */
    private fun renderInFreshRule(
        case: dev.poppy.android.ValidCase,
    ): String {
        // Programmatic ComposeContentTestRule creation requires the JUnit
        // statement-evaluation lifecycle. The cleanest way to drive it
        // imperatively is `runComposeUiTest` from Compose 1.7+'s
        // multiplatform-test API — but it isn't available in our androidx
        // dependency set. Instead, we use the rule we already have on the
        // first invocation, and for subsequent cases we delegate to a
        // standalone runner via the `runComposeUiTest` API exposed by the
        // androidx.compose.ui:ui-test-junit4 module's
        // `androidx.compose.ui.test.runComposeUiTest` helper.
        var captured: String? = null
        runComposeUiTest {
            // `runComposeUiTest` provides a ComposeUiTest receiver, not a
            // ComposeContentTestRule. Adapt SnapshotHarness to use it inline:
            val result = dev.poppy.android.Poppy.validate(case.documentJson)
            require(result is dev.poppy.android.ValidationResult.Ok) {
                "snapshot harness can only render valid cases; ${case.slug} did not validate"
            }
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    dev.poppy.android.LocalPoppyImageLoader provides SnapshotImageLoader,
                ) {
                    dev.poppy.android.PoppyTheme {
                        dev.poppy.android.Poppy(result.document, dev.poppy.android.TestHost())
                    }
                }
            }
            waitForIdle()
            // Use the unmerged tree to retain structural hierarchy (Stack
            // wrappers); see SnapshotTest's identical comment.
            val raw = onRoot(useUnmergedTree = true).printToString(maxDepth = Int.MAX_VALUE)
            captured = SnapshotHarness.normalize(raw)
        }
        return captured ?: error("snapshot harness did not capture output for ${case.slug}")
    }
}
