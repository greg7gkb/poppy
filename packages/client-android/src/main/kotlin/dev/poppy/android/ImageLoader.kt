/*
 * PoppyImageLoader — image-loading abstraction for the Android renderer.
 *
 * Per docs/phase-2-plan.md §"Image loading abstraction": the renderer never
 * references Coil types directly; it goes through [PoppyImageLoader] +
 * [LocalPoppyImageLoader]. The default impl ([CoilImageLoader]) uses
 * coil-compose's AsyncImage; hosts can swap to Glide / Fresco / a stub for
 * tests by overriding the CompositionLocal.
 */

package dev.poppy.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/** Logical context an image is being loaded for. v0.1 only has one. */
enum class ImageContext { COMPONENT_IMAGE }

/**
 * Default URL allowlist: HTTPS only. Mirrors `@poppy/client-web`'s
 * `isUrlAllowedDefault` policy. Hosts override via [PoppyHost.isUrlAllowed]
 * (not the image loader — URL gating is a host concern, not a loader concern).
 */
fun defaultAllowImageUrl(url: String): Boolean = url.startsWith("https://")

/**
 * The image-loading SPI. Implementations render an image using whatever
 * loader they wrap. Implementations MUST forward [contentDescription] to the
 * platform's accessibility tree (Compose's `Modifier.semantics` does this
 * automatically when set on the underlying widget).
 */
interface PoppyImageLoader {
    @Composable
    fun Image(
        url: String,
        contentDescription: String?,
        modifier: Modifier,
        contentScale: ContentScale,
    )
}

/** Default implementation: Coil's AsyncImage. */
object CoilImageLoader : PoppyImageLoader {
    @Composable
    override fun Image(
        url: String,
        contentDescription: String?,
        modifier: Modifier,
        contentScale: ContentScale,
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

/**
 * CompositionLocal for the active image loader. Defaults to [CoilImageLoader].
 * Override per-tree:
 *
 *     CompositionLocalProvider(LocalPoppyImageLoader provides MyLoader) {
 *         Poppy(doc, host)
 *     }
 */
val LocalPoppyImageLoader = staticCompositionLocalOf<PoppyImageLoader> { CoilImageLoader }
