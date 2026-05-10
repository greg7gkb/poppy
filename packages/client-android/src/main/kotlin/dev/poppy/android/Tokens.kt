/*
 * Token enums for the v0.1 wire format. Mirrors `packages/schema/src/types.ts`.
 *
 * Wire-format identifiers are lowercase strings; we map them via @SerialName
 * on each enum value. Renderers consume the enum, the JSON layer never escapes.
 *
 * The token *values* (e.g. md = 16.dp) live in [PoppyTheme]; this file only
 * declares the names.
 */

package dev.poppy.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Spacing {
    @SerialName("none") NONE,
    @SerialName("xs") XS,
    @SerialName("sm") SM,
    @SerialName("md") MD,
    @SerialName("lg") LG,
    @SerialName("xl") XL,
}

@Serializable
enum class Size {
    @SerialName("xs") XS,
    @SerialName("sm") SM,
    @SerialName("md") MD,
    @SerialName("lg") LG,
    @SerialName("xl") XL,
}

@Serializable
enum class Color {
    @SerialName("default") DEFAULT,
    @SerialName("primary") PRIMARY,
    @SerialName("secondary") SECONDARY,
    @SerialName("danger") DANGER,
    @SerialName("success") SUCCESS,
}

@Serializable
enum class Weight {
    @SerialName("regular") REGULAR,
    @SerialName("medium") MEDIUM,
    @SerialName("bold") BOLD,
}

@Serializable
enum class Alignment {
    @SerialName("start") START,
    @SerialName("center") CENTER,
    @SerialName("end") END,
    @SerialName("stretch") STRETCH,
}

@Serializable
enum class Fit {
    @SerialName("contain") CONTAIN,
    @SerialName("cover") COVER,
    @SerialName("fill") FILL,
}

@Serializable
enum class Axis {
    @SerialName("horizontal") HORIZONTAL,
    @SerialName("vertical") VERTICAL,
}
