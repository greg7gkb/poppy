/*
 * Image component renderer.
 *
 * Per docs/phase-2-plan.md §"Image loading abstraction": this composable does
 * NOT reference Coil. It dispatches through LocalPoppyImageLoader so hosts can
 * swap loaders. The default loader is CoilImageLoader (see ImageLoader.kt).
 *
 * URL gating goes through host.isUrlAllowed (mirroring web's renderImage).
 * Rejected URLs surface via host.onError and the image is not loaded.
 */

package dev.poppy.android.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import dev.poppy.android.Component
import dev.poppy.android.Fit
import dev.poppy.android.ImageContext
import dev.poppy.android.LocalPoppyImageLoader
import dev.poppy.android.PoppyHost

@Composable
internal fun PoppyImage(
    image: Component.Image,
    host: PoppyHost,
    modifier: Modifier = Modifier,
) {
    var m: Modifier = modifier
    if (image.id != null) {
        m = m
            .semantics { testTagsAsResourceId = true }
            .testTag(image.id!!)
    }
    if (image.width != null && image.height != null) {
        m = m.size(width = image.width!!.dp, height = image.height!!.dp)
    } else if (image.width != null) {
        m = m.size(width = image.width!!.dp, height = image.width!!.dp)
    }

    val allowed = host.isUrlAllowed(image.url, ImageContext.COMPONENT_IMAGE)
    if (!allowed) {
        host.onError(IllegalArgumentException("Poppy: rejected image URL \"${image.url}\""))
        return
    }

    val contentScale = when (image.fit) {
        Fit.CONTAIN -> ContentScale.Fit
        Fit.COVER -> ContentScale.Crop
        Fit.FILL -> ContentScale.FillBounds
        null -> ContentScale.Fit
    }
    LocalPoppyImageLoader.current.Image(
        url = image.url,
        contentDescription = image.alt,
        modifier = m,
        contentScale = contentScale,
    )
}
