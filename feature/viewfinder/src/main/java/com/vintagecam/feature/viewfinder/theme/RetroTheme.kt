package com.vintagecam.feature.viewfinder.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessor object providing idiomatic access to vintage camera design tokens.
 */
object RetroTheme {
    val colors: RetroColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRetroColors.current

    val typography: RetroTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalRetroTypography.current
}

/**
 * Theme composable wrapping the vintage analog camera interface.
 * Enforces a strict dark titanium and leatherette camera body palette.
 */
@Composable
fun RetroTheme(
    colors: RetroColors = RetroColors(),
    typography: RetroTypography = RetroTypography(),
    content: @Composable () -> Unit
) {
    val darkScheme = darkColorScheme(
        primary = colors.knurledBrass,
        onPrimary = colors.leatheretteBlack,
        secondary = colors.chromeBezel,
        onSecondary = colors.leatheretteBlack,
        background = colors.leatheretteBlack,
        onBackground = colors.vintageWhite,
        surface = colors.titaniumDark,
        onSurface = colors.vintageWhite,
        error = colors.shutterRed,
        onError = colors.vintageWhite
    )

    CompositionLocalProvider(
        LocalRetroColors provides colors,
        LocalRetroTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = darkScheme,
            content = content
        )
    }
}

/**
 * Skeuomorphic modifier drawing an authentic mechanical knurled grip texture.
 * Used on manual exposure wheels, lens rings, and hardware dials.
 */
fun Modifier.knurledTexture(
    stripeWidth: Dp = 2.dp,
    highlightColor: Color = Color(0x33FFFFFF),
    shadowColor: Color = Color(0x66000000)
): Modifier = this.drawBehind {
    val stripeWidthPx = stripeWidth.toPx()
    val period = stripeWidthPx * 2
    var x = 0f
    while (x < size.width) {
        // Shadow groove
        drawRect(
            color = shadowColor,
            topLeft = Offset(x, 0f),
            size = Size(stripeWidthPx, size.height)
        )
        // Highlight ridge
        drawRect(
            color = highlightColor,
            topLeft = Offset(x + stripeWidthPx, 0f),
            size = Size(stripeWidthPx, size.height)
        )
        x += period
    }
}

/**
 * Skeuomorphic modifier drawing mechanical chamfered bevel borders with top-left highlight
 * and bottom-right shadow.
 */
fun Modifier.skeuomorphicBevel(
    borderWidth: Dp = 1.5.dp,
    highlightColor: Color = Color(0x4DFFFFFF),
    shadowColor: Color = Color(0x80000000)
): Modifier = this.drawBehind {
    val strokePx = borderWidth.toPx()
    val w = size.width
    val h = size.height

    // Top highlight
    drawLine(
        color = highlightColor,
        start = Offset(0f, strokePx / 2),
        end = Offset(w, strokePx / 2),
        strokeWidth = strokePx
    )
    // Left highlight
    drawLine(
        color = highlightColor,
        start = Offset(strokePx / 2, 0f),
        end = Offset(strokePx / 2, h),
        strokeWidth = strokePx
    )

    // Bottom shadow
    drawLine(
        color = shadowColor,
        start = Offset(0f, h - strokePx / 2),
        end = Offset(w, h - strokePx / 2),
        strokeWidth = strokePx
    )
    // Right shadow
    drawLine(
        color = shadowColor,
        start = Offset(w - strokePx / 2, 0f),
        end = Offset(w - strokePx / 2, h),
        strokeWidth = strokePx
    )
}
