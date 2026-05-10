/*
 * Host integration contract for the Android renderer. Mirrors the web
 * `PoppyHost` interface in packages/client-web/src/render.ts.
 *
 * Hosts implement this and pass it to [Poppy]. The renderer dispatches actions
 * verbatim — it does NOT interpret URIs.
 */

package dev.poppy.android

interface PoppyHost {
    /** Called when a Button (or future actionable component) fires. */
    fun onAction(action: Action)

    /** Surface validation/runtime errors. Default: no-op (caller logs). */
    fun onError(throwable: Throwable) {}

    /**
     * Decide whether an image URL is allowed to load. Default: HTTPS-only.
     * Hosts that need to allow http://, file://, or in-app schemes override.
     */
    fun isUrlAllowed(url: String, context: ImageContext): Boolean =
        defaultAllowImageUrl(url)
}
