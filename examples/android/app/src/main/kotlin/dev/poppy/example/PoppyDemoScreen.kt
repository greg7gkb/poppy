/*
 * The Poppy integration demo.
 *
 * What this file shows, in order:
 *
 *   1. Poppy.validate(jsonString) — never throws; returns Ok|Failure.
 *   2. Implementing PoppyHost — the only required interaction surface.
 *      onAction is called when a Button (or future actionable component)
 *      fires. onError is called when validation rejects a doc or a URL is
 *      blocked. isUrlAllowed is the URL gate (default safe; override to
 *      restrict).
 *   3. PoppyTheme override — pass token maps to retheme the rendered tree.
 *   4. LocalPoppyImageLoader override — swap the default Coil-backed
 *      loader for a custom one (e.g. Glide, an in-memory stub for tests,
 *      or a host's existing image pipeline).
 *   5. Poppy(document, host) — the actual render call. Pure data in,
 *      Composable out.
 *
 * Run on a connected device or emulator:
 *
 *     ./gradlew :app:installDebug
 *
 * The screen renders the kitchen-sink corpus document; click the buttons
 * to see actions surface as Toasts.
 */

package dev.poppy.example

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.poppy.android.Action
import dev.poppy.android.ImageContext
import dev.poppy.android.LocalPoppyImageLoader
import dev.poppy.android.Poppy
import dev.poppy.android.PoppyColors
import dev.poppy.android.PoppyHost
import dev.poppy.android.PoppyTheme
import dev.poppy.android.PoppyThemeValues
import dev.poppy.android.ValidationResult

@Composable
fun PoppyDemoScreen() {
    // (1) Validate a Poppy document. In a real app the JSON would come from
    //     a network request; here we use the kitchen-sink corpus inline so
    //     the example is self-contained.
    val result = remember { Poppy.validate(SampleDocument.kitchenSinkJson) }

    when (result) {
        is ValidationResult.Failure -> {
            // The document failed schema validation. Surface for debugging;
            // production hosts likely log this and fall back to native UI.
            Text(
                text = "Failed to validate document:\n" +
                    result.errors.joinToString("\n") { "${it.path} (${it.keyword}): ${it.message}" },
                modifier = Modifier.padding(16.dp),
            )
        }
        is ValidationResult.Ok -> {
            DemoBody(documentResult = result)
        }
    }
}

@Composable
private fun DemoBody(documentResult: ValidationResult.Ok) {
    val ctx = LocalContext.current

    // (2) Implement PoppyHost. This is the only required interaction
    //     surface — it bridges the renderer back to the host app.
    val host = remember {
        object : PoppyHost {
            override fun onAction(action: Action) {
                when (action) {
                    is Action.Navigate ->
                        Toast.makeText(ctx, "navigate → ${action.uri}", Toast.LENGTH_SHORT)
                            .show()
                }
            }

            override fun onError(throwable: Throwable) {
                // Default impl is a no-op; demo surfaces it so misuses are
                // visible. Real apps would log to Crashlytics, Sentry, etc.
                Toast.makeText(ctx, "Poppy: ${throwable.message}", Toast.LENGTH_LONG).show()
            }

            // Default implementation rejects javascript:, vbscript:, file:,
            // and non-image data: URIs. Override to broaden or tighten.
            override fun isUrlAllowed(url: String, context: ImageContext): Boolean =
                super.isUrlAllowed(url, context)
        }
    }

    // (3) Apply a custom theme. Defaults match the web client's CSS; here
    //     we tweak the primary color so the demo visibly differs from the
    //     defaults.
    val customTheme = PoppyThemeValues(
        colors = PoppyColors(primary = Color(0xFFE91E63)),
    )

    // (4) (Demonstration only) — keep the default Coil image loader. To swap
    //     to a custom loader, wrap the Poppy() call in a
    //     CompositionLocalProvider:
    //
    //     CompositionLocalProvider(LocalPoppyImageLoader provides MyLoader) {
    //         Poppy(document, host)
    //     }
    //
    //     The default is `CoilImageLoader`. The full interface is in
    //     `dev.poppy.android.PoppyImageLoader`.
    val useCustomLoader by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE") val _example = useCustomLoader
    val loader = LocalPoppyImageLoader.current

    PoppyTheme(values = customTheme) {
        CompositionLocalProvider(LocalPoppyImageLoader provides loader) {
            // (5) The render call. document → typed tree → Composable.
            Poppy(
                document = documentResult.document,
                host = host,
                modifier = Modifier.fillMaxSize().padding(16.dp),
            )
        }
    }
}
