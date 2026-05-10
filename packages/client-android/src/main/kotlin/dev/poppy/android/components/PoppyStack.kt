/*
 * Stack component renderer.
 *
 * Maps:
 *   axis=vertical   → Column
 *   axis=horizontal → Row
 *   spacing         → Arrangement.spacedBy(theme.spacing[token])
 *   padding         → Modifier.padding(theme.spacing[token])
 *   alignment       → cross-axis alignment (Alignment.Horizontal for Column,
 *                     Alignment.Vertical for Row)
 *
 * `alignment: stretch` is implemented by passing a fill modifier to each
 * child (Modifier.fillMaxWidth() inside a Column; fillMaxHeight() inside a
 * Row). This mirrors the SwiftUI renderer's per-child frame approach and the
 * web client's `align-items: stretch`. The container's outer alignment stays
 * Start/Top in this case — the per-child fill modifier is what produces the
 * stretch semantics.
 *
 * Per case 015's behavioral invariant: when `id` is set, expose it as a
 * Compose test tag so semantics-tree assertions can locate the stack.
 */

package dev.poppy.android.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dev.poppy.android.Alignment
import dev.poppy.android.Component
import dev.poppy.android.LocalPoppyTheme
import dev.poppy.android.PoppyHost
import dev.poppy.android.RenderComponent

@Composable
internal fun PoppyStack(
    stack: Component.Stack,
    host: PoppyHost,
    modifier: Modifier = Modifier,
) {
    val theme = LocalPoppyTheme.current
    var m: Modifier = modifier
    if (stack.id != null) {
        m = m
            .semantics { testTagsAsResourceId = true }
            .testTag(stack.id!!)
    }
    if (stack.padding != null) {
        m = m.padding(theme.spacing[stack.padding!!])
    }

    val mainArrangement = stack.spacing?.let { Arrangement.spacedBy(theme.spacing[it]) }
    val isStretch = stack.alignment == Alignment.STRETCH

    when (stack.axis) {
        dev.poppy.android.Axis.VERTICAL -> Column(
            modifier = m,
            verticalArrangement = mainArrangement ?: Arrangement.Top,
            horizontalAlignment = stack.alignment.toHorizontal(),
        ) {
            val childModifier = if (isStretch) Modifier.fillMaxWidth() else Modifier
            for (child in stack.children) RenderComponent(child, host, childModifier)
        }
        dev.poppy.android.Axis.HORIZONTAL -> Row(
            modifier = m,
            horizontalArrangement = mainArrangement ?: Arrangement.Start,
            verticalAlignment = stack.alignment.toVertical(),
        ) {
            val childModifier = if (isStretch) Modifier.fillMaxHeight() else Modifier
            for (child in stack.children) RenderComponent(child, host, childModifier)
        }
    }
}

/**
 * Cross-axis horizontal alignment for a Column. `stretch` falls back to
 * `Start` at the container level — the per-child `Modifier.fillMaxWidth()`
 * applied above produces the stretch semantics.
 */
private fun Alignment?.toHorizontal(): ComposeAlignment.Horizontal = when (this) {
    Alignment.START -> ComposeAlignment.Start
    Alignment.CENTER -> ComposeAlignment.CenterHorizontally
    Alignment.END -> ComposeAlignment.End
    Alignment.STRETCH -> ComposeAlignment.Start
    null -> ComposeAlignment.Start
}

private fun Alignment?.toVertical(): ComposeAlignment.Vertical = when (this) {
    Alignment.START -> ComposeAlignment.Top
    Alignment.CENTER -> ComposeAlignment.CenterVertically
    Alignment.END -> ComposeAlignment.Bottom
    Alignment.STRETCH -> ComposeAlignment.Top
    null -> ComposeAlignment.Top
}
