/*
 * Render a typed PoppyDocument to the stable plain-text format committed
 * alongside each valid corpus case as `snapshot.android.txt`.
 *
 * Mirrors the iOS renderer at packages/client-ios/Tests/PoppyTests/
 * SnapshotRenderer.swift verbatim — same indentation, same key order, same
 * quoting rules. Cross-platform reviewers can diff snapshot.android.txt
 * against snapshot.ios.txt directly to spot wire-format-level divergences.
 *
 * Why a custom dumper instead of Compose's printToString()?
 *
 *   1. Compose's semantics tree is built for accessibility, not visual
 *      styling. It captures Text content, ContentDescription, Role, and
 *      TestTag — but NOT colors, sizes, weights, spacing, padding, or
 *      alignment. A snapshot that doesn't include the styling tokens can't
 *      catch the most common kind of regression (a token applied wrong).
 *   2. Plain layout containers like Column/Row don't emit semantic nodes
 *      unless they carry semantics modifiers — so structural hierarchy is
 *      invisible too.
 *   3. Test-time image stubs leak into the snapshot (a placeholder Text
 *      node with the URL appears where an Image should be).
 *
 * Walking the typed document tree avoids all three. The snapshot reflects
 * what the schema actually says, which is what reviewers care about.
 *
 * Format example (for case 015-kitchen-sink):
 *
 *   PoppyDocument version="0.1"
 *     Stack id="screen-root" axis=vertical padding=lg spacing=md alignment=stretch
 *       Stack axis=horizontal spacing=sm alignment=center
 *         Image url="..." alt="User avatar" width=48 height=48 fit=cover
 *         Stack axis=vertical spacing=xs
 *           Text size=lg weight=bold value="Greg"
 *           Text color=secondary size=sm value="Signed in"
 *       Text color=primary value="You have 3 unread notifications."
 *       Stack axis=horizontal spacing=sm alignment=end
 *         Button label="Dismiss" action.type=navigate action.uri="poppy://notifications/dismiss-all"
 *         Button label="View all" action.type=navigate action.uri="poppy://notifications"
 */

package dev.poppy.android.snapshot

import dev.poppy.android.Action
import dev.poppy.android.Alignment
import dev.poppy.android.Axis
import dev.poppy.android.Color
import dev.poppy.android.Component
import dev.poppy.android.Fit
import dev.poppy.android.PoppyDocument
import dev.poppy.android.Size
import dev.poppy.android.Spacing
import dev.poppy.android.Weight

internal object SnapshotRenderer {

    fun render(document: PoppyDocument): String {
        val lines = mutableListOf<String>()
        lines += "PoppyDocument version=\"${document.version}\""
        renderComponent(document.root, depth = 1, into = lines)
        return lines.joinToString("\n") + "\n"
    }

    private fun renderComponent(component: Component, depth: Int, into: MutableList<String>) {
        when (component) {
            is Component.Stack -> {
                into += line(depth, "Stack", stackAttrs(component))
                for (child in component.children) renderComponent(child, depth + 1, into)
            }
            is Component.Text -> into += line(depth, "Text", textAttrs(component))
            is Component.Image -> into += line(depth, "Image", imageAttrs(component))
            is Component.Button -> into += line(depth, "Button", buttonAttrs(component))
        }
    }

    // --- Per-component attribute lists ---------------------------------------
    //
    // Order is fixed for stability and parity with iOS's SnapshotRenderer.swift.
    // Optional fields are emitted only when set.

    private fun stackAttrs(s: Component.Stack): List<Pair<String, String>> = buildList {
        s.id?.let { add("id" to quoted(it)) }
        add("axis" to s.axis.wire())
        s.padding?.let { add("padding" to it.wire()) }
        s.spacing?.let { add("spacing" to it.wire()) }
        s.alignment?.let { add("alignment" to it.wire()) }
    }

    private fun textAttrs(t: Component.Text): List<Pair<String, String>> = buildList {
        t.id?.let { add("id" to quoted(it)) }
        t.color?.let { add("color" to it.wire()) }
        t.size?.let { add("size" to it.wire()) }
        t.weight?.let { add("weight" to it.wire()) }
        add("value" to quoted(t.value))
    }

    private fun imageAttrs(i: Component.Image): List<Pair<String, String>> = buildList {
        i.id?.let { add("id" to quoted(it)) }
        add("url" to quoted(i.url))
        add("alt" to quoted(i.alt))
        i.width?.let { add("width" to it.toString()) }
        i.height?.let { add("height" to it.toString()) }
        i.fit?.let { add("fit" to it.wire()) }
    }

    private fun buttonAttrs(b: Component.Button): List<Pair<String, String>> = buildList {
        b.id?.let { add("id" to quoted(it)) }
        add("label" to quoted(b.label))
        when (val a = b.action) {
            is Action.Navigate -> {
                add("action.type" to "navigate")
                add("action.uri" to quoted(a.uri))
            }
        }
    }

    // --- Formatting helpers --------------------------------------------------

    private fun line(depth: Int, head: String, attrs: List<Pair<String, String>>): String {
        val indent = "  ".repeat(depth)
        val body = attrs.joinToString(" ") { (k, v) -> "$k=$v" }
        return if (body.isEmpty()) "$indent$head" else "$indent$head $body"
    }

    private fun quoted(s: String): String {
        val escaped = s.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }

    // --- Wire-format names for token enums -----------------------------------
    //
    // The schema uses lowercase strings; our enum constants are uppercase. The
    // mapping is consistently `name.lowercase()` for every v0.1 token, which
    // also matches what kotlinx.serialization emits via @SerialName. We use
    // `.lowercase()` rather than reflecting on @SerialName for simplicity —
    // a deviation here would be caught by the corpus snapshot diff against
    // iOS's output.

    private fun Axis.wire(): String = name.lowercase()
    private fun Spacing.wire(): String = name.lowercase()
    private fun Size.wire(): String = name.lowercase()
    private fun Color.wire(): String = name.lowercase()
    private fun Weight.wire(): String = name.lowercase()
    private fun Alignment.wire(): String = name.lowercase()
    private fun Fit.wire(): String = name.lowercase()
}
