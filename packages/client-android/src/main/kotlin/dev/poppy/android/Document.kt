/*
 * Poppy v0.1 document tree as a `@Serializable sealed class` hierarchy.
 *
 * Mirrors `packages/schema/src/types.ts` and the JSON Schema files under
 * `packages/schema/schemas/`. Keep these in lockstep — the conformance corpus
 * catches drift.
 *
 * Per ADR-0009, this hierarchy *is* the validator: kotlinx.serialization's
 * polymorphic decoder rejects unknown discriminators and missing required
 * fields. See [Validate] for the entry point and error mapping.
 */

package dev.poppy.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The root of every Poppy v0.1 document. */
@Serializable
data class PoppyDocument(
    /** Wire-format version, MAJOR.MINOR. See ADR-0006. */
    val version: String,
    /** The single root component. */
    val root: Component,
    /**
     * Optional canonical schema URL. Editors may use this; renderers ignore
     * it entirely (the [version] field is authoritative).
     */
    @SerialName("\$schema") val schemaUrl: String? = null,
)

/** Discriminated union of all v0.1 component types. Discriminator: `"type"`. */
@Serializable
sealed class Component {
    abstract val id: String?

    @Serializable
    @SerialName("Stack")
    data class Stack(
        override val id: String? = null,
        val axis: Axis,
        val children: List<Component>,
        val spacing: Spacing? = null,
        val padding: Spacing? = null,
        val alignment: Alignment? = null,
    ) : Component()

    @Serializable
    @SerialName("Text")
    data class Text(
        override val id: String? = null,
        val value: String,
        val color: Color? = null,
        val size: Size? = null,
        val weight: Weight? = null,
    ) : Component()

    @Serializable
    @SerialName("Image")
    data class Image(
        override val id: String? = null,
        val url: String,
        val alt: String,
        val width: Int? = null,
        val height: Int? = null,
        val fit: Fit? = null,
    ) : Component()

    @Serializable
    @SerialName("Button")
    data class Button(
        override val id: String? = null,
        val label: String,
        val action: Action,
    ) : Component()
}

/**
 * Discriminated union of all v0.1 action types. Renderers dispatch actions to
 * the host's [PoppyHost.onAction] callback verbatim; they do NOT interpret
 * URIs, route, prefix, or otherwise transform actions.
 */
@Serializable
sealed class Action {
    @Serializable
    @SerialName("navigate")
    data class Navigate(val uri: String) : Action()
}
