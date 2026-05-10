/*
 * Text component renderer.
 *
 * Token resolution: color/size/weight all go through LocalPoppyTheme — no
 * raw Color/sp/FontWeight literals here.
 */

package dev.poppy.android.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dev.poppy.android.Component
import dev.poppy.android.LocalPoppyTheme

@Composable
internal fun PoppyText(text: Component.Text, modifier: Modifier = Modifier) {
    val theme = LocalPoppyTheme.current
    var m: Modifier = modifier
    if (text.id != null) {
        m = m
            .semantics { testTagsAsResourceId = true }
            .testTag(text.id!!)
    }
    Text(
        text = text.value,
        modifier = m,
        color = theme.colors[text.color ?: dev.poppy.android.Color.DEFAULT],
        fontSize = theme.sizes[text.size ?: dev.poppy.android.Size.MD],
        fontWeight = theme.weights[text.weight ?: dev.poppy.android.Weight.REGULAR],
    )
}
