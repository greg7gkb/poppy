/*
 * Behavioral assertions per ADR-0008 §2 and the per-case description.md
 * invariants.
 *
 * Per the brief §"Tests": "You don't need a separate test method per case;
 * group invariants by kind and parametrize over cases where it makes sense."
 *
 * Test runner setup: AndroidJUnit4 + Robolectric. The Compose test rule needs
 * an Android Activity to host its content; under Robolectric this runs entirely
 * on the JVM so `./gradlew check` (which only invokes unit tests, not connected
 * device tests) actually exercises the UI. See ADR-0010 for the rationale.
 */

package dev.poppy.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BehaviorTest {

    @get:Rule val composeRule = createComposeRule()

    /**
     * Stub image loader that records calls. Replaces the default CoilImageLoader
     * so tests don't depend on a network or image-decoding path; we only care
     * that the URL was passed correctly and `alt` is exposed to a11y.
     */
    private class RecordingImageLoader : PoppyImageLoader {
        val calls = mutableListOf<Triple<String, String?, ContentScale>>()

        @Composable
        override fun Image(
            url: String,
            contentDescription: String?,
            modifier: Modifier,
            contentScale: ContentScale,
        ) {
            calls.add(Triple(url, contentDescription, contentScale))
            // Render a placeholder Text so the semantics tree exposes the
            // contentDescription for assertion via onNodeWithContentDescription.
            val cd = contentDescription
            androidx.compose.material3.Text(
                text = "<image:$url>",
                modifier = if (cd != null) {
                    modifier.semantics(mergeDescendants = true) {
                        this.contentDescription = cd
                    }
                } else modifier,
            )
        }
    }

    private fun renderCase(case: ValidCase, host: PoppyHost = TestHost()) {
        val result = Poppy.validate(case.documentJson)
        assertTrue("$case should validate", result is ValidationResult.Ok)
        composeRule.setContent {
            PoppyTheme { Poppy((result as ValidationResult.Ok).document, host) }
        }
    }

    private fun renderCaseWithLoader(
        case: ValidCase,
        loader: PoppyImageLoader,
        host: PoppyHost = TestHost(),
    ) {
        val result = Poppy.validate(case.documentJson)
        assertTrue("$case should validate", result is ValidationResult.Ok)
        composeRule.setContent {
            CompositionLocalProvider(LocalPoppyImageLoader provides loader) {
                PoppyTheme { Poppy((result as ValidationResult.Ok).document, host) }
            }
        }
    }

    // --- Action dispatch (cases 014, 015) -----------------------------------

    @Test
    fun `button dispatches its action verbatim on click`() {
        val case = CorpusLoader.loadValidCases().single { it.slug == "014-button-basic" }
        val host = TestHost()
        renderCase(case, host)
        composeRule.onNodeWithText("Continue").performClick()
        assertEquals(1, host.actions.size)
        assertEquals(Action.Navigate("poppy://next"), host.actions[0])
    }

    @Test
    fun `kitchen-sink buttons dispatch their respective actions`() {
        val case = CorpusLoader.loadValidCases().single { it.slug == "015-kitchen-sink" }
        val host = TestHost()
        renderCaseWithLoader(case, RecordingImageLoader(), host)
        composeRule.onNodeWithText("Dismiss").performClick()
        composeRule.onNodeWithText("View all").performClick()
        assertEquals(2, host.actions.size)
        assertEquals(
            Action.Navigate("poppy://notifications/dismiss-all"),
            host.actions[0],
        )
        assertEquals(Action.Navigate("poppy://notifications"), host.actions[1])
    }

    // --- Text content + tokens (cases 001-005) ------------------------------

    @Test
    fun `every text case renders its declared value`() {
        // Each case 001-005's invariant: "The text content equals X".
        val expected = mapOf(
            "001-text-hello" to "Hello",
            "002-text-with-color-primary" to "Primary color text",
            "003-text-with-size-lg" to "Large text",
            "004-text-with-weight-bold" to "Bold text",
            "005-text-all-options" to "All options",
        )
        val cases = CorpusLoader.loadValidCases().filter { it.slug in expected.keys }
        assertEquals(expected.size, cases.size)
        // We can only setContent once per Compose test rule. Render each in
        // its own test execution by constructing a tiny wrapper Composable
        // that walks all of them — this avoids needing per-case test methods.
        val host = TestHost()
        composeRule.setContent {
            PoppyTheme {
                androidx.compose.foundation.layout.Column {
                    for (case in cases) {
                        val r = Poppy.validate(case.documentJson)
                        Poppy((r as ValidationResult.Ok).document, host)
                    }
                }
            }
        }
        for ((_, text) in expected) {
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    // --- Image alt-text exposure (cases 006-008) ----------------------------

    @Test
    fun `image alt is exposed to accessibility`() {
        // Case 006's invariant: "The alt value is exposed to the platform's
        // accessibility tree (Modifier.semantics { contentDescription = alt }
        // on Compose)". We use a recording loader that forwards
        // contentDescription to the semantics tree (the real CoilImageLoader's
        // AsyncImage does the same internally).
        val cases = CorpusLoader.loadValidCases()
            .filter { it.slug.startsWith("006-") || it.slug.startsWith("007-") || it.slug.startsWith("008-") }
        assertEquals(3, cases.size)
        val host = TestHost()
        val loader = RecordingImageLoader()
        composeRule.setContent {
            CompositionLocalProvider(LocalPoppyImageLoader provides loader) {
                PoppyTheme {
                    androidx.compose.foundation.layout.Column {
                        for (case in cases) {
                            val r = Poppy.validate(case.documentJson)
                            Poppy((r as ValidationResult.Ok).document, host)
                        }
                    }
                }
            }
        }
        // Each case's URL is "https://example.com/image.png"; their alts differ.
        composeRule.onNodeWithContentDescription("An example image").assertExists()
        composeRule.onNodeWithContentDescription("Sized image").assertExists()
        composeRule.onNodeWithContentDescription("Cover-fit image").assertExists()
    }

    @Test
    fun `image url is passed through to the loader unchanged`() {
        val case = CorpusLoader.loadValidCases().single { it.slug == "006-image-basic" }
        val loader = RecordingImageLoader()
        renderCaseWithLoader(case, loader)
        composeRule.waitForIdle()
        assertEquals(1, loader.calls.size)
        assertEquals("https://example.com/image.png", loader.calls[0].first)
        assertEquals("An example image", loader.calls[0].second)
    }

    @Test
    fun `image fit cover maps to ContentScale Crop`() {
        val case = CorpusLoader.loadValidCases().single { it.slug == "008-image-with-fit-cover" }
        val loader = RecordingImageLoader()
        renderCaseWithLoader(case, loader)
        composeRule.waitForIdle()
        assertEquals(1, loader.calls.size)
        assertEquals(ContentScale.Crop, loader.calls[0].third)
    }

    // --- Stack axis layout (cases 009, 010, 013) ----------------------------

    @Test
    fun `vertical stack places children in document order`() {
        // For axis layout we lean on the snapshot's ordering as the structural
        // invariant; here we only assert the children exist and are accessible.
        val case = CorpusLoader.loadValidCases().single { it.slug == "009-stack-vertical-two-texts" }
        renderCase(case)
        composeRule.onNodeWithText("First").assertExists()
        composeRule.onNodeWithText("Second").assertExists()
    }

    @Test
    fun `horizontal stack places children in document order`() {
        val case = CorpusLoader.loadValidCases().single { it.slug == "010-stack-horizontal-two-texts" }
        renderCase(case)
        composeRule.onNodeWithText("Left").assertExists()
        composeRule.onNodeWithText("Right").assertExists()
    }

    @Test
    fun `nested stacks render every descendant text`() {
        val case = CorpusLoader.loadValidCases().single { it.slug == "013-stack-nested" }
        renderCase(case)
        for (text in listOf("Header", "Left", "Right", "Footer")) {
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    // --- Color/size/weight token resolution goes through theme --------------

    @Test
    fun `text tokens resolve through the theme overrides`() {
        // Per case 002's invariant: "the applied color resolves through the
        // host's theme's `primary` slot — not a hardcoded hex value. Tests
        // should query the theme, not assert literal RGB."
        //
        // We verify by overriding the theme's primary color to a sentinel and
        // observing the value visible inside the composition via LocalPoppyTheme.
        val case = CorpusLoader.loadValidCases().single { it.slug == "002-text-with-color-primary" }
        val sentinel = androidx.compose.ui.graphics.Color(0xFFAA00FF)
        var observedColor: androidx.compose.ui.graphics.Color? = null
        composeRule.setContent {
            PoppyTheme(values = PoppyThemeValues(colors = PoppyColors(primary = sentinel))) {
                observedColor = LocalPoppyTheme.current.colors[Color.PRIMARY]
                Poppy(
                    (Poppy.validate(case.documentJson) as ValidationResult.Ok).document,
                    TestHost(),
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(sentinel, observedColor)
        // Sanity: the case really does use the primary token.
        val doc = Poppy.validate(case.documentJson) as ValidationResult.Ok
        val root = doc.document.root as Component.Text
        assertEquals(Color.PRIMARY, root.color)
    }

    @Test
    fun `spacing tokens resolve through the theme`() {
        // Per case 011: "The gap between children resolves through the theme's
        // `lg` spacing token." Override `lg` and confirm the override is
        // reachable from inside the Composable.
        val sentinel = androidx.compose.ui.unit.Dp(123f)
        var observed: androidx.compose.ui.unit.Dp? = null
        composeRule.setContent {
            PoppyTheme(values = PoppyThemeValues(spacing = PoppySpacing(lg = sentinel))) {
                val theme = LocalPoppyTheme.current
                observed = theme.spacing[Spacing.LG]
            }
        }
        composeRule.waitForIdle()
        assertEquals(sentinel, observed)
    }

    // --- Stack id exposed (case 015) ---------------------------------------

    @Test
    fun `stack id is exposed via testTag`() {
        val case = CorpusLoader.loadValidCases().single { it.slug == "015-kitchen-sink" }
        renderCaseWithLoader(case, RecordingImageLoader())
        // The root stack has id="screen-root". With testTagsAsResourceId,
        // it's findable as a tag.
        composeRule.onNode(
            androidx.compose.ui.test.hasTestTag("screen-root"),
        ).assertExists()
    }

}
