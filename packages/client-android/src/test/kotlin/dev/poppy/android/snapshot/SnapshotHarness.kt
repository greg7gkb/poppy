/*
 * Snapshot harness for the Android Compose renderer.
 *
 * Per ADR-0008 §"Implementation, Phase 2 onward": each renderer adds a
 * snapshot per valid corpus case. Compose's choice is
 * `SemanticsNodeInteraction.printToString()` on the root node — a textual
 * dump of the semantics tree.
 *
 * Determinism: the raw printToString output includes per-run identifiers
 * (node IDs, exact pixel rects) that drift between machines and Robolectric
 * versions. We normalize to a tree-structure-only view that captures what
 * matters for cross-platform reviewers — what nodes exist, in what order,
 * with what semantic properties.
 */

package dev.poppy.android.snapshot

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import dev.poppy.android.LocalPoppyImageLoader
import dev.poppy.android.Poppy
import dev.poppy.android.PoppyHost
import dev.poppy.android.PoppyImageLoader
import dev.poppy.android.PoppyTheme
import dev.poppy.android.TestHost
import dev.poppy.android.ValidCase
import dev.poppy.android.ValidationResult
import androidx.compose.runtime.Composable

/**
 * A deterministic image loader for snapshots. Renders the URL + alt as a
 * textual placeholder so the snapshot reflects `Image` semantics without
 * depending on Coil's network/cache state.
 */
internal object SnapshotImageLoader : PoppyImageLoader {
    @Composable
    override fun Image(
        url: String,
        contentDescription: String?,
        modifier: Modifier,
        contentScale: ContentScale,
    ) {
        val cd = contentDescription
        androidx.compose.material3.Text(
            text = "<image src=\"$url\" fit=\"${contentScale.name()}\">",
            modifier = if (cd != null) {
                modifier.semantics(mergeDescendants = true) {
                    this.contentDescription = cd
                }
            } else modifier,
        )
    }

    private fun ContentScale.name(): String = when (this) {
        ContentScale.Crop -> "crop"
        ContentScale.Fit -> "fit"
        ContentScale.FillBounds -> "fill"
        ContentScale.FillHeight -> "fill-height"
        ContentScale.FillWidth -> "fill-width"
        ContentScale.Inside -> "inside"
        ContentScale.None -> "none"
        else -> "other"
    }
}

internal object SnapshotHarness {
    /**
     * Render a corpus case in [composeRule], dump the semantics tree, and
     * return a normalized snapshot string suitable for committing to
     * `snapshot.android.txt`.
     */
    fun render(rule: ComposeContentTestRule, case: ValidCase): String {
        val result = Poppy.validate(case.documentJson)
        require(result is ValidationResult.Ok) {
            "snapshot harness can only render valid cases; ${case.slug} did not validate"
        }
        val host: PoppyHost = TestHost()
        rule.setContent {
            CompositionLocalProvider(LocalPoppyImageLoader provides SnapshotImageLoader) {
                PoppyTheme { Poppy(result.document, host) }
            }
        }
        rule.waitForIdle()
        val raw = rule.onRoot().printToString(maxDepth = Int.MAX_VALUE)
        return normalize(raw)
    }

    /**
     * Normalize Compose's printToString output for stable cross-machine
     * snapshots. We strip:
     *   - `Node #N` IDs (run-specific monotonic IDs)
     *   - Bounds: `Bounds(...)` rects (depend on screen size, density,
     *     Robolectric SDK version)
     *   - The trailing "Has 1 sibling" annotation (test-rule specific)
     *   - Trailing whitespace
     *
     * Intent: what survives is *the structure* — node kinds, order,
     * semantic-tree properties (Text, ContentDescription, Role, TestTag, etc.),
     * which is precisely what reviewers compare across platforms.
     */
    fun normalize(raw: String): String {
        val lines = raw.lines().map { line ->
            line
                // "Node #12 at (l=0, t=0, r=200, b=400)px" → "Node"
                .replace(Regex("Node #\\d+ at \\([^)]*\\)px"), "Node")
                .replace(Regex("Node #\\d+ at \\([^)]*\\)"), "Node")
                .replace(Regex("Node #\\d+"), "Node")
                .replace(Regex("Bounds\\([^)]*\\)"), "")
                .trimEnd()
        }
        // Strip any line that's ONLY a "Has N sibling(s)" annotation.
        val filtered = lines.filterNot { it.trim().matches(Regex("Has \\d+ sibling.*")) }
        return filtered.joinToString("\n").trimEnd() + "\n"
    }
}
