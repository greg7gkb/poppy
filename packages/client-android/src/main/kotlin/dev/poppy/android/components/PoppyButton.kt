/*
 * Button component renderer.
 *
 * Per case 014's behavioral invariant: clicking the button MUST call
 * host.onAction(action) exactly once with the verbatim action object. The
 * renderer does NOT interpret, parse, prefix, or transform the URI.
 */

package dev.poppy.android.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dev.poppy.android.Component
import dev.poppy.android.PoppyHost

@Composable
internal fun PoppyButton(
    button: Component.Button,
    host: PoppyHost,
    modifier: Modifier = Modifier,
) {
    var m: Modifier = modifier
    if (button.id != null) {
        m = m
            .semantics { testTagsAsResourceId = true }
            .testTag(button.id!!)
    }
    Button(
        onClick = { host.onAction(button.action) },
        modifier = m,
    ) {
        Text(button.label)
    }
}
