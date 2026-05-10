/*
 * PoppyTheme — token→value resolution for the Android renderer.
 *
 * Per docs/phase-2-plan.md §"Theming" and ADR-0010:
 *   - Spacing, Color, Size, Weight tokens live behind a CompositionLocal.
 *   - Defaults match the web client's CSS (see packages/client-web/src/styles/poppy.css).
 *   - Hosts override by wrapping with PoppyTheme(...) { Poppy(doc, host) }.
 *
 * Components must read tokens via LocalPoppyTheme.current.spacing(.MD), never
 * via raw .dp / Color literals.
 */

package dev.poppy.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Per-token spacing values (logical density-independent pixels). */
@Immutable
data class PoppySpacing(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
) {
    operator fun get(token: Spacing): Dp = when (token) {
        Spacing.NONE -> none
        Spacing.XS -> xs
        Spacing.SM -> sm
        Spacing.MD -> md
        Spacing.LG -> lg
        Spacing.XL -> xl
    }
}

/** Per-token text sizes, scaled per accessibility text size (sp). */
@Immutable
data class PoppySizes(
    val xs: TextUnit = 12.sp,
    val sm: TextUnit = 14.sp,
    val md: TextUnit = 16.sp,
    val lg: TextUnit = 20.sp,
    val xl: TextUnit = 28.sp,
) {
    operator fun get(token: Size): TextUnit = when (token) {
        Size.XS -> xs
        Size.SM -> sm
        Size.MD -> md
        Size.LG -> lg
        Size.XL -> xl
    }
}

/** Per-token font weights. Defaults mirror the web (regular=400, medium=500, bold=700). */
@Immutable
data class PoppyWeights(
    val regular: FontWeight = FontWeight.Normal, // 400
    val medium: FontWeight = FontWeight.Medium,  // 500
    val bold: FontWeight = FontWeight.Bold,      // 700
) {
    operator fun get(token: Weight): FontWeight = when (token) {
        Weight.REGULAR -> regular
        Weight.MEDIUM -> medium
        Weight.BOLD -> bold
    }
}

/**
 * Per-token color values. Defaults mirror the web client's CSS:
 *   default = #111, primary = #0B66FF, secondary = #6B7280,
 *   danger = #D4351C, success = #1E7C2C.
 *
 * See packages/client-web/src/styles/poppy.css for the canonical values.
 */
@Immutable
data class PoppyColors(
    val default: ComposeColor = ComposeColor(0xFF111111),
    val primary: ComposeColor = ComposeColor(0xFF0B66FF),
    val secondary: ComposeColor = ComposeColor(0xFF6B7280),
    val danger: ComposeColor = ComposeColor(0xFFD4351C),
    val success: ComposeColor = ComposeColor(0xFF1E7C2C),
) {
    operator fun get(token: Color): ComposeColor = when (token) {
        Color.DEFAULT -> default
        Color.PRIMARY -> primary
        Color.SECONDARY -> secondary
        Color.DANGER -> danger
        Color.SUCCESS -> success
    }
}

/**
 * The complete theme bundle. A host overrides any subset of tokens by passing
 * a custom instance to [PoppyTheme]. Use [LocalPoppyTheme] to read inside
 * components.
 */
@Immutable
data class PoppyThemeValues(
    val spacing: PoppySpacing = PoppySpacing(),
    val sizes: PoppySizes = PoppySizes(),
    val weights: PoppyWeights = PoppyWeights(),
    val colors: PoppyColors = PoppyColors(),
)

/**
 * CompositionLocal for the active theme. `staticCompositionLocalOf` because
 * theme changes invalidate the entire subtree (rare; the alternative would
 * be `compositionLocalOf` which is more invalidation-granular but heavier).
 */
val LocalPoppyTheme = staticCompositionLocalOf { PoppyThemeValues() }

/**
 * Wrap a UI subtree with a Poppy theme. Use this once at the host's tree root
 * (or above [Poppy] if you want different themes per Poppy island).
 */
@Composable
fun PoppyTheme(
    values: PoppyThemeValues = PoppyThemeValues(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPoppyTheme provides values,
        LocalPoppyImageLoader provides LocalPoppyImageLoader.current,
        content = content,
    )
}
