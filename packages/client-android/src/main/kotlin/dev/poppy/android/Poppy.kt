/*
 * Top-level Poppy composable. Entry point for hosts.
 *
 * Usage:
 *
 *     val result = Poppy.validate(json)
 *     if (result is ValidationResult.Ok) {
 *         setContent {
 *             PoppyTheme { Poppy(result.document, host) }
 *         }
 *     }
 *
 * The renderer recomposes when the input PoppyDocument changes (Compose's
 * standard equality-based skipping). Action dispatch is verbatim — the
 * renderer never interprets URIs.
 */

package dev.poppy.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.poppy.android.components.PoppyButton
import dev.poppy.android.components.PoppyImage
import dev.poppy.android.components.PoppyStack
import dev.poppy.android.components.PoppyText

@Composable
fun Poppy(document: PoppyDocument, host: PoppyHost, modifier: Modifier = Modifier) {
    RenderComponent(document.root, host, modifier)
}

/**
 * Internal entry for child rendering. Component composables call back into
 * this for their children — see PoppyStack.
 */
@Composable
internal fun RenderComponent(
    component: Component,
    host: PoppyHost,
    modifier: Modifier = Modifier,
) {
    when (component) {
        is Component.Stack -> PoppyStack(component, host, modifier)
        is Component.Text -> PoppyText(component, modifier)
        is Component.Image -> PoppyImage(component, host, modifier)
        is Component.Button -> PoppyButton(component, host, modifier)
    }
}
